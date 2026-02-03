'use client';

import React, { useEffect, useRef, useState } from 'react';
import { useSession } from 'next-auth/react';
import { Button, Stack, styled, Typography } from '@mui/material';
import HolidaysApi, { Holiday } from '../../lib/holidays';
import {
    DataTable,
    useDataTableApiRef,
} from '@datacentric/datacentric-ui/components/datatable';
import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import { HolidayModal, useHolidayModal } from './holidays-modal';
import { emptyPage } from '@datacentric/datacentric-ui/lib/spring-pagination';
import useConfirmation from '@datacentric/datacentric-ui/components/confirmation';
import { useToast } from '@/components/toast-provider';
import PermissionsApi from '../../lib/permissions';
import { ServiceError } from '@/lib/service-base';
import { CloudUpload } from '@mui/icons-material';
import HolidayApi from '../../lib/holidays';
import { ErrorMessageModal } from '../../components/error-message-modal';
import { FileUpload } from '@datacentric/datacentric-ui/components/file-upload';

export default function HolidaysPage() {
    const { data: session, update } = useSession();
    const { t } = useTranslation(undefined, 'timesense');
    const dataTableApiRef = useDataTableApiRef<Holiday>();
    const holidayModalApiRef = useHolidayModal();
    const [currentSort, setCurrentSort] = useState('holidayDate');
    const confirmation = useConfirmation();
    const { toast } = useToast();
    const modalOpen = useRef(false);
    const [canCreateHoliday, setCanCreateHoliday] = useState(false);

    const [errorModalOpen, setErrorModalOpen] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');
    const [errorDetails, setErrorDetails] = useState<string[]>([]);

    const openErrorModal = (error: { message: string; details: string[] }) => {
        setErrorMessage(error.message);
        setErrorDetails(error.details);
        setErrorModalOpen(true);
    };

    const closeErrorModal = () => {
        setErrorModalOpen(false);
    };

    /**
     * Open the dialog to create a new environment
     */
    const handleCreateHoliday = async () => {
        if (!canCreateHoliday) {
            return;
        }
        if (modalOpen.current) {
            return;
        }

        modalOpen.current = true;
        try {
            const result =
                await holidayModalApiRef.current.openDialog('create');
            if (result) {
                toast(t('Holiday created'));
                dataTableApiRef.current?.refreshData();
            }
        } finally {
            modalOpen.current = false;
        }
    };

    const handleEditHoliday = async (e: Holiday) => {
        if (!canCreateHoliday) {
            return false;
        }
        if (modalOpen.current) {
            return false;
        }

        modalOpen.current = true;
        try {
            const api = new HolidaysApi(session!);
            const holiday = await api.getHoliday(e.id!);
            const result = await holidayModalApiRef.current.openDialog('edit', {
                ...holiday,
            });
            if (result) {
                toast(t('Holiday updated'));
                return true;
            }
            return false;
        } finally {
            modalOpen.current = false;
        }
    };

    const handleDeleteHoliday = async (e: Holiday) => {
        if (!canCreateHoliday) {
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
                    t('Holiday deletion question', { name: e.name }),
                    t('Yes'),
                    t('No')
                ))
            ) {
                return false;
            }
            const api = new HolidaysApi(session!);
            try {
                await api.deleteHoliday(e.id!);
                toast(t('Holiday deleted'));
                return true;
            } catch (e: any) {
                toast(e.message);
                return false;
            }
        } finally {
            modalOpen.current = false;
        }
    };

    const handleCreateAbsenceConfiguration = async (
        event: React.ChangeEvent<HTMLInputElement>
    ) => {
        const file = event.target.files?.[0];
        if (!file) {
            console.log('no file');
            return;
        }

        const formData = new FormData();
        formData.append('file', file);

        try {
            const api = new HolidayApi(session!);
            const result = await api.importHolidays(formData);

            if (result.messageCode !== 'API_HOLIDAY_201_01') {
                openErrorModal({
                    message: result.message,
                    details: result.data?.message_args || [],
                });
            } else {
                toast(result.data!.message_args[0]);
                dataTableApiRef.current?.refreshData();
            }
        } catch (e: any) {
            toast('Error importing holidays configuration!');
            openErrorModal({
                message: e.message || 'Unknown error',
                details: [e.responseText || ''],
            });
        } finally {
            event.target.value = '';
        }
    };

    useEffect(() => {
        (async () => {
            const api = new PermissionsApi(session!);
            setCanCreateHoliday(
                (await api.getSystemPermissions()).includes('MANAGE_TIMEOFF')
            );
        })();
    }, [session]);

    return (
        <>
            <Stack direction="column" spacing={2}>
                <Typography variant="h3">{t('Holidays')}</Typography>
                <Typography>{t('Holidays Page Text')}</Typography>
                <Stack direction="row" spacing={2}>
                    <Button
                        variant="outlined"
                        disabled={!canCreateHoliday}
                        onClick={handleCreateHoliday}>
                        {t('Add Holiday')}
                    </Button>
                    <FileUpload
                        // managers will be able to approve absences but wont be able to set
                        // absences configurations
                        disabled={!canCreateHoliday}
                        label={t('Set Holidays Configuration')}
                        onUpload={handleCreateAbsenceConfiguration}
                    />
                </Stack>
                <DataTable<Holiday>
                    columns={[
                        { field: 'holidayDate', title: t('HolidayDate') },
                        {
                            field: 'name',
                            title: t('Name'),
                            filter: true,
                        },
                    ]}
                    currentSort={currentSort}
                    apiRef={dataTableApiRef}
                    rowActions={[
                        {
                            label: t('Edit'),
                            icon: 'edit',
                            action: handleEditHoliday,
                            isEnabled: () => canCreateHoliday,
                        },
                        {
                            label: t('Delete'),
                            icon: 'delete',
                            action: handleDeleteHoliday,
                            isEnabled: () => canCreateHoliday,
                        },
                    ]}
                    fetcher={async (
                        page: number,
                        rowsPerPage: number,
                        currentFilter?: string
                    ) => {
                        const api = new HolidaysApi(session!);
                        try {
                            return api.getHolidaysPage(
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
                                console.error('Error fetching holidays:', e);
                                return emptyPage();
                            }
                        }
                    }}
                    initialPage={0}
                    initialRowsPerPage={25}
                    onSortChange={setCurrentSort}
                />
            </Stack>
            <HolidayModal apiRef={holidayModalApiRef} />
            <ErrorMessageModal
                open={errorModalOpen}
                onClose={closeErrorModal}
                message={errorMessage}
                details={errorDetails}
                title={t('Error Importing Holidays Configurations')}
            />
            {confirmation.dialog}
        </>
    );
}
