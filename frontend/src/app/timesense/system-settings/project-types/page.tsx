'use client';

import React, { useEffect, useRef, useState } from 'react';
import { useSession } from 'next-auth/react';
import { Button, Stack, Typography } from '@mui/material';
import ProjectTypesApi, { ProjectType } from '../../lib/project-types';
import {
    DataTable,
    useDataTableApiRef,
} from '@datacentric/datacentric-ui/components/datatable';
import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import { ProjectTypeModal, useProjectTypeModal } from './project-type-modal';
import { emptyPage } from '@datacentric/datacentric-ui/lib/spring-pagination';
import useConfirmation from '@datacentric/datacentric-ui/components/confirmation';
import { useToast } from '@/components/toast-provider';
import PermissionsApi from '../../lib/permissions';
import { ServiceError } from '@/lib/service-base';

export default function ProjectTypesPage() {
    const { data: session, update } = useSession();
    const { t } = useTranslation(undefined, 'timesense');
    const dataTableApiRef = useDataTableApiRef<ProjectType>();
    const projectTypeModalApiRef = useProjectTypeModal();
    const [currentSort, setCurrentSort] = useState('name');
    const confirmation = useConfirmation();
    const { toast } = useToast();
    const modalOpen = useRef(false);
    const [canCreateProjectType, setCanCreateProjectType] = useState(false);

    /**
     * Open the dialog to create a new environment
     */
    const handleCreateProjectType = async () => {
        if (!canCreateProjectType) {
            return;
        }
        if (modalOpen.current) {
            return;
        }

        modalOpen.current = true;
        try {
            const result =
                await projectTypeModalApiRef.current.openDialog('create');
            if (result) {
                toast(t('Project Type created'));
                dataTableApiRef.current?.refreshData();
            }
        } finally {
            modalOpen.current = false;
        }
    };

    const handleEditProjectType = async (e: ProjectType) => {
        if (!canCreateProjectType) {
            return false;
        }
        if (modalOpen.current) {
            return false;
        }

        modalOpen.current = true;
        try {
            const api = new ProjectTypesApi(session!);
            const projectType = await api.getProjectType(e.id!);
            const result = await projectTypeModalApiRef.current.openDialog(
                'edit',
                {
                    ...projectType,
                }
            );
            if (result) {
                toast(t('Project Type updated'));
                return true;
            }
            return false;
        } finally {
            modalOpen.current = false;
        }
    };

    const handleDeleteProjectType = async (e: ProjectType) => {
        if (!canCreateProjectType) {
            return false;
        }
        if (modalOpen.current) {
            return false;
        }

        modalOpen.current = true;
        try {
            if (
                !(await confirmation.ask(
                    t('Confirm deletion'),
                    t('Project Type deletion question', { name: e.name }),
                    t('Yes'),
                    t('No')
                ))
            ) {
                return false;
            }
            const api = new ProjectTypesApi(session!);
            try {
                await api.deleteProjectType(e.id!);
                toast(t('Project Type deleted'));
                return true;
            } catch (e: any) {
                toast(e.message);
                return false;
            }
        } finally {
            modalOpen.current = false;
        }
    };

    useEffect(() => {
        (async () => {
            const api = new PermissionsApi(session!);
            setCanCreateProjectType(
                (await api.getSystemPermissions()).includes('CREATE_PROJECTS')
            );
        })();
    }, [session]);

    return (
        <>
            <Stack direction="column" spacing={2}>
                <Typography variant="h3">{t('Project Types')}</Typography>
                <Stack direction="row" spacing={2}>
                    <Button
                        disabled={!canCreateProjectType}
                        onClick={handleCreateProjectType}>
                        {t('Add Project Type')}
                    </Button>
                </Stack>
                <DataTable<ProjectType>
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
                    ]}
                    currentSort={currentSort}
                    apiRef={dataTableApiRef}
                    rowActions={[
                        {
                            label: t('Edit'),
                            icon: 'edit',
                            action: handleEditProjectType,
                            isEnabled: () => canCreateProjectType,
                        },
                        {
                            label: t('Delete'),
                            icon: 'delete',
                            action: handleDeleteProjectType,
                            isEnabled: () => canCreateProjectType,
                        },
                    ]}
                    fetcher={async (
                        page: number,
                        rowsPerPage: number,
                        currentFilter?: string
                    ) => {
                        const api = new ProjectTypesApi(session!);
                        try {
                            return api.getProjectTypes(
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
                                console.error('Error fetching clients:', e);
                                return emptyPage();
                            }
                        }
                    }}
                    initialPage={0}
                    initialRowsPerPage={10}
                    onSortChange={setCurrentSort}
                />
            </Stack>
            <ProjectTypeModal apiRef={projectTypeModalApiRef} />
            {confirmation.dialog}
        </>
    );
}
