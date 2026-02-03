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
import AbsenceSubTypeApi, { AbsenceSubType } from '../../lib/absence-sub-types';
import { validateNotEmpty } from '@/components/validation-utils';

interface AbsenceSubTypeModalApi {
    openDialog: (
        action: 'create' | 'edit' | 'view',
        absSubType?: AbsenceSubType
    ) => Promise<AbsenceSubType | undefined>;
    closeDialog: (save: boolean) => void;
}

interface AbsenceSubTypeModalProps {
    apiRef: React.MutableRefObject<AbsenceSubTypeModalApi | null>;
}

export function useAbsenceSubTypeModal() {
    return useRef<AbsenceSubTypeModalApi>({
        openDialog: async () => undefined,
        closeDialog: () => { },
    });
}

export const AbsenceSubTypeModal = ({ apiRef }: AbsenceSubTypeModalProps) => {
    const { data: session } = useSession();
    const [currentAction, setCurrentAction] = useState('create');
    const { t } = useTranslation(undefined, 'timesense');
    const [isDialogOpen, openAbsenceSubTypeDialog, closeAbsenceSubTypeDialog] = useModal();
    const currentAbsenceSubTypeRef = useRef<AbsenceSubType | null>(null);
    const firstControlRef = useRef<HTMLInputElement | null>(null);

    const [nameError, setNameError] = useState(false);

    apiRef.current = {
        openDialog: async (action: string, data?: AbsenceSubType) => {
            currentAbsenceSubTypeRef.current = data || {
                name: '',
            };
            setCurrentAction(action);
            const result = await openAbsenceSubTypeDialog();
            if (result) {
                const api = new AbsenceSubTypeApi(session!);
                if (action === 'edit') {
                    return (await api.updateAbsenceSubType(currentAbsenceSubTypeRef.current!)).data;
                }
                return (await api.createAbsenceSubType(currentAbsenceSubTypeRef.current!)).data;
            }
            return undefined;
        },
        closeDialog: () => {
            closeAbsenceSubTypeDialog(false);
        },
    };

    const handleApply = () => {
        const isNameValid = validateNotEmpty(currentAbsenceSubTypeRef.current?.name, setNameError);

        if (isNameValid) {

            closeAbsenceSubTypeDialog(true);
        }
    };
    return (
        <Dialog
            open={isDialogOpen}
            onClose={() => closeAbsenceSubTypeDialog(false)}
            fullWidth>
            <DialogTitle>
                {currentAction === 'create' ? t('Create Absence Sub Type') : t('Edit Absence Sub Type')}
            </DialogTitle>
            <DialogContent>
                <Stack direction="column" spacing={2}>
                    <TextField
                        label={t('Id')}
                        variant="standard"
                        fullWidth
                        value={currentAbsenceSubTypeRef.current?.id}
                        disabled
                    />
                    <TextField
                        label={t('Name')}
                        variant="standard"
                        fullWidth
                        defaultValue={currentAbsenceSubTypeRef.current?.name}
                        onChange={evt => {
                            const newValue = evt.target.value;
                            currentAbsenceSubTypeRef.current!.name = newValue;
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
                        defaultValue={currentAbsenceSubTypeRef.current?.description}
                        onChange={evt => {
                            const newValue = evt.target.value;
                            currentAbsenceSubTypeRef.current!.description = newValue;
                        }}
                    />
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button onClick={() => closeAbsenceSubTypeDialog(false)}>
                    {t('Cancel')}
                </Button>
                <Button onClick={handleApply}>
                    {t('Save')}
                </Button>
            </DialogActions>
        </Dialog>
    );
};
