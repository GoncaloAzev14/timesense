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
import { useEffect, useRef, useState } from 'react';
import RoleApi, { Role } from '../../lib/roles';
import Combobox from '@datacentric/datacentric-ui/components/combobox';
import { validateNotEmpty } from '@/components/validation-utils';

interface RoleModalApi {
    openDialog: (action: string, data?: Role) => Promise<Role | undefined>;
    closeDialog: () => void;
}

interface RoleModalProps {
    apiRef: React.MutableRefObject<RoleModalApi>;
}

export function useRoleModal() {
    return useRef<RoleModalApi>({
        openDialog: async () => undefined,
        closeDialog: () => {},
    });
}

export const RoleModal = ({ apiRef }: RoleModalProps) => {
    const { data: session } = useSession();
    const [currentAction, setCurrentAction] = useState('create');
    const { t } = useTranslation(undefined, 'timesense');
    const [isDialogOpen, openEnvDialog, closeEnvDialog] = useModal();
    const currentRoleRef = useRef<Role | null>(null);
    const firstControlRef = useRef<HTMLInputElement | null>(null);
    const [roleList, setRoleList] = useState<Role[]>([]);

    const [nameError, setNameError] = useState(false);

    apiRef.current = {
        openDialog: async (action: string, data?: Role) => {
            currentRoleRef.current = data || {
                name: '',
            };
            setCurrentAction(action);
            const result = await openEnvDialog();
            if (result) {
                const api = new RoleApi(session!);
                if (action === 'edit') {
                    return (await api.updateRole(currentRoleRef.current!)).data;
                }
                return (await api.createRole(currentRoleRef.current!)).data;
            }
            return undefined;
        },
        closeDialog: () => {
            closeEnvDialog(false);
        },
    };

    useEffect(() => {
        if (!isDialogOpen) {
            return;
        }
        const id = setTimeout(() => {
            firstControlRef.current?.focus();
        }, 10);
        (async () => {
            const api = new RoleApi(session!);
            setRoleList((await api.getRoles(0, 1000)).content);
        })();
        return () => clearTimeout(id);
    }, [isDialogOpen, session]);

    const handleApply = () => {
        const isNameValid = validateNotEmpty(
            currentRoleRef.current?.name,
            setNameError
        );

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
                {currentAction === 'create' ? t('Create Role') : t('Edit Role')}
            </DialogTitle>
            <DialogContent>
                <Stack direction="column" spacing={2}>
                    <TextField
                        label={t('Id')}
                        variant="standard"
                        fullWidth
                        value={currentRoleRef.current?.id}
                        disabled
                    />
                    <TextField
                        label={t('Name')}
                        variant="standard"
                        fullWidth
                        defaultValue={currentRoleRef.current?.name}
                        onChange={evt => {
                            const newValue = evt.target.value;
                            currentRoleRef.current!.name = newValue;
                            validateNotEmpty(newValue, setNameError);
                        }}
                        inputRef={firstControlRef}
                        error={nameError}
                        helperText={nameError ? t('Name cannot be empty!') : ''}
                    />
                    <Combobox
                        label={t('Parent Role')}
                        value={currentRoleRef.current?.parentRole?.id!}
                        onChange={e => {
                            currentRoleRef.current!.parentRole = {
                                id: e.id,
                            };
                        }}
                        entries={roleList.map((element, _) => ({
                            id: element.id!,
                            name: element.name!,
                        }))}
                    />
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button onClick={() => closeEnvDialog(false)}>
                    {t('Cancel')}
                </Button>
                <Button onClick={handleApply}>{t('Save')}</Button>
            </DialogActions>
        </Dialog>
    );
};
