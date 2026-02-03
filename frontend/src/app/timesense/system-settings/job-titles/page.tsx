'use client';

import React, { useEffect, useRef, useState } from 'react';
import { useSession } from 'next-auth/react';
import { Button, Stack, Typography } from '@mui/material';
import JobTitlesApi, { JobTitle } from '../../lib/job-titles';
import {
    DataTable,
    useDataTableApiRef,
} from '@datacentric/datacentric-ui/components/datatable';
import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import { emptyPage } from '@datacentric/datacentric-ui/lib/spring-pagination';
import useConfirmation from '@datacentric/datacentric-ui/components/confirmation';
import { useToast } from '@/components/toast-provider';
import PermissionsApi from '../../lib/permissions';
import { ServiceError } from '@/lib/service-base';
import { JobTitleModal, useJobTitleModal } from './job-title-modal';

export default function JobTitlesPage() {
    const { data: session, update } = useSession();
    const { t } = useTranslation(undefined, 'timesense');
    const dataTableApiRef = useDataTableApiRef<JobTitle>();
    const [currentSort, setCurrentSort] = useState('name');
    const confirmation = useConfirmation();
    const { toast } = useToast();
    const modalOpen = useRef(false);
    const [canCreateJobTitle, setCanCreateJobTitle] = useState(false);
    const jobTitleModalApiRef = useJobTitleModal();

    const handleCreateJobTitle = async () => {
        if (!canCreateJobTitle) {
            return;
        }
        if (modalOpen.current) {
            return;
        }

        modalOpen.current = true;
        try {
            const result =
                await jobTitleModalApiRef.current.openDialog('create');
            if (result) {
                toast(t('JobTitle created'));
                dataTableApiRef.current?.refreshData();
            }
        } finally {
            modalOpen.current = false;
        }
    };

    const handleEditJobTitle = async (e: JobTitle) => {
        if (!canCreateJobTitle) {
            return false;
        }
        if (modalOpen.current) {
            return false;
        }

        modalOpen.current = true;
        try {
            const api = new JobTitlesApi(session!);
            const jobTitle = await api.getJobTitle(e.id!);
            const result = await jobTitleModalApiRef.current.openDialog(
                'edit',
                {
                    ...jobTitle,
                }
            );
            if (result) {
                toast(t('JobTitle updated'));
                return true;
            }
            return false;
        } finally {
            modalOpen.current = false;
        }
    };

    const handleDeleteJobTitle = async (e: JobTitle) => {
        if (!canCreateJobTitle) {
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
                    t('JobTitle deletion question', { name: e.name }),
                    t('Yes'),
                    t('No')
                ))
            ) {
                return false;
            }
            const api = new JobTitlesApi(session!);
            try {
                await api.deleteJobTitle(e.id!);
                toast(t('JobTitle deleted'));
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
            setCanCreateJobTitle(
                (await api.getSystemPermissions()).includes('MANAGE_SECURITY')
            );
        })();
    }, [session]);

    return (
        <>
            <Stack direction="column" spacing={2}>
                <Typography variant="h3">{t('JobTitles')}</Typography>
                <Stack direction="row" spacing={2}>
                    <Button
                        disabled={!canCreateJobTitle}
                        onClick={handleCreateJobTitle}>
                        {t('Add JobTitle')}
                    </Button>
                </Stack>
                <DataTable<JobTitle>
                    columns={[
                        { field: 'id', title: t('Id') },
                        {
                            field: 'name',
                            title: t('Name'),
                            filter: true,
                        },
                        {
                            field: 'rate',
                            title: t('Rate'),
                        },
                        {
                            field: 'startDate',
                            title: t('Start Date'),
                            formatMask: 'yyyy-MM-dd',
                        },
                        {
                            field: 'endDate',
                            title: t('End Date'),
                            formatMask: 'yyyy-MM-dd',
                        },
                    ]}
                    currentSort={currentSort}
                    apiRef={dataTableApiRef}
                    rowActions={[
                        {
                            label: t('Edit'),
                            icon: 'edit',
                            action: handleEditJobTitle,
                            isEnabled: () => canCreateJobTitle,
                        },
                        {
                            label: t('Delete'),
                            icon: 'delete',
                            action: handleDeleteJobTitle,
                            isEnabled: () => canCreateJobTitle,
                        },
                    ]}
                    fetcher={async (
                        page: number,
                        rowsPerPage: number,
                        currentFilter?: string
                    ) => {
                        const api = new JobTitlesApi(session!);
                        try {
                            return api.getJobTitles(
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
                                console.error('Error fetching jobTitles:', e);
                                return emptyPage();
                            }
                        }
                    }}
                    initialPage={0}
                    initialRowsPerPage={10}
                    onSortChange={setCurrentSort}
                />
            </Stack>
            <JobTitleModal apiRef={jobTitleModalApiRef} />
            {confirmation.dialog}
        </>
    );
}
