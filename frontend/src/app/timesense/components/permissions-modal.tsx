'use client';

import React, { MutableRefObject, useRef, useState } from 'react';
import { useModal } from '@datacentric/datacentric-ui/lib/browser-utils';
import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import {
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableRow,
} from '@mui/material';
import { useSession } from 'next-auth/react';
import PermissionsApi, {
    Permissions,
    PermissionsMap,
    PERMISSIONS_SUBJECT_TYPES,
    emptyPermissions,
} from '../lib/permissions';
import UserApi from '../lib/users';
import UserGroupsApi from '../lib/user-groups';
import RolesApi from '../lib/roles';
import ChipField, { Option } from '@datacentric/datacentric-ui/components/chipfield';

export interface PermissionsModalHookReturn {
    open: (
        title: string,
        resourceType: string,
        resourceId: number,
        listOfPermissions: string[]
    ) => Promise<boolean>;
    dialog: (canEdit: boolean) => React.ReactElement;
}

/**
 * Hook to open a modal to edit permissions
 *
 * @returns {PermissionsModalHookReturn} Api functions and modal component to add in the page
 */
export default function usePermissionsModal(): PermissionsModalHookReturn {
    const { data: session } = useSession();
    const [isOpen, openDialog, closeDialog] = useModal();
    const [resourceType, setResourceType] = useState('');
    const [resourceId, setResourceId] = useState(0);
    const [title, setTitle] = useState('');
    const [listOfPermissions, setListOfPermissions] = useState<string[]>([]);
    const setFormValue = useRef<(newPermissionsMap: PermissionsMap) => void>(
        () => { }
    );

    return {
        open: async (
            title: string,
            resourceType: string,
            resourceId: number,
            listOfPermissions: string[]
        ) => {
            setResourceType(resourceType);
            setResourceId(resourceId);
            setTitle(title);
            setListOfPermissions(listOfPermissions);
            const api = new PermissionsApi(session!);
            const currentPermissions = await api.getPermissionsForResource(
                resourceType,
                resourceId
            );
            setFormValue.current!(currentPermissions);
            return await openDialog();
        },
        dialog: (canEdit: boolean) => {
            return (
                <PermissionsModal
                    isOpen={isOpen}
                    title={title}
                    listOfPermissions={listOfPermissions}
                    refApi={setFormValue}
                    onApply={async (newPermissionsMap: PermissionsMap) => {
                        const api = new PermissionsApi(session!);
                        await api.savePermissionsForResource(
                            resourceType,
                            resourceId,
                            newPermissionsMap
                        );
                        closeDialog(true);
                    }}
                    onClose={() => closeDialog(false)}
                    canEdit={canEdit}
                />
            );
        },
    };
}

export interface PermissionsModalProps {
    isOpen: boolean;
    listOfPermissions: string[];
    title: string;
    refApi: MutableRefObject<(newPermissionsMap: PermissionsMap) => void>;
    onApply: (newPermissions: PermissionsMap) => void;
    onClose: () => void;
    canEdit: boolean;
}

const PermissionsModal: React.FC<PermissionsModalProps> = props => {
    const { data: session } = useSession();
    const { t } = useTranslation(undefined, 'timesense');
    const [permissions, setPermissions] = useState<PermissionsMap>(new Map());

    props.refApi.current = (newPermissionsMap: PermissionsMap) => {
        setPermissions(newPermissionsMap);
    };

    // set the value of the permissions in the modal state
    const handleOnChange =
        (perm: string, type: keyof Permissions) => (v: Option[]) => {
            let newValue = new Map(permissions.entries());
            let entry = newValue.get(perm)!;
            if (!entry) {
                newValue.set(perm, emptyPermissions());
                entry = newValue.get(perm)!;
            }
            entry[type] = v.map(x => ({ id: x.id, name: x.label }));
            setPermissions(newValue);
        };

    const optionsFetcher = (type: keyof Permissions) => {
        let cachedQuery: string | null = null;
        let cachedOptions: Option[] = [];
        return async (query: string) => {
            if (query === cachedQuery) {
                return cachedOptions;
            }
            let newOptions: Option[] = [];

            // TODO: Change the number of options we download to allow
            // progressive downloading of more.
            switch (type) {
                case 'user':
                    newOptions = (
                        await new UserApi(session!).getUsers(
                            0,
                            100,
                            'name',
                            `name~${query}`
                        )
                    ).content.map(v => ({ id: v.id!, label: v.name! }));
                    break;
                case 'group':
                    newOptions = (
                        await new UserGroupsApi(session!).getUserGroups(
                            0,
                            10,
                            'name',
                            `name~${query}`
                        )
                    ).content.map(v => ({ id: v.id!, label: v.name! }));
                    break;
                case 'role':
                    newOptions = (
                        await new RolesApi(session!).getRoles(
                            0,
                            10,
                            'name',
                            `name~${query}`
                        )
                    ).content.map(v => ({ id: v.id!, label: v.name! }));
                    break;
                default:
                    // Should never happen
                    console.error('Unknown type', type);
                    return [];
            }
            // cache this result so that we don't keep fetching the same data
            // over and over again
            cachedQuery = query;
            cachedOptions = newOptions;
            return newOptions;
        };
    };

    const generateSubjectListCell = (perm: string, type: keyof Permissions) => {
        const listOfSubjects = permissions?.get(perm)?.[type] || [];
        return (
            <TableCell key={type}>
                <ChipField
                    disabled={!props.canEdit}
                    label={type}
                    fetchOptions={optionsFetcher(type)}
                    value={listOfSubjects.map(s => ({
                        id: s.id,
                        label: s.name,
                    }))}
                    onChange={handleOnChange(perm, type)}
                />
            </TableCell>
        );
    };

    return (
        <Dialog fullWidth maxWidth="lg" open={props.isOpen}>
            <DialogTitle>{props.title}</DialogTitle>
            <DialogContent>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell>{t('Permission')}</TableCell>
                            <TableCell>{t('Users')}</TableCell>
                            <TableCell>{t('Groups')}</TableCell>
                            <TableCell>{t('Roles')}</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {props.listOfPermissions.map((perm, idx) => (
                            <TableRow key={idx}>
                                <TableCell>{perm}</TableCell>
                                {PERMISSIONS_SUBJECT_TYPES.map(type =>
                                    generateSubjectListCell(perm, type)
                                )}
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </DialogContent>
            <DialogActions>
                <Button onClick={props.onClose}>{t('Cancel')}</Button>
                <Button
                    disabled={!props.canEdit}
                    onClick={() => props.onApply(permissions)}>
                    {t('Apply')}
                </Button>
            </DialogActions>
        </Dialog>
    );
};
