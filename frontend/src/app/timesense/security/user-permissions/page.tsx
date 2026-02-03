'use client';

import React, { useEffect, useState } from 'react';
import { useSession } from 'next-auth/react';
import {
    Alert, Collapse,
    Grid, IconButton, List, ListItem, ListItemIcon, ListItemText, Stack, Typography
} from '@mui/material';
import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import PermissionsApi, { ResourcePermission } from '../../lib/permissions';
import VerifiedUser from "@mui/icons-material/VerifiedUser";
import { ExpandLess, ExpandMore } from '@mui/icons-material';
import Loading from '@/components/load';

export default function UserPermissionsPage() {
    const { data: session } = useSession();
    const { t } = useTranslation(undefined, 'timesense');
    const [userPermissions, setUserPermissions] = useState<ResourcePermission[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);
    const [systemPermissionsOpen, setSystemPermissionsOpen] = useState<boolean>(true);
    const [projectPermissionsExpand, setProjectPermissionsExpand] = useState<boolean>(true);

    useEffect(() => {
        const fetchUserPermissions = async () => {
            try {
                const api = new PermissionsApi(session!);
                const response = await api.getUserPermissions();
                setUserPermissions(response);
            } catch (e: any) {
                setError(e.message);
            } finally {
                setLoading(false);
            }
        };

        fetchUserPermissions();
    }, []); // pass an empty array to guarantee the function only runs when the component renders

    const systemPermissions = userPermissions.filter((perm) => perm.resourceType === "System");
    const projectPermissions =
        userPermissions.filter((perm) => !systemPermissions.includes(perm));

    {/*Group project permissions by resourceId

        reduce function is used to convert projectPermissions array into an object with:
                        - keys   -> resourceId 
                        - values -> ResourcePermission[]
        acc  :- the accumulator object that is built over the iterations
        perm :- the current element during each iteration(ResourcePermission) */}
    const groupedProjectPermissions =
        projectPermissions.reduce<Record<number, ResourcePermission[]>>(
            (acc, perm) => {
                // check if the current resourceId exists in the accumulator, if not inits a new one
                if (!acc[perm.resourceId]) {
                    acc[perm.resourceId] = [];
                }
                acc[perm.resourceId].push(perm);
                return acc;
            },
            {} // each acc object starts with an empty object 
        );

    return (
        <Stack>
            <Typography variant="h3" gutterBottom>
                {t("User Permissions")}
            </Typography>
            <Typography>
                {t("In this page you can check your system and project permissions.")}
            </Typography>

            {loading && <Loading />}
            {error && <Alert severity="error">{error}</Alert>}

            {!loading && !error && (
                <Grid container spacing={4} sx={{ mt: 1 }}>

                    {/* System Permissions */}
                    <Grid item xs={12}>
                        <Stack direction="row" alignItems="center">
                            <Typography variant="h6" fontWeight="bold" gutterBottom>
                                {t("System Permissions")}
                            </Typography>
                            <IconButton onClick={() =>
                                setSystemPermissionsOpen(!systemPermissionsOpen)}
                            >
                                {systemPermissionsOpen ? <ExpandLess /> : <ExpandMore />}
                            </IconButton>
                        </Stack>

                        <Collapse in={systemPermissionsOpen}>
                            {systemPermissions.length > 0 ? (
                                <List
                                    sx={{
                                        border: "1px solid #ddd",
                                        borderRadius: 2, p: 1, width: "32%"
                                    }}>
                                    {systemPermissions.map((perm, index) => (
                                        <ListItem
                                            key={index}
                                            sx={{ borderBottom: index !== systemPermissions.length - 1 ? "1px solid #eee" : "none" }}
                                        >
                                            <ListItemIcon>
                                                <VerifiedUser color="primary" />
                                            </ListItemIcon>
                                            <ListItemText primary={perm.accessType}
                                                primaryTypographyProps={{ fontSize: "small" }}
                                            />
                                        </ListItem>
                                    ))}
                                </List>
                            ) : (
                                <Typography variant="body2" textAlign="center">
                                    {t("You currently have no system permissions assigned.")}
                                </Typography>
                            )}
                        </Collapse>
                    </Grid>

                    {/* Project Permissions */}
                    <Grid item xs={12}>
                        <Stack direction="row" alignItems="center">
                            <Typography variant="h6" fontWeight="bold" gutterBottom>
                                {t("Project Permissions")}
                            </Typography>
                            <IconButton onClick={() => setProjectPermissionsExpand(!projectPermissionsExpand)}>
                                {projectPermissionsExpand ? <ExpandLess /> : <ExpandMore />}
                            </IconButton>
                        </Stack>

                        <Collapse in={projectPermissionsExpand}>
                            {Object.keys(groupedProjectPermissions).length > 0 ? (
                                <Grid container spacing={4}>
                                    {/* Dynamically create columns for each resourceId */}
                                    {Object.entries(groupedProjectPermissions).map(([resourceId, permissions], index) => (
                                        <Grid item xs={4} key={resourceId}>
                                            <Typography variant="subtitle1" fontWeight="bold" sx={{ mt: 2 }}>
                                                {t('Project Id:', { resourceId: resourceId })}
                                            </Typography>
                                            <List sx={{ border: "1px solid #ddd", borderRadius: 2, p: 1 }}>
                                                {permissions.map((perm, index) => (
                                                    <ListItem
                                                        key={perm.id}
                                                        sx={{ borderBottom: index !== permissions.length - 1 ? "1px solid #eee" : "none" }}
                                                    >
                                                        <ListItemIcon>
                                                            <VerifiedUser color="primary" />
                                                        </ListItemIcon>
                                                        <ListItemText
                                                            primary={perm.accessType}
                                                            primaryTypographyProps={{ fontSize: "small" }}
                                                        />
                                                    </ListItem>
                                                ))}
                                            </List>
                                        </Grid>
                                    ))}
                                </Grid>
                            ) : (
                                <Typography variant="body2" textAlign="start">
                                    {t("You currently have no project permissions assigned.")}
                                </Typography>
                            )}
                        </Collapse>
                    </Grid>

                </Grid>
            )}
        </Stack>
    );
};
