'use client';

import React, { useCallback, useEffect, useRef, useState } from 'react';
import { CalendarMatrix } from './calendar-matrix';
import {
    Box,
    Button,
    Collapse,
    Divider,
    Stack,
    Typography,
} from '@mui/material';
import { ExpandLess, ExpandMore } from '@mui/icons-material';
import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import AbsenceApi, { AbsencesByDateMap } from '../../lib/absences';
import Combobox, {
    Entry,
} from '@datacentric/datacentric-ui/components/combobox';
import UserApi, { User } from '../../lib/users';
import { useSession } from 'next-auth/react';
import CheckBoxOutlineBlankIcon from '@mui/icons-material/CheckBoxOutlineBlank';
import CheckBoxIcon from '@mui/icons-material/CheckBox';
import StatusApi from '../../lib/status';
import AbsenceTypeApi from '../../lib/absence-types';
import { useDebounced } from '@datacentric/datacentric-ui/lib/browser-utils';
import { UnplannedVacsTable } from '../../components/unplanned-vacs-table';
import format from '@datacentric/datacentric-ui/lib/formatter';
import { startOfDay } from 'date-fns';
import { useToast } from '@/components/toast-provider';
import DCToggleButton from '../../components/dc-toggle-button';

export type BusinessYear = {
    id?: number;
    name: string;
};

const DEBOUNCE_INTERVAL_MS = 200;

export default function GlobalAbsencesPage() {
    const USER_SCOPE_COMPANY = 'SCOPE-COMPANY';
    const USER_SCOPE_MANAGER = 'SCOPE-MANAGER';
    const USER_SCOPE_TEAM = 'SCOPE-TEAM';
    const ABSENCE_SCOPE_TEAM = 'SCOPE-TEAM';
    const ABSENCE_SCOPE_COMPANY = 'SCOPE-COMPANY';

    const icon = <CheckBoxOutlineBlankIcon fontSize="small" />;
    const checkedIcon = <CheckBoxIcon fontSize="small" />;

    // force render
    const [, setUpdate] = useState(0);
    const forceRender = () => setUpdate(prev => prev + 1);

    const { data: session, update } = useSession();
    const { t } = useTranslation(undefined, 'timesense');

    const [showFilters, setShowFilters] = useState(true);
    const [usersList, setUsersList] = useState<Entry[]>([]);
    const [statusList, setStatusList] = useState<Entry[] | null>([]);
    const [absenceTypesList, setAbsenceTypesList] = useState<Entry[]>([]);
    const [selectedUsers, setSelectedUsers] = useState<number[]>([]);
    const [selectedStatus, setSelectedStatus] = useState<number[]>([]);
    const [selectedAbsenceTypes, setSelectedAbsenceTypes] = useState<number[]>(
        []
    );
    const [selectedYears, setSelectedYears] = useState<Entry[]>([]);
    const [seeUserTeam, setSeeUserTeam] = useState(false);

    const [calendarData, setCalendarData] = useState<AbsencesByDateMap | null>(
        null
    );
    const [users, setUsers] = useState<User[] | null>(null);
    const [userTeam, setUserTeam] = useState<Entry[] | null>(null);
    const [year, setYear] = useState(new Date().getFullYear());
    const [showHeatMap, setShowHeatMap] = useState<boolean>(false);

    const currentYear = new Date().getFullYear();

    const yearsList: Entry[] = Array.from({ length: 2 }, (_, i) => ({
        id: i,
        name: (currentYear - i).toString(),
    }));

    const USERS_PAGE_SIZE = 10;
    const [loading, setLoading] = useState(false);

    const usersProvider = useCallback(
        async (filter: string, nextRecord: number, byIds?: number[]) => {
            if (!session) {
                return { entries: [], endOfList: false };
            }
            const usersApi = new UserApi(session!);
            try {
                if (byIds && byIds.length > 0) {
                    const ids = `(${byIds.join(',')})`;
                    const data = await usersApi.getUsers(
                        nextRecord / USERS_PAGE_SIZE,
                        USERS_PAGE_SIZE,
                        'name',
                        `id=${ids}`,
                        USER_SCOPE_COMPANY
                    );
                    const entries = data.content.map(e => ({
                        id: e.id!,
                        name: e.name!,
                    }));
                    return {
                        entries,
                        endOfList:
                            data.totalElements <=
                            nextRecord + data.content.length,
                    };
                } else {
                    const data = await usersApi.getUsers(
                        nextRecord / USERS_PAGE_SIZE,
                        USERS_PAGE_SIZE,
                        'name',
                        filter !== '' ? `name~${filter}` : '',
                        USER_SCOPE_COMPANY
                    );
                    const entries = data.content.map(e => ({
                        id: e.id!,
                        name: e.name!,
                    }));
                    return {
                        entries,
                        endOfList:
                            data.totalElements <=
                            nextRecord + data.content.length,
                    };
                }
            } catch (e) {
                console.error('Failed to load users', e);
                return { entries: [], endOfList: false };
            }
        },
        []
    );

    const fetchAbsenceTypes = async () => {
        if (!session) return;

        const absTypesApi = new AbsenceTypeApi(session!);
        try {
            const data = await absTypesApi.getAbsenceTypes(0, 1000, 'name');

            let types = data.content.map(e => ({
                id: e.id!,
                name: e.name!,
            }));

            // Apply role-based filter
            const hasOnlyUserRole =
                userInfo?.userRoles &&
                userInfo.userRoles.length === 1 &&
                userInfo.userRoles[0].name === 'User';

            if (hasOnlyUserRole) {
                types = types.filter(t => t.name === 'VACATION');
            }

            setAbsenceTypesList(types);
        } catch (e) {
            console.error('Failed to load absence types', e);
        }
    };

    const fetchStatus = async () => {
        if (!session) {
            return;
        }
        const statusApi = new StatusApi(session!);
        try {
            const filter = 'type!=Project';
            statusApi.getAllStatus(0, 1000, 'name', filter).then(data => {
                setStatusList(
                    data.content.map(e => ({
                        id: e.id!,
                        name: e.name!,
                    }))
                );
            });
        } catch (e) {
            console.error('Failed to load status', e);
        }
    };

    const fetchInitialData = async (
        year: string,
        userFilter?: number[],
        statusFilter?: number[],
        typesFilter?: number[],
        businessYearFilter?: string[]
    ) => {
        if (!session) {
            return;
        }
        const absenceApi = new AbsenceApi(session!);
        try {
            absenceApi
                .getAbsencesByDate(
                    year,
                    userFilter,
                    statusFilter,
                    typesFilter,
                    businessYearFilter,
                    ABSENCE_SCOPE_COMPANY
                )
                .then(data => {
                    setCalendarData(data.data);
                });
        } catch (e) {
            console.error('Failed to load absences by date map', e);
        }
    };

    const fetchUsersList = async () => {
        if (selectedUsers[0] !== undefined) {
            const userApi = new UserApi(session!);
            try {
                const ids = `(${selectedUsers.map(String).join(',')})`;
                const res = (
                    await userApi.getUsers(
                        0,
                        100,
                        undefined,
                        `id=${ids}`,
                        USER_SCOPE_COMPANY
                    )
                ).content;
                setUsers(res);
            } catch (e) {
                console.error('Failed to load users list!', e);
            }
        } else {
            setUsers(null);
        }
    };

    const [userInfo, setUserInfo] = useState<User>();
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

    const fetchUserTeam = async () => {
        if (!session) {
            return;
        }
        const usersApi = new UserApi(session!);
        const isManager = userInfo?.userRoles?.some(
            role => role.name === 'Manager' || role.name === 'Admin'
        );
        if (isManager) {
            try {
                const userTeam = (
                    await usersApi.getUsers(
                        0,
                        USERS_PAGE_SIZE,
                        'name',
                        undefined,
                        USER_SCOPE_MANAGER
                    )
                ).content;
                const usersEntry = userTeam.map(e => ({
                    id: e.id!,
                    name: e.name!,
                }));
                setUserTeam(usersEntry);
                setTimeout(() => forceRender(), 100);
            } catch (e) {
                console.error('Failed to load users', e);
            }
        }
    };

    // Primeiro useEffect - só depende de session
    useEffect(() => {
        if (!session) return;

        fetchStatus();
        fetchAbsenceTypes();
    }, [session]);

    useEffect(() => {
        if (!session || !userInfo) return;

        fetchUserTeam();
    }, [session, userInfo]);

    useEffect(() => {
        fetchUserInfo();
    }, [session!]);

    useEffect(() => {
        fetchInitialData(
            year.toString(),
            selectedUsers,
            selectedStatus,
            selectedAbsenceTypes,
            selectedYears.map(i => i.name)
        );
        setTimeout(() => forceRender(), 100);
    }, [session!, year]);

    const debouncedUsers = useDebounced(selectedUsers, DEBOUNCE_INTERVAL_MS);
    const debouncedStatus = useDebounced(selectedStatus, DEBOUNCE_INTERVAL_MS);
    const debouncedAbsenceTypes = useDebounced(
        selectedAbsenceTypes,
        DEBOUNCE_INTERVAL_MS
    );
    const debouncedBusinessYear = useDebounced(
        selectedYears,
        DEBOUNCE_INTERVAL_MS
    );

    useEffect(() => {
        fetchInitialData(
            year.toString(),
            debouncedUsers,
            debouncedStatus,
            debouncedAbsenceTypes,
            debouncedBusinessYear.map(i => i.name)
        );
        fetchUsersList();
        setTimeout(() => forceRender(), 100);
    }, [
        debouncedUsers,
        debouncedStatus,
        debouncedAbsenceTypes,
        debouncedBusinessYear,
        year,
    ]);

    const { toast } = useToast();
    const fetchAbsenceDetails = async (currentDate: Date) => {
        try {
            const startDateStr = format(
                startOfDay(currentDate),
                'yyyy-MM-dd HH:mm:ss'
            );
            const api = new AbsenceApi(session!);
            const details = (
                await api.getAbsencesDetailsByDate(
                    startDateStr,
                    debouncedUsers,
                    debouncedStatus,
                    debouncedAbsenceTypes,
                    debouncedBusinessYear.map(i => i.name),
                    ABSENCE_SCOPE_COMPANY
                )
            ).data;

            return details;
        } catch (e) {
            console.error('Failed to load absences list for date!', e);
            toast('Failed to load absences list for date!');
            return [];
        }
    };

    const handleSeeUserTeamChange = (isUserTeam: boolean) => {
        setSeeUserTeam(isUserTeam);
        if (isUserTeam && userTeam) {
            const ids = userTeam.map(entry => entry.id);
            setSelectedUsers(ids);
        } else {
            setSelectedUsers([]);
        }
    };

    return (
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
                {t('Manage Timeoff')}
            </Typography>

            {/* Toggle Filters Button */}
            <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 1 }}>
                <Button
                    onClick={() => setShowFilters(!showFilters)}
                    endIcon={showFilters ? <ExpandLess /> : <ExpandMore />}
                    variant="outlined"
                    size="small">
                    {showFilters ? t('Hide Filters') : t('Show Filters')}
                </Button>
            </Box>

            {/* Filters Section */}
            <Collapse in={showFilters}>
                <Box sx={{ mb: 2 }}>
                    <Stack direction={'column'}>
                        {userInfo?.userRoles &&
                        userInfo.userRoles.length === 1 &&
                        userInfo.userRoles[0].name === 'User' ? undefined : (
                            <DCToggleButton
                                value={seeUserTeam ? 'team' : 'all'}
                                options={[
                                    { value: 'team', label: t('My Team') },
                                    { value: 'all', label: t('All Users') },
                                ]}
                                onChange={newValue =>
                                    handleSeeUserTeamChange(newValue === 'team')
                                }
                                sx={{
                                    alignSelf: 'flex-start',
                                    height: 40,
                                    display: 'flex',
                                    alignItems: 'center',
                                    marginBottom: 2,
                                }}
                            />
                        )}
                        <Stack
                            direction={{ xs: 'column', sm: 'row' }}
                            spacing={2}>
                            <Combobox
                                size="small"
                                label={t('Users')}
                                multiple
                                limitTags={2}
                                entries={usersProvider}
                                value={selectedUsers}
                                onChangeMultiple={newValue => {
                                    const ids = newValue.map(entry => entry.id);
                                    fetchUsersList();
                                    setSelectedUsers(ids);
                                }}
                                sx={{ width: 400 }}
                            />
                            {userInfo?.userRoles &&
                            userInfo.userRoles.length === 1 &&
                            userInfo.userRoles[0].name ===
                                'User' ? undefined : (
                                <Combobox
                                    size="small"
                                    label={t('Absence Types')}
                                    multiple
                                    entries={absenceTypesList!}
                                    value={selectedAbsenceTypes}
                                    ListboxProps={{
                                        style: {
                                            maxHeight: 170, // value in px
                                        },
                                    }}
                                    onChangeMultiple={newValue => {
                                        const ids = newValue.map(
                                            entry => entry.id
                                        );
                                        setSelectedAbsenceTypes(ids);
                                    }}
                                    sx={{ width: 350 }}
                                />
                            )}

                            <Combobox
                                size="small"
                                multiple
                                label={t('Status')}
                                entries={statusList!}
                                value={selectedStatus}
                                ListboxProps={{
                                    style: {
                                        maxHeight: 170, // value in px
                                    },
                                }}
                                onChangeMultiple={newValue => {
                                    const ids = newValue.map(entry => entry.id);
                                    setSelectedStatus(ids);
                                }}
                                sx={{ width: 250 }}
                            />

                            <Combobox
                                size="small"
                                multiple
                                label={t('Business Years')}
                                entries={yearsList}
                                value={selectedYears.map(e => e.id)}
                                onChangeMultiple={(newValue: Entry[]) => {
                                    setSelectedYears(newValue);
                                }}
                                sx={{ width: 200 }}
                            />

                            {users ? (
                                <Box
                                    sx={{
                                        display: 'flex',
                                        justifyContent: 'flex-end',
                                        mb: 1,
                                    }}>
                                    <UnplannedVacsTable users={users!} />
                                </Box>
                            ) : undefined}
                        </Stack>
                    </Stack>
                </Box>
            </Collapse>

            <Divider sx={{ my: 2 }} />

            {/* Calendar section */}
            <Box sx={{ flex: '1 1 70%', overflow: 'auto', mt: 2 }}>
                <CalendarMatrix
                    year={year}
                    data={calendarData!}
                    showHeatMap={showHeatMap}
                    totalUsers={usersList.length}
                    onFetchDetails={fetchAbsenceDetails}
                    onYearChange={setYear}
                    onHeatMapChange={setShowHeatMap}
                />
            </Box>
        </Box>
    );
}
