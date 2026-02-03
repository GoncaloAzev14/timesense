'use client';

import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useSession } from 'next-auth/react';
import { Button, Stack, Typography } from '@mui/material';
import {
    DataTable,
    useDataTableApiRef,
} from '@datacentric/datacentric-ui/components/datatable';
import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import { emptyPage } from '@datacentric/datacentric-ui/lib/spring-pagination';
import useConfirmation from '@datacentric/datacentric-ui/components/confirmation';
import { useToast } from '@/components/toast-provider';
import { ServiceError } from '@/lib/service-base';
import TimeRecordApi, {
    FilteredTimeRecord,
    TimeRecord,
} from '../../lib/time-records';
import Combobox, {
    Entry,
} from '@datacentric/datacentric-ui/components/combobox';
import UserApi, { User } from '../../lib/users';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider/LocalizationProvider';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import dayjs from 'dayjs';
import { CloudDownload } from '@mui/icons-material';
import { useDebounced } from '@datacentric/datacentric-ui/lib/browser-utils';
import { endOfDay, format, startOfDay } from 'date-fns';
import ProjectApi, { Project } from '../../lib/projects';

export default function TimeRecordsPage() {
    const USERS_PAGE_SIZE = 100;
    const DEBOUNCE_INTERVAL_MS = 200;

    const { data: session } = useSession();
    const { t } = useTranslation(undefined, 'timesense');
    const dataTableApiRef = useDataTableApiRef<FilteredTimeRecord>();
    const [currentSort, setCurrentSort] = useState<string>('-startDate');
    const confirmation = useConfirmation();
    const { toast } = useToast();
    const modalOpen = useRef(false);
    const [usersList, setUsersList] = useState<Entry[]>([]);
    const [loading, setLoading] = useState(false);
    const [selectedUsers, setSelectedUsers] = useState<number[]>([]);
    const [users, setUsers] = useState<User[] | null>(null);
    const [startDateDay, setStartDateDay] = useState<Date>();
    const [endDateDay, setEndDateDay] = useState<Date>();

    const [projectsList, setProjectsList] = useState<Entry[]>([]);
    const [selectedProjects, setSelectedProjects] = useState<number[]>([]);
    const [projects, setProjects] = useState<Project[] | null>(null);

    // force render
    const [, setUpdate] = useState(0);
    const forceRender = () => setUpdate(prev => prev + 1);

    const debouncedUsers = useDebounced(selectedUsers, DEBOUNCE_INTERVAL_MS);
    const debouncedProjects = useDebounced(
        selectedProjects,
        DEBOUNCE_INTERVAL_MS
    );
    const debouncedStartDateDay = useDebounced(
        startDateDay,
        DEBOUNCE_INTERVAL_MS
    );
    const debouncedEndDateDay = useDebounced(endDateDay, DEBOUNCE_INTERVAL_MS);

    const fetchUsers = async (page: number) => {
        if (!session) {
            return;
        }
        setLoading(true);
        const usersApi = new UserApi(session!);
        try {
            const data = await usersApi.getUsers(page, USERS_PAGE_SIZE, 'name');

            const newUsers = data.content.map(e => ({
                id: e.id!,
                name: e.name!,
            }));

            setUsersList(prev => {
                // filter out duplicates in case there are multiple calls to the useEffect
                const existingIds = new Set(prev.map(user => user.id));
                const uniqueNewUsers = newUsers.filter(
                    user => !existingIds.has(user.id)
                );
                return [...prev, ...uniqueNewUsers];
            });
        } catch (e) {
            console.error('Failed to load users', e);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchUsers(0);
    }, [session]);

    const fetchUsersList = async () => {
        if (selectedUsers[0] !== undefined) {
            const userApi = new UserApi(session!);
            try {
                const ids = `(${selectedUsers.map(String).join(',')})`;
                const res = (
                    await userApi.getUsers(0, 100, undefined, `id=${ids}`)
                ).content;
                setUsers(res);
            } catch (e) {
                console.error('Failed to load users list!', e);
            }
        } else {
            setUsers(null);
        }
    };

    const fetchProjects = async (page: number) => {
        if (!session) {
            return;
        }
        setLoading(true);
        const projectApi = new ProjectApi(session!);
        try {
            const data = await projectApi.getProjects(
                page,
                USERS_PAGE_SIZE,
                'name'
            );

            const newProjects = data.content.map(e => ({
                id: e.id!,
                name: e.name!,
            }));

            setProjectsList(prev => {
                // filter out duplicates in case there are multiple calls to the useEffect
                const existingIds = new Set(prev.map(project => project.id));
                const uniqueNewUsers = newProjects.filter(
                    project => !existingIds.has(project.id)
                );
                return [...prev, ...uniqueNewUsers];
            });
        } catch (e) {
            console.error('Failed to load projects', e);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchProjects(0);
    }, [session]);

    const fetchProjectsList = async () => {
        if (selectedProjects[0] !== undefined) {
            const projectApi = new ProjectApi(session!);
            try {
                const ids = `(${selectedProjects.map(String).join(',')})`;
                const res = (
                    await projectApi.getProjects(
                        0,
                        100,
                        undefined,
                        `id=${ids}`,
                        undefined,
                        undefined,
                        undefined
                    )
                ).content;
                setProjects(res);
            } catch (e) {
                console.error('Failed to load users list!', e);
            }
        } else {
            setUsers(null);
        }
    };

    const timeRecordsPageFetch = useCallback(
        async (
            page: number,
            rowsPerPage: number,
            _currentFilters: string | undefined
        ) => {
            try {
                const api = new TimeRecordApi(session!);
                let startDateStr = undefined;
                let endDateStr = undefined;
                if (debouncedStartDateDay) {
                    startDateStr = format(
                        startOfDay(debouncedStartDateDay),
                        'yyyy-MM-dd HH:mm:ss'
                    );
                }
                if (debouncedEndDateDay) {
                    endDateStr = format(
                        endOfDay(debouncedEndDateDay),
                        'yyyy-MM-dd HH:mm:ss'
                    );
                }

                const pageData = await api.getFilteredTimeRecords(
                    page,
                    rowsPerPage,
                    currentSort,
                    debouncedProjects,
                    debouncedUsers,
                    startDateStr,
                    endDateStr
                );

                if (Object.keys(pageData).length === 0) {
                    return emptyPage<FilteredTimeRecord>();
                }

                return pageData;
            } catch (e: any) {
                if (e instanceof ServiceError) {
                    const errorContent = await e.getContentAsJson();
                    return emptyPage<FilteredTimeRecord>();
                } else {
                    return emptyPage<FilteredTimeRecord>();
                }
            }
        },
        [
            session,
            debouncedUsers,
            debouncedProjects,
            debouncedStartDateDay,
            debouncedEndDateDay,
            currentSort,
        ]
    );

    const handleDraftTimeRecord = async (e: FilteredTimeRecord) => {
        if (modalOpen.current) {
            return false;
        }
        modalOpen.current = true;
        try {
            if (
                !(await confirmation.ask(
                    t('Confirm draft'),
                    t('Time record draft question'),
                    t('Yes'),
                    t('No')
                ))
            ) {
                return false;
            }
            const api = new TimeRecordApi(session!);
            try {
                let timeRecord = {
                    id: e.id,
                } as TimeRecord;

                await api.updateTimeRecord(timeRecord, 'draft');
                toast(t('Time Record Set to Draft Status!'));
                return true;
            } catch (e: any) {
                toast(e.message);
                return false;
            }
        } finally {
            modalOpen.current = false;
        }
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
                    t('Time Record export question'),
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
                if (debouncedStartDateDay) {
                    startDateStr = format(
                        startOfDay(debouncedStartDateDay),
                        'yyyy-MM-dd HH:mm:ss'
                    );
                }
                if (debouncedEndDateDay) {
                    endDateStr = format(
                        endOfDay(debouncedEndDateDay),
                        'yyyy-MM-dd HH:mm:ss'
                    );
                }
                await api.exportTimeRecords(
                    undefined,
                    debouncedProjects,
                    debouncedUsers,
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

    const handleDraftTimeRecords = async () => {
        if (modalOpen.current) {
            return false;
        }
        modalOpen.current = true;
        try {
            if (
                !(await confirmation.ask(
                    t('Confirm bulk draft'),
                    t('Time record draft question'),
                    t('Yes'),
                    t('No')
                ))
            ) {
                return;
            }
            const api = new TimeRecordApi(session!);
            try {
                await api.draftTimeRecords(
                    dataTableApiRef.current.getSelectedRows().map(r => r.id!)
                );
                toast(t('Time Records Drafted!'));
                dataTableApiRef.current.refreshData();
            } catch (e: any) {
                toast(e.message);
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
                    {t('General Time Records')}
                </Typography>
                <Typography>{t('General Time Records Page Text')}</Typography>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                    <Button
                        size="small"
                        component="label"
                        variant="outlined"
                        sx={{ marginBottom: 2 }}
                        onClick={handleDraftTimeRecords}>
                        {t('Draft Selected Records')}
                    </Button>
                    {/* Filter Section */}
                    <Combobox
                        size="small"
                        label={t('Users')}
                        multiple
                        entries={usersList!}
                        onChangeMultiple={newValue => {
                            const ids = newValue.map(entry => entry.id);
                            fetchUsersList();
                            setSelectedUsers(ids);
                        }}
                        value={selectedUsers}
                        sx={{ width: 330 }}
                    />
                    <Combobox
                        size="small"
                        label={t('Projects')}
                        multiple
                        entries={projectsList!}
                        onChangeMultiple={newValue => {
                            const ids = newValue.map(entry => entry.id);
                            fetchProjectsList();
                            setSelectedProjects(ids);
                        }}
                        value={selectedProjects}
                        sx={{ width: 330 }}
                    />
                    <LocalizationProvider dateAdapter={AdapterDayjs}>
                        <DatePicker
                            label={t('StartDate')}
                            defaultValue={
                                startDateDay ? dayjs(startDateDay) : null
                            }
                            onChange={evt => {
                                setStartDateDay(evt?.toDate());
                            }}
                            format="DD/MM/YYYY"
                            slotProps={{
                                textField: {
                                    size: 'small',
                                },
                                field: {
                                    clearable: true,
                                    onClear: () => setStartDateDay(undefined),
                                },
                            }}
                        />
                        <DatePicker
                            label={t('EndDate')}
                            defaultValue={endDateDay ? dayjs(endDateDay) : null}
                            onChange={evt => {
                                setEndDateDay(evt?.endOf('day').toDate());
                            }}
                            format="DD/MM/YYYY"
                            slotProps={{
                                textField: {
                                    size: 'small',
                                },
                                field: {
                                    clearable: true,
                                    onClear: () => setEndDateDay(undefined),
                                },
                            }}
                        />
                    </LocalizationProvider>
                    <Button
                        size="small"
                        component="label"
                        variant="outlined"
                        startIcon={<CloudDownload />}
                        sx={{ marginBottom: 2 }}
                        onClick={handleExportTimeRecords}>
                        {t('Export Time Records')}
                    </Button>
                </Stack>
                <DataTable<FilteredTimeRecord>
                    columns={[
                        { field: 'userName', title: t('User') },
                        { field: 'projectName', title: t('Project') },
                        { field: 'taskName', title: t('Task') },
                        { field: 'description', title: t('Description') },
                        { field: 'hours', title: t('Hours') },
                        { field: 'statusName', title: t('Status') },
                        {
                            field: 'startDate',
                            title: t('StartDate'),
                            formatter: value => {
                                if (!value) return '';
                                return format(new Date(value), 'yyyy-MM-dd');
                            },
                        },
                        {
                            field: 'endDate',
                            title: t('EndDate'),
                            formatter: value => {
                                if (!value) return '';
                                return format(new Date(value), 'yyyy-MM-dd');
                            },
                        },
                    ]}
                    apiRef={dataTableApiRef}
                    rowActions={[
                        {
                            label: t('Draft'),
                            icon: 'edit_document',
                            position: 'left',
                            action: handleDraftTimeRecord,
                            hideWhenDisabled: true,
                            isEnabled: r => r.statusName === 'APPROVED',
                        },
                    ]}
                    fetcher={timeRecordsPageFetch}
                    currentSort={currentSort}
                    onSortChange={setCurrentSort}
                    initialPage={0}
                    initialRowsPerPage={10}
                    withRowSelection={true}
                />
            </Stack>
            {confirmation.dialog}
        </>
    );
}
