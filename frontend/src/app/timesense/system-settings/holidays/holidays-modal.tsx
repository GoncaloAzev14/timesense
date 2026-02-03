'use client';

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
import HolidayApi, { Holiday } from '../../lib/holidays';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import dayjs from 'dayjs';
import { validateNotEmpty } from '@/components/validation-utils';

interface HolidayModalApi {
    openDialog: (action: string, data?: Holiday) => Promise<Holiday | undefined>;
    closeDialog: () => void;
}

interface HolidayModalProps {
    apiRef: React.MutableRefObject<HolidayModalApi>;
}

export function useHolidayModal() {
    return useRef<HolidayModalApi>({
        openDialog: async () => undefined,
        closeDialog: () => { },
    });
}

export const HolidayModal = ({ apiRef }: HolidayModalProps) => {
    const { data: session } = useSession();
    const [currentAction, setCurrentAction] = useState('create');
    const { t } = useTranslation(undefined, 'timesense');
    const [isDialogOpen, openEnvDialog, closeEnvDialog] = useModal();
    const currentHolidayRef = useRef<Holiday | null>(null);
    const firstControlRef = useRef<HTMLInputElement | null>(null);

    const [nameError, setNameError] = useState(false);

    apiRef.current = {
        openDialog: async (action: string, data?: Holiday) => {
            currentHolidayRef.current = data || {
                holidayDate: new Date(),
                name: '',
            };
            setCurrentAction(action);
            const result = await openEnvDialog();
            if (result) {
                const api = new HolidayApi(session!);
                if (action === 'edit') {
                    return (await api.updateHoliday(currentHolidayRef.current!)).data;
                }
                return (await api.createHolidays(currentHolidayRef.current!)).data;
            }
            return undefined;
        },
        closeDialog: () => {
            closeEnvDialog(false);
        },
    };

    const handleApply = () => {
        const isNameValid = validateNotEmpty(currentHolidayRef.current?.name, setNameError);

        if (isNameValid) {

            closeEnvDialog(true);
        }
    };

    const [, setUpdate] = useState(0);
    const forceRender = () => setUpdate((prev) => prev + 1);

    return (
        <Dialog
            open={isDialogOpen}
            onClose={() => closeEnvDialog(false)}
            fullWidth>
            <DialogTitle>
                {currentAction === 'create' ? t('Create Holiday') : t('Edit Holiday ')}
            </DialogTitle>
            <DialogContent>
                <Stack direction="column" spacing={2}>
                    <LocalizationProvider dateAdapter={AdapterDayjs}>
                        <DatePicker
                            label={t('HolidayDate')}
                            defaultValue={dayjs(currentHolidayRef.current?.holidayDate)}
                            onChange={evt => {
                                currentHolidayRef.current!.holidayDate = evt!.toDate();
                                setTimeout(() => forceRender(), 100);
                            }}
                            format='DD/MM/YYYY'
                        />
                    </LocalizationProvider>
                    <TextField
                        label={t('Name')}
                        variant="standard"
                        fullWidth
                        defaultValue={currentHolidayRef.current?.name}
                        onChange={evt => {
                            const newValue = evt.target.value;
                            currentHolidayRef.current!.name = newValue;
                            validateNotEmpty(newValue, setNameError);
                        }}
                        inputRef={firstControlRef}
                        error={nameError}
                        helperText={nameError ? t('Name cannot be empty!') : ''}
                    />
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
