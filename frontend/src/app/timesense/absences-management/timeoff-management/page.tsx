'use client';

import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useSession } from 'next-auth/react';
import { Box, Button, Divider, Stack, styled, ToggleButton, ToggleButtonGroup, Typography } from '@mui/material';
import {
    DataTable,
    useDataTableApiRef,
} from '@datacentric/datacentric-ui/components/datatable';
import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import { emptyPage } from '@datacentric/datacentric-ui/lib/spring-pagination';
import useConfirmation from '@datacentric/datacentric-ui/components/confirmation';
import { useKeyboardShortcut } from '@datacentric/datacentric-ui/lib/browser-utils';
import { useToast } from '@/components/toast-provider';
import { ServiceError } from '@/lib/service-base';
import AbsenceApi, { Absence, AbsencesByDateMap } from '../../lib/absences';
import PermissionsApi from '../../lib/permissions';
import useDenyModal from '../../components/deny-modal';
import { CalendarMatrix } from '../global-absences/calendar-matrix';
import StatusApi, { Status } from '../../lib/status';
import format from '@datacentric/datacentric-ui/lib/formatter';
import { eachDayOfInterval } from 'date-fns';
import { CloudUpload } from '@mui/icons-material';
import UserApi, { User } from '../../lib/users';
import { ErrorMessageModal } from '../../components/error-message-modal';
import { FileUpload } from '@datacentric/datacentric-ui/components/file-upload';

/*
    Absences Management page where the admins if the system will be able to approve requested
    absences and set absences calendar configurations
*/
export default function AbsencesManagementPage() {
    const { data: session, update } = useSession();
    const { t } = useTranslation(undefined, 'timesense');
    const dataTableApiRef = useDataTableApiRef<Absence>();
    const [currentSort, setCurrentSort] = useState<string>('createdAt');
    const confirmation = useConfirmation();
    const { toast } = useToast();
    const modalOpen = useRef(false);
    const [canManageAbsences, setCanManageAbsences] = useState(false);
    const denyModal = useDenyModal();
    const [year, setYear] = useState(new Date().getFullYear());
    const [canImportAbsences, setCanImportAbsences] = useState(false);
    const [errorModalOpen, setErrorModalOpen] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');
    const [errorDetails, setErrorDetails] = useState<string[]>([]);
    const [userInfo, setUserInfo] = useState<User>();
    const [pageAction, setPageAction] = useState(true);
    const [calendarData, setCalendarData] = useState<AbsencesByDateMap | null>(
        null
    );

    const handlePageActionChange = (isApproval: boolean) => {
        setPageAction(isApproval);
    }

    const closeErrorModal = () => {
        setErrorModalOpen(false);
    };

    // force render
    const [, setUpdate] = useState(0);
    const forceRender = () => setUpdate(prev => prev + 1);

    useEffect(() => {
        (async () => {
            const api = new PermissionsApi(session!);
            setCanManageAbsences(
                (await api.getSystemPermissions()).includes('MANAGE_TIMEOFF')
            );
        })();
    }, [session]);

    const fetchUserInfo = async () => {
        if (!session) {
            return;
        }
        const usersApi = new UserApi(session!);
        try {
            const user = await usersApi.getUserInfo();
            setUserInfo(user);
            setTimeout(() => forceRender(), 100);
        } catch (e) {
            console.error('Failed to load users', e);
        }
    };

    useEffect(() => {
        fetchUserInfo();
        setTimeout(() => forceRender(), 100);
    }, [session!]);

    useEffect(() => {
        if (!session || !userInfo?.id) return;
        setCanImportAbsences(
            !!userInfo?.userRoles?.some(role => role.name === 'Admin')
        );
    }, [userInfo]);

    const handleApproveAbsence = async (e: Absence) => {
        if (modalOpen.current) {
            return false;
        }
        modalOpen.current = true;
        try {
            if (
                !(await confirmation.ask(
                    t('Confirm approve'),
                    t('Absence approve question', { name: e.name }),
                    t('Yes'),
                    t('No')
                ))
            ) {
                return false;
            }
            const api = new AbsenceApi(session!);
            try {
                await api.updateAbsence(e, 'approve');
                toast(t('Absence Approved!'));
                dataTableApiRef.current.refreshData();
                return true;
            } catch (e: any) {
                toast(e.message);
                return false;
            }
        } finally {
            setSelectedRows(s => !s);
            modalOpen.current = false;
        }
    };

    const handleDraftAbsence = async (e: Absence) => {
        if (modalOpen.current) {
            return false;
        }
        modalOpen.current = true;
        try {
            if (
                !(await confirmation.ask(
                    t('Confirm draft absences'),
                    t('Absence draft question', { name: e.name }),
                    t('Yes'),
                    t('No')
                ))
            ) {
                return false;
            }
            const api = new AbsenceApi(session!);
            try {
                const idList = [e.id!];
                await api.draftAbsences(idList);
                toast(t('Absences Drafted!'));
                dataTableApiRef.current.refreshData();
                return true;
            } catch (e: any) {
                toast(e.message);
                return false;
            }
        } finally {
            setSelectedRows(s => !s);
            modalOpen.current = false;
        }
    };

    const handleDenyAbsence = async (e: Absence) => {
        if (modalOpen.current) {
            return false;
        }
        modalOpen.current = true;
        try {
            const observation = await denyModal.ask(
                t('Deny Absence'),
                t('Please enter a comment on why you denied this absence'),
                t('Deny'),
                t('Cancel')
            );
            if (observation !== null) {
                const api = new AbsenceApi(session!);
                try {
                    const abs = await api.getAbsence(e.id!);
                    abs.observations = observation;
                    const result = await api.updateAbsence(abs, 'deny');
                    if (result) {
                        toast('Absence Denied!');
                        dataTableApiRef.current?.refreshData();
                        return true;
                    }
                } catch (e: any) {
                    toast(e.messages);
                    return false;
                }
            } else {
                return false;
            }
        } finally {
            setSelectedRows(s => !s);
            modalOpen.current = false;
            return false;
        }
    };

    const absenceManagementPageFetch = useCallback(
        async (
            page: number,
            rowsPerPage: number,
            currentFilters: string | undefined
        ) => {
            const api = new AbsenceApi(session!);
            try {
                let staticFilter = pageAction ? 'PENDING' : 'DONE';
                if (currentFilters) {
                    currentFilters = currentFilters + `,status.name=${staticFilter}`;
                } else {
                    currentFilters = `status.name=${staticFilter}`;
                }
                return api.getAllAbsences(
                    page,
                    rowsPerPage,
                    currentSort,
                    currentFilters
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
                    return emptyPage<Absence>();
                } else {
                    return emptyPage<Absence>();
                }
            }
        },
        [session, update, currentSort, pageAction]
    );


    const [actionStatus, setActionStatus] = useState<Status>();
    const fetchPendingStatus = async (statusName: string) => {
        if (!session) return;
        const statusApi = new StatusApi(session);
        try {
            const data = await statusApi.getAllStatus(
                0,
                1000,
                'name',
                `name=${statusName}`
            );
            setActionStatus(data.content[0]);
            setTimeout(() => forceRender(), 100);
        } catch (e) {
            console.error('Failed to load pending status', e);
        }
    };
    useEffect(() => {
        let status = pageAction ? 'PENDING' : 'DONE';
        fetchPendingStatus(status);
    }, [pageAction, session]);
    useEffect(() => {
        const absenceApi = new AbsenceApi(session!);
        if (!actionStatus) return;
        try {
            const currentYear = year.toString();
            absenceApi
                .getAbsencesByDate(currentYear, [], [actionStatus?.id!])
                .then(data => {
                    setCalendarData(data.data);
                });
        } catch (e) {
            console.error('Failed to load absences by date map', e);
        }
    }, [session, year, pageAction, actionStatus]);

    const [selectedRows, setSelectedRows] = useState(false);
    const handleRowSelection = () => {
        setSelectedRows(s => !s);
        setTimeout(() => forceRender(), 100);
    };

    useEffect(() => {
        const updatedMap: AbsencesByDateMap = {
            ...calendarData,
        };
        const selectedRows = dataTableApiRef.current.getSelectedRows();

        // First, clear all selections
        Object.keys(updatedMap).forEach(dateKey => {
            updatedMap[dateKey].isSelected = false;
        });

        selectedRows.forEach(absence => {
            const start = absence.startDate;
            const end = absence.endDate;

            const daysInRange = eachDayOfInterval({ start, end });

            daysInRange.forEach(date => {
                const dateKey = formatDate(date);
                if (updatedMap[dateKey]) {
                    updatedMap[dateKey].isSelected = true;
                }
            });
        });

        setCalendarData(updatedMap);
    }, [selectedRows]);

    const openErrorModal = (error: { message: string; details: string[] }) => {
        setErrorMessage(error.message);
        setErrorDetails(error.details);
        setErrorModalOpen(true);
    };

    const handleImportAbsences = async (
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
            const api = new AbsenceApi(session!);
            const result = await api.importAbsences(formData);

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

    const handleExportAbsenceAttachments = async (absenceId: number) => {
        if (modalOpen.current) {
            return false;
        }
        modalOpen.current = true;
        try {
            if (
                !(await confirmation.ask(
                    t('Confirm Absence Attachments Export'),
                    t('Absence Attchments export question'),
                    t('Yes'),
                    t('No')
                ))
            ) {
                return false;
            }
            const api = new AbsenceApi(session!);
            try {
                let exportDate = new Date().toLocaleDateString('pt-PT');
                let startDateStr = undefined;
                let endDateStr = undefined;
                await api.exportAbsenceAttachments(absenceId);
                toast('Absence Attachments exported');
                return true;
            } catch (e: any) {
                toast(e.messages);
                return false;
            }
        } finally {
            modalOpen.current = false;
        }
    };

    return (
        <>
            <Stack direction="column" spacing={2}>
                <Typography variant="h3"> {t('Timeoff Management')}</Typography>
                <Typography>{t('Absences Management Page Text')}</Typography>
                <Stack direction="row" spacing={2}>
                    <ToggleButtonGroup
                        value={pageAction ? "approval" : "manage"}
                        exclusive
                        onChange={(e, newValue) => {
                            if (newValue !== null) {
                                handlePageActionChange(newValue === "approval");
                            }
                        }}
                        sx={(theme) => ({
                            backgroundColor: theme.palette.background.paper,
                            borderRadius: 3,
                            p: 0.5,
                            boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
                            '& .MuiToggleButtonGroup-grouped': {
                                border: 0,
                                mx: 0.3,
                                borderRadius: 2,
                                textTransform: 'none',
                                px: 3,
                                py: 0.8,
                                fontSize: '0.9rem',
                                fontWeight: 500,
                                transition: 'all 0.25s ease',
                                '&.Mui-selected': {
                                    backgroundColor: theme.palette.info.main,
                                    color: '#fff',
                                    boxShadow: '0 2px 6px rgba(0,0,0,0.15)',
                                    '&:hover': {
                                        backgroundColor: 'info.dark',
                                    }
                                }
                            }
                        })}
                    >
                        <ToggleButton value="approval">
                            {t("Approve Absences")}
                        </ToggleButton>

                        <ToggleButton value="manage">
                            {t("Manage Absences")}
                        </ToggleButton>
                    </ToggleButtonGroup>
                    {!pageAction ? undefined : (
                        <Button
                            hidden={!pageAction}
                            onClick={async () => {
                                if (modalOpen.current) {
                                    return false;
                                }
                                modalOpen.current = true;
                                try {
                                    if (
                                        !(await confirmation.ask(
                                            t('Confirm bulk approve'),
                                            t('Absence approve question'),
                                            t('Yes'),
                                            t('No')
                                        ))
                                    ) {
                                        return;
                                    }
                                    const api = new AbsenceApi(session!);
                                    try {
                                        await api.approveAbsences(
                                            dataTableApiRef.current
                                                .getSelectedRows()
                                                .map(r => r.id!)
                                        );
                                        toast(t('Absences Approved!'));
                                        dataTableApiRef.current.refreshData();
                                    } catch (e: any) {
                                        toast(e.message);
                                    }
                                } finally {
                                    modalOpen.current = false;
                                }
                            }}>
                            {t('Approve selected')}
                        </Button>)}
                    {!pageAction ? undefined : (
                        <Button
                            hidden={!pageAction}
                            onClick={async () => {
                                if (modalOpen.current) {
                                    return false;
                                }
                                modalOpen.current = true;
                                try {
                                    var reason =
                                        await confirmation.askWithTextPrompt(
                                            t('Confirm bulk deny'),
                                            t('Absence deny question'),
                                            t('Yes'),
                                            t('No')
                                        );
                                    if (reason === null) {
                                        return;
                                    }
                                    const api = new AbsenceApi(session!);
                                    try {
                                        await api.denyAbsences(
                                            dataTableApiRef.current
                                                .getSelectedRows()
                                                .map(r => r.id!),
                                            reason
                                        );
                                        toast(t('Absences Denied!'));
                                        dataTableApiRef.current.refreshData();
                                    } catch (e: any) {
                                        toast(e.message);
                                    }
                                } finally {
                                    modalOpen.current = false;
                                }
                            }}>
                            {t('Deny selected')}
                        </Button>)}
                    {pageAction ? undefined : (
                        <Button
                            onClick={async () => {
                                if (modalOpen.current) {
                                    return false;
                                }
                                modalOpen.current = true;
                                try {
                                    if (
                                        !(await confirmation.ask(
                                            t('Confirm bulk draft absences'),
                                            t('Absence draft question'),
                                            t('Yes'),
                                            t('No')
                                        ))
                                    ) {
                                        return;
                                    }
                                    const api = new AbsenceApi(session!);
                                    try {
                                        await api.draftAbsences(
                                            dataTableApiRef.current
                                                .getSelectedRows()
                                                .map(r => r.id!)
                                        );
                                        toast(t('Absences Drafted!'));
                                        dataTableApiRef.current.refreshData();
                                    } catch (e: any) {
                                        toast(e.message);
                                    }
                                } finally {
                                    modalOpen.current = false;
                                }
                            }}>
                            {t('Draft selected')}
                        </Button>)}

                    <FileUpload
                        // managers will be able to approve absences but wont be able to set
                        // absences configurations
                        disabled={!canImportAbsences}
                        label={t('Import Absences')}
                        onUpload={handleImportAbsences}
                    />
                </Stack>
                <DataTable<Absence>
                    columns={[
                        { field: 'name', title: t('Description') },
                        { field: 'user.name', title: t('User'), filter: true },
                        {
                            field: 'type.name',
                            title: t('Absence Type'),
                            filter: true,
                        },
                        {
                            field: 'startDate',
                            title: t('Start Date'),
                            formatMask: 'yyyy-MM-dd',
                        },
                        {
                            field: 'endDate',
                            title: t('End Date'),
                            formatMask: 'yyyy-MM-dd',
                        },
                        { field: 'reason', title: t('Reason') },
                        { field: 'workDays', title: t('Business Days') },
                        { field: 'businessYear', title: t('Business Year') },
                        {
                            field: 'createdAt',
                            title: t('Submitted At'),
                            formatMask: 'yyyy-MM-dd',
                        },
                    ]}
                    apiRef={dataTableApiRef}
                    rowActions={[
                        {
                            label: t('Approve'),
                            icon: 'check',
                            action: handleApproveAbsence,
                            isEnabled: () => canManageAbsences && pageAction,
                            hideWhenDisabled: true,
                            position: 'left',
                        },
                        {
                            label: t('Deny'),
                            icon: 'cancel',
                            action: handleDenyAbsence,
                            isEnabled: () => canManageAbsences && pageAction,
                            hideWhenDisabled: true,
                            position: 'left',
                        },
                        {
                            label: t('Draft'),
                            icon: 'edit_document',
                            action: handleDraftAbsence,
                            isEnabled: () => canManageAbsences && !pageAction,
                            hideWhenDisabled: true,
                            position: 'left',
                        },
                        {
                            label: t('Download Attachments'),
                            icon: 'download',
                            action: (row) => handleExportAbsenceAttachments(row.id!),
                            isEnabled: (row) => canManageAbsences && row.hasAttachments === true,
                            hideWhenDisabled: true,
                            position: 'left',
                        },
                    ]}
                    fetcher={absenceManagementPageFetch}
                    currentSort={currentSort}
                    onSortChange={setCurrentSort}
                    initialPage={0}
                    initialRowsPerPage={10}
                    withRowSelection={true}
                    onRowSelectionChange={handleRowSelection}
                />
                <Divider />
                <CalendarMatrix
                    year={year}
                    data={calendarData!}
                    onYearChange={setYear}
                />
            </Stack>
            <ErrorMessageModal
                open={errorModalOpen}
                onClose={closeErrorModal}
                message={errorMessage}
                details={errorDetails}
                title={t('Error Importing Absences')}
            />
            {confirmation.dialog}
            {denyModal.dialog}
        </>
    );
}

function formatDate(date: Date): string {
    return format(date, 'yyyy-MM-dd');
}
