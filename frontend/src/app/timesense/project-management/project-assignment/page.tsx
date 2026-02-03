'use client';

import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useSession } from 'next-auth/react';
import { Button, Stack, Typography } from '@mui/material';
import {
    DataTable,
    useDataTableApiRef,
} from '@datacentric/datacentric-ui/components/datatable';
import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import { emptyPage } from '@datacentric/datacentric-ui/lib/spring-pagination';
import useConfirmation from '@datacentric/datacentric-ui/components/confirmation';
import { useKeyboardShortcut } from '@datacentric/datacentric-ui/lib/browser-utils';
import { useToast } from '@/components/toast-provider';
import PermissionsApi from '../../lib/permissions';
import { ServiceError } from '@/lib/service-base';
import {
    ProjectAssignmentModal,
    useProjectAssignmentModal,
} from './project-assign-modal';
import ProjectAssignmentApi, {
    ProjectAssignment,
} from '../../lib/project-assignment';

export default function ProjectAssignmentsPage() {
    const { data: session, update } = useSession();
    const { t } = useTranslation(undefined, 'timesense');
    const projectAssignModalRef = useProjectAssignmentModal();
    const dataTableApiRef = useDataTableApiRef<ProjectAssignment>();
    const [currentSort, setCurrentSort] = useState<string>('id');
    const confirmation = useConfirmation();
    const { toast } = useToast();
    const modalOpen = useRef(false);
    const [runningCreate, setRunningCreate] = useState(false);
    const [canCreateProjectAssigns, setCanCreateProjectAssigns] =
        useState(false);

    const handleCreateProjectAssignment = useCallback(async () => {
        if (!canCreateProjectAssigns) {
            return;
        }

        if (modalOpen.current) {
            return;
        }

        modalOpen.current = true;
        try {
            const result = await projectAssignModalRef.current.openDialog(
                'create',
                {
                    id: 0,
                    user: { id: 0, name: '' },
                    project: { id: 0, name: '' },
                }
            );
            if (result) {
                toast(t('Project Assignment created'));
                dataTableApiRef.current?.refreshData();
            }
        } finally {
            modalOpen.current = false;
        }
    }, [
        projectAssignModalRef,
        dataTableApiRef,
        t,
        canCreateProjectAssigns,
        toast,
    ]);

    const handleEditProjectAssignment = async (e: ProjectAssignment) => {
        if (modalOpen.current) {
            return false;
        }

        const projectAssignApi = new ProjectAssignmentApi(session!);
        const projectAssign = await projectAssignApi.getProjectAssignment(
            e.id!
        );
        modalOpen.current = true;
        try {
            const result = await projectAssignModalRef.current.openDialog(
                'edit',
                {
                    ...projectAssign,
                }
            );
            if (result) {
                toast(t('Project Assignment updated'));
                return true;
            }
            return false;
        } finally {
            modalOpen.current = false;
        }
    };

    const handleDeleteProjectAssignment = async (e: ProjectAssignment) => {
        if (modalOpen.current) {
            return false;
        }

        modalOpen.current = true;
        try {
            if (
                !(await confirmation.ask(
                    t('Confirm deletion'),
                    t('Project Assignment deletion question', { name: e.id }),
                    t('Yes'),
                    t('No')
                ))
            ) {
                return false;
            }
            const api = new ProjectAssignmentApi(session!);
            try {
                await api.deleteProjectAssignment(e.id!);
                toast(t('Project Assignment deleted'));
                return true;
            } catch (e: any) {
                toast(e.messages);
                return false;
            }
        } finally {
            modalOpen.current = false;
        }
    };

    useKeyboardShortcut('n', handleCreateProjectAssignment, [
        runningCreate,
        setRunningCreate,
    ]);

    const projectAssignmentPageFetch = useCallback(
        async (page: number, rowsPerPage: number) => {
            const api = new ProjectAssignmentApi(session!);
            try {
                return api.getProjectAssignments(
                    page,
                    rowsPerPage,
                    currentSort
                );
            } catch (e: any) {
                if (e instanceof ServiceError) {
                    const errorContent = await e.getContentAsJson();
                    if (errorContent?.status === 401) {
                        update({
                            ...session!,
                            error: 'EndpointAccessTokenError',
                        });
                    }
                    return emptyPage<ProjectAssignment>();
                } else {
                    return emptyPage<ProjectAssignment>();
                }
            }
        },
        [session, update, currentSort]
    );

    useEffect(() => {
        (async () => {
            const api = new PermissionsApi(session!);
            setCanCreateProjectAssigns(
                (await api.getSystemPermissions()).includes('CREATE_PROJECTS')
            );
        })();
    }, [session]);

    return (
        <>
            <Stack direction="column" spacing={2}>
                <Typography variant="h3">
                    {' '}
                    {t('Project Assignments')}
                </Typography>
                <Typography>{t('ProjectAssignments Page Text')}</Typography>
                <Stack direction="row" spacing={2}>
                    <Button
                        disabled={!canCreateProjectAssigns}
                        onClick={async () => {
                            setRunningCreate(true);
                            await handleCreateProjectAssignment();
                            setRunningCreate(false);
                        }}>
                        {t('Add ProjectAssignment')}
                    </Button>
                </Stack>
                <DataTable<ProjectAssignment>
                    columns={[
                        { field: 'id', title: t('Id') },
                        { field: 'user.name', title: t('User') },
                        { field: 'project.name', title: t('Project') },
                        { field: 'allocation', title: t('Allocation') },
                        { field: 'description', title: t('Description') },
                        {
                            field: 'startDate',
                            title: t('StartDate'),
                            formatMask: 'yyyy-MM-dd',
                        },
                        {
                            field: 'endDate',
                            title: t('EndDate'),
                            formatMask: 'yyyy-MM-dd',
                        },
                        {
                            field: 'updatedAt',
                            title: t('Last Modified'),
                            formatMask: 'timeago',
                            showToolTip: true,
                        },
                    ]}
                    apiRef={dataTableApiRef}
                    rowActions={[
                        {
                            label: t('Edit'),
                            icon: 'edit',
                            action: handleEditProjectAssignment,
                            isEnabled: () => canCreateProjectAssigns,
                        },
                        {
                            label: t('Delete'),
                            icon: 'delete',
                            action: handleDeleteProjectAssignment,
                            isEnabled: () => canCreateProjectAssigns,
                        },
                    ]}
                    fetcher={projectAssignmentPageFetch}
                    currentSort={currentSort}
                    onSortChange={setCurrentSort}
                    initialPage={0}
                    initialRowsPerPage={10}
                />
            </Stack>
            <ProjectAssignmentModal apiRef={projectAssignModalRef} />
            {confirmation.dialog}
        </>
    );
}
