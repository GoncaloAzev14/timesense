'use client';

import React, { useEffect, useState } from 'react';
import { useSession } from 'next-auth/react';
import {
    Button, Card, CardContent, Link, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography
} from '@mui/material';
import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import { useToast } from '@/components/toast-provider';
import RolesApi from '../../lib/roles';
import UserApi from '../../lib/users';
import UserGroupsApi from '../../lib/user-groups';
import PermissionsApi, {
    Permissions,
    PermissionsMap,
    PERMISSIONS_SUBJECT_TYPES,
    emptyPermissions,
} from '../../lib/permissions';
import ChipField, { Option } from '@datacentric/datacentric-ui/components/chipfield';
import { LockOutlined } from '@mui/icons-material';
import Loading from '@/components/load';

export default function PermissionsPage() {
    const { data: session } = useSession();
    const { t } = useTranslation(undefined, 'timesense');
    const { toast } = useToast();
    const [permissions, setPermissions] = useState<PermissionsMap>(new Map());
    const [canApply, setCanApply] = useState(false);
    const [loading, setLoading] = useState<boolean>(true);

    const listOfPermissions = [
        'CREATE_PROJECTS',
        'MANAGE_TIMEOFF',
        'MANAGE_SECURITY',
    ]

    useEffect(() => {
        const fetchData = async () => {
            const api = new PermissionsApi(session!);
            const currentPermissions = await api.getPermissionsForResource('System', 0);
            setPermissions(currentPermissions);
        };

        fetchData();
    }, [session]);

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

            switch (type) {
                case 'user':
                    newOptions = (
                        await new UserApi(session!).getUsers(
                            0,
                            10,
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
                    console.error('Unknown type', type);
                    return [];
            }

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

    const handleApply = async () => {
        const api = new PermissionsApi(session!);
        await api.savePermissionsForResource('System', 0, permissions);
        toast(t('Permissions saved successfully!'));
    };

    useEffect(() => {
        (async () => {
            try {
                const api = new PermissionsApi(session!);
                setCanApply(
                    (await api.getSystemPermissions()).includes(
                        'MANAGE_SECURITY'
                    )
                );
            } catch (e: any) {
                return false;
            } finally {
                setLoading(false);
            }

        })();
    }, [session]);

    return (
        <Stack>
            {loading && <Loading />}
            {!loading && (
                canApply ? (
                    <Stack direction="column" spacing={1} sx={{ width: "100%" }}>
                        <Typography variant="h3">{t('System Permissions')}</Typography>
                        <Typography>{t('System Permissions Text')}</Typography>
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
                                {listOfPermissions.map((perm, idx) => (
                                    <TableRow key={idx}>
                                        <TableCell>{perm}</TableCell>
                                        {PERMISSIONS_SUBJECT_TYPES.map(type =>
                                            generateSubjectListCell(perm, type)
                                        )}
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                        <Stack direction="row" sx={{ width: "100%" }} justifyContent="flex-end">
                            <Button
                                disabled={!canApply}
                                variant="contained"
                                color="primary"
                                onClick={handleApply}>
                                {t('Apply')}
                            </Button>
                        </Stack>
                    </Stack>
                ) : (
                    <Card sx={{ maxWidth: 400, mx: "auto", p: 3, textAlign: "center", boxShadow: 3, borderRadius: 2 }}>
                        <CardContent>
                            <LockOutlined color="error" sx={{ fontSize: 48, mb: 2 }} />
                            <Typography variant="h6" fontWeight="bold">
                                {t("Access Denied")}
                            </Typography>
                            <Typography variant="body1" sx={{ mt: 1, color: "text.secondary" }}>
                                {t("You currently don't have permission to access this page.")}
                            </Typography>
                            <Typography variant="body2" sx={{ mt: 2 }}>
                                {t("Want to check your permissions?")} {" "}
                                <Link href="/timesense/security/user-permissions" underline="hover" color="primary" fontWeight="bold">
                                    {t("Click here")}
                                </Link>
                            </Typography>
                        </CardContent>
                    </Card>
                )
            )}
        </Stack>
    );
}
