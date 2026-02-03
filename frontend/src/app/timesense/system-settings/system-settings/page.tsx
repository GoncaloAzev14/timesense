'use client';

import React, { useEffect, useRef, useState } from 'react';
import { useSession } from 'next-auth/react';
import { Box, Button, Stack, Typography } from '@mui/material';
import SystemSettingsApi, { SystemSetting } from '../../lib/system-settings';
import {
    DataTable,
    useDataTableApiRef,
} from '@datacentric/datacentric-ui/components/datatable';
import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import {
    SystemSettingModal,
    useSystemSettingModal,
} from './system-settings-modal';
import { emptyPage } from '@datacentric/datacentric-ui/lib/spring-pagination';
import useConfirmation from '@datacentric/datacentric-ui/components/confirmation';
import { useToast } from '@/components/toast-provider';
import PermissionsApi from '../../lib/permissions';
import { ServiceError } from '@/lib/service-base';

export default function SystemSettingsPage() {
    const { data: session, update } = useSession();
    const { t } = useTranslation(undefined, 'timesense');
    const dataTableApiRef = useDataTableApiRef<SystemSetting>();

    const systemSettingModalApiRef = useSystemSettingModal();

    const [currentSort, setCurrentSort] = useState('name');
    const confirmation = useConfirmation();
    const { toast } = useToast();
    const modalOpen = useRef(false);
    const [canCreateSystemSetting, setCanCreateSystemSetting] = useState(false);

    /**
     * Open the dialog to create a new system setting
     */
    const handleCreateSystemSettings = async () => {
        if (!canCreateSystemSetting) {
            return;
        }
        if (modalOpen.current) {
            return;
        }

        modalOpen.current = true;
        try {
            const result =
                await systemSettingModalApiRef.current.openDialog('create');
            if (result) {
                toast(t('System Setting created'));
                dataTableApiRef.current?.refreshData();
            }
        } finally {
            modalOpen.current = false;
        }
    };

    const handleEditSystemSetting = async (e: SystemSetting) => {
        if (!canCreateSystemSetting) {
            return false;
        }
        if (modalOpen.current) {
            return false;
        }

        modalOpen.current = true;
        try {
            const api = new SystemSettingsApi(session!);
            const setting = await api.getSystemSetting(e.id!);
            const result = await systemSettingModalApiRef.current.openDialog(
                'edit',
                {
                    ...setting,
                }
            );
            if (result) {
                toast(t('System Setting updated'));
                return true;
            }
            return false;
        } finally {
            modalOpen.current = false;
        }
    };

    const handleCloseBusinessYear = async () => {
        if (!canCreateSystemSetting) {
            return false;
        }
        if (modalOpen.current) {
            return false;
        }

        modalOpen.current = true;
        try {
            if (
                !(await confirmation.ask(
                    t('Close Business Year'),
                    t('You are about to advance the business year. This action cannot be undone.'),
                    t('Continue'),
                    t('Cancel')
                ))
            ) {
                return false;
            }
            const api = new SystemSettingsApi(session!);
            try {
                await api.closeBusinessYear();
                toast(t('Business Year Closed'));
                dataTableApiRef.current?.refreshData();
                return true;
            } catch (e: any) {
                toast(e.message);
                return false;
            }
        } finally {
            modalOpen.current = false;
        }
    }

    useEffect(() => {
        (async () => {
            const api = new PermissionsApi(session!);
            setCanCreateSystemSetting(
                (await api.getSystemPermissions()).includes('MANAGE_SECURITY')
            );
        })();
    }, [session]);

    return (
        <>
            <Stack direction="column" spacing={2}>
                <Typography variant="h3">{t('System Settings')}</Typography>
                <Typography>{t('System Settings Page Text')}</Typography>
                <Stack direction="row" spacing={2}>
                    <Button
                        variant='outlined'
                        disabled={!canCreateSystemSetting}
                        onClick={handleCreateSystemSettings}>
                        {t('Add System Setting')}
                    </Button>
                    <Box sx={{ flexGrow: 1 }} />
                    <Button
                        variant='contained'
                        disabled={!canCreateSystemSetting}
                        onClick={handleCloseBusinessYear}>
                        {t('Close Business Year')}
                    </Button>
                </Stack>
                <DataTable<SystemSetting>
                    columns={[
                        { field: 'id', title: t('Id') },
                        {
                            field: 'name',
                            title: t('Name'),
                            filter: true,
                        },
                        {
                            field: 'value',
                            title: t('Value'),
                        },
                        {
                            field: 'updatedBy.name',
                            title: t('Updated By'),
                        },
                    ]}
                    currentSort={currentSort}
                    apiRef={dataTableApiRef}
                    rowActions={[
                        {
                            label: t('Edit'),
                            icon: 'edit',
                            action: handleEditSystemSetting,
                            isEnabled: r =>
                                canCreateSystemSetting && !!r.userEditable,
                        },
                    ]}
                    fetcher={async (
                        page: number,
                        rowsPerPage: number,
                        currentFilter?: string
                    ) => {
                        const api = new SystemSettingsApi(session!);
                        try {
                            return api.getSystemSettings(
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
                                    'Error fetching system settings:',
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
            <SystemSettingModal apiRef={systemSettingModalApiRef} />
            {confirmation.dialog}
        </>
    );
}
