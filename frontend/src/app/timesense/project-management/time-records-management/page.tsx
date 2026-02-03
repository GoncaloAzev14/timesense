'use client';

import { useSession } from 'next-auth/react';
import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import { useCallback, useEffect, useRef, useState } from 'react';
import TimeRecordApi, { TimeRecord } from '../../lib/time-records';
import { ServiceError } from '@/lib/service-base';
import { emptyPage } from '@datacentric/datacentric-ui/lib/spring-pagination';
import {
    Button,
    Stack,
    ToggleButton,
    ToggleButtonGroup,
    Typography,
} from '@mui/material';
import {
    DataTable,
    useDataTableApiRef,
} from '@datacentric/datacentric-ui/components/datatable';
import useConfirmation from '@datacentric/datacentric-ui/components/confirmation';
import { useToast } from '@/components/toast-provider';
import useDenyModal from '../../components/deny-modal';
import { format } from 'date-fns';

export default function TimeRecordsManagement() {
    const { data: session, update } = useSession();
    const { t } = useTranslation(undefined, 'timesense');
    const [currentSort, setCurrentSort] = useState<string>('-startDate');
    const dataTableApiRef = useDataTableApiRef<TimeRecord>();
    const confirmation = useConfirmation();
    const { toast } = useToast();
    const modalOpen = useRef(false);
    const denyModal = useDenyModal();

    const [filterMode, setFilterMode] = useState<
        'my_teams' | 'my_projects' | 'company'
    >('company');

    const TimeRecordsManagementPageFetch = useCallback(
        async (
            page: number,
            rowsPerPage: number,
            currentFilters: string | undefined
        ) => {
            const api = new TimeRecordApi(session!);

            try {
                if (currentFilters) {
                    currentFilters =
                        currentFilters + ',status.name=PENDING,hours!=0';
                } else {
                    currentFilters = 'status.name=PENDING,hours!=0';
                }
                return api.getTimeRecords(
                    page,
                    rowsPerPage,
                    currentSort,
                    currentFilters,
                    filterMode
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
                    return emptyPage<TimeRecord>();
                }
            }
        },
        [session, update, currentSort, filterMode]
    );

    const handleApproveTimeRecord = async (e: TimeRecord) => {
        if (modalOpen.current) {
            return false;
        }
        modalOpen.current = true;
        try {
            if (
                !(await confirmation.ask(
                    t('Confirm approve TimeRecord'),
                    t('Time record approve question'),
                    t('Yes'),
                    t('No')
                ))
            ) {
                return false;
            }
            const api = new TimeRecordApi(session!);
            try {
                await api.updateTimeRecord(e, 'approve');
                toast(t('Time Record Approved!'));
                return true;
            } catch (e: any) {
                toast(e.message);
                return false;
            }
        } finally {
            modalOpen.current = false;
        }
    };

    const handleDenyTimeRecord = async (e: TimeRecord) => {
        if (modalOpen.current) {
            return false;
        }
        modalOpen.current = true;
        try {
            var reason = await confirmation.askWithTextPrompt(
                t('Confirm deny TimeRecord'),
                t('Time record deny question'),
                t('Yes'),
                t('No')
            );

            if (reason === null) {
                return false;
            }
            e.reason = reason;

            const api = new TimeRecordApi(session!);
            try {
                await api.updateTimeRecord(e, 'deny');
                toast(t('Time Record Denied!'));
                return true;
            } catch (e: any) {
                toast(e.message);
                return false;
            }
        } finally {
            modalOpen.current = false;
        }
    };

    return (
        <>
            <Stack direction="column" spacing={2}>
                <Typography variant="h3">
                    {' '}
                    {t('Time Records Management')}
                </Typography>
                <Typography>
                    {t('Aprove or reject time records submitted by users.')}
                </Typography>
                <Stack direction="row" spacing={2}>
                    <Button
                        onClick={async () => {
                            if (modalOpen.current) {
                                return false;
                            }
                            modalOpen.current = true;
                            try {
                                if (
                                    !(await confirmation.ask(
                                        t('Confirm bulk approve'),
                                        t('Time record approve question'),
                                        t('Yes'),
                                        t('No')
                                    ))
                                ) {
                                    return;
                                }
                                const api = new TimeRecordApi(session!);
                                try {
                                    await api.approveTimeRecords(
                                        dataTableApiRef.current
                                            .getSelectedRows()
                                            .map(r => r.id!)
                                    );
                                    toast(t('Time Record Approved!'));
                                    dataTableApiRef.current.refreshData();
                                } catch (e: any) {
                                    toast(e.message);
                                }
                            } finally {
                                modalOpen.current = false;
                            }
                        }}>
                        {t('Approve selected')}
                    </Button>
                    <Button
                        onClick={async () => {
                            if (modalOpen.current) {
                                return false;
                            }
                            modalOpen.current = true;
                            try {
                                var reason =
                                    await confirmation.askWithTextPrompt(
                                        t('Confirm bulk deny'),
                                        t('Time record deny question'),
                                        t('Yes'),
                                        t('No')
                                    );
                                if (reason === null) {
                                    return;
                                }

                                const api = new TimeRecordApi(session!);
                                try {
                                    await api.denyTimeRecords(
                                        dataTableApiRef.current
                                            .getSelectedRows()
                                            .map(r => r.id!),
                                        reason
                                    );
                                    toast(t('Time Records Denied!'));
                                    dataTableApiRef.current.refreshData();
                                } catch (e: any) {
                                    toast(e.message);
                                }
                            } finally {
                                modalOpen.current = false;
                            }
                        }}>
                        {t('Deny selected')}
                    </Button>
                    <ToggleButtonGroup
                        value={filterMode}
                        exclusive
                        onChange={(e, newValue) => {
                            if (newValue !== null) {
                                setFilterMode(newValue);
                            }
                        }}
                        sx={theme => ({
                            'backgroundColor': theme.palette.background.paper,
                            'borderRadius': 3,
                            'p': 0.5,
                            'boxShadow': '0 1px 3px rgba(0,0,0,0.1)',
                            '& .MuiToggleButtonGroup-grouped': {
                                'border': 0,
                                'mx': 0.3,
                                'borderRadius': 2,
                                'textTransform': 'none',
                                'px': 3,
                                'py': 0.8,
                                'fontSize': '0.9rem',
                                'fontWeight': 500,
                                'transition': 'all 0.25s ease',
                                '&.Mui-selected': {
                                    'backgroundColor': theme.palette.info.main,
                                    'color': '#fff',
                                    'boxShadow': '0 2px 6px rgba(0,0,0,0.15)',
                                    '&:hover': {
                                        backgroundColor: 'info.dark',
                                    },
                                },
                            },
                        })}>
                        <ToggleButton value="my_teams">
                            {t("My Team's Projects")}
                        </ToggleButton>

                        <ToggleButton value="my_projects">
                            {t('My Projects')}
                        </ToggleButton>

                        <ToggleButton value="company">
                            {t('All Projects')}
                        </ToggleButton>
                    </ToggleButtonGroup>
                </Stack>
                <DataTable<TimeRecord>
                    columns={[
                        { field: 'user.name', title: t('User'), filter: true },
                        {
                            field: 'startDate',
                            title: t('Start Date'),
                            formatter: value => {
                                if (!value) return '';
                                return format(new Date(value), 'yyyy-MM-dd');
                            },
                        },
                        {
                            field: 'project.name',
                            title: t('Project'),
                            filter: true,
                        },
                        {
                            field: 'project.description',
                            title: t('Project Name'),
                        },
                        { field: 'task.name', title: t('Task') },
                        { field: 'hours', title: t('Hours Recorded') },
                        { field: 'description', title: t('Description') },
                        {
                            field: 'createdAt',
                            title: t('Submitted At'),
                            formatMask: 'yyyy-MM-dd',
                        },
                        {
                            field: 'updatedAt',
                            title: t('Updated At'),
                            formatMask: 'yyyy-MM-dd',
                        },
                    ]}
                    apiRef={dataTableApiRef}
                    rowActions={[
                        {
                            label: t('Approve'),
                            icon: 'check',
                            position: 'left',
                            action: handleApproveTimeRecord,
                        },
                        {
                            label: t('Deny'),
                            icon: 'cancel',
                            position: 'left',
                            action: handleDenyTimeRecord,
                        },
                    ]}
                    fetcher={TimeRecordsManagementPageFetch}
                    currentSort={currentSort}
                    onSortChange={setCurrentSort}
                    initialPage={0}
                    initialRowsPerPage={10}
                    withRowSelection={true}
                />
            </Stack>
            {confirmation.dialog}
            {denyModal.dialog}
        </>
    );
}
