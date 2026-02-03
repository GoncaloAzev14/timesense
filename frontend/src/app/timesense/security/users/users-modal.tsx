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
import { MutableRefObject, useEffect, useRef, useState } from 'react';
import UserApi, { User } from '../../lib/users';
import ChipField from '@datacentric/datacentric-ui/components/chipfield';
import RolesApi from '../../lib/roles';
import UserGroupApi from '../../lib/user-groups';
import { validateNotEmpty } from '@/components/validation-utils';
import Combobox, {
    Entry,
} from '@datacentric/datacentric-ui/components/combobox';
import dayjs from 'dayjs';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import JobTitleApi, { JobTitle } from '../../lib/job-titles';

type ModalActions = 'create' | 'edit';

export function useUserModal() {
    const { data: session } = useSession();
    const [isOpen, openDialog, closeDialog] = useModal();
    const currentUserRef = useRef<User | null>(null);
    const [currentAction, setCurrentAction] = useState<ModalActions>('create');

    return {
        open: async (mode: ModalActions, user?: User) => {
            currentUserRef.current = user || {
                id: 0,
                name: '',
                email: '',
                userRoles: [],
                userGroups: [],
            };
            setCurrentAction(mode);
            const result = await openDialog();
            if (result) {
                const api = new UserApi(session!);
                if (mode === 'edit') {
                    return (await api.updateUser(currentUserRef.current!)).data;
                } else {
                    return (await api.createUser(currentUserRef.current!)).data;
                }
            }
            return undefined;
        },
        dialog: (
            <UserModal
                isOpen={isOpen}
                currentAction={currentAction}
                user={currentUserRef}
                onApply={() => closeDialog(true)}
                onClose={() => closeDialog(false)}
            />
        ),
    };
}

interface UserModalProps {
    isOpen: boolean;
    currentAction: ModalActions;
    user: MutableRefObject<User | null>;
    onApply: () => void;
    onClose: () => void;
}

const UserModal = ({
    isOpen,
    currentAction,
    user,
    onApply,
    onClose,
}: UserModalProps) => {
    const { data: session } = useSession();
    const { t } = useTranslation(undefined, 'timesense');
    const firstControlRef = useRef<HTMLInputElement | null>(null);
    const [nameError, setNameError] = useState(false);
    const [emailError, setEmailError] = useState(false);
    const [selectedRoles, setSelectedRoles] = useState(
        (user.current?.userRoles || []).map(r => ({
            id: r.id!,
            label: r.name!,
        }))
    );
    const [selectedGroups, setSelectedGroups] = useState(
        (user.current?.userGroups || []).map(r => ({
            id: r.id!,
            label: r.name!,
        }))
    );
    const [managersList, setManagersList] = useState<Entry[] | null>([]);
    const [jobTitleList, setJobTitleList] = useState<Entry[] | null>([]);

    useEffect(() => {
        if (!isOpen) {
            return;
        }
        setNameError(false);
        setEmailError(false);
        const id = setTimeout(() => {
            firstControlRef.current?.focus();
        }, 10);
        return () => clearTimeout(id);
    }, [isOpen]);

    useEffect(() => {
        const userApi = new UserApi(session!);
        const jobTitleApi = new JobTitleApi(session!);
        try {
            userApi
                .getUsers(0, 1000, 'name,id', 'userRoles.name=Manager')
                .then(data => {
                    setManagersList(
                        data.content.map(e => ({
                            id: e.id!,
                            name: e.name!,
                        }))
                    );
                });
            jobTitleApi.getJobTitles(0, 1000, 'name').then(data => {
                setJobTitleList(
                    data.content.map(e => ({
                        id: e.id!,
                        name: e.name!,
                    }))
                );
            });
        } catch (e) {
            console.error('Failed to load lists of values', e);
        }
    }, [session]);

    const handleApply = () => {
        const isNameValid = validateNotEmpty(user.current?.name, setNameError);
        const isEmailValid = validateNotEmpty(
            user.current?.email,
            setEmailError
        );

        if (isNameValid && isEmailValid) {
            onApply();
        }
    };

    const handleRoleChange = (value: any[]) => {
        setSelectedRoles(value);
        user.current!.userRoles = value.map(v => ({
            id: v.id,
            name: v.label,
        }));
    };

    const handleGroupChange = (value: any[]) => {
        setSelectedGroups(value);
        user.current!.userGroups = value.map(v => ({
            id: v.id,
            name: v.label,
        }));
    };

    // force render
    const [, setUpdate] = useState(0);
    const forceRender = () => setUpdate(prev => prev + 1);

    const handleManagerChange = (evt: Entry) => {
        if (!user.current!.lineManager) {
            user.current!.lineManager = { id: evt.id, name: '' };
        } else {
            user.current!.lineManager!.id = evt.id;
        }
        user.current!.lineManagerId = evt.id;
        setTimeout(() => forceRender(), 100);
    };

    const handleJobTitleChange = (evt: Entry) => {
        if (!user.current!.jobTitle) {
            user.current!.jobTitle = { id: evt.id, name: '', rate: 0 };
        } else {
            user.current!.jobTitle!.id = evt.id;
        }
        setTimeout(() => forceRender(), 100);
    };

    return (
        user.current && (
            <Dialog open={isOpen} fullWidth>
                <DialogTitle>
                    {currentAction === 'create'
                        ? t('Create User')
                        : t('Edit User')}
                </DialogTitle>
                <DialogContent>
                    <LocalizationProvider dateAdapter={AdapterDayjs}>
                        <Stack direction="column" spacing={2}>
                            <TextField
                                label={t('Id')}
                                variant="standard"
                                fullWidth
                                value={user.current.id}
                                disabled
                            />
                            <TextField
                                label={t('Name')}
                                variant="standard"
                                fullWidth
                                defaultValue={user.current.name}
                                onChange={evt => {
                                    const newValue = evt.target.value;
                                    user.current!.name = newValue;
                                    validateNotEmpty(newValue, setNameError);
                                }}
                                inputRef={firstControlRef}
                                error={nameError}
                                helperText={
                                    nameError ? t('Name cannot be empty!') : ''
                                }
                            />
                            <TextField
                                label={t('Email')}
                                variant="standard"
                                fullWidth
                                defaultValue={user.current.email}
                                onChange={evt => {
                                    const newValue = evt.target.value;
                                    user.current!.email = newValue;
                                    validateNotEmpty(newValue, setNameError);
                                }}
                                error={emailError}
                                helperText={
                                    emailError
                                        ? t('Email cannot be empty!')
                                        : ''
                                }
                            />
                            <Combobox
                                label={t('Managers')}
                                value={user.current?.lineManager?.id}
                                onChange={handleManagerChange}
                                entries={managersList!}
                            />
                            <Combobox
                                label={t('Job Title')}
                                value={user.current.jobTitle?.id}
                                onChange={handleJobTitleChange}
                                entries={jobTitleList!}
                            />
                            <Stack direction="row" spacing={2}>
                                <TextField
                                    type="number"
                                    label={t('Previous Year Vacation Days')}
                                    variant="standard"
                                    fullWidth
                                    defaultValue={
                                        user.current.prevYearVacationDays
                                    }
                                    onChange={evt => {
                                        const newValue = parseFloat(
                                            evt.target.value
                                        );
                                        user.current!.prevYearVacationDays =
                                            newValue;
                                    }}
                                    inputRef={firstControlRef}
                                />
                                <TextField
                                    type="number"
                                    label={t('Current Year Vacation Days')}
                                    variant="standard"
                                    fullWidth
                                    defaultValue={
                                        user.current.currentYearVacationDays
                                    }
                                    onChange={evt => {
                                        const newValue = parseFloat(
                                            evt.target.value
                                        );
                                        user.current!.currentYearVacationDays =
                                            newValue;
                                    }}
                                    inputRef={firstControlRef}
                                />
                            </Stack>
                            <ChipField
                                label={t('User Roles')}
                                value={(user.current.userRoles || []).map(
                                    r => ({
                                        id: r.id!,
                                        label: r.name!,
                                    })
                                )}
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
                            <ChipField
                                label={t('User Groups')}
                                value={(user.current.userGroups || []).map(
                                    r => ({
                                        id: r.id!,
                                        label: r.name!,
                                    })
                                )}
                                onChange={handleGroupChange}
                                fetchOptions={async search => {
                                    return (
                                        await new UserGroupApi(
                                            session!
                                        ).getUserGroups(
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
                            <Stack direction="row" spacing={2}>
                                <DatePicker
                                    label={t('Admission Date')}
                                    defaultValue={dayjs(
                                        user.current?.admissionDate
                                            ? dayjs(user.current?.admissionDate)
                                            : dayjs(new Date())
                                    )}
                                    onChange={evt => {
                                        user.current!.admissionDate =
                                            evt?.toDate();
                                    }}
                                    format="DD/MM/YYYY"
                                />
                                <DatePicker
                                    label={t('Exit Date')}
                                    defaultValue={
                                        user.current?.exitDate
                                            ? dayjs(user.current?.exitDate)
                                            : null
                                    }
                                    onChange={evt => {
                                        user.current!.exitDate = evt?.toDate();
                                    }}
                                    format="DD/MM/YYYY"
                                />
                            </Stack>
                        </Stack>
                    </LocalizationProvider>
                </DialogContent>
                <DialogActions>
                    <Button onClick={onClose}>{t('Cancel')}</Button>
                    <Button onClick={handleApply}>{t('Save')}</Button>
                </DialogActions>
            </Dialog>
        )
    );
};
