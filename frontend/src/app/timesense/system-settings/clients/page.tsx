'use client';

import React, { useEffect, useRef, useState } from 'react';
import { useSession } from 'next-auth/react';
import { Button, Stack, Typography } from '@mui/material';
import ClientsApi, { Client } from '../../lib/clients';
import {
    DataTable,
    useDataTableApiRef,
} from '@datacentric/datacentric-ui/components/datatable';
import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import { ClientModal, useClientModal } from './client-modal';
import { emptyPage } from '@datacentric/datacentric-ui/lib/spring-pagination';
import useConfirmation from '@datacentric/datacentric-ui/components/confirmation';
import { useToast } from '@/components/toast-provider';
import PermissionsApi from '../../lib/permissions';
import { ServiceError } from '@/lib/service-base';

export default function ClientsPage() {
    const { data: session, update } = useSession();
    const { t } = useTranslation(undefined, 'timesense');
    const dataTableApiRef = useDataTableApiRef<Client>();
    const clientModalApiRef = useClientModal();
    const [currentSort, setCurrentSort] = useState('name');
    const confirmation = useConfirmation();
    const { toast } = useToast();
    const modalOpen = useRef(false);
    const [canCreateClient, setCanCreateClient] = useState(false);

    /**
     * Open the dialog to create a new environment
     */
    const handleCreateClient = async () => {
        if (!canCreateClient) {
            return;
        }
        if (modalOpen.current) {
            return;
        }

        modalOpen.current = true;
        try {
            const result = await clientModalApiRef.current.openDialog('create');
            if (result) {
                toast(t('Client created'));
                dataTableApiRef.current?.refreshData();
            }
        } finally {
            modalOpen.current = false;
        }
    };

    const handleEditClient = async (e: Client) => {
        if (!canCreateClient) {
            return false;
        }
        if (modalOpen.current) {
            return false;
        }

        modalOpen.current = true;
        try {
            const api = new ClientsApi(session!);
            const client = await api.getClient(e.id!);
            const result = await clientModalApiRef.current.openDialog('edit', {
                ...client,
            });
            if (result) {
                toast(t('Client updated'));
                return true;
            }
            return false;
        } finally {
            modalOpen.current = false;
        }
    };

    const handleDeleteClient = async (e: Client) => {
        if (!canCreateClient) {
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
                    t('Client deletion question', { name: e.name }),
                    t('Yes'),
                    t('No')
                ))
            ) {
                return false;
            }
            const api = new ClientsApi(session!);
            try {
                await api.deleteClient(e.id!);
                toast(t('Client deleted'));
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
            setCanCreateClient(
                (await api.getSystemPermissions()).includes('CREATE_PROJECTS')
            );
        })();
    }, [session]);

    return (
        <>
            <Stack direction="column" spacing={2}>
                <Typography variant="h3">{t('Clients')}</Typography>
                <Stack direction="row" spacing={2}>
                    <Button
                        disabled={!canCreateClient}
                        onClick={handleCreateClient}>
                        {t('Add Client')}
                    </Button>
                </Stack>
                <DataTable<Client>
                    columns={[
                        { field: 'id', title: t('Id') },
                        {
                            field: 'name',
                            title: t('Name'),
                            filter: true,
                        },
                        { field: 'clientTicker', title: t('Client Ticker') },
                    ]}
                    currentSort={currentSort}
                    apiRef={dataTableApiRef}
                    rowActions={[
                        {
                            label: t('Edit'),
                            icon: 'edit',
                            action: handleEditClient,
                            isEnabled: () => canCreateClient,
                        },
                        {
                            label: t('Delete'),
                            icon: 'delete',
                            action: handleDeleteClient,
                            isEnabled: () => canCreateClient,
                        },
                    ]}
                    fetcher={async (
                        page: number,
                        rowsPerPage: number,
                        currentFilter?: string
                    ) => {
                        const api = new ClientsApi(session!);
                        try {
                            return api.getClients(
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
            <ClientModal apiRef={clientModalApiRef} />
            {confirmation.dialog}
        </>
    );
}
