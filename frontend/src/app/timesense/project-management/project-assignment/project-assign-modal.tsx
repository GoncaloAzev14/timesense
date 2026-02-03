'use client';

import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import {
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Stack,
    TextField,
    FormHelperText,
} from '@mui/material';
import { useEffect, useRef, useState } from 'react';
import { useModal } from '@datacentric/datacentric-ui/lib/browser-utils';
import { useSession } from 'next-auth/react';
import Combobox, {
    Entry,
} from '@datacentric/datacentric-ui/components/combobox';
import { validateNotEmpty } from '@/components/validation-utils';
import ProjectApi from '../../lib/projects';
import UserApi from '../../lib/users';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import dayjs from 'dayjs';
import PermissionsApi from '../../lib/permissions';
import ProjectAssignmentApi, {
    ProjectAssignment,
} from '../../lib/project-assignment';

interface ProjectAssignmentModalApi {
    openDialog: (
        action: 'create' | 'edit' | 'view',
        projectAssignment?: ProjectAssignment
    ) => Promise<ProjectAssignment | undefined>;
    closeDialog: (save: boolean) => void;
}

interface ProjectAssignmentModalProps {
    apiRef: React.MutableRefObject<ProjectAssignmentModalApi | null>;
}

export function useProjectAssignmentModal() {
    const apiRef = useRef<ProjectAssignmentModalApi>({
        openDialog: async () => {
            return undefined;
        },
        closeDialog: () => {},
    });
    return apiRef;
}

export function ProjectAssignmentModal({
    apiRef,
}: ProjectAssignmentModalProps) {
    const { data: session, update } = useSession();
    const [currentAction, setCurrentAction] = useState('create');
    const [
        isDialogOpen,
        openProjectAssignmentDialog,
        closeProjectAssignmentDialog,
    ] = useModal();
    const [project, setProject] = useState<string | undefined>(undefined);
    const [user, setUser] = useState<string | undefined>(undefined);
    const currentProjectAssignRef = useRef<ProjectAssignment | null>(null);
    const { t } = useTranslation(undefined, 'timesense');
    const [canCreateProjectAssignments, setCanCreateProjectAssignments] =
        useState(false);
    const [projectsList, setProjectsList] = useState<Entry[] | null>([]);
    const [usersList, setUsersList] = useState<Entry[] | null>([]);

    const [userError, setUserError] = useState(false);
    const [projectError, setProjectError] = useState(false);
    const [allocationError, setAllocationError] = useState(false);
    const [startDateError, setStartDateError] = useState(false);

    // force render
    const [, setUpdate] = useState(0);
    const forceRender = () => setUpdate(prev => prev + 1);

    const handleProjectChange = (evt: Entry) => {
        currentProjectAssignRef.current!.project = {
            id: evt.id,
            name: evt.name,
        };
        setTimeout(() => forceRender(), 100);
        setProject(evt.name);
        setProjectError(false);
    };

    const handleUserChange = (evt: Entry) => {
        currentProjectAssignRef.current!.user = {
            id: evt.id,
            name: evt.name,
        };
        setTimeout(() => forceRender(), 100);
        setUser(evt.name);
        setUserError(false);
    };

    // TODO: Use project permissions instead of system permission
    useEffect(() => {
        (async () => {
            const api = new PermissionsApi(session!);
            setCanCreateProjectAssignments(
                (await api.getSystemPermissions()).includes('CREATE_PROJECTS')
            );
        })();
    }, [session]);

    apiRef.current = {
        openDialog: async (action, projectAssign) => {
            currentProjectAssignRef.current = projectAssign || {
                id: 0,
                user: undefined,
                project: undefined,
                allocation: 0,
            };
            setCurrentAction(action);
            setUserError(false);
            setProjectError(false);
            setAllocationError(false);
            setStartDateError(false);

            if (action === 'edit' || action === 'create') {
                await loadFormOptions();
            }

            const result = await openProjectAssignmentDialog();
            if (result) {
                const api = new ProjectAssignmentApi(session!);
                if (action === 'view') {
                    return await api.getProjectAssignment(
                        currentProjectAssignRef.current.id!
                    );
                } else if (action === 'edit') {
                    return (
                        await api.updateProjectAssignment(
                            currentProjectAssignRef.current!
                        )
                    ).data;
                } else {
                    if (currentProjectAssignRef.current.endDate == undefined) {
                        currentProjectAssignRef.current.endDate = new Date();
                    }
                    return (
                        await api.createProjectAssignment(
                            currentProjectAssignRef.current!
                        )
                    ).data;
                }
            }
            return undefined;
        },
        closeDialog: () => {
            closeProjectAssignmentDialog(false);
        },
    };

    const loadFormOptions = async () => {
        if (!session) {
            return;
        }

        const projectApi = new ProjectApi(session!);
        const userApi = new UserApi(session!);
        try {
            projectApi.getProjects(0, 1000, 'name,id').then(data => {
                setProjectsList(
                    data.content.map(e => ({
                        id: e.id!,
                        name: e.name!,
                    }))
                );
            });
        } catch (e) {
            console.error('Failed to load projects', e);
        }
        try {
            userApi.getUsers(0, 1000, 'name,id').then(data => {
                setUsersList(
                    data.content.map(e => ({
                        id: e.id!,
                        name: e.name!,
                    }))
                );
            });
        } catch (e) {
            console.error('Failed to load users', e);
        }
    };

    const handleApply = () => {
        const isUserValid = validateNotEmpty(
            currentProjectAssignRef.current?.user?.name,
            setUserError
        );
        const isProjectValid = validateNotEmpty(
            currentProjectAssignRef.current?.project?.name,
            setProjectError
        );
        const isAllocationValid = validateNotEmpty(
            currentProjectAssignRef.current?.project?.name,
            setProjectError
        );
        const isStartDateValid = validateNotEmpty(
            currentProjectAssignRef.current?.startDate?.toString(),
            setProjectError
        );

        if (
            isUserValid &&
            isProjectValid &&
            isAllocationValid &&
            isStartDateValid
        ) {
            closeProjectAssignmentDialog(true);
        } else {
            console.log(
                'Validation failed',
                isUserValid,
                isProjectValid,
                isAllocationValid,
                isStartDateValid
            );
        }
    };

    return (
        <Dialog
            open={isDialogOpen}
            onClose={() => closeProjectAssignmentDialog(false)}
            fullWidth>
            <DialogTitle>
                {currentAction === 'create'
                    ? t('Create Project Assignment')
                    : currentAction === 'view'
                      ? t('View Project Assignment')
                      : t('Edit Project Assignment')}
            </DialogTitle>
            <DialogContent>
                <Stack direction="column" spacing={2}>
                    <TextField
                        label={t('Id')}
                        variant="standard"
                        fullWidth
                        value={currentProjectAssignRef.current?.id}
                        disabled
                    />
                    <Combobox
                        label={t('Users')}
                        value={currentProjectAssignRef.current?.user?.id}
                        onChange={handleUserChange}
                        entries={usersList!}
                    />
                    {userError && (
                        <FormHelperText error>
                            {t('Please select an user!')}
                        </FormHelperText>
                    )}
                    <Combobox
                        label={t('Projects')}
                        value={currentProjectAssignRef.current?.project?.id}
                        onChange={handleProjectChange}
                        entries={projectsList!}
                    />
                    {userError && (
                        <FormHelperText error>
                            {t('Please select a project!')}
                        </FormHelperText>
                    )}
                    <TextField
                        label={t('Allocation')}
                        variant="standard"
                        fullWidth
                        type="number"
                        defaultValue={
                            currentProjectAssignRef.current?.allocation
                        }
                        onChange={evt => {
                            currentProjectAssignRef.current!.allocation =
                                parseFloat(evt.target.value);
                        }}
                        InputProps={{ inputProps: { min: 0, max: 100 } }}
                    />
                    <TextField
                        label={t('Description')}
                        variant="standard"
                        fullWidth
                        defaultValue={
                            currentProjectAssignRef.current?.description
                        }
                        onChange={evt => {
                            currentProjectAssignRef.current!.description =
                                evt.target.value;
                        }}
                    />
                    <LocalizationProvider dateAdapter={AdapterDayjs}>
                        <DatePicker
                            label={t('StartDate')}
                            defaultValue={
                                currentProjectAssignRef.current?.startDate
                                    ? dayjs(
                                          currentProjectAssignRef.current
                                              ?.startDate
                                      )
                                    : dayjs(new Date())
                            }
                            onChange={evt => {
                                currentProjectAssignRef.current!.startDate =
                                    evt?.toDate();
                            }}
                            format="DD/MM/YYYY"
                        />
                        <DatePicker
                            label={t('EndDate')}
                            defaultValue={
                                currentProjectAssignRef.current?.endDate
                                    ? dayjs(
                                          currentProjectAssignRef.current
                                              ?.endDate
                                      )
                                    : dayjs(new Date())
                            }
                            onChange={evt => {
                                currentProjectAssignRef.current!.endDate =
                                    evt?.toDate();
                            }}
                            format="DD/MM/YYYY"
                        />
                    </LocalizationProvider>
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button onClick={() => closeProjectAssignmentDialog(false)}>
                    {t('Cancel')}
                </Button>
                <Button onClick={handleApply}>{t('Save')}</Button>
            </DialogActions>
        </Dialog>
    );
}
