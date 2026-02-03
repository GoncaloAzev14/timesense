'use client';

import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useSession } from 'next-auth/react';
import { Box, Button, Fab, FormControl, FormControlLabel, FormGroup, FormLabel, Stack, Switch, ToggleButton, ToggleButtonGroup, Typography } from '@mui/material';
import ProjectApi, { Project } from '../../lib/projects';
import {
    DataTable,
    useDataTableApiRef,
} from '@datacentric/datacentric-ui/components/datatable';
import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import { emptyPage } from '@datacentric/datacentric-ui/lib/spring-pagination';
import { useProjectModal, ProjectModal } from './project-modal';
import useConfirmation from '@datacentric/datacentric-ui/components/confirmation';
import { useDebounced } from '@datacentric/datacentric-ui/lib/browser-utils';
import { useToast } from '@/components/toast-provider';
import PermissionsApi from '../../lib/permissions';
import { ServiceError } from '@/lib/service-base';
import usePermissionsModal from '../../components/permissions-modal';
import { useRouter } from 'next/navigation';
import Loading from '@/components/load';
import Combobox, { Entry } from '@datacentric/datacentric-ui/components/combobox';
import UserApi, { User } from '../../lib/users';
import ClientApi, { Client } from '../../lib/clients';
import StatusApi, { Status } from '../../lib/status';
import { FileUpload } from '@datacentric/datacentric-ui/components/file-upload';
import { ErrorMessageModal } from '../../components/error-message-modal';

const PROJECT_PERMISSION_TYPES = ['EDIT_PROJECTS', 'RECORD_TIME_PROJECTS', 'TIME_APPROVAL'];

export default function ProjectsPage() {
    const USERS_PAGE_SIZE = 100;
    const DEBOUNCE_INTERVAL_MS = 200;
    const USERS_SCOPE_COMPANY = "SCOPE-COMPANY";
    const PROJECTS_SCOPE_USER = "SCOPE-USER";
    const PROJECTS_SCOPE_COMPANY = "SCOPE-COMPANY";

    const { data: session, update } = useSession();
    const { t } = useTranslation(undefined, 'timesense');
    const projectModalRef = useProjectModal();
    const dataTableApiRef = useDataTableApiRef<Project>();
    const [currentSort, setCurrentSort] = useState<string>('name');
    const confirmation = useConfirmation();
    const { toast } = useToast();
    const modalOpen = useRef(false);
    const [runningCreate, setRunningCreate] = useState(false);
    const [canCreateProjects, setCanCreateProjects] = useState(false);
    const permsModal = usePermissionsModal();
    const router = useRouter();
    const [userInfo, setUserInfo] = useState<User>();
    const [isManager, setIsManager] = useState(false);
    // page loading
    const [isLoading, setIsLoading] = useState(false);
    const [seeUserProjects, setSeeUserProjects] = useState(false);

    // component loading
    const [loading, setLoading] = useState(false);
    const [clients, setClients] = useState<Client[] | null>(null);
    const [clientsList, setClientsList] = useState<Entry[]>([]);
    const [selectedClients, setSelectedClients] = useState<number[]>([]);

    const [managers, setManagers] = useState<User[] | null>(null);
    const [managersList, setManagersList] = useState<Entry[]>([]);
    const [selectedManagers, setSelectedManagers] = useState<number[]>([]);

    const [status, setStatus] = useState<Status[] | null>(null);
    const [statusList, setStatusList] = useState<Entry[]>([]);
    const [selectedStatus, setSelectedStatus] = useState<number[]>([]);

    const debouncedStatus = useDebounced(selectedStatus, DEBOUNCE_INTERVAL_MS);
    const debouncedManagers = useDebounced(selectedManagers, DEBOUNCE_INTERVAL_MS);
    const debouncedClients = useDebounced(selectedClients, DEBOUNCE_INTERVAL_MS);

    const fetchStatus = async (page: number) => {
        if (!session) {
            return;
        }
        setLoading(true);
        const statusApi = new StatusApi(session!);
        try {

            const data = await statusApi.getAllStatus(page, USERS_PAGE_SIZE, 'name', 'type!=Other');

            const newStatus = data.content.map(e => ({
                id: e.id!,
                name: e.name!,
            }));

            setStatusList(prev => {
                const existingIds = new Set(prev.map(status => status.id));
                const uniqueNewStatus = newStatus.filter(
                    status => !existingIds.has(status.id)
                );
                return [...prev, ...uniqueNewStatus];
            });
        } catch (e) {
            console.error('Failed to load status', e);
        } finally {
            setLoading(false);
        }
    };

    const fetchStatusList = async () => {
        if (selectedStatus[0] !== undefined) {
            const statusApi = new StatusApi(session!);
            try {
                // Convert number[] → comma-separated string
                const ids = `(${selectedStatus.map(String).join(",")})`;
                const res = (
                    (await statusApi.getAllStatus(0, 100, undefined, `id=${ids}`)).content
                )
                setStatus(res)
            } catch (e) {
                console.error('Failed to load status list!', e);
            }
        } else {
            setStatus(null);
        }
    };

    const fetchClients = async (page: number) => {
        if (!session) {
            return;
        }
        setLoading(true);
        const clientsApi = new ClientApi(session!);
        try {
            const data = await clientsApi.getClients(page, USERS_PAGE_SIZE, 'name');

            const newClients = data.content.map(e => ({
                id: e.id!,
                name: e.name!,
            }));

            setClientsList(prev => {
                // filter out duplicates in case there are multiple calls to the useEffect
                const existingIds = new Set(prev.map(user => user.id));
                const uniqueNewClients = newClients.filter(
                    client => !existingIds.has(client.id)
                );
                return [...prev, ...uniqueNewClients];
            });
        } catch (e) {
            console.error('Failed to load clients', e);
        } finally {
            setLoading(false);
        }
    };

    const fetchClientsList = async () => {
        if (selectedClients[0] !== undefined) {
            const clientApi = new ClientApi(session!);
            try {
                const ids = `(${selectedClients.map(String).join(",")})`;
                const res = (
                    (await clientApi.getClients(0, 100, undefined, `id=${ids}`)).content
                );
                setClients(res);
            } catch (e) {
                console.error('Failed to load clients list!', e);
            }
        } else {
            setClients(null);
        }
    };

    const fetchManagers = async (page: number) => {
        if (!session) {
            return;
        }
        setLoading(true);
        const usersApi = new UserApi(session!);
        try {
            const data = await usersApi.getUsers(page, USERS_PAGE_SIZE, 'name', 'userRoles.name=Manager', USERS_SCOPE_COMPANY);

            const newUsers = data.content.map(e => ({
                id: e.id!,
                name: e.name!,
            }));

            setManagersList(prev => {
                const existingIds = new Set(prev.map(user => user.id));
                const uniqueNewUsers = newUsers.filter(
                    user => !existingIds.has(user.id)
                );
                return [...prev, ...uniqueNewUsers];
            });
        } catch (e) {
            console.error('Failed to load managers', e);
        } finally {
            setLoading(false);
        }
    };

    const fetchManagersList = async () => {
        if (selectedManagers[0] !== undefined) {
            const userApi = new UserApi(session!);
            try {
                const ids = `(${selectedManagers.map(String).join(",")})`;

                const res = (
                    (await userApi.getUsers(0, 100, undefined, `id=${ids}`, USERS_SCOPE_COMPANY)).content
                )
                setManagers(res);
            } catch (e) {
                console.error('Failed to load managers list!', e);
            }
        } else {
            setManagers(null);
        }
    };

    useEffect(() => {
        fetchClients(0);
        fetchManagers(0);
        fetchStatus(0);
    }, [session]);

    const handleCreateProject = useCallback(async () => {
        if (!canCreateProjects && !isManager) {
            return;
        }

        if (modalOpen.current) {
            return;
        }

        modalOpen.current = true;
        try {
            const result = await projectModalRef.current.openDialog('create', {
                id: 0,
                name: '',
                client: { id: 0, name: '' },
            });
            if (result) {
                toast(t('Project created'));
                dataTableApiRef.current?.refreshData();
            }
        } finally {
            modalOpen.current = false;
        }
    }, [projectModalRef, dataTableApiRef, t, canCreateProjects, toast]);

    const handleEditProject = async (e: Project) => {
        if (modalOpen.current) {
            return false;
        }

        const projectApi = new ProjectApi(session!);
        const project = await projectApi.getProject(e.id!);
        modalOpen.current = true;
        try {
            const result = await projectModalRef.current.openDialog('edit', {
                ...project,
            });
            if (result) {
                toast(t('Project updated'));
                return true;
            }
            return false;
        } finally {
            modalOpen.current = false;
        }
    };

    const handleDeleteProject = async (e: Project) => {
        if (modalOpen.current) {
            return false;
        }

        modalOpen.current = true;
        try {
            if (
                !(await confirmation.ask(
                    t('Confirm deletion'),
                    t('Project deletion question', { name: e.name }),
                    t('Yes'),
                    t('No')
                ))
            ) {
                return false;
            }
            const api = new ProjectApi(session!);
            try {
                await api.deleteProject(e.id!);
                toast(t('Project deleted'));
                return true;
            } catch (e: any) {
                toast(e.messages);
                return false;
            }
        } finally {
            modalOpen.current = false;
        }
    };

    const handleCloseProject = async (e: Project) => {
        if (modalOpen.current) {
            return false;
        }

        modalOpen.current = true;
        try {
            if (
                !(await confirmation.ask(
                    t('Confirm close project'),
                    t('Close project question', { name: e.name }),
                    t('Yes'),
                    t('No')
                ))
            ) {
                return false;
            }
            const api = new ProjectApi(session!);
            try {
                await api.closeProject(e.id!);
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

    const handleViewDetails = async (e: Project) => {
        const projectId = e.id;
        if (projectId) {
            setIsLoading(true);
            router.push(`./projects/${projectId}`);
            return true;
        } else {
            toast(t('Failed to open project details'));
            return false;
        }
    };

    const handleEditProjectPermissions = async (e: Project) => {
        if (modalOpen.current) {
            return false;
        }
        try {
            modalOpen.current = true;
            const result = await permsModal.open(
                t('Permissions title', { name: e.name }),
                'Project',
                e.id!,
                PROJECT_PERMISSION_TYPES
            );
            if (result) {
                toast(t('Permissions updated'));
                return true;
            }
            return false;
        } finally {
            modalOpen.current = false;
        }
    };

    const projectPageFetch = useCallback(
        async (page: number, rowsPerPage: number, currentFilters: string | undefined) => {
            const api = new ProjectApi(session!);
            const scope = seeUserProjects ? PROJECTS_SCOPE_USER : PROJECTS_SCOPE_COMPANY;
            try {
                return api.getProjects(
                    page,
                    rowsPerPage,
                    currentSort,
                    currentFilters,
                    debouncedStatus,
                    debouncedClients,
                    debouncedManagers,
                    scope
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
                    return emptyPage<Project>();
                } else {
                    return emptyPage<Project>();
                }
            }
        },
        [session, update, currentSort, debouncedClients, debouncedManagers, debouncedStatus, seeUserProjects]
    );

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

    const handleImportProjects = async (
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
            const api = new ProjectApi(session!);
            const result = await api.importProjects(formData);

            if (result.messageCode !== 'API_PROJECT_201_01') {
                console.log(result);
                openErrorModal({
                    message: result.message,
                    details: result.data?.message_args || [],
                });
            } else {
                toast(result.data!.message_args[0]);
                dataTableApiRef.current?.refreshData();
            }
        } catch (e: any) {
            toast('Error importing projects!');
            console.log(e);
            openErrorModal({
                message: e.message || 'Unknown error',
                details: [e.responseText || ''],
            });
        } finally {
            event.target.value = '';
        }
    };

    const handleMyProjectsChange = (isUserProjects: boolean) => {
        setSeeUserProjects(isUserProjects);
    }

    useEffect(() => {
        (async () => {
            const api = new PermissionsApi(session!);
            setCanCreateProjects(
                (await api.getSystemPermissions()).includes('CREATE_PROJECTS')
            );
            const usersApi = new UserApi(session!);
            const user = await usersApi.getUserInfo();
            setUserInfo(user);
            setIsManager(
                user!.userRoles!.some(role => role.name === 'Manager')
            );

        })();
    }, [session]);

    return (
        <>
            <Stack direction="column" spacing={2}>

                <Typography variant="h3"> {t('Projects')}</Typography>
                <Typography>{t('Projects Page Text')}</Typography>
                <Stack direction="row" spacing={2} alignItems="center">
                    {(canCreateProjects || isManager) && (
                        <Button
                            variant='outlined'
                            onClick={async () => {
                                setRunningCreate(true);
                                await handleCreateProject();
                                setRunningCreate(false);
                            }}
                        >
                            {t('Add Project')}
                        </Button>
                    )}

                    {/* Filter Section */}
                    <Combobox
                        multiple
                        label={t("Clients")}
                        entries={clientsList || []}
                        onChangeMultiple={(newValue) => {
                            const ids = newValue.map((entry) => entry.id);
                            fetchClientsList();
                            setSelectedClients(ids);
                        }}
                        value={selectedClients}
                        sx={{ width: 330}}
                    />
                    <Combobox
                        multiple
                        label={t("Project Managers")}
                        entries={managersList || []}
                        onChangeMultiple={(newValue) => {
                            const ids = newValue.map((entry) => entry.id);
                            fetchManagersList();
                            setSelectedManagers(ids);
                        }}
                        value={selectedManagers}
                        sx={{ width: 330}}
                    />
                    <Combobox
                        multiple
                        label={t("Status")}
                        entries={statusList || []}
                        onChangeMultiple={(newValue) => {
                            const ids = newValue.map((entry) => entry.id);
                            fetchStatusList();
                            setSelectedStatus(ids);
                        }}
                        value={selectedStatus}
                        sx={{ width: 330}}
                    />
                    <ToggleButtonGroup
                        value={seeUserProjects ? "user" : "all"}
                        exclusive
                        onChange={(e, newValue) => {
                            if (newValue !== null) {
                                handleMyProjectsChange(newValue === "user");
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
                        <ToggleButton value="user">
                            {t("My Projects")}
                        </ToggleButton>

                        <ToggleButton value="all">
                            {t("All Projects")}
                        </ToggleButton>
                    </ToggleButtonGroup>
                    <Box sx={{ flexGrow: 1 }} />
                    {(canCreateProjects || isManager) && (
                        <FileUpload
                            disabled={(!canCreateProjects && !isManager)}
                            label={t('Import Projects')}
                            onUpload={handleImportProjects}
                        />
                    )}
                </Stack>
                <DataTable<Project>
                    columns={[
                        {
                            field: 'name',
                            title: t('Project Code'),
                            isLink: ((canCreateProjects || isManager)),
                            href: row => `./projects/${row.id}`,
                            filter: true,
                            filterMetadata: {
                                defaultOperator: '~',
                            },
                        },
                        {
                            field: 'description',
                            title: t('Project Name'),
                            filter: true,
                            filterMetadata: {
                                defaultOperator: '~',
                            },
                        },
                        { field: 'type.name', title: t('Project Type') },
                        { field: 'manager.name', title: t('Manager') },
                        { field: 'client.name', title: t('Client') },
                        { field: 'createdBy.name', title: t('Created By') },
                        { field: 'updatedBy.name', title: t('Updated By') },
                        { field: 'status.name', title: t('Status') },
                        {
                            field: 'expectedDueDate',
                            title: t('Expected Due Date'),
                            formatMask: 'yyyy-MM-dd',
                        },
                        {
                            field: 'updatedAt',
                            title: t('Last Modified'),
                            formatMask: 'timeago',
                            showToolTip: true,
                        },
                    ]}
                    apiRef={dataTableApiRef}
                    rowActions={[
                        {
                            label: t('Edit'),
                            icon: 'edit',
                            action: handleEditProject,
                            isEnabled: () => (canCreateProjects || isManager),
                            hideWhenDisabled: true
                        },
                        {
                            label: t('Delete'),
                            icon: 'delete',
                            action: handleDeleteProject,
                            isEnabled: () => (canCreateProjects || isManager),
                            hideWhenDisabled: true
                        },
                        {
                            label: t('View'),
                            icon: 'visibility',
                            action: handleViewDetails,
                            isEnabled: () => (canCreateProjects || isManager),
                            hideWhenDisabled: true
                        },
                        {
                            label: t('Permissions'),
                            icon: 'group',
                            action: handleEditProjectPermissions,
                            isEnabled: () => (canCreateProjects || isManager),
                            hideWhenDisabled: true
                        },
                        {
                            label: t('Close Project'),
                            icon: 'free_cancellation',
                            action: handleCloseProject,
                            isEnabled: row => (row.status?.name !== 'FINISHED' && (canCreateProjects || isManager)),
                            hideWhenDisabled: true,
                        },
                    ]}
                    fetcher={projectPageFetch}
                    currentSort={currentSort}
                    onSortChange={setCurrentSort}
                    initialPage={0}
                    initialRowsPerPage={10}
                />
            </Stack>
            <ErrorMessageModal
                open={errorModalOpen}
                onClose={closeErrorModal}
                message={errorMessage}
                details={errorDetails}
                title={t('Error importing projects')}
            />
            <ProjectModal apiRef={projectModalRef} />
            {permsModal.dialog((canCreateProjects || isManager))}
            {confirmation.dialog}
            {isLoading ? <Loading /> : ''}
        </>
    );
}
