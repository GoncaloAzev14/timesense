'use client';

import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import { useModal } from '@datacentric/datacentric-ui/lib/browser-utils';
import {
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Stack,
    TextField,
} from '@mui/material';
import { useSession } from 'next-auth/react';
import { useRef, useState } from 'react';
import ProjectTaskApi, { ProjectTask } from '../../lib/project-tasks';
import { validateNotEmpty } from '@/components/validation-utils';
import ChipField from '@datacentric/datacentric-ui/components/chipfield';
import ProjectTypeApi from '../../lib/project-types';

interface ProjectTaskModalApi {
    openDialog: (
        action: 'create' | 'edit' | 'view',
        projectTask?: ProjectTask
    ) => Promise<ProjectTask | undefined>;
    closeDialog: (save: boolean) => void;
}

interface ProjectTaskModalProps {
    apiRef: React.MutableRefObject<ProjectTaskModalApi | null>;
}

export function useProjectTaskModal() {
    return useRef<ProjectTaskModalApi>({
        openDialog: async () => undefined,
        closeDialog: () => { },
    });
}

export const ProjectTaskModal = ({ apiRef }: ProjectTaskModalProps) => {
    const { data: session } = useSession();
    const [currentAction, setCurrentAction] = useState('create');
    const { t } = useTranslation(undefined, 'timesense');
    const [isDialogOpen, openProjectTaskDialog, closeProjectTaskDialog] = useModal();
    const currentProjectTaskRef = useRef<ProjectTask | null>(null);
    const firstControlRef = useRef<HTMLInputElement | null>(null);

    const [nameError, setNameError] = useState(false);

    const [selectedTypes, setSelectedTypes] = useState(
        (currentProjectTaskRef.current?.projectTypes || []).map(r => ({
            id: r.id!,
            label: r.name!,
        }))
    );

    const handleGroupChange = (value: any[]) => {
        setSelectedTypes(value);
        currentProjectTaskRef.current!.projectTypes = value.map(v => ({
            id: v.id,
            name: v.label,
        }));
    };

    apiRef.current = {
        openDialog: async (action: string, data?: ProjectTask) => {
            currentProjectTaskRef.current = data || {
                name: '',
            };
            setCurrentAction(action);
            const result = await openProjectTaskDialog();
            if (result) {
                const api = new ProjectTaskApi(session!);
                if (action === 'edit') {
                    return (await api.updateProjectTask(currentProjectTaskRef.current!)).data;
                }
                return (await api.createProjectTask(currentProjectTaskRef.current!)).data;
            }
            return undefined;
        },
        closeDialog: () => {
            closeProjectTaskDialog(false);
        },
    };

    const handleApply = () => {
        const isNameValid = validateNotEmpty(currentProjectTaskRef.current?.name, setNameError);

        if (isNameValid) {

            closeProjectTaskDialog(true);
        }
    };
    return (
        <Dialog
            open={isDialogOpen}
            onClose={() => closeProjectTaskDialog(false)}
            fullWidth>
            <DialogTitle>
                {currentAction === 'create' ? t('Create Project Task') : t('Edit Project Task')}
            </DialogTitle>
            <DialogContent>
                <Stack direction="column" spacing={2}>
                    <TextField
                        label={t('Id')}
                        variant="standard"
                        fullWidth
                        value={currentProjectTaskRef.current?.id}
                        disabled
                    />
                    <TextField
                        label={t('Name')}
                        variant="standard"
                        fullWidth
                        defaultValue={currentProjectTaskRef.current?.name}
                        onChange={evt => {
                            const newValue = evt.target.value;
                            currentProjectTaskRef.current!.name = newValue;
                            validateNotEmpty(newValue, setNameError);
                        }}
                        inputRef={firstControlRef}
                        error={nameError}
                        helperText={nameError ? t('Name cannot be empty!') : ''}
                    />
                    <TextField
                        label={t('Description')}
                        variant="standard"
                        fullWidth
                        defaultValue={currentProjectTaskRef.current?.description}
                        onChange={evt => {
                            const newValue = evt.target.value;
                            currentProjectTaskRef.current!.description = newValue;
                        }}
                    />
                    <ChipField
                        label={t('Project Types')}
                        value={(currentProjectTaskRef.current?.projectTypes || []).map(r => ({
                            id: r.id!,
                            label: r.name!,
                        }))}
                        onChange={handleGroupChange}
                        fetchOptions={async search => {
                            return (
                                await new ProjectTypeApi(
                                    session!
                                ).getProjectTypes(
                                    0,
                                    10,
                                    'name',
                                    `name~${search}`
                                )
                            ).content.map(v => ({
                                id: v.id!,
                                label: v.name!,
                            }));
                        }}
                    />
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button onClick={() => closeProjectTaskDialog(false)}>
                    {t('Cancel')}
                </Button>
                <Button onClick={handleApply}>
                    {t('Save')}
                </Button>
            </DialogActions>
        </Dialog>
    );
};
