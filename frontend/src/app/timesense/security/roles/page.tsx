'use client';

import React, { useEffect, useRef, useState } from 'react';
import { useSession } from 'next-auth/react';
import { Button, Stack, Typography } from '@mui/material';
import RolesApi, { Role } from '../../lib/roles';
import {
    DataTable,
    useDataTableApiRef,
} from '@datacentric/datacentric-ui/components/datatable';
import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import { RoleModal, useRoleModal } from './role-modal';
import { emptyPage } from '@datacentric/datacentric-ui/lib/spring-pagination';
import useConfirmation from '@datacentric/datacentric-ui/components/confirmation';
import { useToast } from '@/components/toast-provider';
import PermissionsApi from '../../lib/permissions';
import { ServiceError } from '@/lib/service-base';

export default function RolesPage() {
    const { data: session, update } = useSession();
    const { t } = useTranslation(undefined, 'timesense');
    const dataTableApiRef = useDataTableApiRef<Role>();
    const roleModalApiRef = useRoleModal();
    const [currentSort, setCurrentSort] = useState('name');
    const confirmation = useConfirmation();
    const { toast } = useToast();
    const modalOpen = useRef(false);
    const [canCreateUserRole, setCanCreateUserRole] = useState(false);

    /**
     * Open the dialog to create a new environment
     */
    const handleCreateRole = async () => {
        if (!canCreateUserRole) {
            return;
        }
        if (modalOpen.current) {
            return;
        }

        modalOpen.current = true;
        try {
            const result = await roleModalApiRef.current.openDialog('create');
            if (result) {
                toast(t('Role created'));
                dataTableApiRef.current?.refreshData();
            }
        } finally {
            modalOpen.current = false;
        }
    };

    const handleEditRole = async (e: Role) => {
        if (!canCreateUserRole) {
            return false;
        }
        if (modalOpen.current) {
            return false;
        }

        modalOpen.current = true;
        try {
            const api = new RolesApi(session!);
            const role = await api.getRole(e.id!);
            const result = await roleModalApiRef.current.openDialog('edit', {
                ...role,
            });
            if (result) {
                toast(t('Role updated'));
                return true;
            }
            return false;
        } finally {
            modalOpen.current = false;
        }
    };

    const handleDeleteRole = async (e: Role) => {
        if (!canCreateUserRole) {
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
                    t('Role deletion question', { name: e.name }),
                    t('Yes'),
                    t('No')
                ))
            ) {
                return false;
            }
            const api = new RolesApi(session!);
            try {
                await api.deleteRole(e.id!);
                toast(t('Role deleted'));
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
            setCanCreateUserRole(
                (await api.getSystemPermissions()).includes('MANAGE_SECURITY')
            );
        })();
    }, [session]);

    return (
        <>
            <Stack direction="column" spacing={2}>
                <Typography variant="h3">{t('Roles')}</Typography>
                <Stack direction="row" spacing={2}>
                    <Button
                        disabled={!canCreateUserRole}
                        onClick={handleCreateRole}>
                        {t('Add Role')}
                    </Button>
                </Stack>
                <DataTable<Role>
                    columns={[
                        { field: 'id', title: t('Id') },
                        {
                            field: 'name',
                            title: t('Name'),
                            filter: true,
                        },
                        {
                            field: 'parentRole.name',
                            title: t('Parent Role'),
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
                            action: handleEditRole,
                            isEnabled: () => canCreateUserRole,
                        },
                        {
                            label: t('Delete'),
                            icon: 'delete',
                            action: handleDeleteRole,
                            isEnabled: () => canCreateUserRole,
                        },
                    ]}
                    fetcher={async (
                        page: number,
                        rowsPerPage: number,
                        currentFilter?: string
                    ) => {
                        const api = new RolesApi(session!);
                        try {
                            return api.getRoles(
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
            <RoleModal apiRef={roleModalApiRef} />
            {confirmation.dialog}
        </>
    );
}
