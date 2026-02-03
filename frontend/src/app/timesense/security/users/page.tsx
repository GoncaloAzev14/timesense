'use client';

import React, { useEffect, useRef, useState } from 'react';
import { useSession } from 'next-auth/react';
import { Button, Stack, Typography } from '@mui/material';
import UsersApi, { User } from '../../lib/users';
import {
    DataTable,
    useDataTableApiRef,
} from '@datacentric/datacentric-ui/components/datatable';
import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import { useUserModal } from './users-modal';
import { emptyPage } from '@datacentric/datacentric-ui/lib/spring-pagination';
import useConfirmation from '@datacentric/datacentric-ui/components/confirmation';
import { useToast } from '@/components/toast-provider';
import PermissionsApi from '../../lib/permissions';
import { ServiceError } from '@/lib/service-base';

export default function UsersPage() {
    const { data: session, update } = useSession();
    const { t } = useTranslation(undefined, 'timesense');
    const dataTableApiRef = useDataTableApiRef<User>();
    const [currentSort, setCurrentSort] = useState('name');
    const confirmation = useConfirmation();
    const userModal = useUserModal();
    const { toast } = useToast();
    const modalOpen = useRef(false);
    const [canCreateUser, setCanCreateUser] = useState(false);

    /**
     * Open the dialog to create a new environment
     */
    const handleCreateUser = async () => {
        if (!canCreateUser) {
            return;
        }
        if (modalOpen.current) {
            return;
        }

        modalOpen.current = true;
        try {
            const result = await userModal.open('create');
            if (result) {
                toast(t('User created'));
                dataTableApiRef.current?.refreshData();
            }
        } finally {
            modalOpen.current = false;
        }
    };

    const handleEditUser = async (e: User) => {
        if (!canCreateUser) {
            return false;
        }
        if (modalOpen.current) {
            return false;
        }
        modalOpen.current = true;
        try {
            const user = await new UsersApi(session!).getUser(e.id!);
            const result = await userModal.open('edit', user);
            if (result) {
                toast(t('User updated'));
                return true;
            }
            return false;
        } finally {
            modalOpen.current = false;
        }
    };

    const handleDeleteUser = async (e: User) => {
        if (!canCreateUser) {
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
                    t('User deletion question', { name: e.name }),
                    t('Yes'),
                    t('No')
                ))
            ) {
                return false;
            }
            const api = new UsersApi(session!);
            try {
                await api.deleteUser(e.id!);
                toast(t('User deleted'));
                return true;
            } catch (e: any) {
                toast(e.message);
                return false;
            }
        } finally {
            modalOpen.current = false;
        }
    };

    const handleSynchronizeUsers = async () => {
        const api = new UsersApi(session!);
        try {
            await api.synchronizeUsers();
            toast(t('Users synchronized'));
            dataTableApiRef.current?.refreshData();
        } catch (e: any) {
            toast(e.message);
        }
    };

    useEffect(() => {
        (async () => {
            const api = new PermissionsApi(session!);
            setCanCreateUser(
                (await api.getSystemPermissions()).includes('MANAGE_SECURITY')
            );
        })();
    }, [session]);

    return (
        <>
            <Stack direction="column" spacing={2}>
                <Typography variant="h3">{t('Users')}</Typography>
                <Stack direction="row" spacing={2}>
                    <Button
                        disabled={!canCreateUser}
                        onClick={handleCreateUser}>
                        {t('Add User')}
                    </Button>
                    <Button
                        disabled={!canCreateUser}
                        onClick={handleSynchronizeUsers}>
                        {t('Synchronize Users')}
                    </Button>
                </Stack>
                <DataTable<User>
                    columns={[
                        { field: 'id', title: t('Id') },
                        {
                            field: 'name',
                            title: t('Name'),
                            filter: true,
                        },
                        {
                            field: 'email',
                            title: t('Email'),
                            filter: true,
                        },
                        {
                            field: 'jobTitle.name',
                            title: t('Job Title'),
                            filter: true,
                        },
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
                            action: handleEditUser,
                            isEnabled: () => canCreateUser,
                        },
                        {
                            label: t('Delete'),
                            icon: 'delete',
                            action: handleDeleteUser,
                            isEnabled: () => canCreateUser,
                        },
                    ]}
                    fetcher={async (
                        page: number,
                        rowsPerPage: number,
                        currentFilter?: string
                    ) => {
                        const api = new UsersApi(session!);
                        try {
                            return api.getUsers(
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
                            return emptyPage();
                        }
                    }}
                    initialPage={0}
                    initialRowsPerPage={10}
                    onSortChange={setCurrentSort}
                />
            </Stack>
            {userModal.dialog}
            {confirmation.dialog}
        </>
    );
}
