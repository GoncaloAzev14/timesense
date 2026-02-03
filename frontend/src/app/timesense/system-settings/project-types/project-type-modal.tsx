'use client';

import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import { useModal } from '@datacentric/datacentric-ui/lib/browser-utils';
import {
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Switch,
    FormControlLabel,
    Stack,
    TextField,
} from '@mui/material';
import { useSession } from 'next-auth/react';
import { useRef, useState } from 'react';
import ProjectTypeApi, { ProjectType } from '../../lib/project-types';
import { validateNotEmpty } from '@/components/validation-utils';

interface ProjectTypeModalApi {
    openDialog: (
        action: 'create' | 'edit' | 'view',
        cluster?: ProjectType
    ) => Promise<ProjectType | undefined>;
    closeDialog: (save: boolean) => void;
}

interface ProjectTypeModalProps {
    apiRef: React.MutableRefObject<ProjectTypeModalApi | null>;
}

export function useProjectTypeModal() {
    return useRef<ProjectTypeModalApi>({
        openDialog: async () => undefined,
        closeDialog: () => { },
    });
}

export const ProjectTypeModal = ({ apiRef }: ProjectTypeModalProps) => {
    const { data: session } = useSession();
    const [currentAction, setCurrentAction] = useState('create');
    const { t } = useTranslation(undefined, 'timesense');
    const [isDialogOpen, openProjectTypeDialog, closeProjectTypeDialog] = useModal();
    const currentProjectTypeRef = useRef<ProjectType | null>(null);
    const firstControlRef = useRef<HTMLInputElement | null>(null);

    const [nameError, setNameError] = useState(false);
    const [lineManager, setLineManager] = useState(false);

    apiRef.current = {
        openDialog: async (action: string, data?: ProjectType) => {
            currentProjectTypeRef.current = data || {
                name: '',
            };
            setLineManager(currentProjectTypeRef.current?.lineManager || false);
            setCurrentAction(action);
            const result = await openProjectTypeDialog();
            if (result) {
                const api = new ProjectTypeApi(session!);
                if (action === 'edit') {
                    return (await api.updateProjectType(currentProjectTypeRef.current!)).data;
                }
                return (await api.createProjectType(currentProjectTypeRef.current!)).data;
            }
            return undefined;
        },
        closeDialog: () => {
            closeProjectTypeDialog(false);
        },
    };

    const handleApply = () => {
        const isNameValid = validateNotEmpty(currentProjectTypeRef.current?.name, setNameError);

        if (isNameValid) {

            closeProjectTypeDialog(true);
        }
    };

    const handleChange = (checked: boolean) => {
        setLineManager(checked);
        if (currentProjectTypeRef.current) {
            currentProjectTypeRef.current.lineManager = checked;
        }
    };

    return (
        <Dialog
            open={isDialogOpen}
            onClose={() => closeProjectTypeDialog(false)}
            fullWidth>
            <DialogTitle>
                {currentAction === 'create' ? t('Create Project Type') : t('Edit Project Type')}
            </DialogTitle>
            <DialogContent>
                <Stack direction="column" spacing={2}>
                    <TextField
                        label={t('Id')}
                        variant="standard"
                        fullWidth
                        value={currentProjectTypeRef.current?.id}
                        disabled
                    />
                    <TextField
                        label={t('Name')}
                        variant="standard"
                        fullWidth
                        defaultValue={currentProjectTypeRef.current?.name}
                        onChange={evt => {
                            const newValue = evt.target.value;
                            currentProjectTypeRef.current!.name = newValue;
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
                        defaultValue={currentProjectTypeRef.current?.description}
                        onChange={evt => {
                            const newValue = evt.target.value;
                            currentProjectTypeRef.current!.description = newValue;
                        }}
                    />
                    <FormControlLabel
                        control={
                            <Switch
                                checked={lineManager}
                                onChange={e => handleChange(e.target.checked)}
                            />
                        }
                        label={t('Line Manager')}
                        sx={{ marginTop: 1 }}
                    />
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button onClick={() => closeProjectTypeDialog(false)}>
                    {t('Cancel')}
                </Button>
                <Button onClick={handleApply}>
                    {t('Save')}
                </Button>
            </DialogActions>
        </Dialog>
    );
};
