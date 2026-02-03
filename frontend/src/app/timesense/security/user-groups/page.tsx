'use client';

import React, { useEffect, useRef, useState } from 'react';
import { useSession } from 'next-auth/react';
import { Button, Stack, Typography } from '@mui/material';
import UserGroupsApi, { UserGroup } from '../../lib/user-groups';
import {
    DataTable,
    useDataTableApiRef,
} from '@datacentric/datacentric-ui/components/datatable';
import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import { useUserGroupModal } from './user-group-modal';
import { emptyPage } from '@datacentric/datacentric-ui/lib/spring-pagination';
import useConfirmation from '@datacentric/datacentric-ui/components/confirmation';
import { useToast } from '@/components/toast-provider';
import PermissionsApi from '../../lib/permissions';
import { ServiceError } from '@/lib/service-base';

export default function UserGroupsPage() {
    const { data: session, update } = useSession();
    const { t } = useTranslation(undefined, 'timesense');
    const dataTableApiRef = useDataTableApiRef<UserGroup>();
    const userGroupModal = useUserGroupModal();
    const [currentSort, setCurrentSort] = useState('name');
    const confirmation = useConfirmation();
    const { toast } = useToast();
    const modalOpen = useRef(false);
    const [canCreateUserGroup, setCanCreateUserGroup] = useState(false);

    /**
     * Open the dialog to create a new environment
     */
    const handleCreateUserGroup = async () => {
        if (!canCreateUserGroup) {
            return;
        }
        if (modalOpen.current) {
            return;
        }
        modalOpen.current = true;
        try {
            const result = await userGroupModal.open('create');
            if (result) {
                toast(t('User Group created'));
                dataTableApiRef.current?.refreshData();
            }
        } finally {
            modalOpen.current = false;
        }
    };

    const handleEditUserGroup = async (e: UserGroup) => {
        if (!canCreateUserGroup) {
            return false;
        }
        if (modalOpen.current) {
            return false;
        }
        modalOpen.current = true;
        try {
            const api = new UserGroupsApi(session!);
            const userGroup = await api.getUserGroup(e.id!);
            const result = await userGroupModal.open('edit', userGroup);
            if (result) {
                toast(t('User Group updated'));
                return true;
            }
            return false;
        } finally {
            modalOpen.current = false;
        }
    };

    const handleDeleteUserGroup = async (e: UserGroup) => {
        if (!canCreateUserGroup) {
            return false;
        }
        if (modalOpen.current) {
            return false;
        }

        modalOpen.current = true;
        try {
            if (
                !(await confirmation.ask(
                    t('Confirm deletion'),
                    t('UserGroup deletion question', { name: e.name }),
                    t('Yes'),
                    t('No')
                ))
            ) {
                return false;
            }
            const api = new UserGroupsApi(session!);
            try {
                await api.deleteUserGroup(e.id!);
                toast(t('User Group deleted'));
                return true;
            } catch (e: any) {
                toast(e.message);
                return false;
            }
        } finally {
            modalOpen.current = false;
        }
    };

    useEffect(() => {
        (async () => {
            const api = new PermissionsApi(session!);
            setCanCreateUserGroup(
                (await api.getSystemPermissions()).includes('MANAGE_SECURITY')
            );
        })();
    }, [session]);

    return (
        <>
            <Stack direction="column" spacing={2}>
                <Typography variant="h3">{t('User Groups')}</Typography>
                <Stack direction="row" spacing={2}>
                    <Button
                        disabled={!canCreateUserGroup}
                        onClick={handleCreateUserGroup}>
                        {t('Add User Group')}
                    </Button>
                </Stack>
                <DataTable<UserGroup>
                    columns={[
                        { field: 'id', title: t('Id') },
                        {
                            field: 'name',
                            title: t('Name'),
                            filter: true,
                        },
                        { field: 'createdBy.name', title: t('Created By') },
                        { field: 'updatedBy.name', title: t('Updated By') },
                        {
                            field: 'updatedAt',
                            title: t('Last Modified'),
                            formatMask: 'timeago',
                        },
                    ]}
                    currentSort={currentSort}
                    apiRef={dataTableApiRef}
                    rowActions={[
                        {
                            label: t('Edit'),
                            icon: 'edit',
                            action: handleEditUserGroup,
                            isEnabled: () => canCreateUserGroup,
                        },
                        {
                            label: t('Delete'),
                            icon: 'delete',
                            action: handleDeleteUserGroup,
                            isEnabled: () => canCreateUserGroup,
                        },
                    ]}
                    fetcher={async (
                        page: number,
                        rowsPerPage: number,
                        currentFilter?: string
                    ) => {
                        const api = new UserGroupsApi(session!);
                        try {
                            return api.getUserGroups(
                                page,
                                rowsPerPage,
                                currentSort,
                                currentFilter
                            );
                        } catch (e: any) {
                            if (e instanceof ServiceError) {
                                const errorContent = await e.getContentAsJson();
                                if (errorContent?.status === 401) {
                                    update({
                                        ...session!,
                                        error: 'EndpointAccessTokenError',
                                    });
                                }
                                return emptyPage();
                            } else {
                                console.error(
                                    'Error fetching requested executions:',
                                    e
                                );
                                return emptyPage();
                            }
                        }
                    }}
                    initialPage={0}
                    initialRowsPerPage={10}
                    onSortChange={setCurrentSort}
                />
            </Stack>
            {userGroupModal.dialog}
            {confirmation.dialog}
        </>
    );
}
