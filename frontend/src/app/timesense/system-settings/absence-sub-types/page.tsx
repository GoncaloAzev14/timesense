'use client';

import React, { useEffect, useRef, useState } from 'react';
import { useSession } from 'next-auth/react';
import { Button, Stack, Typography } from '@mui/material';
import AbsenceSubTypesApi, {
    AbsenceSubType,
} from '../../lib/absence-sub-types';
import {
    DataTable,
    useDataTableApiRef,
} from '@datacentric/datacentric-ui/components/datatable';
import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import {
    AbsenceSubTypeModal,
    useAbsenceSubTypeModal,
} from './absence-sub-type-modal';
import { emptyPage } from '@datacentric/datacentric-ui/lib/spring-pagination';
import useConfirmation from '@datacentric/datacentric-ui/components/confirmation';
import { useToast } from '@/components/toast-provider';
import PermissionsApi from '../../lib/permissions';
import { ServiceError } from '@/lib/service-base';

export default function AbsenceSubTypesPage() {
    const { data: session, update } = useSession();
    const { t } = useTranslation(undefined, 'timesense');
    const dataTableApiRef = useDataTableApiRef<AbsenceSubType>();

    const absenceSubTypeModalApiRef = useAbsenceSubTypeModal();

    const [currentSort, setCurrentSort] = useState('name');
    const confirmation = useConfirmation();
    const { toast } = useToast();
    const modalOpen = useRef(false);
    const [canCreateAbsenceSubType, setCanCreateAbsenceSubType] =
        useState(false);

    /**
     * Open the dialog to create a new absence sub type
     */
    const handleCreateAbsenceSubTypes = async () => {
        if (!canCreateAbsenceSubType) {
            return;
        }
        if (modalOpen.current) {
            return;
        }

        modalOpen.current = true;
        try {
            const result =
                await absenceSubTypeModalApiRef.current.openDialog('create');
            if (result) {
                toast(t('Absence Sub Type created'));
                dataTableApiRef.current?.refreshData();
            }
        } finally {
            modalOpen.current = false;
        }
    };

    const handleEditAbsenceSubType = async (e: AbsenceSubType) => {
        if (!canCreateAbsenceSubType) {
            return false;
        }
        if (modalOpen.current) {
            return false;
        }

        modalOpen.current = true;
        try {
            const api = new AbsenceSubTypesApi(session!);
            const setting = await api.getAbsenceSubType(e.id!);
            const result = await absenceSubTypeModalApiRef.current.openDialog(
                'edit',
                {
                    ...setting,
                }
            );
            if (result) {
                toast(t('Absence Sub Type updated'));
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
            setCanCreateAbsenceSubType(
                (await api.getSystemPermissions()).includes('MANAGE_SECURITY')
            );
        })();
    }, [session]);

    return (
        <>
            <Stack direction="column" spacing={2}>
                <Typography variant="h3">{t('Absence Sub Types')}</Typography>
                <Typography>{t('Absence Sub Types Page Text')}</Typography>
                <Stack direction="row" spacing={2}>
                    <Button
                        disabled={!canCreateAbsenceSubType}
                        onClick={handleCreateAbsenceSubTypes}>
                        {t('Add Absence Sub Type')}
                    </Button>
                </Stack>
                <DataTable<AbsenceSubType>
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
                            action: handleEditAbsenceSubType,
                            isEnabled: () => canCreateAbsenceSubType,
                        },
                    ]}
                    fetcher={async (
                        page: number,
                        rowsPerPage: number,
                        currentFilter?: string
                    ) => {
                        const api = new AbsenceSubTypesApi(session!);
                        try {
                            return api.getAbsenceSubTypes(
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
                                    'Error fetching absence sub types:',
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
            <AbsenceSubTypeModal apiRef={absenceSubTypeModalApiRef} />
            {confirmation.dialog}
        </>
    );
}
