'use client';

import React, { useEffect, useRef, useState } from 'react';
import { useSession } from 'next-auth/react';
import { Button, Stack, Typography } from '@mui/material';
import ProjectTasksApi, { ProjectTask } from '../../lib/project-tasks';
import {
    DataTable,
    useDataTableApiRef,
} from '@datacentric/datacentric-ui/components/datatable';
import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import { ProjectTaskModal, useProjectTaskModal } from './project-task-modal';
import { emptyPage } from '@datacentric/datacentric-ui/lib/spring-pagination';
import useConfirmation from '@datacentric/datacentric-ui/components/confirmation';
import { useToast } from '@/components/toast-provider';
import PermissionsApi from '../../lib/permissions';
import { ServiceError } from '@/lib/service-base';

export default function ProjectTasksPage() {
    const { data: session, update } = useSession();
    const { t } = useTranslation(undefined, 'timesense');
    const dataTableApiRef = useDataTableApiRef<ProjectTask>();

    const projectTaskModalApiRef = useProjectTaskModal();

    const [currentSort, setCurrentSort] = useState('name');
    const confirmation = useConfirmation();
    const { toast } = useToast();
    const modalOpen = useRef(false);
    const [canCreateProjectTask, setCanCreateProjectTask] = useState(false);

    /**
     * Open the dialog to create a new absence sub type
     */
    const handleCreateProjectTasks = async () => {
        if (!canCreateProjectTask) {
            return;
        }
        if (modalOpen.current) {
            return;
        }

        modalOpen.current = true;
        try {
            const result =
                await projectTaskModalApiRef.current.openDialog('create');
            if (result) {
                toast(t('Project Task created'));
                dataTableApiRef.current?.refreshData();
            }
        } finally {
            modalOpen.current = false;
        }
    };

    const handleEditProjectTask = async (e: ProjectTask) => {
        if (!canCreateProjectTask) {
            return false;
        }
        if (modalOpen.current) {
            return false;
        }

        modalOpen.current = true;
        try {
            const api = new ProjectTasksApi(session!);
            const setting = await api.getProjectTask(e.id!);
            const result = await projectTaskModalApiRef.current.openDialog(
                'edit',
                {
                    ...setting,
                }
            );
            if (result) {
                toast(t('Project Task updated'));
                return true;
            }
            return false;
        } finally {
            modalOpen.current = false;
        }
    };

    useEffect(() => {
        (async () => {
            const api = new PermissionsApi(session!);
            setCanCreateProjectTask(
                (await api.getSystemPermissions()).includes('CREATE_PROJECTS')
            );
        })();
    }, [session]);

    return (
        <>
            <Stack direction="column" spacing={2}>
                <Typography variant="h3">{t('Project Tasks')}</Typography>
                <Typography>{t('Project Tasks Page Text')}</Typography>
                <Stack direction="row" spacing={2}>
                    <Button
                        disabled={!canCreateProjectTask}
                        onClick={handleCreateProjectTasks}>
                        {t('Add Project Task')}
                    </Button>
                </Stack>
                <DataTable<ProjectTask>
                    columns={[
                        { field: 'id', title: t('Id') },
                        {
                            field: 'name',
                            title: t('Name'),
                            filter: true,
                        },
                        {
                            field: 'description',
                            title: t('Description'),
                        },
                        {
                            field: 'updatedBy.name',
                            title: t('Updated By'),
                        },
                    ]}
                    currentSort={currentSort}
                    apiRef={dataTableApiRef}
                    rowActions={[
                        {
                            label: t('Edit'),
                            icon: 'edit',
                            action: handleEditProjectTask,
                            isEnabled: () => canCreateProjectTask,
                        },
                    ]}
                    fetcher={async (
                        page: number,
                        rowsPerPage: number,
                        currentFilter?: string
                    ) => {
                        const api = new ProjectTasksApi(session!);
                        try {
                            return api.getProjectTasks(
                                page,
                                rowsPerPage,
                                currentSort,
                                currentFilter
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
                                return emptyPage();
                            } else {
                                console.error(
                                    'Error fetching project tasks:',
                                    e
                                );
                                return emptyPage();
                            }
                        }
                    }}
                    initialPage={0}
                    initialRowsPerPage={10}
                    onSortChange={setCurrentSort}
                />
            </Stack>
            <ProjectTaskModal apiRef={projectTaskModalApiRef} />
            {confirmation.dialog}
        </>
    );
}
