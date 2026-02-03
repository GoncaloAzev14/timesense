'use client';

import {
    Box,
    Button,
    FormHelperText,
    MenuItem,
    Paper,
    Stack,
    TextField,
    Typography,
} from '@mui/material';
import Select, { SelectChangeEvent } from '@mui/material/Select';
import { useEffect, useMemo, useRef, useState } from 'react';
import { DateRange, DayPicker } from 'react-day-picker';
import 'react-day-picker/dist/style.css';
import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import { useToast } from '@/components/toast-provider';
import AbsenceApi, { Absence, AbsenceAttachment } from '../../../../lib/absences';
import { useSession } from 'next-auth/react';
import { validateNotEmpty } from '@/components/validation-utils';
import Combobox, {
    Entry,
} from '@datacentric/datacentric-ui/components/combobox';
import AbsenceTypeApi, { AbsenceType } from '../../../../lib/absence-types';
import { countBusinessDays } from '../../../../components/business-days-calculator';
import HolidayApi from '../../../../lib/holidays';
import { useRouter } from 'next/navigation';
import UserApi, { User } from '../../../../lib/users';
import Loading from '@/components/load';
import AbsenceSubTypeApi, {
    AbsenceSubType,
} from '@/app/timesense/lib/absence-sub-types';
import SystemSettingApi from '@/app/timesense/lib/system-settings';
import { ErrorMessageModal } from '@/app/timesense/components/error-message-modal';
import CloudUploadIcon from "@mui/icons-material/CloudUpload";
import { FileUpload } from '@datacentric/datacentric-ui/components/file-upload';
import { ResponseEntity } from '@/lib/service-base';

export type BusinessYear = {
    id?: number;
    name: string;
};

export default function AbsenceFormPage({
    params,
}: {
    params: { action: string; id?: number };
}) {
    const { action, id } = params;
    const router = useRouter();
    const { data: session } = useSession();
    const { t } = useTranslation(undefined, 'timesense');
    const { toast } = useToast();
    const [range, setRange] = useState<DateRange | undefined>();
    const [absenceTypesList, setAbsenceTypesList] = useState<Entry[]>([]);
    const [absenceSubTypesList, setAbsenceSubTypesList] = useState<Entry[]>([]);
    const [holidaysDates, setHolidaysDates] = useState<Date[]>([]);
    const [userScheduledAbsences, setUserScheduledAbsences] = useState<Date[]>(
        []
    );
    const [userPendingAbsences, setUserPendingAbsences] = useState<Date[]>([]);
    const [workDays, setWorkDays] = useState<number | undefined>();
    const [nameError, setNameError] = useState(false);
    const [reasonError, setReasonError] = useState(false);
    const userApi = new UserApi(session!);
    const absenceApi = new AbsenceApi(session!);
    const holidayApi = new HolidayApi(session!);
    const currentAbsenceRef = useRef<Absence | null>(null);
    const currentUserRef = useRef<User | null>(null);
    const currentYearVacationDaysRef = useRef<number>(
        currentUserRef.current?.currentYearVacationDays!
    );
    const prevYearVacationDaysRef = useRef<number>(
        currentUserRef.current?.prevYearVacationDays!
    );
    const [isLoading, setIsLoading] = useState(false);
    const [refresh, setRefresh] = useState(false);
    const originalWorkDaysRef = useRef<number | null>(null);
    const [absenceType, setAbsenceType] = useState<AbsenceType | undefined>();
    const [absenceSubType, setAbsenceSubType] = useState<
        AbsenceSubType | undefined
    >();
    const [name, setName] = useState<string | undefined>(
        currentAbsenceRef.current?.name
    );
    const [absenceTypeError, setAbsenceTypeError] = useState(false);
    const [absenceSubTypeError, setAbsenceSubTypeError] = useState(false);
    const [vacationType, setVacationType] = useState<string>();
    const [selectedSingle, setSelectedSingle] = useState<Date | undefined>();
    const [errorModalOpen, setErrorModalOpen] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');
    const [errorDetails, setErrorDetails] = useState<string[]>([]);
    const attachmentsRef = useRef<File[]>([]);
    const [attachmentNames, setAttachmentNames] = useState<string[]>([]);
    const [existingAttachments, setExistingAttachments] = useState<AbsenceAttachment[]>([]);
    const [attachmentsToDelete, setAttachmentsToDelete] = useState<AbsenceAttachment[]>([]);

    const closeErrorModal = () => {
        setErrorModalOpen(false);
    };

    const openErrorModal = (error: { message: string; details: string[] }) => {
        setErrorMessage(error.message);
        setErrorDetails(error.details);
        setErrorModalOpen(true);
    };

    // useEffect to reset every var when save and create another is pressed
    useEffect(() => {
        setRange(undefined);

        setRefresh(false);
    }, [refresh]);

    const [yearSetting, setYearSetting] = useState<string>();
    const [currentYear, setCurrentYear] = useState<string>();
    const [prevYear, setPrevYear] = useState<string>();
    const [businessYearsList, setBusinessYearList] = useState<BusinessYear[]>();
    const [initialBusinessYear, setInitialBusinessYear] =
        useState<BusinessYear>();

    useEffect(() => {
        const fetchYearSetting = async () => {
            try {
                const sysSettingsApi = new SystemSettingApi(session!);
                const setting =
                    await sysSettingsApi.getSystemSettingByName('current_year');

                const value = setting?.value;
                if (value) {
                    setYearSetting(value);
                    setCurrentYear(value);

                    const prev = (parseInt(value, 10) - 1).toString();
                    setPrevYear(prev);

                    const newBusinessYearsList = [
                        { id: 0, name: value },
                        { id: 1, name: prev },
                    ];
                    setBusinessYearList(newBusinessYearsList); // this is async, but you're already using the correct list below

                    const savedYear = currentAbsenceRef.current?.businessYear;
                    const initialYear =
                        newBusinessYearsList.find(
                            entry => entry.name === savedYear
                        ) ?? newBusinessYearsList[0];

                    setInitialBusinessYear(initialYear);
                }
            } catch (error) {
                console.error('Error fetching current_year setting:', error);
            }
        };

        fetchYearSetting();
    }, [session]);

    // set up variables to use in combobox
    const [businessYear, setBusinessYear] = useState<BusinessYear>(
        initialBusinessYear!
    );
    const [value, setValue] = useState<number>(initialBusinessYear?.id ?? 0);
    useEffect(() => {
        setBusinessYear(initialBusinessYear!);
        setValue(initialBusinessYear?.id ?? 0);
    }, [session, initialBusinessYear]);

    useEffect(() => {
        setValue(businessYear?.id ?? 0);
    }, [businessYear, refresh]);

    const handleBusinessYearChange = (selected: BusinessYear) => {
        setBusinessYear(selected);
        currentAbsenceRef.current!.businessYear = selected.name;

        setTimeout(() => forceRender(), 100);
    };

    // calculate user remaining vacation days
    useEffect(() => {
        if (range?.from && range?.to) {
            const startDate = range.from;
            const endDate = range.to;

            // Calculate workDays
            const workDays = countBusinessDays(
                startDate,
                endDate,
                holidaysDates,
                currentAbsenceRef.current?.recordType
            );

            // Update the currentAbsenceRef
            currentAbsenceRef.current = {
                ...currentAbsenceRef.current,
                startDate,
                endDate,
                workDays,
            } as Absence;

            if (currentAbsenceRef.current.type?.name === 'VACATION') {
                if (action === 'Create') {
                    if (
                        currentAbsenceRef.current.businessYear ===
                        currentYear!.toString()
                    ) {
                        const currentYearVacationDays =
                            currentYearVacationDaysRef.current - workDays;

                        // Update the user remaining vacation days
                        currentUserRef.current = {
                            ...currentUserRef.current,
                            currentYearVacationDays,
                        } as User;
                    } else if (
                        currentAbsenceRef.current.businessYear ===
                        prevYear!.toString()
                    ) {
                        const prevYearVacationDays =
                            prevYearVacationDaysRef.current - workDays;

                        // Update the user remaining vacation days
                        currentUserRef.current = {
                            ...currentUserRef.current,
                            prevYearVacationDays,
                        } as User;
                    }
                } else {
                    // On create mode only update the remaining vacation days if the workDays
                    // are diffrent from the previous
                    let currentYearVacationDays =
                        currentYearVacationDaysRef.current;
                    let prevYearVacationDays = prevYearVacationDaysRef.current;

                    const prevWorkDays = originalWorkDaysRef.current ?? 0;
                    const diff = workDays - prevWorkDays;

                    if (
                        currentAbsenceRef.current.businessYear ===
                        currentYear!.toString()
                    ) {
                        currentYearVacationDays =
                            currentYearVacationDaysRef.current - diff;

                        // Update the user remaining vacation days
                        currentUserRef.current = {
                            ...currentUserRef.current,
                            currentYearVacationDays,
                        } as User;
                    } else if (
                        currentAbsenceRef.current.businessYear ===
                        prevYear!.toString()
                    ) {
                        prevYearVacationDays =
                            prevYearVacationDaysRef.current - diff;

                        // Update the user remaining vacation days
                        currentUserRef.current = {
                            ...currentUserRef.current,
                            prevYearVacationDays,
                        } as User;
                    }
                }
            }
        }
    }, [absenceType, range, businessYear, vacationType]);

    useEffect(() => {
        handleDayPickerChange(undefined);
    }, [session]);

    const handleDayPickerChange = (value: any) => {
        if (value instanceof Date) {
            const from = new Date(value.setHours(0, 0, 0, 0));
            const to = new Date(value.setHours(23, 59, 59, 999));
            setSelectedSingle(value as Date | undefined);
            setRange({ from, to });
        } else {
            setRange(value);
        }
    };

    // Calculate business days
    useEffect(() => {
        if (range?.from && range?.to) {
            const businessDays = countBusinessDays(
                range.from,
                range.to,
                holidaysDates,
                currentAbsenceRef.current?.recordType
            );
            setWorkDays(businessDays);
        } else {
            setWorkDays(0);
        }
    }, [range, vacationType]);

    // Fetch default data
    useEffect(() => {
        const loadAbsence = async () => {
            if (action === 'edit' || action === 'view') {
                if (id) {
                    try {
                        const abs = await absenceApi.getAbsence(id);
                        currentAbsenceRef.current = abs;
                        const attachments = await absenceApi.getAttachmentsByAbsenceId(id);
                        setExistingAttachments(attachments);
                        const user = await userApi.getUser(abs.user!.id!);
                        currentYearVacationDaysRef.current =
                            user.currentYearVacationDays!;
                        prevYearVacationDaysRef.current =
                            user.prevYearVacationDays!;
                        currentUserRef.current = user;
                        originalWorkDaysRef.current =
                            currentAbsenceRef.current.workDays!;
                        setRange({
                            from: new Date(abs.startDate),
                            to: new Date(abs.endDate),
                        });
                    } catch (e) {
                        toast(t('Failed to load absence'));
                    }
                }
            } else {
                try {
                    const user = await userApi.getUserInfo();
                    currentUserRef.current = user;
                    currentYearVacationDaysRef.current =
                        user.currentYearVacationDays!;
                    prevYearVacationDaysRef.current =
                        user.prevYearVacationDays!;
                    let lineManager = undefined;
                    if (
                        user.lineManagerId != null ||
                        user.lineManagerId != undefined
                    ) {
                        lineManager = await userApi.getUser(
                            user.lineManagerId!
                        );
                    }

                    const vacationEntry = absenceTypesList.find(
                        entry => entry.name === 'VACATION'
                    );
                    if (initialBusinessYear) {
                        currentAbsenceRef.current = {
                            name: '',
                            approver: lineManager ? lineManager : undefined,
                            startDate: new Date(),
                            endDate: new Date(),
                            user: currentUserRef.current,
                            businessYear: initialBusinessYear!.name,
                            // set default absence type to VACATION
                            type: {
                                id: vacationEntry?.id,
                                name: vacationEntry ? vacationEntry.name : '',
                            },
                        };
                    }
                } catch (e) {
                    console.log(e);
                    toast(t('Failed to load absence'));
                }
            }
        };
        if (session && absenceTypesList) {
            loadAbsence();
            setTimeout(() => forceRender(), 100);
        }
    }, [session, action, id, refresh, initialBusinessYear]);

    // Fetch holidays
    // Set the default Holidays Dates
    useEffect(() => {
        const fetchHolidays = async () => {
            try {
                const holidays = await holidayApi.getHolidays();
                setHolidaysDates(holidays.map(h => new Date(h.holidayDate)));
            } catch {
                toast(t('Error fetching holidays'));
            }
        };
        fetchHolidays();
    }, [refresh]);

    // Fetch user absences
    useEffect(() => {
        const fetchUserApprovedAbsences = async () => {
            const filter = 'status.name=APPROVED,type.name=VACATION';
            const page = await absenceApi.getUserAbsences(
                0,
                100,
                undefined,
                filter
            );
            const dates: Date[] = [];
            page.content.forEach(a => {
                let start = new Date(a.startDate!);
                const end = new Date(a.endDate!);
                while (start <= end) {
                    dates.push(new Date(start));
                    start.setDate(start.getDate() + 1);
                }
            });
            setUserScheduledAbsences(dates);
        };
        const fetchUserPendingAbsences = async () => {
            const filter = 'status.name=PENDING,type.name=VACATION';
            const page = await absenceApi.getUserAbsences(
                0,
                100,
                undefined,
                filter
            );
            const dates: Date[] = [];
            page.content.forEach(a => {
                let start = new Date(a.startDate!);
                const end = new Date(a.endDate!);
                while (start <= end) {
                    dates.push(new Date(start));
                    start.setDate(start.getDate() + 1);
                }
            });
            setUserPendingAbsences(dates);
        };
        fetchUserApprovedAbsences();
        fetchUserPendingAbsences();
    }, [refresh]);

    // Fetch Absence Types and Sub Types
    useEffect(() => {
        const typeApi = new AbsenceTypeApi(session!);
        try {
            typeApi.getAbsenceTypes(0, 1000).then(data => {
                setAbsenceTypesList(
                    data.content.map(e => ({
                        id: e.id!,
                        name: e.name!,
                    }))
                );
            });
            setTimeout(() => forceRender(), 100);
        } catch (e) {
            console.error('Error fetching absence types');
        }
        const subTypeApi = new AbsenceSubTypeApi(session!);
        try {
            subTypeApi.getAbsenceSubTypes(0, 100).then(data => {
                setAbsenceSubTypesList(
                    data.content.map(e => ({
                        id: e.id!,
                        name: e.name!,
                    }))
                );
            });
            setTimeout(() => forceRender(), 100);
        } catch (e) {
            console.error('Error fetching absence types');
        }
    }, [session, refresh]);

    // force render
    const [, setUpdate] = useState(0);
    const forceRender = () => setUpdate(prev => prev + 1);

    const handleAbsenceTypeChange = (evt: Entry) => {
        currentAbsenceRef.current!.type = evt;
        setAbsenceType(evt);
        setAbsenceTypeError(false);
        setTimeout(() => forceRender(), 100);
    };

    const handleAbsenceSubTypeChange = (evt: Entry) => {
        currentAbsenceRef.current!.subType = evt;
        setAbsenceSubType(evt);
        setAbsenceSubTypeError(false);
        setTimeout(() => forceRender(), 100);
    };

    const handleDownloadAtachment = async (attachmentId: number, fileName: string) => {
        try {
            const absenceId = currentAbsenceRef.current?.id;

            if (!absenceId) {
                return;
            }

            await absenceApi.donwloadAttachment(absenceId, attachmentId, fileName);
        } catch (error) {
            console.error(error);
            toast(t('Failed to download attachment'));
        }
    }

    const showValidationErrors = (result: ResponseEntity<Absence>) => {

        if (
            result.messageCode === 'API_INVALID_VACS_400_01' ||
            result.messageCode === 'API_INTERNAL_400_08'
        ) {
            openErrorModal({
                message: result.message,
                details: [],
            });
            return true;
        }

        if (
            result.messageCode !== 'API_INTERNAL_400_02' ||
            !Array.isArray(result.data)
        ) {
            return false;
        }

        const messages: string[] = [];

        result.data.forEach((err: any) => {
            switch (err.column) {
                case 'name':
                    setNameError(true);
                    messages.push(t('Description cannot be empty'));
                    break;

                case 'reason':
                    setReasonError(true);
                    messages.push(t('Reason cannot be empty'));
                    break;

                default:
                    messages.push(err.message);
            }
        });

        openErrorModal({
            message: messages.join('\n'),
            details: [],
        });

        return true;
    };

    const validateAbsence = (): boolean => {
        const absence = currentAbsenceRef.current!;
        const user = currentUserRef.current!;

        if (absence.recordType === undefined) {
            absence.recordType = 'Day';
        }

        if (!range?.from || !range?.to) {
            toast('Start and End Dates Validation');
            return false;
        }

        if (absence.type?.name === 'VACATION') {
            if (!currentYear || !prevYear) {
                toast(t('System data still loading, please try again'));
                return false;
            }

            let selectedYearVacationDays: number;

            if (absence.businessYear === currentYear) {
                selectedYearVacationDays = user.currentYearVacationDays ?? 0;
            } else if (absence.businessYear === prevYear) {
                selectedYearVacationDays = user.prevYearVacationDays ?? 0;
            } else {
                openErrorModal({
                    message: t('Cannot modify vacation days for this business year'),
                    details: [`Business year: ${absence.businessYear}`],
                });
                return false;
            }

            if (selectedYearVacationDays < 0.0) {
                openErrorModal({
                    message: t("You've exceeded your available vacation days!"),
                    details: [],
                });
                return false;
            }
        }

        if (absence.type?.id === undefined) {
            setAbsenceTypeError(true);
            return false;
        }

        if (
            absence.recordType !== 'Day' &&
            absence.type?.name === 'ABSENCES' &&
            absence.absenceHours === undefined
        ) {
            openErrorModal({
                message: t(
                    'Please enter the number of hours to be recorded on you absence.'
                ),
                details: [],
            });
            return false;
        }

        if (
            absence.type?.name === 'ABSENCES' &&
            absence.subType?.name === undefined
        ) {
            openErrorModal({
                message: t('Please enter the absence sub type.'),
                details: [],
            });
            return false;
        }

        return true;
    };

    const handleSaveAbsence = async () => {

        if (!validateAbsence()) {
            return;
        }

        const absence = currentAbsenceRef.current!;

        try {
            setIsLoading(true);
            if (action === 'edit') {
                const result = await absenceApi.updateAbsence(absence);

                if (showValidationErrors(result)) {
                    setIsLoading(false);
                    return;
                }

                const absenceId = absence?.id;
                if (absenceId) {
                    try {
                        if (attachmentsToDelete.length > 0) {
                            await deleteMarkedAttachments(absenceId);
                            setAttachmentsToDelete([]);
                        }

                        if (attachmentsRef.current.length > 0) {

                            const formData = new FormData();
                            attachmentsRef.current.forEach(file => {
                                formData.append("files", file, file.name);
                            });

                            const uploadResult = await absenceApi.uploadAttachment(absenceId, formData);

                            if (uploadResult.messageCode !== 'API_ABSENCE_200_02') {
                                throw new Error(uploadResult.message || 'Upload failed');
                            }

                            attachmentsRef.current = [];
                            setAttachmentNames([]);
                        }
                    } catch (attachmentError: any) {
                        console.error('Attachment processing error:', attachmentError);
                        setIsLoading(false);
                        openErrorModal({
                            message: t('Error processing attachments: ') + attachmentError.message,
                            details: [],
                        });
                        return;
                    }
                }

                toast(t('Absence Updated!'));
                router.push('/timesense/absences');
            } else {
                setIsLoading(true);
                const result = await absenceApi.createAbsence(absence);

                if (showValidationErrors(result)) {
                    setIsLoading(false);
                    return;
                }

                toast('Absence Created!');
                const absenceId = result.data.id!;
                if (attachmentsRef.current.length > 0) {
                    try {
                        const formData = new FormData();
                        attachmentsRef.current.forEach(file => {
                            formData.append("files", file, file.name);
                        });

                        const uploadResult = await absenceApi.uploadAttachment(absenceId, formData);
                        if (uploadResult.messageCode !== 'API_ABSENCE_200_02') {
                            throw new Error(uploadResult.message || 'Upload failed');
                        }
                    } catch (attachmentError: any) {
                        console.error('Attachment upload error:', attachmentError);
                        openErrorModal({
                            message: t('Absence created but attachment upload failed: ') + attachmentError.message,
                            details: [],
                        });
                        setIsLoading(false);
                        return;
                    }
                }
                toast(t('Absence Created!'));
                router.push('/timesense/absences');
            }
            setIsLoading(false);

        } catch (e: any) {
            setIsLoading(false);
            toast(e.message || t('Failed to save absence'));
            openErrorModal({
                message: e.message || 'Unknown error',
                details: [e.responseText || ''],
            });
            return false;
        }
    };

    const handleSaveAbsenceAndCreateAnother = async () => {

        if (!validateAbsence()) {
            return;
        }

        const absence = currentAbsenceRef.current!;

        try {
            setIsLoading(true);
            if (action === 'edit') {
                const result = await absenceApi.updateAbsence(absence);

                if (
                    result.messageCode === 'API_INTERNAL_400_02' &&
                    Array.isArray(result.data)
                ) {
                    const messages: string[] = [];

                    result.data.forEach((err: any) => {
                        switch (err.column) {
                            case 'name':
                                setNameError(true);
                                messages.push(t('Description cannot be empty'));
                                break;

                            case 'reason':
                                setReasonError(true);
                                messages.push(t('Reason cannot be empty'));
                                break;

                            default:
                                messages.push(err.message);
                        }
                    });

                    openErrorModal({
                        message: messages.join('\n'),
                        details: [],
                    });

                    setIsLoading(false);
                    return;
                }

                if (
                    result.messageCode === 'API_INVALID_VACS_400_01' ||
                    result.messageCode === 'API_INTERNAL_400_08'
                ) {
                    openErrorModal({
                        message: result.message,
                        details: [],
                    });
                }

                // if the action is edit change the page router when creating a new absence
                // in order to not continue editing the previous one with the other url
                const absenceId = currentAbsenceRef.current?.id;
                if (absenceId) {
                    // Primeiro eliminar os marcados para exclusão
                    if (attachmentsToDelete.length > 0) {
                        await deleteMarkedAttachments(absenceId);
                    }

                    // Depois fazer upload dos novos
                    if (attachmentsRef.current.length > 0) {
                        const formData = new FormData();
                        attachmentsRef.current.forEach(file => {
                            formData.append("files", file, file.name);
                        });

                        const uploadResult = await absenceApi.uploadAttachment(absenceId, formData);
                        if (uploadResult.messageCode !== 'API_ABSENCE_200_02') {
                            setIsLoading(false);
                            openErrorModal({
                                message: uploadResult.message,
                                details: [],
                            });
                            return;
                        }
                    }
                }

                toast(t('Absence Updated!'));
                setExistingAttachments([]);
                setAttachmentsToDelete([]);
                attachmentsRef.current = [];
                setAttachmentNames([]);
                router.push('/timesense/absences/timeoff-calendar/create/new');
                router.refresh();
                setRefresh(true);
                setIsLoading(false);
            } else {
                const result = await absenceApi.createAbsence(absence);

                if (
                    result.messageCode === 'API_INTERNAL_400_02' &&
                    Array.isArray(result.data)
                ) {
                    const messages: string[] = [];

                    result.data.forEach((err: any) => {
                        switch (err.column) {
                            case 'name':
                                setNameError(true);
                                messages.push(t('Description cannot be empty'));
                                break;

                            case 'reason':
                                setReasonError(true);
                                messages.push(t('Reason cannot be empty'));
                                break;

                            default:
                                messages.push(err.message);
                        }
                    });

                    openErrorModal({
                        message: messages.join('\n'),
                        details: [],
                    });

                    setIsLoading(false);
                    return;
                }

                if (
                    result.messageCode === 'API_INVALID_VACS_400_01' ||
                    result.messageCode === 'API_INTERNAL_400_08'
                ) {
                    openErrorModal({
                        message: result.message,
                        details: [],
                    });
                }
                const absenceId = result.data.id!;
                if (attachmentsRef.current.length > 0) {
                    try {
                        const formData = new FormData();
                        attachmentsRef.current.forEach(file => {
                            formData.append("files", file, file.name);
                        });

                        const uploadResult = await absenceApi.uploadAttachment(absenceId, formData);
                        if (uploadResult.messageCode !== 'API_ABSENCE_200_02') {
                            throw new Error(uploadResult.message || 'Upload failed');
                        }

                        // limpar attachments
                        attachmentsRef.current = [];
                        setAttachmentNames([]);
                        setExistingAttachments([]);
                        setAttachmentsToDelete([]);

                    } catch (attachmentError: any) {
                        console.error('Attachment upload error:', attachmentError);
                        openErrorModal({
                            message: t('Absence created but attachment upload failed: ') + attachmentError.message,
                            details: [],
                        });
                        setIsLoading(false);
                        return;
                    }
                }

                router.refresh();
                setRefresh(true);
                setIsLoading(false);
            }
        } catch (e: any) {
            setIsLoading(false);
            toast(e.message || t('Failed to save absence'));
            return false;
        }
    };

    const deleteMarkedAttachments = async (absenceId: number) => {

        for (const att of attachmentsToDelete) {
            if (!att.id)
                continue;

            try {

                const success = await absenceApi.deleteAttachment(
                    absenceId,
                    att.id
                );

                if (success) {
                    console.log(`Attachment ${att.id} deleted successfully`);
                } else {
                    console.error(`Failed to delete attachment ${att.id}`);
                }
            } catch (error) {
                console.error(`Error deleting attachment ${att.id}:`, error);
                // Continua com os próximos mesmo se um falhar
            }
        }
    };

    const handleVacationTypeChange = (event: SelectChangeEvent) => {
        setVacationType(event.target.value as string);
        currentAbsenceRef.current!.recordType = event.target.value;
    };

    const fileInputRef = useRef<HTMLInputElement | null>(null);
    const handleSaveAttachment = async (
        event: React.ChangeEvent<HTMLInputElement>
    ) => {
        const files = event.target.files;
        if (!files || files.length === 0) return;

        // Converte FileList para array e adiciona aos existentes
        const newFiles = Array.from(files);
        attachmentsRef.current = [...attachmentsRef.current, ...newFiles];
        setAttachmentNames(attachmentsRef.current.map(f => f.name));

        event.target.value = '';
    };

    return (
        <Stack spacing={1.5}>
            <Typography variant="h3">{t('Absences')}</Typography>

            <Box display="flex" gap={2}>
                <Box flex={3}>
                    <Combobox
                        label={t('Absence Type')}
                        value={currentAbsenceRef.current?.type?.id}
                        onChange={handleAbsenceTypeChange}
                        entries={absenceTypesList}
                        sx={{ width: '100%' }}
                    />
                    {absenceTypeError && (
                        <FormHelperText error>
                            {t('Please select an absence type!')}
                        </FormHelperText>
                    )}
                </Box>
                <Box flex={0.25}>
                    <Select
                        value={
                            currentAbsenceRef.current?.recordType !== undefined
                                ? currentAbsenceRef.current?.recordType
                                : 'Day'
                        }
                        label="Record Type"
                        onChange={handleVacationTypeChange}
                        sx={{ width: '100%' }}>
                        <MenuItem value={'Day'}>{t('Day')}</MenuItem>
                        <MenuItem value={'Morning'}>{t('Morning')}</MenuItem>
                        <MenuItem value={'Afternoon'}>
                            {t('Afternoon')}
                        </MenuItem>
                        {currentAbsenceRef.current?.type?.name ===
                        'ABSENCES' ? (
                            <MenuItem value={'Hourly'}>{t('Hourly')}</MenuItem>
                        ) : undefined}
                    </Select>
                </Box>
                {currentAbsenceRef.current?.type?.name === 'ABSENCES' &&
                currentAbsenceRef.current?.recordType !== 'Day' &&
                currentAbsenceRef.current?.recordType !== undefined ? (
                    <Box flex={1}>
                        <TextField
                            label={t('Hours')}
                            variant="standard"
                            fullWidth
                            type="number"
                            defaultValue={
                                currentAbsenceRef.current?.absenceHours
                            }
                            onChange={evt => {
                                currentAbsenceRef.current!.absenceHours =
                                    parseFloat(evt.target.value);
                            }}
                            InputProps={{ inputProps: { min: 0.0, max: 8.0 } }}
                        />
                    </Box>
                ) : undefined}
                {currentAbsenceRef.current?.type?.name !== 'VACATION' &&
                    currentAbsenceRef.current?.type?.id !== undefined && (
                        <Box flex={3}>
                            <Combobox
                                label={t('Absence Sub Types')}
                                value={currentAbsenceRef.current?.subType?.id}
                                onChange={handleAbsenceSubTypeChange}
                                entries={absenceSubTypesList}
                                sx={{ width: '100%' }}
                            />

                            {absenceSubTypeError && (
                                <FormHelperText error>
                                    {t('Please select an absence sub type!')}
                                </FormHelperText>
                            )}
                        </Box>
                    )}
                <Box flex={1}>
                    <Combobox
                        label={t('Business Year')}
                        value={value}
                        onChange={handleBusinessYearChange}
                        entries={businessYearsList as Entry[]}
                        sx={{ width: '100%' }}
                    />
                </Box>
            </Box>

            <TextField
                label={t('Description')}
                variant="standard"
                fullWidth
                defaultValue={currentAbsenceRef.current?.name}
                onChange={evt => {
                    const newValue = evt.target.value;
                    currentAbsenceRef.current!.name = newValue;
                    validateNotEmpty(newValue, setNameError);
                    setName(newValue);
                }}
                error={nameError}
                helperText={nameError ? t('Name cannot be empty!') : ''}
                InputLabelProps={
                    currentAbsenceRef.current?.name || name
                        ? { shrink: true }
                        : { shrink: false }
                }
            />
            {currentAbsenceRef.current?.type?.name === 'ABSENCES' &&
            currentAbsenceRef.current?.subType?.name !== 'Birthday Leave' ? (
                <TextField
                    label={t('Reason')}
                    variant="standard"
                    fullWidth
                    defaultValue={currentAbsenceRef.current?.reason}
                    onChange={evt => {
                        const newValue = evt.target.value;
                        currentAbsenceRef.current!.reason = newValue;
                        validateNotEmpty(newValue, setReasonError);
                    }}
                    error={reasonError}
                    helperText={reasonError ? t('Reason cannot be empty!') : ''}
                />
            ) : null}

            <Stack
                direction="row"
                spacing={0}
                alignItems="flex-start"
                marginBottom={0}>
                <Box
                    sx={{
                        transform: 'scale(0.85)',
                        transformOrigin: 'top left',
                    }}
                    marginBottom={0}>
                    {currentAbsenceRef.current?.recordType === 'Morning' ||
                    currentAbsenceRef.current?.recordType === 'Afternoon' ||
                    currentAbsenceRef.current?.recordType === 'Hourly' ? (
                        <DayPicker
                            mode="single"
                            selected={selectedSingle}
                            onSelect={handleDayPickerChange}
                            numberOfMonths={8}
                            disabled={[holidaysDates]}
                            modifiers={{
                                holiday: holidaysDates,
                                approvedVacations: userScheduledAbsences,
                                pendingVacations: userPendingAbsences,
                            }}
                            modifiersClassNames={{
                                holiday: 'holiday-day',
                                approvedVacations: 'marked-day',
                                pendingVacations: 'pending-vacations',
                            }}
                        />
                    ) : (
                        <DayPicker
                            mode="range"
                            selected={range}
                            onSelect={handleDayPickerChange}
                            numberOfMonths={8}
                            disabled={[holidaysDates]}
                            modifiers={{
                                holiday: holidaysDates,
                                approvedVacations: userScheduledAbsences,
                                pendingVacations: userPendingAbsences,
                            }}
                            modifiersClassNames={{
                                holiday: 'holiday-day',
                                approvedVacations: 'marked-day',
                                pendingVacations: 'pending-vacations',
                            }}
                        />
                    )}
                </Box>

                <Stack spacing={4}>
                    <Paper elevation={1} sx={{ p: 3, minWidth: 280 }}>
                        <Typography variant="h5">{t('Record Details')}</Typography>
                        {currentAbsenceRef.current?.approver?.name ? (
                            <Typography>
                                <strong>
                                    {t('Approver')}:
                                </strong>
                                {currentAbsenceRef.current?.approver?.name}
                            </Typography>
                        ) : ''}
                        <Typography>
                            <strong>{t('Business Days Used')}:</strong> {workDays}
                        </Typography>
                        {currentAbsenceRef.current?.type?.name === 'VACATION' ? (
                            <Typography
                                color={
                                    action === 'Create'
                                        ? (
                                            (currentAbsenceRef.current?.businessYear === currentYear!.toString()
                                                ? currentYearVacationDaysRef.current - workDays!
                                                : prevYearVacationDaysRef.current - workDays!) < 0.0
                                                ? 'red'
                                                : 'textPrimary'
                                        )
                                        : (
                                            (
                                                currentAbsenceRef.current?.businessYear === currentYear!.toString()
                                                    ? (currentYearVacationDaysRef.current + originalWorkDaysRef.current! - workDays!)
                                                    : (prevYearVacationDaysRef.current + originalWorkDaysRef.current! - workDays!)
                                            ) < 0
                                                ? 'red'
                                                : 'textPrimary'
                                        )
                                }
                            >
                                <strong>{t('Remaining Vacation Days')}:</strong>{' '}
                                {action === 'Create' ? (currentAbsenceRef.current?.businessYear === currentYear!.toString()
                                    ? currentYearVacationDaysRef.current - workDays!
                                    : prevYearVacationDaysRef.current - workDays!)
                                    :
                                    (currentAbsenceRef.current?.businessYear === currentYear!.toString()
                                        ? (currentYearVacationDaysRef.current + originalWorkDaysRef.current! - workDays!)
                                        : (prevYearVacationDaysRef.current + originalWorkDaysRef.current! - workDays!))}
                            </Typography>
                        ) : ''}
                        <Typography>
                            <strong>{t('Status')}:</strong> {currentAbsenceRef.current?.status?.name}
                        </Typography>
                    </Paper>
                    {currentAbsenceRef.current?.type?.name === 'ABSENCES' && (
                        <Paper elevation={1} sx={{ p: 2, minWidth: 280 }}>
                            <Stack spacing={2}>
                                <Typography variant="h6">{t('Attachments')}</Typography>

                                {/* Add new attachments */}
                                <FileUpload
                                    label={t('Add Attachment')}
                                    onUpload={handleSaveAttachment}
                                    multiple
                                />

                                {/* Existing attachments (EDIT mode) */}
                                {action === 'edit' && existingAttachments.length > 0 && (
                                    <Stack spacing={1}>
                                        <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
                                            {t('Existing Attachments')} ({existingAttachments.length})
                                        </Typography>

                                        {existingAttachments.map(att => (
                                            <Paper
                                                key={att.id}
                                                variant="outlined"
                                                sx={{
                                                    p: 1.5,
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    justifyContent: 'space-between',
                                                    '&:hover': {
                                                        bgcolor: 'action.hover'
                                                    }
                                                }}
                                            >
                                                <Typography
                                                    variant="body2"
                                                    component="a"
                                                    onClick={() => handleDownloadAtachment(
                                                        att.id,
                                                        att.originalFileName
                                                    )}
                                                    sx={{
                                                        flex: 1,
                                                        overflow: 'hidden',
                                                        textOverflow: 'ellipsis',
                                                        whiteSpace: 'nowrap',
                                                        cursor: 'pointer',
                                                        color: '#1976d2',
                                                        textDecoration: 'none',
                                                        '&:hover': {
                                                            textDecoration: 'underline',
                                                        }
                                                    }}
                                                >
                                                    {att.originalFileName}
                                                </Typography>

                                                <Button
                                                    size="small"
                                                    color="error"
                                                    variant="outlined"
                                                    onClick={() => {
                                                        setAttachmentsToDelete(prev => [...prev, att]);
                                                        setExistingAttachments(prev =>
                                                            prev.filter(a => a.id !== att.id)
                                                        );
                                                    }}
                                                    sx={{ ml: 1 }}
                                                >
                                                    {t('Remove')}
                                                </Button>
                                            </Paper>
                                        ))}
                                    </Stack>
                                )}

                                {/* Newly added (not yet saved) */}
                                {attachmentNames.length > 0 && (
                                    <Stack spacing={1}>
                                        <Typography variant="subtitle2" sx={{ fontWeight: 600, color: 'info.main' }}>
                                            {t('New Attachments')} ({attachmentNames.length})
                                        </Typography>

                                        {attachmentNames.map((name, index) => (
                                            <Paper
                                                key={index}
                                                variant="outlined"
                                                sx={{
                                                    p: 1.5,
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    justifyContent: 'space-between',
                                                    bgcolor: 'info.lighter',
                                                    borderColor: 'info.light'
                                                }}
                                            >
                                                <Typography
                                                    variant="body2"
                                                    sx={{
                                                        flex: 1,
                                                        overflow: 'hidden',
                                                        textOverflow: 'ellipsis',
                                                        whiteSpace: 'nowrap'
                                                    }}
                                                >
                                                    {name}
                                                </Typography>

                                                <Button
                                                    size="small"
                                                    color="error"
                                                    variant="outlined"
                                                    onClick={() => {
                                                        attachmentsRef.current = attachmentsRef.current.filter((_, i) => i !== index);
                                                        setAttachmentNames(attachmentsRef.current.map(f => f.name));
                                                    }}
                                                    sx={{ ml: 1 }}
                                                >
                                                    {t('Remove')}
                                                </Button>
                                            </Paper>
                                        ))}
                                    </Stack>
                                )}

                                {existingAttachments.length === 0 && attachmentNames.length === 0 && (
                                    <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', py: 2 }}>
                                        {t('No attachments added')}
                                    </Typography>
                                )}
                            </Stack>
                        </Paper>
                    )}
                </Stack>
            </Stack>

            <Stack direction="row" spacing={1} sx={{ mt: 1 }}>
                {/* MAYBE USE A SPLIT BUTTON  */}
                <Button
                    variant="outlined"
                    color="info"
                    onClick={handleSaveAbsence}
                    disabled={!range?.from || !range?.to}
                >
                    {isLoading ? <Loading /> : t('Save')}
                </Button>
                {/* MAYBE USE LOADING */}
                <Button
                    variant="outlined"
                    color="info"
                    onClick={handleSaveAbsenceAndCreateAnother}
                    disabled={!range?.from || !range?.to}>
                    {isLoading ? <Loading /> : t('Save and Create Another')}
                </Button>
                <Button
                    variant="outlined"
                    color="error"
                    onClick={() => router.push('/timesense/absences')}>
                    {t('Cancel')}
                </Button>
            </Stack>

            <ErrorMessageModal
                open={errorModalOpen}
                onClose={closeErrorModal}
                message={errorMessage}
                details={errorDetails}
                title={t('Error Saving Absences')}
            />

            <style>{`
            .holiday-day {
                position: relative;
                background-color:rgb(252, 231, 231) !important;
                color: red;
            }
            .holiday-day::after {
                content: "F";
                position: absolute;
                bottom: 2px;
                right: 2px;
                font-size: 0.75rem;
            }

            .marked-day {
              position: relative;
              background-color:rgb(199, 241, 232) !important;
              color: green;
            }
            .marked-day::after {
              content: "🏖️";
              position: absolute;
              bottom: 2px;
              right: 2px;
              font-size: 0.75rem;
            }

            .pending-vacations {
              position: relative;
              background-color:rgb(234, 237, 241) !important;
              color: grey;
            }
            .pending-vacations::after {
              content: "🏖️";
              position: absolute;
              bottom: 2px;
              right: 2px;
              font-size: 0.75rem;
            }
          `}</style>
        </Stack>
    );
}
