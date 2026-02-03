import {
    Autocomplete,
    Box,
    Button,
    createFilterOptions,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    TextField,
    Theme,
    Tooltip,
    Typography,
} from '@mui/material';
import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import { useSession } from 'next-auth/react';
import EditIcon from '@mui/icons-material/Edit';
import SaveIcon from '@mui/icons-material/Save';
import { useEffect, useRef, useState } from 'react';
import {
    startOfWeek,
    addDays,
    subWeeks,
    addWeeks,
    format,
    isWeekend,
    addHours,
    startOfDay,
    endOfDay,
    isBefore,
    isSameWeek,
    isAfter,
} from 'date-fns';
import ProjectApi, { Project } from '../lib/projects';
import { useToast } from '@/components/toast-provider';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import IconButton from '@mui/material/IconButton';
import TimeRecordApi, { TimeRecord } from '../lib/time-records';
import AddIcon from '@mui/icons-material/Add';
import { colors } from '@/theme';
import StatusApi, { Status } from '../lib/status';
import SystemSettingApi from '../lib/system-settings';
import EditNoteIcon from '@mui/icons-material/EditNote';
import ProjectTaskApi, { ProjectTask } from '../lib/project-tasks';
import { ServiceError } from '@/lib/service-base';
import HolidayApi from '../lib/holidays';
import { ErrorMessageModal } from '../components/error-message-modal';
import { WarningAmberOutlined } from '@mui/icons-material';

interface ProjectTaskPair {
    project: Project;
    task: ProjectTask;
}

export const SetTimeRecordComponent = ({ onSave }: { onSave?: () => void }) => {
    const { t } = useTranslation(undefined, 'timesense');
    const { data: session } = useSession();
    const { toast } = useToast();
    const [selectedProjectTaskPairs, setSelectedProjectTaskPairs] = useState<
        ProjectTaskPair[]
    >([]);
    const [selectedProjectId, setSelectedProjectId] = useState<
        number | undefined
    >(undefined);
    const [selectedTaskId, setSelectedTaskId] = useState<number | undefined>(
        undefined
    );
    const currentTimeRecordRef = useRef<TimeRecord[]>([]);
    const [allProjects, setAllProjects] = useState<Project[]>([]);
    const [pageLock, setPageLock] = useState(false);
    const [startDate, setStartDate] = useState(() => {
        const date = startOfWeek(new Date(), { weekStartsOn: 1 });
        date.setHours(9, 0, 0, 0);
        return date;
    });
    const [timeRecords, setTimeRecords] = useState<Record<string, TimeRecord>>(
        {}
    );
    const [editedCells, setEditedCells] = useState<Set<string>>(new Set());
    const [pendingStatus, setPendingStatus] = useState<Status>();
    const [draftStatus, setDraftStatus] = useState<Status>();
    const [maxHoursPerDay, setMaxHoursPerDay] = useState<number | undefined>();
    const [allowWeekends, setAllowWeekends] = useState<boolean | undefined>();
    const [projectTaskList, setProjectTaskList] = useState<ProjectTask[]>([]);
    const [holidaysDates, setHolidaysDates] = useState<Date[]>([]);

    const [selectedTimeRecordIds, setSelectedTimeRecordIds] = useState<number[]>([]);
    const [editingRow, setEditingRow] = useState<string | null>(null);
    const [editValues, setEditValues] = useState<{ project: Project; task: ProjectTask } | null>(null);

    const [errorModalOpen, setErrorModalOpen] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');
    const [errorDetails, setErrorDetails] = useState<string[]>([]);

    const currentWeekStart = startOfWeek(new Date(), { weekStartsOn: 1 });
    const [maxPrevWeeks, setMaxPrevWeeks] = useState<number>();
    const maxPastDate = subWeeks(
        currentWeekStart,
        maxPrevWeeks ? maxPrevWeeks : 5
    );
    // Limit max {maxPastDate} weeks before current week
    const disablePrev =
        !isBefore(startDate, maxPastDate) && isSameWeek(startDate, maxPastDate);
    // Current week is the max the user can navigate to the future
    const disableNext =
        isSameWeek(startDate, currentWeekStart) ||
        isAfter(startDate, currentWeekStart);

    const [projectTypeTasks, setProjectTypeTasks] = useState<ProjectTask[]>([]);

    const getWeekDates = () => {
        const dates: Date[] = [];
        for (let i = 0; i < 7; i++) {
            dates.push(addDays(startDate, i));
        }
        return dates;
    };
    const weekDates = getWeekDates();

    const handlePrevWeek = () => {
        setStartDate(prev => subWeeks(prev, 1));
    };

    const handleNextWeek = () => {
        setStartDate(prev => addWeeks(prev, 1));
    };

    const openErrorModal = (error: { message: string; details: string[] }) => {
        setErrorMessage(error.message);
        setErrorDetails(error.details);
        setErrorModalOpen(true);
    };

    const handleHourChange = (
        project: Project,
        task: ProjectTask | undefined,
        date: Date,
        value: string
    ) => {
        const key = `${project.id}_${task?.id || 'no-task'}_${format(date, 'yyyy-MM-dd')}`;
        const numericHours = parseFloat(value);

        setTimeRecords(prev => {
            const newRecords = { ...prev };
            if (!isNaN(numericHours) && numericHours >= 0) {
                newRecords[key] = {
                    project: { id: project.id, name: project.name },
                    task: task ? { id: task.id, name: task.name } : undefined,
                    hours: numericHours,
                    startDate: new Date(date),
                    endDate: addHours(new Date(date), numericHours),
                    status: draftStatus, // New/Updated records are marked as Draft
                    description: prev[key]?.description || '',
                };
            } else {
                newRecords[key] = {
                    project: { id: project.id, name: project.name },
                    task: task ? { id: task.id, name: task.name } : undefined,
                    hours: 0,
                    startDate: new Date(date),
                    endDate: addHours(new Date(date), 0),
                    status: draftStatus, // New/Updated records are marked as Draft
                    description: prev[key]?.description || '',
                };
            }
            return newRecords;
        });

        setEditedCells(prev => new Set(prev).add(key));
        setPageLock(true);

        // Update currentTimeRecordRef
        currentTimeRecordRef.current = Object.values(timeRecords).filter(
            record =>
                !(
                    record.project.id === project.id &&
                    (!record.task || record.task.id === task?.id) &&
                    format(new Date(record.startDate), 'yyyy-MM-dd') ===
                    format(date, 'yyyy-MM-dd')
                )
        );
        if (!isNaN(numericHours) && numericHours >= 0) {
            currentTimeRecordRef.current.push({
                project: { id: project.id, name: project.name },
                task: task ? { id: task.id, name: task.name } : undefined,
                hours: numericHours,
                startDate: new Date(date),
                endDate: addHours(new Date(date), numericHours),
                status: draftStatus,
            });
        } else {
            currentTimeRecordRef.current.push({
                project: { id: project.id, name: project.name },
                task: task ? { id: task.id, name: task.name } : undefined,
                hours: 0,
                startDate: new Date(date),
                endDate: addHours(new Date(date), 0),
                status: draftStatus,
            });
        }
    };

    const fetchPendingStatus = async () => {
        if (!session) return;
        const statusApi = new StatusApi(session);
        try {
            const data = await statusApi.getAllStatus(
                0,
                1000,
                'name',
                'name=PENDING'
            );
            setPendingStatus(data.content[0]);
        } catch (e) {
            console.error('Failed to load pending status', e);
        }
    };

    const fetchDraftStatus = async () => {
        if (!session) return;
        const statusApi = new StatusApi(session);
        try {
            const data = await statusApi.getAllStatus(
                0,
                1000,
                'name',
                'name=DRAFT'
            );
            setDraftStatus(data.content[0]);
        } catch (e) {
            console.error('Failed to load drfat status', e);
        }
    };

    const fetchProjectList = async () => {
        if (!session) return;
        const projectsApi = new ProjectApi(session);
        try {
            const data = await projectsApi.getProjects(
                0,
                1000,
                'name',
                'status.name=OPEN'
            );
            setAllProjects(
                data.content.map(e => ({
                    id: e.id!,
                    name: e.name!,
                    description: e.description!,
                }))
            );
        } catch (e) {
            console.error('Failed to load projects', e);
        }
    };

    const fetchInitialProjects = async () => {
        if (!session) return;
        const projectsApi = new ProjectApi(session);
        try {
            const data = await projectsApi.getLastUsedProjectsByUser();
            // Initialize with empty task pairs
            setSelectedProjectTaskPairs([]);
        } catch (e) {
            console.error('Failed to load initial projects', e);
        }
    };

    const fetchTimeRecords = async () => {
        if (!session) return;
        const timeRecordApi = new TimeRecordApi(session);
        try {
            const endDate = addDays(startDate, 6);
            const startDateStr = format(
                startOfDay(startDate),
                'yyyy-MM-dd HH:mm:ss'
            );
            const endDateStr = format(endOfDay(endDate), 'yyyy-MM-dd HH:mm:ss');

            const records = (
                await timeRecordApi.getTimeRecordsByUserAndDate(
                    startDateStr,
                    endDateStr
                )
            ).data;

            const newRecords: Record<string, TimeRecord> = {};
            const newProjectTaskPairs: ProjectTaskPair[] = [];

            records.forEach(record => {
                const taskId = record.task?.id || 'no-task';
                const key = `${record.project.id}_${taskId}_${format(record.startDate, 'yyyy-MM-dd')}`;
                newRecords[key] = {
                    ...record,
                    startDate: new Date(record.startDate),
                    endDate: new Date(record.endDate),
                    status: {
                        id: record.status!.id,
                        name: record.status ? record.status.name : 'Pending',
                    },
                    task: record.task
                        ? { id: record.task.id, name: record.task.name }
                        : undefined,
                };

                // Add project-task pair if task exists and not already added
                if (record.task) {
                    const pairExists = newProjectTaskPairs.some(
                        pair =>
                            pair.project.id === record.project.id &&
                            pair.task.id === record.task!.id
                    );
                    if (!pairExists) {
                        newProjectTaskPairs.push({
                            project: {
                                id: record.project.id,
                                name: record.project.name,
                                description: record.project.description,
                            },
                            task: {
                                id: record.task.id,
                                name: record.task.name,
                            },
                        });
                    }
                }
            });

            setTimeRecords(newRecords);
            currentTimeRecordRef.current = Object.values(newRecords);
            setSelectedProjectTaskPairs(prev => [
                ...prev,
                ...newProjectTaskPairs.filter(
                    newPair =>
                        !prev.some(
                            prevPair =>
                                prevPair.project.id === newPair.project.id &&
                                prevPair.task.id === newPair.task.id
                        )
                ),
            ]);
            setEditedCells(new Set());
        } catch (e) {
            console.error('Failed to load time records', e);
            toast('Failed to load time records');
        }
    };

    const fetchProjectTasks = async () => {
        if (!session) return;
        const projectTaskApi = new ProjectTaskApi(session);
        try {
            const data = await projectTaskApi.getProjectTasks(0, 1000, 'id');
            setProjectTaskList(
                data.content.map(e => ({
                    id: e.id!,
                    name: e.name!,
                }))
            );
        } catch (e) {
            console.error('Failed to load project tasks', e);
            toast('Failed to load project tasks');
        }
    };

    const fetchSystemSettings = async () => {
        if (!session) return;
        const api = new SystemSettingApi(session!);
        try {
            const settings = (await api.getSystemSettings(0, 25)).content;
            const setttingMaxHours = settings.find(
                e => e.name === 'project_max_hours_per_day'
            )?.value;
            const settingAllowWeekends = settings.find(
                e => e.name === 'time_records_allow_weekends'
            )?.value;
            setMaxHoursPerDay(
                setttingMaxHours ? parseInt(setttingMaxHours) : 8
            );
            setAllowWeekends(settingAllowWeekends === 'true');
        } catch (e) {
            console.error('Failed to load system settings', e);
            toast('Failed to load system settings');
        }
    };

    const fetchYearSetting = async () => {
        try {
            const sysSettingsApi = new SystemSettingApi(session!);
            const setting = await sysSettingsApi.getSystemSettingByName(
                'default_time_records_prev_weeks'
            );
            const value = setting?.value;
            if (value) {
                setMaxPrevWeeks(parseInt(value));
            }
        } catch (error) {
            console.error(
                'Error fetching default_time_records_prev_weeks setting:',
                error
            );
        }
    };

    const fetchHolidays = async () => {
        try {
            const holidayApi = new HolidayApi(session!);
            const holidays = await holidayApi.getHolidays();
            setHolidaysDates(holidays.map(h => new Date(h.holidayDate)));
        } catch {
            toast(t('Error fetching holidays'));
        }
    };

    useEffect(() => {
        // fetchInitialProjects();
        fetchProjectList();
        fetchPendingStatus();
        fetchDraftStatus();
        fetchSystemSettings();
        fetchProjectTasks();
        fetchYearSetting();
        setPageLock(false);
        fetchHolidays();
    }, []);

    useEffect(() => {
        fetchTimeRecords();
    }, [startDate]);

    const handleDiscardTimeRecords = async () => {
        if (!session) return;

        setEditedCells(new Set());
        fetchTimeRecords();
        setPageLock(false);
    };

    const handleSaveTimeRecords = async () => {
        if (!session) return;
        const timeRecordApi = new TimeRecordApi(session);
        // Ensure saving the latest timeRecords
        const recordsToSave = Object.values(timeRecords);

        setPageLock(false);
        try {
            await timeRecordApi.createTimeRecordsDraft(recordsToSave);

            toast('Time records saved successfully');
            setEditedCells(new Set()); // Clear edited cells after saving

            if (onSave) onSave();
        } catch (e) {
            if (e instanceof ServiceError) {
                const c = await e.getContentAsJson();
                const parsed = typeof c === 'string' ? JSON.parse(c) : c;
                console.log('Message:', parsed.message);
                toast(parsed.message);
            } else {
                console.error('Failed to save time records', e);
                toast('Failed to save time records');
            }
        }
    };

    const handleSubmitTimeRecords = async () => {
        if (!session) return;
        const timeRecordApi = new TimeRecordApi(session);
        // Ensure submiting the latest timeRecords
        const recordsToSave = Object.values(timeRecords);

        setPageLock(false);
        try {
            await timeRecordApi.createTimeRecordsSubmit(recordsToSave);

            toast('Time records submited successfully');
            setEditedCells(new Set()); // Clear edited cells after submiting

            await fetchTimeRecords(); // Refetch records to update

            if (onSave) onSave();
        } catch (e) {
            if (e instanceof ServiceError) {
                const c = await e.getContentAsJson();
                const parsed = typeof c === 'string' ? JSON.parse(c) : c;
                console.log('Message:', parsed.message);
                toast(parsed.message);
            } else {
                console.error('Failed to submit time records', e);
                toast('Failed to submit time records');
            }
        }
    };

    const availableProjects = allProjects.filter(project => {
        // Check if there are any tasks for this project that haven't been selected yet
        const selectedTasksForProject = selectedProjectTaskPairs
            .filter(pair => pair.project.id === project.id)
            .map(pair => pair.task.id);
        return projectTaskList.some(
            task => !selectedTasksForProject.includes(task.id)
        );
    });

    const handleAddProjectAndTask = () => {
        const selectedProject = allProjects.find(
            p => p.id === selectedProjectId
        );
        const selectedTask = projectTaskList.find(t => t.id === selectedTaskId);
        if (selectedProject && selectedTask) {
            const newPair: ProjectTaskPair = {
                project: selectedProject,
                task: selectedTask,
            };
            setSelectedProjectTaskPairs(prev => {
                if (
                    prev.some(
                        pair =>
                            pair.project.id === selectedProject.id &&
                            pair.task.id === selectedTask.id
                    )
                ) {
                    return prev;
                }
                return [...prev, newPair];
            });
            setSelectedProjectId(undefined);
            setSelectedTaskId(undefined);
        }
    };

    const getCellBackgroundColor = (key: string, theme: Theme) => {
        if (editedCells.has(key)) return theme.palette.tableCell?.draft;
        if (timeRecords[key]) {
            switch (timeRecords[key].status!.name) {
                case 'APPROVED':
                    return theme.palette.tableCell?.approved;
                case 'DENIED':
                    return colors.businessTrip;
                case 'PENDING':
                    return theme.palette.tableCell?.pending;
                case 'DRAFT':
                    return theme.palette.tableCell?.draft;
                default:
                    return '#d4edda';
            }
        }
        return 'inherit';
    };

    const resetDescriptionEdit = () =>
        setDescriptionEdit({
            open: false,
            projectId: null,
            taskId: null,
            date: null,
            value: '',
        });

    const [descriptionEdit, setDescriptionEdit] = useState({
        open: false,
        projectId: null as number | null,
        taskId: null as number | null,
        date: null as Date | null,
        value: '',
    });

    const handleDescriptionOpen = (
        projectId: number,
        taskId: number | null,
        date: Date
    ) => {
        const key = `${projectId}_${taskId || 'no-task'}_${format(date, 'yyyy-MM-dd')}`;
        setDescriptionEdit({
            open: true,
            projectId,
            taskId,
            date,
            value: timeRecords[key]?.description || '',
        });
    };

    const handleDescriptionSave = (
        projectId: number,
        taskId: number | undefined,
        date: Date
    ) => {
        const key = `${projectId}_${taskId || 'no-task'}_${format(date, 'yyyy-MM-dd')}`;
        setTimeRecords(prev => {
            const updated = { ...prev };
            if (updated[key]) {
                updated[key] = {
                    ...updated[key],
                    description: descriptionEdit.value,
                };
            }
            return updated;
        });

        resetDescriptionEdit();
    };

    // function to handle the search of projects by the project name or the project description
    const projectFilterOptions = createFilterOptions({
        stringify: (option: Project) =>
            `${option.name} ${option.description ?? ''}`,
    });

    const handleEditOrSave = async (project: Project, task: ProjectTask) => {
        const rowKey = `${project.id}_${task.id}`;

        if(!session) return;

        if (editingRow === rowKey && editValues) {
            try{

                if (!editValues.project.id || !editValues.task.id) {
                    console.error('Project or Task not selected');
                    return;
                }

                const timeRecordApi = new TimeRecordApi(session);

                await timeRecordApi.updateProjectAndTask(
                    selectedTimeRecordIds,
                    editValues!.project.id,
                    editValues!.task.id
                );

                setEditingRow(null);
                setEditValues(null);
                setSelectedTimeRecordIds([]);
                toast('Updated successfully!');

                await fetchTimeRecords();

                return;

            } catch (e) {
                if (e instanceof ServiceError) {
                    const c = await e.getContentAsJson();
                    const parsed = typeof c === 'string' ? JSON.parse(c) : c;

                if (parsed?.messageCode === "API_TIME_RECORD_409_01") {
                    openErrorModal({
                        message: parsed.message || 'Error occurred',
                        details: [],
                    });
                } else {
                    toast(parsed?.message || 'Operation completed');
                }
            }
        }

        return;
        }

        const projectApi = new ProjectApi(session);
        try {
            const data = await projectApi.getProjectTasksByProjectId(project.id!);
            const typeTasks = data.data.map(e => ({
                id: e.id!,
                name: e.name!,
            }));
            setProjectTypeTasks(typeTasks);
        } catch (e) {
            console.error('Failed to load project tasks', e);
            toast('Failed to load project tasks');
        }

        setEditingRow(rowKey);
        setEditValues({ project, task});

        const ids = weekDates
            .map(date => {
                const key = `${project.id}_${task.id}_${format(date, "yyyy-MM-dd")}`;
                return timeRecords[key]?.id;
            })
            .filter(id => id !== undefined);

        setSelectedTimeRecordIds(ids);
    };

    const handleSelectProject = async (newValue: Project) => {
        setSelectedProjectId(newValue ? newValue.id : undefined);
        if (!session) return;
        const projectTaskApi = new ProjectTaskApi(session);
        const projectApi = new ProjectApi(session!);
        try {
            if (newValue !== null) {

                const data = await projectApi.getProjectTasksByProjectId(newValue.id!);
                const typeTasks = data.data.map(e => ({
                    id: e.id!,
                    name: e.name!,
                }));
                setProjectTypeTasks(typeTasks);
            }
        } catch (e) {
            console.error('Failed to load project tasks', e);
            toast('Failed to load project tasks');
        }
    };

    return (
        <Box>
            <Stack
                direction="row"
                justifyContent="space-between"
                alignItems="center"
                mb={2}>
                <Typography variant="h6">{t('Weekly Timesheet')}</Typography>
                <Stack direction="row" spacing={1}>
                    <IconButton
                        onClick={handlePrevWeek}
                        size="small"
                        disabled={pageLock || disablePrev}>
                        <ChevronLeftIcon />
                    </IconButton>
                    <IconButton
                        onClick={handleNextWeek}
                        size="small"
                        disabled={pageLock || disableNext}>
                        <ChevronRightIcon />
                    </IconButton>
                </Stack>
            </Stack>

            <TableContainer sx={{ overflowX: 'auto', minWidth: '1000px' }}>
                <Table size="small" stickyHeader>
                    <TableHead>
                        <TableRow>
                            <TableCell
                                sx={theme => ({
                                    backgroundColor:
                                        theme.palette.tableHeader?.main,
                                    width: 50,
                                    minWidth: 50,
                                    maxWidth: 50,
                                    fontWeight: 'bold',
                                })}>
                                {t('')}
                            </TableCell>
                            <TableCell
                                sx={theme => ({
                                    backgroundColor:
                                        theme.palette.tableHeader?.main,
                                    minWidth: 150,
                                    maxWidth: 230,
                                    fontWeight: 'bold',
                                })}>
                                {t('Project')}
                            </TableCell>
                            <TableCell
                                sx={theme => ({
                                    backgroundColor:
                                        theme.palette.tableHeader?.main,
                                    minWidth: 150,
                                    fontWeight: 'bold',
                                })}>
                                {t('Task')}
                            </TableCell>
                            {weekDates.map((date, i) => (
                                <TableCell
                                    key={i}
                                    sx={theme => ({
                                        textAlign: 'center',
                                        backgroundColor:
                                            theme.palette.tableHeader?.light,
                                        minWidth: 80,
                                    })}>
                                    <Typography variant="caption">
                                        {t(format(date, 'EEE dd/MM'))}
                                    </Typography>
                                </TableCell>
                            ))}
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {selectedProjectTaskPairs.map(({ project, task }) => (
                            <TableRow key={`${project.id}_${task.id}`}>
                                <TableCell
                                    sx={{
                                        textAlign: 'center',
                                        backgroundColor: theme => theme.palette.tableCell?.dark,
                                    }}>
                                    <IconButton size="small" onClick={() => handleEditOrSave(project, task)}>
                                        {editingRow === `${project.id}_${task.id}` ? (
                                            <SaveIcon fontSize="small" />
                                        ) : (
                                            <EditIcon fontSize="small" />
                                        )}
                                    </IconButton>
                                </TableCell>

                                <TableCell
                                    sx={theme => ({
                                        fontWeight: 'bold',
                                        backgroundColor:
                                            theme.palette.tableCell?.dark,
                                        maxWidth: 230,
                                        overflow: 'hidden',
                                        textOverflow: 'ellipsis',
                                    })}>
                                    {editingRow === `${project.id}_${task.id}` ? (
                                        <Autocomplete
                                            value={availableProjects.find(p => p.id === selectedProjectId) || project}
                                            onChange={(event, newValue) => setEditValues(prev => prev ? { ...prev, project: newValue! } : null)
                                            }
                                            options={availableProjects}
                                            getOptionLabel={option => option.name}
                                            renderInput={params => <TextField {...params} size="small" />}
                                        />
                                    ) : (
                                    <Box
                                        sx={{
                                            display: 'flex',
                                            flexDirection: 'column',
                                            maxWidth: 230,
                                        }}>
                                        <Typography
                                            variant="body1"
                                            fontWeight="500"
                                            sx={{
                                                whiteSpace: 'nowrap',
                                                overflow: 'hidden',
                                                textOverflow: 'ellipsis',
                                            }}>
                                            {project.name}
                                        </Typography>

                                        {project.description && (
                                            <Typography
                                                variant="body2"
                                                color="text.secondary"
                                                sx={{
                                                    whiteSpace: 'normal',
                                                    wordBreak: 'break-word',
                                                    maxWidth: 300,
                                                    cursor: 'default',
                                                }}>
                                                {project.description}
                                            </Typography>
                                        )}
                                    </Box>
                                    )}
                                </TableCell>
                                <TableCell
                                    sx={theme => ({
                                        backgroundColor:
                                            theme.palette.tableCell?.dark,
                                        maxWidth: 222,
                                        overflow: 'hidden',
                                        textOverflow: 'ellipsis',
                                    })}>
                                    {editingRow === `${project.id}_${task.id}` ? (
                                        <Autocomplete
                                            value={projectTypeTasks.find(t => t.id === selectedTaskId) || task}
                                            onChange={(event, newValue) => setEditValues(prev => prev ? { ...prev, task: newValue! } : null)
                                        }
                                            options={projectTypeTasks}
                                            getOptionLabel={option => option.name}
                                            renderInput={params => <TextField {...params} size="small" />}
                                        />
                                    ) : (
                                    <Tooltip title={task.name}>
                                        <Typography
                                            variant="body2"
                                            sx={{
                                                whiteSpace: 'nowrap',
                                                overflow: 'hidden',
                                                textOverflow: 'ellipsis',
                                                maxWidth: 222,
                                                cursor: 'default',
                                            }}>
                                            {task.name}
                                        </Typography>
                                    </Tooltip>
                                    )}
                                </TableCell>
                                {weekDates.map(date => {
                                    const key = `${project.id}_${task.id}_${format(date, 'yyyy-MM-dd')}`;
                                    const weekend = isWeekend(date);
                                    const isHoliday = holidaysDates.some(
                                        holiday =>
                                            format(holiday, 'yyyy-MM-dd') ===
                                            format(date, 'yyyy-MM-dd')
                                    );
                                    const disableDay =
                                        (weekend || isHoliday) &&
                                        !allowWeekends;
                                    return (
                                        // Time Records Hours and Desc Cells
                                        <TableCell
                                            key={key}
                                            sx={theme => ({
                                                backgroundColor: disableDay
                                                    ? theme.palette.tableCell
                                                        ?.weekend
                                                    : theme.palette.tableCell
                                                        ?.default,
                                                position: 'relative',
                                            })}>
                                            <Stack
                                                direction="row"
                                                spacing={0.5}
                                                alignItems="center">
                                                <TextField
                                                    type="number"
                                                    inputProps={{
                                                        min: 0,
                                                        max: maxHoursPerDay,
                                                        style: {
                                                            textAlign: 'center',
                                                        },
                                                    }}
                                                    value={
                                                        timeRecords[key]
                                                            ?.hours ?? ''
                                                    }
                                                    onChange={e =>
                                                        handleHourChange(
                                                            project,
                                                            task,
                                                            date,
                                                            e.target.value
                                                        )
                                                    }
                                                    variant="outlined"
                                                    size="small"
                                                    fullWidth
                                                    sx={theme => ({
                                                        'backgroundColor':
                                                            getCellBackgroundColor(
                                                                key,
                                                                theme
                                                            ),
                                                        '& input': {
                                                            padding: '4px',
                                                        },
                                                        'borderRadius': 1,
                                                    })}
                                                    disabled={
                                                        timeRecords[key]?.status
                                                            ?.name ===
                                                        'APPROVED' ||
                                                        disableDay
                                                    }
                                                />
                                                <IconButton
                                                    size="small"
                                                    onClick={() =>
                                                        handleDescriptionOpen(
                                                            project.id!,
                                                            task.id!,
                                                            date
                                                        )
                                                    }
                                                    sx={{ p: 0.5 }}
                                                    disabled={
                                                        timeRecords[key]?.status
                                                            ?.name ===
                                                        'APPROVED' ||
                                                        disableDay
                                                    }>
                                                    <EditNoteIcon fontSize="small" />
                                                </IconButton>
                                            </Stack>
                                            {timeRecords[key]?.status?.name === 'DENIED' &&
                                                timeRecords[key]?.reason &&
                                                <Tooltip title={timeRecords[key]?.reason}>
                                                    <Box sx={{
                                                        position: 'absolute',
                                                        top: 2,
                                                        right: 2,
                                                        width: 0,
                                                        height: 0,
                                                        borderLeft: '12px solid transparent',
                                                        borderTop: '12px solid red',
                                                        display: 'flex',
                                                        alignItems: 'center',
                                                        justifyContent: 'center',
                                                        cursor: 'pointer',
                                                        zIndex: 10,
                                                    }}>
                                                        &nbsp;
                                                    </Box>
                                                </Tooltip>
                                            }
                                        </TableCell>
                                    );
                                })}
                            </TableRow>
                        ))}
                        {currentTimeRecordRef.current.length > 0 && (
                            <TableRow
                                sx={theme => ({
                                    backgroundColor:
                                        theme.palette.tableCell?.dark,
                                    fontWeight: 'bold',
                                })}>
                                <TableCell
                                    sx={{
                                        backgroundColor: 'inherit',
                                        fontWeight: 'bold',
                                        textAlign: 'start',
                                    }}>
                                    {t('Total')}
                                </TableCell>
                                <TableCell
                                    sx={{
                                        backgroundColor: 'inherit',
                                    }}
                                />
                                <TableCell sx={{ backgroundColor: 'inherit' }} />
                                {weekDates.map(date => {
                                    const dateKey = format(date, 'yyyy-MM-dd');
                                    // sum all hours for this date
                                    const totalForDate = Object.entries(
                                        timeRecords
                                    )
                                        .filter(([key]) =>
                                            key.endsWith(dateKey)
                                        )
                                        .reduce(
                                            (sum, [, record]) =>
                                                sum +
                                                (record?.hours
                                                    ? Number(record.hours)
                                                    : 0),
                                            0
                                        );

                                    return (
                                        <TableCell
                                            key={`total_${dateKey}`}
                                            sx={theme => ({
                                                textAlign: 'center',
                                                fontWeight: 'bold',
                                                backgroundColor: 'inherit',
                                                color:
                                                    totalForDate > 0
                                                        ? theme.palette.text
                                                            .primary
                                                        : theme.palette.text
                                                            .disabled,
                                                borderBottom: `2px solid ${theme.palette.divider}`,
                                            })}>
                                            {totalForDate ?? 0}
                                        </TableCell>
                                    );
                                })}
                                <TableCell sx={{ backgroundColor: 'inherit' }} />
                            </TableRow>
                        )}

                        <TableRow>
                            <TableCell sx={theme => ({ backgroundColor: theme.palette.tableCell?.dark })} />
                            <TableCell colSpan={weekDates.length + 2}>
                                <Stack
                                    direction="row"
                                    spacing={2}
                                    alignItems="center">
                                    <Autocomplete
                                        value={
                                            availableProjects.find(
                                                p => p.id === selectedProjectId
                                            ) || null
                                        }
                                        onChange={(event, newValue) =>
                                            handleSelectProject(newValue!)
                                        }
                                        options={availableProjects}
                                        getOptionLabel={option => option.name}
                                        renderInput={params => (
                                            <TextField
                                                {...params}
                                                placeholder={t(
                                                    'Select project'
                                                )}
                                                size="small"
                                                sx={{ minWidth: 230 }}
                                            />
                                        )}
                                        filterOptions={projectFilterOptions}
                                        renderOption={(props, option) => (
                                            <li {...props} key={option.id}>
                                                <Box
                                                    sx={{
                                                        display: 'flex',
                                                        flexDirection: 'column',
                                                        minWidth: 220,
                                                    }}>
                                                    <Typography
                                                        variant="body1"
                                                        fontWeight="500">
                                                        {option.name}
                                                    </Typography>
                                                    {option.description && (
                                                        <Tooltip
                                                            title={
                                                                option.description
                                                            }
                                                            placement="top-start">
                                                            <Typography
                                                                variant="body2"
                                                                color="text.secondary"
                                                                sx={{
                                                                    whiteSpace:
                                                                        'nowrap',
                                                                    overflow:
                                                                        'hidden',
                                                                    textOverflow:
                                                                        'ellipsis',
                                                                    maxWidth: 200,
                                                                    cursor: 'default',
                                                                }}>
                                                                {
                                                                    option.description
                                                                }
                                                            </Typography>
                                                        </Tooltip>
                                                    )}
                                                </Box>
                                            </li>
                                        )}
                                    />
                                    <Autocomplete
                                        value={
                                            projectTypeTasks.find(
                                                t => t.id === selectedTaskId
                                            ) || null
                                        }
                                        onChange={(event, newValue) =>
                                            setSelectedTaskId(
                                                newValue
                                                    ? newValue.id
                                                    : undefined
                                            )
                                        }
                                        options={projectTypeTasks}
                                        getOptionLabel={option => option.name}
                                        renderInput={params => (
                                            <TextField
                                                {...params}
                                                placeholder={t('Select task')}
                                                size="small"
                                                sx={{ minWidth: 200 }}
                                            />
                                        )}
                                        isOptionEqualToValue={(option, value) =>
                                            option.id === value.id
                                        }
                                        disableClearable={false}
                                        disabled={
                                            selectedProjectId === undefined
                                        }
                                    />
                                    <IconButton
                                        onClick={handleAddProjectAndTask}
                                        disabled={
                                            !selectedProjectId ||
                                            !selectedTaskId
                                        }>
                                        <AddIcon />
                                    </IconButton>
                                </Stack>
                            </TableCell>
                        </TableRow>
                    </TableBody>
                </Table>

                {/* Description edit dialog */}
                <Dialog
                    open={descriptionEdit.open}
                    onClose={() =>
                        setDescriptionEdit({ ...descriptionEdit, open: false })
                    }
                    fullWidth={true}
                    maxWidth={'md'}>
                    <DialogTitle>{t('Task Description')}</DialogTitle>
                    <DialogContent>
                        <TextField
                            fullWidth
                            multiline
                            minRows={3}
                            value={descriptionEdit.value}
                            onChange={e =>
                                setDescriptionEdit({
                                    ...descriptionEdit,
                                    value: e.target.value,
                                })
                            }
                            placeholder={t('Describe the work done...')}
                            inputProps={{ maxLength: 400 }}
                            helperText={` ${t('Maximum 400 characters allowed')} ${descriptionEdit.value.length}/400`}
                        />
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={resetDescriptionEdit}>
                            {t('Cancel')}
                        </Button>
                        <Button
                            onClick={() => {
                                handleDescriptionSave(
                                    descriptionEdit.projectId!,
                                    descriptionEdit.taskId!,
                                    descriptionEdit.date!
                                );
                            }}
                            variant="contained">
                            {t('OK')}
                        </Button>
                    </DialogActions>
                </Dialog>
            </TableContainer>
            <Stack
                direction="row"
                spacing={2}
                marginTop={1}
                justifyContent="flex-end">
                <Button variant="outlined" onClick={handleSaveTimeRecords}>
                    {t('Save week')}
                </Button>

                <Button variant="outlined" onClick={handleSubmitTimeRecords}>
                    {t('Submit')}
                </Button>

                <Button variant="outlined" onClick={handleDiscardTimeRecords}>
                    {t('Discard')}
                </Button>
            </Stack>
            <ErrorMessageModal
                open={errorModalOpen}
                onClose={() => setErrorModalOpen(false)}
                message={errorMessage}
                details={errorDetails}
                title={t('Error Editing Time Records')}
            />
        </Box>
    );
};
