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
import UserGroupApi, { UserGroup } from '../../lib/user-groups';
import ChipField from '@datacentric/datacentric-ui/components/chipfield';
import RolesApi from '../../lib/roles';
import { validateNotEmpty } from '@/components/validation-utils';

type ModalActions = 'create' | 'edit';

export function useUserGroupModal() {
    const { data: session } = useSession();
    const [isOpen, openDialog, closeDialog] = useModal();
    const [currentAction, setCurrentAction] = useState<ModalActions>('create');
    const currentGroupRef = useRef<UserGroup | null>(null);

    return {
        open: async (mode: ModalActions, group?: UserGroup) => {
            currentGroupRef.current = group || (
                {
                    id: 0,
                    name: '',
                    roles: [],
                }
            );
            setCurrentAction(mode);
            const result = await openDialog();
            if (result) {
                const api = new UserGroupApi(session!);
                if (mode === 'edit') {
                    return (await api.updateUserGroup(currentGroupRef.current!)).data;
                } else {
                    return (await api.createUserGroup(currentGroupRef.current!)).data;
                }
            }
            return undefined;
        },
        dialog: (
            <UserGroupModal
                isOpen={isOpen}
                currentAction={currentAction}
                userGroup={currentGroupRef}
                onApply={() => closeDialog(true)}
                onClose={() => closeDialog(false)}
            />
        ),
    };
}

interface UserGroupModalProps {
    isOpen: boolean;
    currentAction: ModalActions;
    userGroup: React.MutableRefObject<UserGroup | null>;
    onApply: () => void;
    onClose: () => void;
}

const UserGroupModal = ({
    isOpen,
    currentAction,
    userGroup,
    onApply,
    onClose,
}: UserGroupModalProps) => {
    const { data: session } = useSession();
    const { t } = useTranslation(undefined, 'timesense');
    const firstControlRef = useRef<HTMLInputElement | null>(null);
    const [nameError, setNameError] = useState(false);
    const [selectedRoles, setSelectedRoles] = useState(
        (userGroup.current?.roles || []).map(r => ({ id: r.id!, label: r.name! }))
    );

    useEffect(() => {
        if (!isOpen) {
            return;
        }
        setNameError(false);
        const handler = setTimeout(() => {
            firstControlRef.current?.focus();
        }, 10);
        return () => clearTimeout(handler);
    }, [isOpen]);

    const handleApply = () => {
        const isNameValid = validateNotEmpty(userGroup.current?.name, setNameError);

        if (isNameValid) {
            onApply();
        }
    };

    const handleRoleChange = (value: any[]) => {
        setSelectedRoles(value);
        userGroup.current!.roles = value.map(v => ({
            id: v.id,
            name: v.label,
        }));
    }

    return (
        userGroup.current && (
            <Dialog open={isOpen} fullWidth>
                <DialogTitle>
                    {currentAction === 'create'
                        ? t('Create User Group')
                        : t('Edit User Group')}
                </DialogTitle>
                <DialogContent>
                    <Stack direction="column" spacing={2}>
                        <TextField
                            label={t('Id')}
                            variant="standard"
                            fullWidth
                            value={userGroup.current.id}
                            disabled
                        />
                        <TextField
                            label={t('Name')}
                            variant="standard"
                            fullWidth
                            defaultValue={userGroup.current.name}
                            onChange={evt => {
                                const newValue = evt.target.value;
                                userGroup.current!.name = newValue;
                                validateNotEmpty(newValue, setNameError);
                            }}
                            inputRef={firstControlRef}
                            error={nameError}
                            helperText={nameError ? t('Name cannot be empty!') : ''}
                        />
                        <TextField
                            label={t('uuid')}
                            variant="standard"
                            fullWidth
                            defaultValue={userGroup.current.tokenId}
                            onChange={evt => {
                                const newValue = evt.target.value;
                                userGroup.current!.tokenId = newValue;
                            }}
                            inputRef={firstControlRef}
                            disabled
                            helperText={t('Used only for external created groups')}
                        />
                        <ChipField
                            label={t('Roles')}
                            value={(userGroup.current!.roles || []).map(r => ({
                                id: r.id!,
                                label: r.name!,
                            }))}
                            onChange={handleRoleChange}
                            fetchOptions={async search => {
                                return (
                                    await new RolesApi(session!).getRoles(
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
                    <Button onClick={onClose}>{t('Cancel')}</Button>
                    <Button onClick={handleApply}>{t('Save')}</Button>
                </DialogActions>
            </Dialog>
        )
    );
};
