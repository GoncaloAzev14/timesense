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
import SystemSettingApi, { SystemSetting } from '../../lib/system-settings';
import { validateNotEmpty } from '@/components/validation-utils';

interface SystemSettingModalApi {
    openDialog: (
        action: 'create' | 'edit' | 'view',
        cluster?: SystemSetting
    ) => Promise<SystemSetting | undefined>;
    closeDialog: (save: boolean) => void;
}

interface SystemSettingModalProps {
    apiRef: React.MutableRefObject<SystemSettingModalApi | null>;
}

export function useSystemSettingModal() {
    return useRef<SystemSettingModalApi>({
        openDialog: async () => undefined,
        closeDialog: () => { },
    });
}

export const SystemSettingModal = ({ apiRef }: SystemSettingModalProps) => {
    const { data: session } = useSession();
    const [currentAction, setCurrentAction] = useState('create');
    const { t } = useTranslation(undefined, 'timesense');
    const [isDialogOpen, openSystemSettingDialog, closeSystemSettingDialog] = useModal();
    const currentSystemSettingRef = useRef<SystemSetting | null>(null);
    const firstControlRef = useRef<HTMLInputElement | null>(null);

    const [nameError, setNameError] = useState(false);

    apiRef.current = {
        openDialog: async (action: string, data?: SystemSetting) => {
            currentSystemSettingRef.current = data || {
                name: '',
            };
            setCurrentAction(action);
            const result = await openSystemSettingDialog();
            if (result) {
                const api = new SystemSettingApi(session!);
                if (action === 'edit') {
                    return (await api.updateSystemSetting(currentSystemSettingRef.current!)).data;
                }
                return (await api.createSystemSetting(currentSystemSettingRef.current!)).data;
            }
            return undefined;
        },
        closeDialog: () => {
            closeSystemSettingDialog(false);
        },
    };

    const handleApply = () => {
        const isNameValid = validateNotEmpty(currentSystemSettingRef.current?.name, setNameError);

        if (isNameValid) {

            closeSystemSettingDialog(true);
        }
    };
    return (
        <Dialog
            open={isDialogOpen}
            onClose={() => closeSystemSettingDialog(false)}
            fullWidth>
            <DialogTitle>
                {currentAction === 'create' ? t('Create System Setting') : t('Edit System Setting')}
            </DialogTitle>
            <DialogContent>
                <Stack direction="column" spacing={2}>
                    <TextField
                        label={t('Id')}
                        variant="standard"
                        fullWidth
                        value={currentSystemSettingRef.current?.id}
                        disabled
                    />
                    <TextField
                        label={t('Name')}
                        variant="standard"
                        fullWidth
                        defaultValue={currentSystemSettingRef.current?.name}
                        onChange={evt => {
                            const newValue = evt.target.value;
                            currentSystemSettingRef.current!.name = newValue;
                            validateNotEmpty(newValue, setNameError);
                        }}
                        inputRef={firstControlRef}
                        error={nameError}
                        helperText={nameError ? t('Name cannot be empty!') : ''}
                    />
                    <TextField
                        label={t('Value')}
                        variant="standard"
                        fullWidth
                        defaultValue={currentSystemSettingRef.current?.value}
                        onChange={evt => {
                            const newValue = evt.target.value;
                            currentSystemSettingRef.current!.value = newValue;
                        }}
                    />
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button onClick={() => closeSystemSettingDialog(false)}>
                    {t('Cancel')}
                </Button>
                <Button onClick={handleApply}>
                    {t('Save')}
                </Button>
            </DialogActions>
        </Dialog>
    );
};
