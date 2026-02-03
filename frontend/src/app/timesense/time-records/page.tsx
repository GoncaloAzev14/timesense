'use client';

import { Box, Button, Stack, Typography } from '@mui/material';
import { SetTimeRecordComponent } from './set-time-records';
import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import { useCallback, useRef, useState } from 'react';
import TimeRecordApi, { TimeRecord } from '../lib/time-records';
import {
    DataTable,
    useDataTableApiRef,
} from '@datacentric/datacentric-ui/components/datatable';
import { ServiceError } from '@/lib/service-base';
import { emptyPage } from '@datacentric/datacentric-ui/lib/spring-pagination';
import { useSession } from 'next-auth/react';
import UserApi, { User } from '../lib/users';
import { format, startOfDay, startOfWeek, subWeeks } from 'date-fns';
import useConfirmation from '@datacentric/datacentric-ui/components/confirmation';
import { useToast } from '@/components/toast-provider';
import { CloudDownload } from '@mui/icons-material';

export default function TimeRecordsPage() {
    const { data: session, update } = useSession();
    const { t } = useTranslation(undefined, 'timesense');
    const userTimeRecords = useDataTableApiRef<TimeRecord>();
    const [currentSort, setCurrentSort] = useState('-startDate');
    const userRef = useRef<User>();
    const [saveSignal, setSaveSignal] = useState(false);
    const modalOpen = useRef(false);
    const confirmation = useConfirmation();
    const { toast } = useToast();
    const SCOPE_USER = "SCOPE-USER";

    const fetchUserInfo = async () => {
        if (!session) {
            return;
        }
        const usersApi = new UserApi(session!);
        try {
            userRef.current = await usersApi.getUserInfo();
        } catch (e) {
            console.error('Failed to load users', e);
        }
    };

    const userTimeRecordsPageFetch = useCallback(
        async (page: number, rowsPerPage: number) => {
            fetchUserInfo();

            const currentWeekStart = startOfWeek(new Date(), {
                weekStartsOn: 1,
            });
            const startDate = subWeeks(currentWeekStart, 8);
            const startDateStr = format(
                startOfDay(startDate),
                'yyyy-MM-dd HH:mm:ss'
            );

            const api = new TimeRecordApi(session!);
            try {
                return api.getUserTimeRecords(
                    page,
                    rowsPerPage,
                    startDateStr,
                    currentSort
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
                    return emptyPage<TimeRecord>();
                } else {
                    console.error('Error fetching clients:', e);
                    return emptyPage<TimeRecord>();
                }
            }
        },
        [session, update, currentSort, saveSignal]
    );

    const onTimeRecordSave = () => {
        setSaveSignal(prev => !prev);
    };

    const handleExportTimeRecords = async () => {
        if (modalOpen.current) {
            return false;
        }
        modalOpen.current = true;
        try {
            if (
                !(await confirmation.ask(
                    t('Confirm Time Record export'),
                    t('User Time Record export question'),
                    t('Yes'),
                    t('No')
                ))
            ) {
                return false;
            }
            const api = new TimeRecordApi(session!);
            try {
                let exportDate = new Date().toLocaleDateString('pt-PT');
                let startDateStr = undefined;
                let endDateStr = undefined;
                await api.exportTimeRecords(
                    SCOPE_USER,
                    undefined,
                    undefined,
                    startDateStr,
                    endDateStr,
                    exportDate
                );
                toast('Time Records exported');
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
            <Box
                sx={{
                    height: '100vh',
                    display: 'flex',
                    flexDirection: 'column',
                    p: 3,
                }}>
                <Typography
                    variant="h4"
                    fontWeight="bold"
                    align="left"
                    gutterBottom>
                    {t('Project Time Records')}
                </Typography>
                <SetTimeRecordComponent onSave={onTimeRecordSave} />

                {/* Show User Time Records on active projects and within a time interval
                of 2 months(?) */}
                <Stack alignItems="row" direction="row" spacing={2} marginTop={3}>
                    <Typography variant="h6" marginTop={4} fontWeight={'bold'}>
                        {t('Time Records Summary')}
                    </Typography>
                    <Box sx={{ flexGrow: 1 }} />
                    <Button
                        size='small'
                        component="label"
                        variant="outlined"
                        startIcon={<CloudDownload />}
                        sx={{ marginBottom: 0 }}
                        onClick={handleExportTimeRecords}>
                        {t('Export Time Records')}
                    </Button>
                </Stack>
                <DataTable<TimeRecord>
                    columns={[
                        { field: 'project.name', title: t('Project Code') },
                        { field: 'project.description', title: t('Project Name') },
                        { field: 'task.name', title: t('Task') },
                        { field: 'hours', title: t('Hours Recorded') },
                        { field: 'description', title: t('Description') },
                        { field: 'status.name', title: t('Status') },
                        {
                            field: 'startDate',
                            title: t('Start'),
                            formatMask: 'yyyy-MM-dd HH:mm',
                        },
                        {
                            field: 'endDate',
                            title: t('End'),
                            formatMask: 'yyyy-MM-dd HH:mm',
                        },
                        {
                            field: 'approvedAt',
                            title: t('Aproved At'),
                            formatMask: 'yyyy-MM-dd',
                        },
                    ]}
                    currentSort={currentSort}
                    apiRef={userTimeRecords}
                    fetcher={userTimeRecordsPageFetch}
                    initialPage={0}
                    initialRowsPerPage={10}
                    onSortChange={setCurrentSort}
                />
            </Box>
            {confirmation.dialog}
        </>
    );
}
