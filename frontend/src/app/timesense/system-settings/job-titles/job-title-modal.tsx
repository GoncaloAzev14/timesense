'use jobTitle';

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
import { useRef, useState } from 'react';
import JobTitleApi, { JobTitle } from '../../lib/job-titles';
import { validateNotEmpty } from '@/components/validation-utils';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import dayjs from 'dayjs';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';

interface JobTitleModalApi {
    openDialog: (action: string, data?: JobTitle) => Promise<JobTitle | undefined>;
    closeDialog: () => void;
}

interface JobTitleModalProps {
    apiRef: React.MutableRefObject<JobTitleModalApi>;
}

export function useJobTitleModal() {
    return useRef<JobTitleModalApi>({
        openDialog: async () => undefined,
        closeDialog: () => { },
    });
}

export const JobTitleModal = ({ apiRef }: JobTitleModalProps) => {
    const { data: session } = useSession();
    const [currentAction, setCurrentAction] = useState('create');
    const { t } = useTranslation(undefined, 'timesense');
    const [isDialogOpen, openEnvDialog, closeEnvDialog] = useModal();
    const currentJobTitleRef = useRef<JobTitle | null>(null);
    const firstControlRef = useRef<HTMLInputElement | null>(null);

    const [nameError, setNameError] = useState(false);

    apiRef.current = {
        openDialog: async (action: string, data?: JobTitle) => {
            currentJobTitleRef.current = data || {
                name: '',
                rate: 0,
                startDate: new Date()
            };
            setCurrentAction(action);
            const result = await openEnvDialog();
            if (result) {
                const api = new JobTitleApi(session!);
                if (action === 'edit') {
                    return (await api.updateJobTitle(currentJobTitleRef.current!)).data;
                }
                return (await api.createJobTitle(currentJobTitleRef.current!)).data;
            }
            return undefined;
        },
        closeDialog: () => {
            closeEnvDialog(false);
        },
    };

    const handleApply = () => {
        const isNameValid = validateNotEmpty(currentJobTitleRef.current?.name, setNameError);

        if (isNameValid) {

            closeEnvDialog(true);
        }
    };

    return (
        <Dialog
            open={isDialogOpen}
            onClose={() => closeEnvDialog(false)}
            fullWidth>
            <DialogTitle>
                {currentAction === 'create' ? t('Create JobTitle') : t('Edit JobTitle ')}
            </DialogTitle>
            <DialogContent>
                <Stack direction="column" spacing={2}>
                    <TextField
                        label={t('Id')}
                        variant="standard"
                        fullWidth
                        value={currentJobTitleRef.current?.id}
                        disabled
                    />
                    <TextField
                        label={t('Name')}
                        variant="standard"
                        fullWidth
                        defaultValue={currentJobTitleRef.current?.name}
                        onChange={evt => {
                            const newValue = evt.target.value;
                            currentJobTitleRef.current!.name = newValue;
                            validateNotEmpty(newValue, setNameError);
                        }}
                        inputRef={firstControlRef}
                        error={nameError}
                        helperText={nameError ? t('Name cannot be empty!') : ''}
                    />
                    <TextField
                        type='number'
                        label={t('Rate')}
                        variant="standard"
                        fullWidth
                        defaultValue={currentJobTitleRef.current?.rate}
                        onChange={evt => {
                            const newValue = parseFloat(evt.target.value);
                            currentJobTitleRef.current!.rate = newValue;
                        }}
                        inputRef={firstControlRef}
                    />
                    <LocalizationProvider dateAdapter={AdapterDayjs}>
                        <DatePicker
                            label={t('StartDate')}
                            defaultValue={dayjs(currentJobTitleRef.current?.startDate)}
                            onChange={evt => {
                                currentJobTitleRef.current!.startDate = evt?.toDate();
                            }}
                        />
                        <DatePicker
                            label={t('EndDate')}
                            defaultValue={
                                currentJobTitleRef.current?.endDate
                                    ? dayjs(currentJobTitleRef.current.endDate)
                                    : null}
                            onChange={evt => {
                                currentJobTitleRef.current!.endDate = evt?.toDate();
                            }}
                        />
                    </LocalizationProvider>


                </Stack>
            </DialogContent>
            <DialogActions>
                <Button onClick={() => closeEnvDialog(false)}>
                    {t('Cancel')}
                </Button>
                <Button onClick={handleApply}>
                    {t('Save')}
                </Button>
            </DialogActions>
        </Dialog>
    );
};
