'use client';

import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useSession } from 'next-auth/react';
import { Box, Button, Stack, Typography } from '@mui/material';
import {
    DataTable,
    useDataTableApiRef,
} from '@datacentric/datacentric-ui/components/datatable';
import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import { emptyPage } from '@datacentric/datacentric-ui/lib/spring-pagination';
import useConfirmation from '@datacentric/datacentric-ui/components/confirmation';
import { useToast } from '@/components/toast-provider';
import { ServiceError } from '@/lib/service-base';
import AbsenceApi, { Absence, AbsencesByDateMap } from '../lib/absences';
import { useRouter } from 'next/navigation';
import Loading from '@/components/load';
import UserApi, { User } from '../lib/users';
import { UnplannedVacsTable } from '../components/unplanned-vacs-table';
import { CalendarMatrix } from '../absences-management/global-absences/calendar-matrix';

export default function AbsencesPage() {
    const { data: session, update } = useSession();
    const { t } = useTranslation(undefined, 'timesense');
    const dataTableApiRef = useDataTableApiRef<Absence>();
    const modalOpen = useRef(false);
    const [currentSort, setCurrentSort] = useState<string>('-startDate');
    const confirmation = useConfirmation();
    const { toast } = useToast();
    const router = useRouter();
    const [isLoading, setIsLoading] = useState(false);
    const [userInfo, setUserInfo] = useState<User>();
    const [year, setYear] = useState(new Date().getFullYear());

    // force render
    const [, setUpdate] = useState(0);
    const forceRender = () => setUpdate(prev => prev + 1);

    const handleCreateAbsence = async () => {
        setIsLoading(true);
        router.push('/timesense/absences/timeoff-calendar/create/new');
    };

    const handleEditAbsence = async (e: Absence) => {
        setIsLoading(true);
        router.push(`/timesense/absences/timeoff-calendar/edit/${e.id}`);
        return true;
    };

    const handleDeleteAbsence = async (e: Absence) => {
        if (modalOpen.current) {
            return false;
        }
        modalOpen.current = true;
        try {
            if (
                !(await confirmation.ask(
                    t('Confirm deletion'),
                    t('Absence deletion question', { name: e.name }),
                    t('Yes'),
                    t('No')
                ))
            ) {
                return false;
            }
            const api = new AbsenceApi(session!);
            const usersApi = new UserApi(session!);
            try {
                await api.deleteAbsence(e.id!);
                toast(t('Absence deleted!'));

                dataTableApiRef.current?.refreshData();
                const user = await usersApi.getUserInfo();
                setUserInfo(user);

                return true;
            } catch (e: any) {
                toast(e.message);
                return false;
            }
        } finally {
            modalOpen.current = false;
        }
    };

    const absencePageFetch = useCallback(
        async (
            page: number,
            rowsPerPage: number,
            currentFilters: string | undefined
        ) => {
            const api = new AbsenceApi(session!);
            try {
                return api.getUserAbsences(
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
        [session, update, currentSort]
    );

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

    const [calendarData, setCalendarData] = useState<AbsencesByDateMap | null>(
        null
    );

    useEffect(() => {
        if (!session || !userInfo?.id) return;
        const absenceApi = new AbsenceApi(session!);
        const userFilter = [userInfo!.id];

        if (userFilter[0] != undefined) {
            try {
                const currentYear = year.toString();
                absenceApi
                    .getAbsencesByDate(currentYear, userFilter)
                    .then(data => {
                        setCalendarData(data.data);
                    });
                setTimeout(() => forceRender(), 100);
            } catch (e) {
                console.error('Failed to load absences by date map', e);
            }
        }
    }, [session, userInfo, year]);

    const canDeleteDoneAbsence = (absence: Absence): boolean => {
        if (absence?.status?.name !== "DONE") {
            return true;
        }
        return userInfo?.userRoles?.some(
            role => role.name === 'Manager' ||
            role.name === 'Admin') === true;
    };

    return (
        <>
            <Stack direction="column" spacing={2}>
                <Typography variant="h3"> {t('Absences')}</Typography>
                <Stack
                    direction="row"
                    justifyContent="space-between"
                    alignItems="flex-start"
                    sx={{ width: '100%' }}>
                    <Stack direction="column">
                        <Typography marginBottom={1}>
                            {t('Absences Page Text')}
                        </Typography>
                        <Box
                            sx={{
                                width: '100%',
                                display: 'flex',
                                justifyContent: 'space-between',
                                alignItems: 'flex-start',
                                mb: 2,
                            }}>
                            <Button
                                variant="outlined"
                                onClick={async () => {
                                    await handleCreateAbsence();
                                }}
                                disabled={isLoading}>
                                {isLoading ? (
                                    <Loading />
                                ) : (
                                    t('Create New Absence')
                                )}
                            </Button>
                        </Box>
                    </Stack>

                    {userInfo && (
                        <Box>
                            <UnplannedVacsTable users={[userInfo!]} />
                        </Box>
                    )}
                </Stack>

                <CalendarMatrix
                    year={year}
                    data={calendarData!}
                    onYearChange={setYear}
                />

                <Typography variant="h6" fontWeight="bold" marginTop={10}>
                    {t('Overall Absences Records')}
                </Typography>

                <DataTable<Absence>
                    columns={[
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
                        {
                            field: 'type.name',
                            title: t('Absence Type'),
                            filter: true,
                        },
                        {
                            field: 'subType.name',
                            title: t('Absence Sub Type'),
                            filter: true,
                        },
                        { field: 'name', title: t('Description') },
                        {
                            field: 'recordType',
                            title: t('Record Type'),
                        },
                        { field: 'businessYear', title: t('Business Year') },
                        { field: 'workDays', title: t('Work Days') },
                        {
                            field: 'absenceHours',
                            title: t('Absence Hours'),
                        },
                        {
                            field: 'status.name',
                            title: t('Status'),
                            filter: true,
                        },
                        { field: 'reason', title: t('Reason') },
                        { field: 'approvedBy.name', title: t('Approved By') },
                        { field: 'observations', title: t('Observations') },
                    ]}
                    apiRef={dataTableApiRef}
                    rowActions={[
                        {
                            label: t('Edit'),
                            icon: 'edit',
                            action: handleEditAbsence,
                            isEnabled: (row) => row.status!.name !== "DONE"
                        },
                        {
                            label: t('Delete'),
                            icon: 'delete',
                            action: handleDeleteAbsence,
                            isEnabled: canDeleteDoneAbsence
                        },
                    ]}
                    fetcher={absencePageFetch}
                    currentSort={currentSort}
                    onSortChange={setCurrentSort}
                    initialPage={0}
                    initialRowsPerPage={10}
                />
            </Stack>
            {confirmation.dialog}
        </>
    );
}
