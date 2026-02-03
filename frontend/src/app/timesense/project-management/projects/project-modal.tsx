"use client";

import { useTranslation } from "@datacentric/datacentric-ui/lib/i18n-client";
import {
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Stack,
    TextField,
    FormHelperText,
    Grid,
    Box,
} from "@mui/material";
import { useEffect, useRef, useState } from "react";
import { useModal } from "@datacentric/datacentric-ui/lib/browser-utils";
import { useSession } from "next-auth/react";
import Combobox, { Entry } from "@datacentric/datacentric-ui/components/combobox";
import { validateNotEmpty } from "@/components/validation-utils";
import { renderBasicField } from "../../components/common-column-renders";
import ProjectApi, { Project } from "../../lib/projects";
import ProjectTypeApi from "../../lib/project-types";
import UserApi from "../../lib/users";
import { DatePicker } from "@mui/x-date-pickers/DatePicker";
import { LocalizationProvider } from "@mui/x-date-pickers/LocalizationProvider/LocalizationProvider";
import { AdapterDayjs } from "@mui/x-date-pickers/AdapterDayjs";
import dayjs from "dayjs";
import ClientApi, { Client } from "../../lib/clients";
import StatusApi from "../../lib/status";
import ProjectTaskApi, { ProjectTask } from "../../lib/project-tasks";
import ChipField from "@datacentric/datacentric-ui/components/chipfield";

interface ProjectModalApi {
    openDialog: (
        action: "create" | "edit" | "view",
        project?: Project,
    ) => Promise<Project | undefined>;
    closeDialog: (save: boolean) => void;
}

interface ProjectModalProps {
    apiRef: React.MutableRefObject<ProjectModalApi | null>;
}

export function useProjectModal() {
    const apiRef = useRef<ProjectModalApi>({
        openDialog: async () => {
            return undefined;
        },
        closeDialog: () => {},
    });
    return apiRef;
}

export function ProjectModal({ apiRef }: ProjectModalProps) {
    const { data: session, update } = useSession();
    const [currentAction, setCurrentAction] = useState("create");
    const [isDialogOpen, openProjectDialog, closeProjectDialog] = useModal();
    const [projectType, setProjectType] = useState<string | undefined>(undefined);
    const [manager, setManager] = useState<string | undefined>(undefined);
    const [client, setClient] = useState<string | undefined>(undefined);
    const [status, setStatus] = useState<string | undefined>(undefined);
    const currentProjectRef = useRef<Project | null>(null);
    const { t } = useTranslation(undefined, "timesense");
    const [projectTypesList, setProjectTypesList] = useState<Entry[] | null>([]);
    const [managersList, setManagersList] = useState<Entry[] | null>([]);
    const [clientsList, setClientsList] = useState<Entry[] | null>([]);
    const [clientsObjList, setClientsObjList] = useState<Client[] | null>([]);
    const [statusList, setStatusList] = useState<Entry[] | null>([]);
    const [nameError, setNameError] = useState(false);
    const [projectTypeError, setProjectTypeError] = useState(false);
    const [projectManagerError, setProjectManagerError] = useState(false);
    const [projectClientError, setProjectClientError] = useState(false);
    const [projectCode, setProjectCode] = useState<string>("");
    const [projectTasksList, setProjectTasksList] = useState<ProjectTask[]>(
        currentProjectRef.current?.tasks || [],
    );

    useEffect(() => {
        if (
            currentProjectRef.current?.client !== undefined &&
            currentProjectRef.current?.name === undefined
        ) {
            setProjectCode(
                `${currentProjectRef.current?.client?.clientTicker || ""}_${dayjs().year()}_`,
            );
        }
    }, [currentProjectRef.current?.client]);

    // force render
    const [, setUpdate] = useState(0);
    const forceRender = () => setUpdate((prev) => prev + 1);

    const handleNameChange = async (evt: React.ChangeEvent<HTMLInputElement>) => {
        const newValue = evt.target.value;
        setProjectCode(newValue);
        currentProjectRef.current!.name = newValue;
        validateNotEmpty(newValue, setNameError);
        let existingProject = await new ProjectApi(session!).getProjectByCode(newValue);
        if (existingProject !== null && existingProject.id !== currentProjectRef.current?.id) {
            setNameError(true);
        }
        setTimeout(() => forceRender(), 100);
    };

    useEffect(() => {
        if (currentProjectRef.current?.tasks) {
            setProjectTasksList(currentProjectRef.current.tasks);
            setTimeout(() => forceRender(), 100);
        } else {
            setProjectTasksList([]);
        }
    }, [session, currentProjectRef.current]);

    const handleProjectTypeChange = async (evt: Entry) => {
        const projectTaskApi = new ProjectTaskApi(session!);
        currentProjectRef.current!.type = {
            id: evt.id,
            name: evt.name,
            description: "",
        };

        if (currentAction === "create") {
            // load tasks for that type
            const response = await projectTaskApi.getProjectTypeTasks(evt.id);
            const tasks = response.data;
            const defaultTasks = tasks.map((t) => ({
                id: t.id,
                name: t.name,
                description: t.description,
            }));

            currentProjectRef.current!.tasks = defaultTasks;
            setProjectTasksList(defaultTasks);
        }

        setTimeout(() => forceRender(), 100);
        setProjectType(evt.name);
        setProjectTypeError(false);
    };

    const handleManagerChange = (evt: Entry) => {
        currentProjectRef.current!.manager = {
            id: evt.id,
            name: evt.name,
        };
        setTimeout(() => forceRender(), 100);
        setManager(evt.name);
    };

    const handleClientChange = (evt: Entry) => {
        const selectedClient = clientsObjList!.find((client) => client.id === evt.id);
        if (!selectedClient) return;

        currentProjectRef.current!.client = {
            id: selectedClient.id,
            name: selectedClient.name,
            clientTicker: selectedClient.clientTicker,
        };

        // Generate prefix
        const prefix = `DC_${selectedClient.clientTicker || ""}_${dayjs().year()}_`;

        if (currentAction === "create") {
            setProjectCode(prefix);
            currentProjectRef.current!.name = prefix;
        }
        setTimeout(() => forceRender(), 100);
    };

    const handleStatusChange = (evt: Entry) => {
        currentProjectRef.current!.status = {
            id: evt.id,
            name: evt.name,
        };
        setTimeout(() => forceRender(), 100);
        setStatus(evt.name);
    };

    apiRef.current = {
        openDialog: async (action, project) => {
            currentProjectRef.current = project || {
                id: 0,
                name: "",
                description: "",
                type: { name: "", description: "" },
                manager: undefined,
                client: undefined,
                tasks: [],
            };
            setCurrentAction(action);
            setNameError(false);
            setProjectTypeError(false);

            if (action === "edit" || action === "create") {
                await loadFormOptions();
            }

            // Initialize the field value
            if (project?.name) {
                setProjectCode(project.name);
            } else if (project?.client?.clientTicker) {
                setProjectCode(`${project.client.clientTicker}_${dayjs().year()}_`);
            } else {
                setProjectCode("");
            }

            const result = await openProjectDialog();
            if (result) {
                const api = new ProjectApi(session!);
                if (action === "view") {
                    return await api.getProject(currentProjectRef.current.id!);
                } else if (action === "edit") {
                    return (await api.updateProject(currentProjectRef.current!)).data;
                } else {
                    return (await api.createProject(currentProjectRef.current!)).data;
                }
            }
            return undefined;
        },
        closeDialog: () => {
            closeProjectDialog(false);
        },
    };

    const loadFormOptions = async () => {
        if (!session) {
            return;
        }

        const projectTypeApi = new ProjectTypeApi(session!);
        const userApi = new UserApi(session!);
        const clientApi = new ClientApi(session!);
        const statusApi = new StatusApi(session!);
        try {
            projectTypeApi.getProjectTypes(0, 1000, "name,id").then((data) => {
                setProjectTypesList(
                    data.content.map((e) => ({
                        id: e.id!,
                        name: e.name!,
                    })),
                );
            });
        } catch (e) {
            console.error("Failed to load project types", e);
        }
        try {
            userApi.getUsers(0, 1000, "name,id", "userRoles.name=Manager").then((data) => {
                setManagersList(
                    data.content.map((e) => ({
                        id: e.id!,
                        name: e.name!,
                    })),
                );
            });
        } catch (e) {
            console.error("Failed to load managers", e);
        }
        try {
            clientApi.getClients(0, 1000, "name,id").then((data) => {
                setClientsList(
                    data.content.map((e) => ({
                        id: e.id!,
                        name: e.name!,
                    })),
                );
                setClientsObjList(
                    data.content.map((e) => ({
                        id: e.id!,
                        name: e.name!,
                        clientTicker: e.clientTicker,
                    })),
                );
            });
        } catch (e) {
            console.error("Failed to load clients", e);
        }
        try {
            const filter = "type!=Other";
            statusApi.getAllStatus(0, 1000, "name,id", filter).then((data) => {
                setStatusList(
                    data.content.map((e) => ({
                        id: e.id!,
                        name: e.name!,
                    })),
                );
            });
        } catch (e) {
            console.error("Failed to load status", e);
        }
    };

    const handleApply = async () => {
        let isNameValid = validateNotEmpty(currentProjectRef.current?.name, setNameError);

        if (!currentProjectRef.current?.name) {
            let project = await new ProjectApi(session!).getProjectByCode(projectCode);
            if (project && project.id !== currentProjectRef.current?.id) {
                setNameError(true);
                isNameValid = false;
            }
        }
        const isProjectTypeValid = validateNotEmpty(
            currentProjectRef.current?.type?.name,
            setProjectTypeError,
        );

        const isManagerValid = validateNotEmpty(
            currentProjectRef.current?.manager?.name,
            setProjectManagerError,
        );

        const isClientValid = validateNotEmpty(
            currentProjectRef.current?.client?.name,
            setProjectClientError,
        );

        if (isNameValid && isProjectTypeValid && isManagerValid && isClientValid) {
            closeProjectDialog(true);
        }
    };

    const handleClose = () => {
        closeProjectDialog(false);
    };

    return (
        <Dialog open={isDialogOpen} onClose={() => closeProjectDialog(false)} fullWidth>
            <DialogTitle>
                {currentAction === "create"
                    ? t("Create Project")
                    : currentAction === "view"
                      ? t("View Project")
                      : t("Edit Project")}
            </DialogTitle>
            {currentAction === "create" || currentAction === "edit" ? (
                <DialogContent>
                    <Stack direction="column" spacing={2}>
                        <TextField
                            label={t("Id")}
                            variant="standard"
                            fullWidth
                            value={currentProjectRef.current?.id}
                            disabled
                        />
                        <Combobox
                            label={t("Client")}
                            value={currentProjectRef.current?.client?.id || undefined}
                            onChange={handleClientChange}
                            entries={clientsList!}
                        />
                        {projectClientError && (
                            <FormHelperText error>{t("Please select a client!")}</FormHelperText>
                        )}
                        <TextField
                            label={t("Project Code")}
                            variant="standard"
                            fullWidth
                            value={projectCode}
                            onChange={handleNameChange}
                            error={nameError}
                            helperText={
                                nameError
                                    ? t("Project Code cannot be empty and must be unique!")
                                    : ""
                            }
                        />
                        <TextField
                            label={t("Project Name")}
                            variant="standard"
                            fullWidth
                            defaultValue={currentProjectRef.current?.description}
                            onChange={(evt) => {
                                const newValue = evt.target.value;
                                currentProjectRef.current!.description = newValue;
                            }}
                        />
                        <Combobox
                            label={t("Project Types")}
                            value={currentProjectRef.current?.type?.id}
                            onChange={handleProjectTypeChange}
                            entries={projectTypesList!}
                        />
                        {projectTypeError && (
                            <FormHelperText error>
                                {t("Please select a project type!")}
                            </FormHelperText>
                        )}
                        <Stack spacing={2}>
                            <Stack spacing={1}>
                                <ChipField
                                    label={t("Project Tasks")}
                                    value={projectTasksList.map((r) => ({
                                        id: r.id!,
                                        label: r.name!,
                                    }))}
                                    filterSelectedOptions={true}
                                    disabled={false}
                                    fetchOptions={async (query: string) => {
                                        const taskApi = new ProjectTaskApi(session!);
                                        const resp = await taskApi.getProjectTasks(
                                            0,
                                            1000,
                                            "name,id",
                                        );

                                        return resp.content
                                            .filter((t) =>
                                                t.name.toLowerCase().includes(query.toLowerCase()),
                                            )
                                            .map((t) => ({
                                                id: t.id!,
                                                label: t.name!,
                                            }));
                                    }}
                                    onChange={(newOptions) => {
                                        const updatedTasks: ProjectTask[] = newOptions.map((o) => ({
                                            id: o.id,
                                            name: o.label,
                                            description: "",
                                        }));

                                        setProjectTasksList(updatedTasks);
                                        currentProjectRef.current!.tasks = updatedTasks;
                                    }}
                                />
                            </Stack>
                        </Stack>
                        <Combobox
                            label={t("Managers")}
                            value={currentProjectRef.current?.manager?.id || undefined}
                            onChange={handleManagerChange}
                            entries={managersList!}
                        />
                        {projectManagerError && (
                            <FormHelperText error>
                                {t("Please select a project manager!")}
                            </FormHelperText>
                        )}
                        <LocalizationProvider dateAdapter={AdapterDayjs}>
                            <DatePicker
                                label={t("StartDate")}
                                defaultValue={dayjs(currentProjectRef.current?.startDate)}
                                onChange={(evt) => {
                                    currentProjectRef.current!.startDate = evt?.toDate();
                                }}
                                format="DD/MM/YYYY"
                            />
                            <DatePicker
                                label={t("Expected Due Date")}
                                defaultValue={
                                    currentProjectRef.current?.expectedDueDate
                                        ? dayjs(currentProjectRef.current?.expectedDueDate)
                                        : dayjs(new Date())
                                }
                                onChange={(evt) => {
                                    currentProjectRef.current!.expectedDueDate = evt?.toDate();
                                }}
                                format="DD/MM/YYYY"
                            />
                        </LocalizationProvider>
                        <Combobox
                            label={t("Status")}
                            value={currentProjectRef.current?.status?.id}
                            onChange={handleStatusChange}
                            entries={statusList!}
                        />
                        <TextField
                            label={t("Real Budget")}
                            variant="standard"
                            fullWidth
                            defaultValue={currentProjectRef.current?.realBudget}
                            onChange={(evt) => {
                                const newValue = evt.target.value;
                                currentProjectRef.current!.realBudget = parseFloat(newValue);
                            }}
                        />
                    </Stack>
                </DialogContent>
            ) : (
                <DialogContent>
                    <Box>
                        <Box
                            sx={{
                                padding: "20px 2px",
                            }}
                        >
                            <Stack direction="column" spacing={2}>
                                <Grid container spacing={2}>
                                    {renderBasicField(t("Name"), currentProjectRef.current?.name)}
                                    {renderBasicField(
                                        t("Description"),
                                        currentProjectRef.current?.description,
                                    )}
                                    {renderBasicField(
                                        t("Project Type"),
                                        currentProjectRef.current?.type?.name,
                                    )}
                                    {renderBasicField(
                                        t("Manager"),
                                        currentProjectRef.current?.manager?.name,
                                    )}
                                    {renderBasicField(
                                        t("Client"),
                                        currentProjectRef.current?.client?.name,
                                    )}
                                    {renderBasicField(
                                        t("StartDate"),
                                        currentProjectRef.current?.startDate?.toDateString(),
                                    )}
                                    {renderBasicField(
                                        t("Expected Due Date"),
                                        currentProjectRef.current?.expectedDueDate?.toString(),
                                    )}
                                    {renderBasicField(
                                        t("EndDate"),
                                        currentProjectRef.current?.endDate?.toString(),
                                    )}
                                    {renderBasicField(
                                        t("Status"),
                                        currentProjectRef.current?.status?.name,
                                    )}
                                </Grid>
                            </Stack>
                        </Box>
                    </Box>
                </DialogContent>
            )}
            {currentAction === "create" || currentAction === "edit" ? (
                <DialogActions>
                    <Button onClick={handleClose}>{t("Cancel")}</Button>
                    <Button onClick={handleApply} disabled={nameError || projectTypeError}>
                        {t("Save")}
                    </Button>
                </DialogActions>
            ) : (
                <DialogActions>
                    <Button onClick={handleClose}>{t("Cancel")}</Button>
                </DialogActions>
            )}
        </Dialog>
    );
}
