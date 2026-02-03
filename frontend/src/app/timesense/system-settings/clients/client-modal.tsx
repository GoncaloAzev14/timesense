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
import ClientApi, { Client } from '../../lib/clients';
import { validateNotEmpty } from '@/components/validation-utils';

interface ClientModalApi {
    openDialog: (action: string, data?: Client) => Promise<Client | undefined>;
    closeDialog: () => void;
}

interface ClientModalProps {
    apiRef: React.MutableRefObject<ClientModalApi>;
}

export function useClientModal() {
    return useRef<ClientModalApi>({
        openDialog: async () => undefined,
        closeDialog: () => { },
    });
}

export const ClientModal = ({ apiRef }: ClientModalProps) => {
    const { data: session } = useSession();
    const [currentAction, setCurrentAction] = useState('create');
    const { t } = useTranslation(undefined, 'timesense');
    const [isDialogOpen, openEnvDialog, closeEnvDialog] = useModal();
    const currentClientRef = useRef<Client | null>(null);
    const firstControlRef = useRef<HTMLInputElement | null>(null);

    const [nameError, setNameError] = useState(false);

    apiRef.current = {
        openDialog: async (action: string, data?: Client) => {
            currentClientRef.current = data || {
                name: '',
            };
            setCurrentAction(action);
            const result = await openEnvDialog();
            if (result) {
                const api = new ClientApi(session!);
                if (action === 'edit') {
                    return (await api.updateClient(currentClientRef.current!)).data;
                }
                return (await api.createClient(currentClientRef.current!)).data;
            }
            return undefined;
        },
        closeDialog: () => {
            closeEnvDialog(false);
        },
    };

    const handleApply = () => {
        const isNameValid = validateNotEmpty(currentClientRef.current?.name, setNameError);

        if (isNameValid) {

            closeEnvDialog(true);
        }
    };

    return (
        <Dialog
            open={isDialogOpen}
            onClose={() => closeEnvDialog(false)}
            fullWidth>
            <DialogTitle>
                {currentAction === 'create' ? t('Create Client ') : t('Edit Client ')}
            </DialogTitle>
            <DialogContent>
                <Stack direction="column" spacing={2}>
                    <TextField
                        label={t('Id')}
                        variant="standard"
                        fullWidth
                        value={currentClientRef.current?.id}
                        disabled
                    />
                    <TextField
                        label={t('Name')}
                        variant="standard"
                        fullWidth
                        defaultValue={currentClientRef.current?.name}
                        onChange={evt => {
                            const newValue = evt.target.value;
                            currentClientRef.current!.name = newValue;
                            validateNotEmpty(newValue, setNameError);
                        }}
                        inputRef={firstControlRef}
                        error={nameError}
                        helperText={nameError ? t('Name cannot be empty!') : ''}
                    />
                    <TextField
                        label={t('Client Ticker')}
                        variant="standard"
                        fullWidth
                        defaultValue={currentClientRef.current?.clientTicker}
                        onChange={evt => {
                            const newValue = evt.target.value;
                            currentClientRef.current!.clientTicker = newValue;
                        }}
                        inputRef={firstControlRef}
                    />
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button onClick={() => closeEnvDialog(false)}>
                    {t('Cancel')}
                </Button>
                <Button onClick={handleApply}>
                    {t('Save')}
                </Button>
            </DialogActions>
        </Dialog>
    );
};
