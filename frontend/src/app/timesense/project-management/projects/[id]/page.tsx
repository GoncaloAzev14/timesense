'use client';

import ProjectApi, {
    Project,
    ProjectCostByWeekMap,
} from '@/app/timesense/lib/projects';
import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import {
    Box,
    Button,
    Divider,
    Grid,
    IconButton,
    Stack,
    Tab,
    Tabs,
    Tooltip,
    Typography,
} from '@mui/material';
import { useSession } from 'next-auth/react';
import { useParams, useRouter } from 'next/navigation';
import { useCallback, useEffect, useRef, useState } from 'react';
import format from '@datacentric/datacentric-ui/lib/formatter';
import { useToast } from '@/components/toast-provider';
import ProjectAssignmentApi, {
    ProjectAssignment,
} from '@/app/timesense/lib/project-assignment';
import { GanttChart, GanttData, Row } from '@/components/gant-chart';
import {
    format as formatter,
    addWeeks,
    isBefore,
    startOfWeek,
    startOfDay,
    endOfDay,
    endOfWeek,
} from 'date-fns';
import GenericChart, {
    ChartInput,
    formatDateToLabel,
    LegendProp,
    RawWeeklyData,
} from '@/components/chartjs';
import PermissionsApi from '@/app/timesense/lib/permissions';
import {
    ProjectAssignmentModal,
    useProjectAssignmentModal,
} from '../../project-assignment/project-assign-modal';
import useConfirmation from '@datacentric/datacentric-ui/components/confirmation';
import { DataTable } from '@datacentric/datacentric-ui/components/datatable';
import {
    emptyPage,
    Page,
} from '@datacentric/datacentric-ui/lib/spring-pagination';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider/LocalizationProvider';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import dayjs from 'dayjs';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import Combobox, {
    Entry,
} from '@datacentric/datacentric-ui/components/combobox';
import UserApi, { User } from '@/app/timesense/lib/users';
import CheckBoxOutlineBlankIcon from '@mui/icons-material/CheckBoxOutlineBlank';
import CheckBoxIcon from '@mui/icons-material/CheckBox';
import { CloudDownload } from '@mui/icons-material';
import { ServiceError } from '@/lib/service-base';
import { useDebounced } from '@datacentric/datacentric-ui/lib/browser-utils';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';

function LabeledValue({
    label,
    value,
    color,
    description,
}: {
    label: string;
    value: string | number | undefined;
    color?: string;
    description?: string;
}) {
    return (
        <Stack direction="row" spacing={1} alignItems="start">
            <Typography fontWeight="bold">{label}:</Typography>
            {description && (
                <Tooltip title={description} arrow>
                    <IconButton size="small">
                        <InfoOutlinedIcon fontSize="small" />
                    </IconButton>
                </Tooltip>
            )}
            <Typography color={color}>{value}</Typography>
        </Stack>
    );
}

interface TabPanelProps {
    children?: React.ReactNode;
    index: number;
    value: number;
}
function CustomTabPanel(props: TabPanelProps) {
    const { children, value, index, ...other } = props;

    return (
        <div
            role="tabpanel"
            hidden={value !== index}
            id={`simple-tabpanel-${index}`}
            aria-labelledby={`simple-tab-${index}`}
            {...other}>
            {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
        </div>
    );
}

const DEBOUNCE_INTERVAL_MS = 200;

export default function ProjectPage() {
    const { data: session } = useSession();
    const [project, setProject] = useState<Project | null>();
    const { t } = useTranslation(undefined, 'timesense');
    const params = useParams<{ id: string }>();
    const { toast } = useToast();
    const confirmation = useConfirmation();
    const [projCostByWeek, setProjCostByWeek] =
        useState<ProjectCostByWeekMap[]>();
    const [projCostByMonth, setProjCostByMonth] =
        useState<ProjectCostByWeekMap[]>();
    const [projBudget, setProjBudget] = useState<number>();
    const [weeklyDates, setWeeklyDates] = useState<string[]>();
    const [projCurrentCost, setProjCurrentCost] = useState<
        number | undefined
    >();

    const [projEstimatedMargin, setProjEstimatedMargin] = useState<
        number | undefined
    >();
    const [projRealMargin, setProjRealMargin] = useState<number | undefined>();

    const [projUserAllocations, setProjUserAllocations] = useState<
        ProjectAssignment[] | undefined
    >();
    const [projWeeks, setProjWeeks] = useState<Date[] | undefined>();
    const [projUsers, setProjUsers] = useState<Row[] | undefined>();
    const [ganttData, setGanttData] = useState<GanttData[] | undefined>();
    const [costGanttData, setCostGanttData] = useState<
        GanttData[] | undefined
    >();

    const [burnUpChartData, setBurnUpChartData] = useState<ChartInput[]>();
    const [projectCumulativeCost, setProjectCumulativeCost] =
        useState<number[]>();
    const [burnDownChartData, setBurnDownChartData] = useState<ChartInput[]>();
    const [projectRemainingBudget, setProjectRemainingBudget] =
        useState<number[]>();

    const router = useRouter();
    const projectAssignModalRef = useProjectAssignmentModal();
    const modalOpen = useRef(false);
    const rerenderCharts = useRef(0);
    const [canCreateProjectAssigns, setCanCreateProjectAssigns] =
        useState(false);
    const [tabSelected, setTabSelected] = useState(0);

    const icon = <CheckBoxOutlineBlankIcon fontSize="small" />;
    const checkedIcon = <CheckBoxIcon fontSize="small" />;
    const USERS_PAGE_SIZE = 100;
    const [loading, setLoading] = useState(false);
    const [hasMore, setHasMore] = useState(true);
    const listboxRef = useRef(null);
    const [startDateWeek, setStartDateWeek] = useState<Date>();
    const [endDateWeek, setEndDateWeek] = useState<Date>();
    const [startDateDay, setStartDateDay] = useState<Date>();
    const [endDateDay, setEndDateDay] = useState<Date>();
    const [usersList, setUsersList] = useState<Entry[]>([]);
    const [selectedUsers, setSelectedUsers] = useState<number[]>([]);
    const [users, setUsers] = useState<User[] | null>(null);

    const debouncedUsers = useDebounced(selectedUsers, DEBOUNCE_INTERVAL_MS);
    const debouncedStartDateWeek = useDebounced(
        startDateWeek,
        DEBOUNCE_INTERVAL_MS
    );
    const debouncedEndDateWeek = useDebounced(
        endDateWeek,
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

            if (newUsers.length < USERS_PAGE_SIZE) {
                setHasMore(false);
            }
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

    // force render
    const [, setUpdate] = useState(0);
    const forceRender = () => setUpdate(prev => prev + 1);

    useEffect(() => {
        (async () => {
            const api = new PermissionsApi(session!);
            setCanCreateProjectAssigns(
                (await api.getSystemPermissions()).includes(
                    'CREATE_PROJECTS'
                ) ||
                    (
                        await api.getUserPermissionsForResource(
                            'Project',
                            parseInt(params!.id!)
                        )
                    ).includes('EDIT_PROJECTS')
            );
        })();
    }, [session]);

    const handleCloseProject = async () => {
        if (modalOpen.current) {
            return false;
        }

        modalOpen.current = true;
        try {
            if (
                !(await confirmation.ask(
                    t('Confirm close project'),
                    t('Close project question'),
                    t('Yes'),
                    t('No')
                ))
            ) {
                return false;
            }
            const api = new ProjectApi(session!);
            try {
                await api.closeProject(parseInt(params!.id!));
                toast(t('Project closed successfully!'));
                return true;
            } catch (e: any) {
                toast(e.messages);
                return false;
            }
        } finally {
            modalOpen.current = false;
        }
    };

    const handleNewProjectAssignment = useCallback(async () => {
        if (!canCreateProjectAssigns) {
            return;
        }

        if (modalOpen.current) {
            return;
        }

        modalOpen.current = true;
        try {
            const result = await projectAssignModalRef.current.openDialog(
                'create',
                {
                    id: 0,
                    user: undefined,
                    project: project!,
                }
            );
            if (result) {
                toast(t('Project Assignment created'));
            }
        } finally {
            modalOpen.current = false;
            rerenderCharts.current++;
            router.refresh();
        }
    }, [projectAssignModalRef, t, canCreateProjectAssigns, toast]);

    // Calculate project costs and budget
    useEffect(() => {
        (async () => {
            if (!params) {
                return;
            }
            const projectApi = new ProjectApi(session!);
            try {
                const projCostByWeek = await projectApi.getProjectCostByWeek(
                    parseInt(params!.id)
                );
                const projCostByUserWeek =
                    await projectApi.getProjectCostByWeek(
                        parseInt(params!.id),
                        true
                    );
                setProjCostByWeek(projCostByWeek);
                const costGanttData =
                    mapProjectCostsToGanttData(projCostByUserWeek);
                setCostGanttData(costGanttData);
            } catch (e: any) {
                toast(e.messages);
            }
            try {
                const projectBudget = await projectApi.getProjectBudget(
                    parseInt(params!.id)
                );
                setProjBudget(projectBudget);
            } catch (e: any) {
                toast(e.messages);
            }
            const { ...project } = await projectApi.getProject(
                parseInt(params!.id)
            );
            setProject(project);
        })();
    }, [params, session, rerenderCharts]);

    useEffect(() => {
        (async () => {
            if (!params || !project) {
                return;
            }
            const projectApi = new ProjectApi(session!);
            try {
                const projUserAllocations =
                    await projectApi.getProjectUserAllocations(
                        parseInt(params!.id)
                    );
                setProjUserAllocations(projUserAllocations);
                const projectWeeks = getMondaysBetweenDates(
                    project?.startDate!,
                    project?.expectedDueDate!
                );
                setProjWeeks(projectWeeks);
                setTimeout(() => forceRender(), 100);

                if (projUserAllocations) {
                    const ganttChartData = mapProjectAssignmentsToGanttData(
                        projUserAllocations!
                    );
                    setGanttData(ganttChartData!);

                    const usersAllocated =
                        mapProjectAssignmentsToUsersList(projUserAllocations);
                    setProjUsers(usersAllocated);
                }
            } catch (e: any) {
                toast(e.messages);
            }
        })();
    }, [session, params, project, rerenderCharts]);

    useEffect(() => {
        if (project) {
            // WEEKLY DATES
            const weeks = [];
            const endDate = project.endDate ? project.endDate : new Date();

            const expectedEndDate = project.expectedDueDate
                ? project.expectedDueDate
                : new Date();
            // Get the maximum allocation end date (if any)
            const allocationEndDates = projUserAllocations
                ?.map(a => (a.endDate ? new Date(a.endDate) : null))
                .filter((d): d is Date => d !== null);
            const maxAllocationEndDate =
                allocationEndDates && allocationEndDates.length > 0
                    ? new Date(
                          Math.max(...allocationEndDates.map(d => d.getTime()))
                      )
                    : new Date();

            // Each ProjectCostByWeekMap.startWeek is a string like 'dd/MM/yyyy' or ISO date
            const costWeekEndDates = projCostByWeek
                ?.map(c => {
                    if (!c.startWeek) return null;
                    const start = new Date(c.startWeek);
                    return endOfWeek(start, { weekStartsOn: 1 });
                })
                .filter((d): d is Date => d !== null);

            const maxCostWeekEndDate =
                costWeekEndDates && costWeekEndDates.length > 0
                    ? new Date(
                          Math.max(...costWeekEndDates.map(d => d.getTime()))
                      )
                    : new Date();

            const maxEndDate = new Date(
                Math.max(
                    endDate.getTime(),
                    expectedEndDate.getTime(),
                    maxAllocationEndDate.getTime(),
                    maxCostWeekEndDate.getTime()
                )
            );

            let current = startOfWeek(project?.startDate!, { weekStartsOn: 1 });
            while (
                isBefore(current, maxEndDate) ||
                current.getTime() === maxEndDate.getTime()
            ) {
                weeks.push(formatter(current, 'dd/MM/yyyy'));
                current = addWeeks(current, 1);
            }
            setWeeklyDates(weeks);
        }
    }, [params, session, project, projUserAllocations]);

    useEffect(() => {
        if (weeklyDates) {
            // CUMULATIVE COSTS FOR LINE CHART DATA
            const projectCumulativeCost = buildCostsAndCumulative(
                weeklyDates!,
                projCostByWeek!
            ).cumulativeCosts;
            setProjectCumulativeCost(projectCumulativeCost!);
            setTimeout(() => forceRender(), 100);

            // REMAINING BUDGET FOR LINE CHART DATA
            const remainingBudgetObj = buildRemainingBudget(
                weeklyDates!,
                projCostByWeek!,
                projBudget ? projBudget : 0
            );
            const projectRemainingBudget = remainingBudgetObj.remainingBudget;
            setProjCurrentCost(remainingBudgetObj.currentCost);
            setProjectRemainingBudget(projectRemainingBudget!);
            setTimeout(() => forceRender(), 100);
        }
    }, [weeklyDates, projBudget, rerenderCharts, project?.realBudget]);

    useEffect(() => {
        if (projectRemainingBudget) {
            let projRealMargin = 0.0;
            let projEstimatedMargin = 0.0;
            if (project?.realBudget != undefined) {
                projRealMargin = project?.realBudget - projCurrentCost!;
                let pBudget = projBudget ? projBudget : 0;
                projEstimatedMargin = project?.realBudget - pBudget;
            }
            setProjRealMargin(projRealMargin);
            setProjEstimatedMargin(projEstimatedMargin);
        }
    }, [projectRemainingBudget, projBudget, project?.realBudget]);

    useEffect(() => {
        const burnUpChartDatasets = [
            {
                type: 'line',
                label: t('Cumulative Cost (€)'),
                data: projectCumulativeCost,
                color: 'rgb(75, 141, 192)',
                yAxisID: 'right',
            },
            {
                type: 'bar',
                label: t('Weekly Costs (€)'),
                data: projCostByWeek,
                color: 'rgba(255, 224, 99, 0.6)',
                yAxisID: 'left',
                labelType: 'weekly',
            },
        ] as ChartInput[];
        setBurnUpChartData(burnUpChartDatasets);

        const burnDownChartDatasets = [
            {
                type: 'line',
                label: t('Remaining Budget ($)'),
                data: projectRemainingBudget,
                color: 'rgb(75, 141, 192)',
                yAxisID: 'left',
            },
            {
                type: 'bar',
                label: t('Weekly Costs ($)'),
                data: projCostByWeek,
                color: 'rgba(255, 224, 99, 0.6)',
                yAxisID: 'right',
                labelType: 'weekly',
            },
        ] as ChartInput[];
        setBurnDownChartData(burnDownChartDatasets);
    }, [projectCumulativeCost, projCostByWeek, rerenderCharts]);

    const legendProps = {
        display: true,
        position: 'bottom',
    } as LegendProp;

    const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
        setTabSelected(newValue);
        setSelectedUsers([]);
    };

    const fetchCostsDetails = useCallback(
        async (
            type: string,
            _page: number,
            _rowsPerPage: number,
            _currentFilters: string | undefined
        ) => {
            try {
                const projectApi = new ProjectApi(session!);
                let startDateStr = undefined;
                let endDateStr = undefined;
                if (debouncedStartDateWeek) {
                    startDateStr = format(
                        startOfDay(debouncedStartDateWeek),
                        'yyyy-MM-dd HH:mm:ss'
                    );
                }
                if (debouncedEndDateWeek) {
                    endDateStr = format(
                        endOfDay(debouncedEndDateWeek),
                        'yyyy-MM-dd HH:mm:ss'
                    );
                }
                let pageData;
                if (type === 'week') {
                    pageData = await projectApi.getProjectCostByWeekPage(
                        parseInt(params!.id),
                        _page,
                        _rowsPerPage,
                        _currentFilters,
                        debouncedUsers,
                        startDateStr,
                        endDateStr
                    );
                } else {
                    pageData = await projectApi.getProjectCostByMonthPage(
                        parseInt(params!.id),
                        _page,
                        _rowsPerPage,
                        _currentFilters,
                        debouncedUsers,
                        startDateStr,
                        endDateStr
                    );
                }

                if (Object.keys(pageData).length === 0) {
                    return emptyPage<ProjectCostByWeekMap>();
                }
                return pageData;
            } catch (e: any) {
                if (e instanceof ServiceError) {
                    const errorContent = await e.getContentAsJson();
                    return emptyPage<ProjectCostByWeekMap>();
                } else {
                    return emptyPage<ProjectCostByWeekMap>();
                }
            }
        },
        [session, debouncedUsers, debouncedStartDateWeek, debouncedEndDateWeek]
    );

    const fetchCostsDetailsByDay = useCallback(
        async (
            _page: number,
            _rowsPerPage: number,
            _currentFilters: string | undefined
        ) => {
            try {
                const projectApi = new ProjectApi(session!);

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

                const pageData = await projectApi.getProjectCostByDay(
                    parseInt(params!.id),
                    _page,
                    _rowsPerPage,
                    _currentFilters,
                    debouncedUsers,
                    startDateStr,
                    endDateStr
                );

                if (Object.keys(pageData).length === 0) {
                    return emptyPage<ProjectCostByWeekMap>();
                }

                return pageData;
            } catch (e: any) {
                if (e instanceof ServiceError) {
                    const errorContent = await e.getContentAsJson();
                    return emptyPage<ProjectCostByWeekMap>();
                } else {
                    return emptyPage<ProjectCostByWeekMap>();
                }
            }
        },
        [session, debouncedUsers, debouncedStartDateDay, debouncedEndDateDay]
    );

    const handleExportTimeRecords = async () => {
        if (modalOpen.current) {
            return false;
        }
        modalOpen.current = true;
        try {
            if (
                !(await confirmation.ask(
                    t('Confirm Project Time Record export'),
                    t('Project Time Record export question'),
                    t('Yes'),
                    t('No')
                ))
            ) {
                return false;
            }
            const api = new ProjectApi(session!);
            try {
                let projName = project ? project.name : '';
                let exportDate = new Date().toLocaleDateString('pt-PT');
                await api.exportProjectTimeRecords(
                    parseInt(params!.id),
                    projName,
                    exportDate
                );
                toast('Project TimeRecords exported');
                return true;
            } catch (e: any) {
                toast(e.messages);
                return false;
            }
        } finally {
            modalOpen.current = false;
        }
    };

    const handleGanttRowClick = async (id: number | undefined) => {
        if (modalOpen.current) {
            return false;
        }

        const projectAssignApi = new ProjectAssignmentApi(session!);
        const projectAssign = await projectAssignApi.getProjectAssignment(id!);
        modalOpen.current = true;
        try {
            const result = await projectAssignModalRef.current.openDialog(
                'edit',
                {
                    ...projectAssign,
                }
            );
            if (result) {
                toast(t('Project Assignment updated'));
            }
        } finally {
            modalOpen.current = false;
            router.refresh();
        }
    };

    return (
        <>
            {project && (
                <>
                    <Grid container spacing={2} rowGap={1}>
                        <Grid item xs={12}>
                            <Typography variant="h4" fontWeight={'bold'}>
                                {t('Project')}: {project.name} -{' '}
                                {project.description}
                            </Typography>
                        </Grid>
                        <Grid item xs={12}>
                            <Divider />
                        </Grid>

                        <Grid item xs={12}>
                            <Stack
                                direction="row"
                                spacing={1}
                                alignItems="start"
                                marginBottom={2}
                                maxWidth="90%">
                                <Stack
                                    direction="column"
                                    spacing={1}
                                    width="100%">
                                    <LabeledValue
                                        label={t('Project Type')}
                                        value={project.type!.name}
                                    />
                                    <LabeledValue
                                        label={t('Client')}
                                        value={project.client!.name}
                                    />
                                    <LabeledValue
                                        label={t('Project Manager')}
                                        value={project.manager!.name}
                                    />
                                </Stack>
                                <Stack
                                    direction="column"
                                    spacing={1}
                                    width="100%">
                                    <LabeledValue
                                        label={t('StartDate')}
                                        value={format(
                                            project.startDate,
                                            'YYYY-MM-DD'
                                        )}
                                    />
                                    <LabeledValue
                                        label={t('Expected Due Date')}
                                        value={format(
                                            project.expectedDueDate,
                                            'YYYY-MM-DD'
                                        )}
                                    />
                                    {project.endDate ? (
                                        <LabeledValue
                                            label={t('EndDate')}
                                            value={format(
                                                project.endDate,
                                                'YYYY-MM-DD'
                                            )}
                                        />
                                    ) : (
                                        ''
                                    )}
                                    <LabeledValue
                                        label={t('Status')}
                                        value={project.status!.name}
                                    />
                                </Stack>
                                <Stack
                                    direction="column"
                                    spacing={1}
                                    width="100%">
                                    <LabeledValue
                                        label={t('Project Budget')}
                                        value={
                                            '€ ' +
                                            format(
                                                project.realBudget,
                                                '#,###.00'
                                            )
                                        }
                                        description="Total budget assigned to the project."
                                    />
                                    <LabeledValue
                                        label={t('Project Allocations')}
                                        value={
                                            '€ ' +
                                            format(projBudget, '#,###.00')
                                        }
                                        description="Combined value of all user allocations for the project."
                                    />
                                    <LabeledValue
                                        label={t('Current Cost')}
                                        value={
                                            '€ ' +
                                            format(projCurrentCost, '#,###.00')
                                        }
                                        description="Costs recorded by users up to today."
                                    />
                                </Stack>
                                <Stack
                                    direction="column"
                                    spacing={1}
                                    width="100%">
                                    <LabeledValue
                                        label={t('Real Margin')}
                                        value={
                                            '€ ' +
                                            format(projRealMargin, '#,###.00')
                                        }
                                        color={
                                            projRealMargin && projRealMargin < 0
                                                ? 'red'
                                                : undefined
                                        }
                                        description="Difference between the project budget and the current cost."
                                    />
                                    <LabeledValue
                                        label={t('Estimated Margin')}
                                        value={
                                            '€ ' +
                                            format(
                                                projEstimatedMargin,
                                                '#,###.00'
                                            )
                                        }
                                        color={
                                            projEstimatedMargin &&
                                            projEstimatedMargin < 0
                                                ? 'red'
                                                : undefined
                                        }
                                        description="Difference between the project budget and the allocated costs."
                                    />
                                </Stack>
                            </Stack>

                            <Grid item xs={12}>
                                <Divider />
                            </Grid>

                            <Box display="flex" justifyContent="flex-start">
                                <Box minWidth={700} height={400} marginTop={2}>
                                    {weeklyDates && burnUpChartData ? (
                                        <GenericChart
                                            title={t('Burn Up Chart')}
                                            labels={weeklyDates!}
                                            datasets={burnUpChartData!}
                                            width={700}
                                            height={400}
                                            leftAxisLabel={t(
                                                'Weekly Costs (€)'
                                            )}
                                            rightAxisLabel="Cumulative Cost (€)"
                                            legend={legendProps}
                                        />
                                    ) : (
                                        ''
                                    )}
                                </Box>
                                <Box
                                    width={600}
                                    height={400}
                                    marginInline={10}
                                    marginTop={2}>
                                    {weeklyDates && burnDownChartData ? (
                                        <GenericChart
                                            title={t('Burn Down Chart')}
                                            labels={weeklyDates!}
                                            datasets={burnDownChartData!}
                                            width={700}
                                            height={400}
                                            leftAxisLabel={t(
                                                'Remaining Budget (€)'
                                            )}
                                            rightAxisLabel={t(
                                                'Weekly Costs (€)'
                                            )}
                                            legend={legendProps}
                                        />
                                    ) : (
                                        ''
                                    )}
                                </Box>
                            </Box>

                            <Box width="100%" marginTop={2} overflow="auto">
                                <Tabs
                                    value={tabSelected}
                                    onChange={handleTabChange}>
                                    <Tab
                                        id="tab-0"
                                        label={t('Project Allocations')}
                                    />
                                    <Tab
                                        id="tab-1"
                                        label={t('Weekly Project Costs')}
                                    />
                                    <Tab
                                        id="tab-2"
                                        label={t('Monthly Project Costs')}
                                    />
                                    <Tab
                                        id="tab-3"
                                        label={t('User Time Reports')}
                                    />
                                </Tabs>
                                <CustomTabPanel value={tabSelected} index={0}>
                                    <Stack
                                        alignItems="row"
                                        direction="row"
                                        spacing={2}>
                                        <Typography
                                            variant="h6"
                                            fontWeight="bold"
                                            marginBottom={1}>
                                            {t('Project Allocations')}
                                        </Typography>
                                        <Box>
                                            <Button
                                                variant="outlined"
                                                color="info"
                                                onClick={
                                                    handleNewProjectAssignment
                                                }
                                                disabled={
                                                    project
                                                        ? project!.status!
                                                              .name ===
                                                          'FINISHED'
                                                        : true
                                                }>
                                                {t('Create New Assignment')}
                                            </Button>
                                        </Box>
                                        <Box>
                                            <Button
                                                variant="outlined"
                                                color="info"
                                                onClick={handleCloseProject}
                                                disabled={
                                                    project
                                                        ? project!.status!
                                                              .name ===
                                                          'FINISHED'
                                                        : true
                                                }>
                                                {t('Close Project')}
                                            </Button>
                                        </Box>
                                    </Stack>
                                    {projUserAllocations &&
                                    projUserAllocations.length ? (
                                        <GanttChart
                                            rowLabel={'User'}
                                            rows={projUsers!}
                                            showOptionalColumn={true}
                                            optionalColumnLabel="Task"
                                            columns={projWeeks!}
                                            data={ganttData!}
                                            columnMode="weekly"
                                            onClickAction={handleGanttRowClick}
                                        />
                                    ) : (
                                        ''
                                    )}
                                </CustomTabPanel>
                                <CustomTabPanel value={tabSelected} index={1}>
                                    <Stack
                                        direction={{ xs: 'column', sm: 'row' }}
                                        spacing={2}>
                                        <Combobox
                                            size="small"
                                            label={t('Users')}
                                            multiple
                                            entries={usersList!}
                                            onChangeMultiple={newValue => {
                                                const ids = newValue.map(
                                                    entry => entry.id
                                                );
                                                fetchUsersList();
                                                setSelectedUsers(ids);
                                            }}
                                            value={selectedUsers}
                                            sx={{ width: 400 }}
                                        />
                                        <LocalizationProvider
                                            dateAdapter={AdapterDayjs}>
                                            <DatePicker
                                                label={t('StartDate')}
                                                defaultValue={
                                                    startDateWeek
                                                        ? dayjs(startDateWeek)
                                                        : null
                                                }
                                                onChange={evt => {
                                                    setStartDateWeek(
                                                        evt?.toDate()
                                                    );
                                                }}
                                                format="DD/MM/YYYY"
                                                slotProps={{
                                                    textField: {
                                                        size: 'small',
                                                    },
                                                    field: {
                                                        clearable: true,
                                                        onClear: () =>
                                                            setStartDateWeek(
                                                                undefined
                                                            ),
                                                    },
                                                }}
                                            />
                                            <DatePicker
                                                label={t('EndDate')}
                                                defaultValue={
                                                    endDateWeek
                                                        ? dayjs(endDateWeek)
                                                        : null
                                                }
                                                onChange={evt => {
                                                    setEndDateWeek(
                                                        evt?.toDate()
                                                    );
                                                }}
                                                format="DD/MM/YYYY"
                                                slotProps={{
                                                    textField: {
                                                        size: 'small',
                                                    },
                                                    field: {
                                                        clearable: true,
                                                        onClear: () =>
                                                            setEndDateWeek(
                                                                undefined
                                                            ),
                                                    },
                                                }}
                                            />
                                        </LocalizationProvider>
                                    </Stack>
                                    <DataTable<ProjectCostByWeekMap>
                                        columns={[
                                            {
                                                field: 'user.name',
                                                title: t('User'),
                                            },
                                            {
                                                field: 'startWeek',
                                                title: t('Start Week'),
                                                formatter: value => {
                                                    if (!value) return '';
                                                    return format(
                                                        new Date(value),
                                                        'yyyy-MM-dd'
                                                    ); // Formats ISO string to "YYYY-MM-DD"
                                                },
                                            },
                                            {
                                                field: 'hours',
                                                title: t('Hours'),
                                            },
                                            {
                                                field: 'cost',
                                                title: t('Cost (€)'),
                                            },
                                        ]}
                                        initialPage={0}
                                        initialRowsPerPage={10}
                                        initialData={projCostByWeek}
                                        fetcher={(page, rowsPerPage, filters) =>
                                            fetchCostsDetails(
                                                'week',
                                                page,
                                                rowsPerPage,
                                                filters
                                            )
                                        }
                                    />
                                </CustomTabPanel>
                                <CustomTabPanel value={tabSelected} index={2}>
                                    <Stack
                                        direction={{ xs: 'column', sm: 'row' }}
                                        spacing={2}>
                                        <Combobox
                                            size="small"
                                            label={t('Users')}
                                            multiple
                                            entries={usersList!}
                                            onChangeMultiple={newValue => {
                                                const ids = newValue.map(
                                                    entry => entry.id
                                                );
                                                fetchUsersList();
                                                setSelectedUsers(ids);
                                            }}
                                            value={selectedUsers}
                                            sx={{ width: 400 }}
                                        />
                                        <LocalizationProvider
                                            dateAdapter={AdapterDayjs}>
                                            <DatePicker
                                                label={t('StartDate')}
                                                defaultValue={
                                                    startDateWeek
                                                        ? dayjs(startDateWeek)
                                                        : null
                                                }
                                                onChange={evt => {
                                                    setStartDateWeek(
                                                        evt?.toDate()
                                                    );
                                                }}
                                                format="DD/MM/YYYY"
                                                slotProps={{
                                                    textField: {
                                                        size: 'small',
                                                    },
                                                    field: {
                                                        clearable: true,
                                                        onClear: () =>
                                                            setStartDateWeek(
                                                                undefined
                                                            ),
                                                    },
                                                }}
                                            />
                                            <DatePicker
                                                label={t('EndDate')}
                                                defaultValue={
                                                    endDateWeek
                                                        ? dayjs(endDateWeek)
                                                        : null
                                                }
                                                onChange={evt => {
                                                    setEndDateWeek(
                                                        evt?.toDate()
                                                    );
                                                }}
                                                format="DD/MM/YYYY"
                                                slotProps={{
                                                    textField: {
                                                        size: 'small',
                                                    },
                                                    field: {
                                                        clearable: true,
                                                        onClear: () =>
                                                            setEndDateWeek(
                                                                undefined
                                                            ),
                                                    },
                                                }}
                                            />
                                        </LocalizationProvider>
                                    </Stack>
                                    <DataTable<ProjectCostByWeekMap>
                                        columns={[
                                            {
                                                field: 'user.name',
                                                title: t('User'),
                                            },
                                            {
                                                field: 'month',
                                                title: t('Month'),
                                                formatter: value => {
                                                    if (!value) return '';
                                                    return format(
                                                        new Date(value),
                                                        'yyyy-MM-dd'
                                                    ); // Formats ISO string to "YYYY-MM-DD"
                                                },
                                            },
                                            {
                                                field: 'hours',
                                                title: t('Hours'),
                                            },
                                            {
                                                field: 'cost',
                                                title: t('Cost (€)'),
                                            },
                                        ]}
                                        initialPage={0}
                                        initialRowsPerPage={10}
                                        initialData={projCostByMonth}
                                        fetcher={(page, rowsPerPage, filters) =>
                                            fetchCostsDetails(
                                                'month',
                                                page,
                                                rowsPerPage,
                                                filters
                                            )
                                        }
                                    />
                                </CustomTabPanel>
                                <CustomTabPanel value={tabSelected} index={3}>
                                    <Stack
                                        direction={{ xs: 'column', sm: 'row' }}
                                        spacing={2}>
                                        <Combobox
                                            size="small"
                                            label={t('Users')}
                                            multiple
                                            entries={usersList!}
                                            onChangeMultiple={newValue => {
                                                const ids = newValue.map(
                                                    entry => entry.id
                                                );
                                                fetchUsersList();
                                                setSelectedUsers(ids);
                                            }}
                                            value={selectedUsers}
                                            sx={{ width: 400 }}
                                        />
                                        <LocalizationProvider
                                            dateAdapter={AdapterDayjs}>
                                            <DatePicker
                                                label={t('StartDate')}
                                                defaultValue={
                                                    startDateDay
                                                        ? dayjs(startDateDay)
                                                        : null
                                                }
                                                onChange={evt => {
                                                    setStartDateDay(
                                                        evt?.toDate()
                                                    );
                                                }}
                                                format="DD/MM/YYYY"
                                                slotProps={{
                                                    textField: {
                                                        size: 'small',
                                                    },
                                                    field: {
                                                        clearable: true,
                                                        onClear: () =>
                                                            setStartDateDay(
                                                                undefined
                                                            ),
                                                    },
                                                }}
                                            />
                                            <DatePicker
                                                label={t('EndDate')}
                                                defaultValue={
                                                    endDateDay
                                                        ? dayjs(endDateDay)
                                                        : null
                                                }
                                                onChange={evt => {
                                                    setEndDateDay(
                                                        evt
                                                            ?.endOf('day')
                                                            .toDate()
                                                    );
                                                }}
                                                format="DD/MM/YYYY"
                                                slotProps={{
                                                    textField: {
                                                        size: 'small',
                                                    },
                                                    field: {
                                                        clearable: true,
                                                        onClear: () =>
                                                            setEndDateDay(
                                                                undefined
                                                            ),
                                                    },
                                                }}
                                            />
                                        </LocalizationProvider>
                                        <Button
                                            component="label"
                                            variant="outlined"
                                            startIcon={<CloudDownload />}
                                            sx={{ marginBottom: 2 }}
                                            onClick={handleExportTimeRecords}>
                                            {t('Export Project Time Records')}
                                        </Button>
                                    </Stack>
                                    <DataTable<ProjectCostByWeekMap>
                                        columns={[
                                            {
                                                field: 'userName',
                                                title: t('User'),
                                            },
                                            {
                                                field: 'start_date',
                                                title: t('Start Date'),
                                                formatter: value => {
                                                    if (!value) return '';
                                                    return format(
                                                        new Date(value),
                                                        'yyyy-MM-dd'
                                                    );
                                                },
                                            },
                                            {
                                                field: 'task',
                                                title: t('Task'),
                                            },
                                            {
                                                field: 'description',
                                                title: t('Description'),
                                            },
                                            {
                                                field: 'hours',
                                                title: t('Hours'),
                                            },
                                            {
                                                field: 'cost',
                                                title: t('Cost (€)'),
                                            },
                                        ]}
                                        initialPage={0}
                                        initialRowsPerPage={10}
                                        initialData={projCostByWeek}
                                        fetcher={fetchCostsDetailsByDay}
                                    />
                                </CustomTabPanel>
                            </Box>
                        </Grid>
                    </Grid>
                </>
            )}
            <ProjectAssignmentModal apiRef={projectAssignModalRef} />
            {confirmation.dialog}
        </>
    );
}

function getMondaysBetweenDates(startDate: Date, endDate: Date) {
    const result = [];

    // Normalize to Monday of the start week
    const startMonday = new Date(startDate);
    const day = startMonday.getDay(); // 0 (Sun) - 6 (Sat)

    // Move back to Monday (0 -> -6, 1 -> 0, 2 -> -1, ..., 6 -> -5)
    const diffToMonday = (day + 6) % 7;
    startMonday.setDate(startMonday.getDate() - diffToMonday);
    startMonday.setHours(0, 0, 0, 0);

    // Loop and collect all Mondays until end date
    const current = new Date(startMonday);
    while (current <= endDate) {
        result.push(new Date(current));
        current.setDate(current.getDate() + 7);
    }

    return result;
}

function mapProjectCostsToGanttData(
    projCosts: ProjectCostByWeekMap[]
): GanttData[] {
    return (
        projCosts
            .slice()
            // sort by oldest date to proper display on gantt chart
            .sort((a, b) => (a.startWeek! > b.startWeek! ? 1 : -1))
            .map(pc => ({
                rowId: pc.user!.id,
                start: new Date(pc.startWeek!),
                end: addWeeks(pc.startWeek!, 1),
                label1: pc.user!.name,
                label2: pc.cost.toString() + '€',
            }))
    );
}

function mapProjectAssignmentsToGanttData(
    projAssignments: ProjectAssignment[]
): GanttData[] {
    return (
        projAssignments
            .slice()
            // sort by oldest date to proper display on gantt chart
            .sort((a, b) => a.startDate!.getTime() - b.startDate!.getTime())
            .map(pa => ({
                rowId: pa.user!.id,
                start: new Date(pa.startDate!),
                end: pa.endDate!,
                label1: pa.description,
                label2: pa.allocation?.toString() + '%',
                optionalCol: pa.description,
                onClickId: pa.id,
            }))
    );
}

function mapProjectAssignmentsToUsersList(
    projAssignments: ProjectAssignment[]
): Row[] {
    const seen = new Set<number>();
    const uniqueUsers: Row[] = [];

    for (const pa of projAssignments) {
        const userId = pa.user!.id;
        if (!seen.has(userId)) {
            seen.add(userId);
            uniqueUsers.push({
                id: userId,
                name: pa.user!.name,
            });
        }
    }

    return uniqueUsers;
}

function buildCostsAndCumulative(
    labels: string[],
    rawCostsByWeek: ProjectCostByWeekMap[]
): {
    normalizedCosts: number[];
    cumulativeCosts: number[];
} {
    // Map costs to label date strings
    const costMap = new Map(
        rawCostsByWeek.map(entry => [
            formatDateToLabel(entry.startWeek!),
            entry.cost,
        ])
    );

    const normalizedCosts: number[] = [];
    const cumulativeCosts: number[] = [];

    let cumulative = 0;

    for (const label of labels) {
        const cost = costMap.get(label) ?? 0;
        normalizedCosts.push(cost);
        cumulative += cost;
        cumulativeCosts.push(cumulative);
    }

    return { normalizedCosts, cumulativeCosts };
}

function buildRemainingBudget(
    labels: string[],
    rawCostsByWeek: RawWeeklyData[],
    startingBudget: number
): {
    remainingBudget: number[];
    currentCost: number;
} {
    const costMap = new Map(
        rawCostsByWeek.map(entry => [
            formatDateToLabel(entry.startWeek),
            entry.cost,
        ])
    );
    const remainingBudget: number[] = [];
    let currentCost = 0;

    let budget = startingBudget;

    for (const label of labels) {
        const cost = costMap.get(label) ?? 0;
        budget -= cost;
        currentCost += cost;
        remainingBudget.push(budget);
    }

    return { remainingBudget, currentCost };
}
