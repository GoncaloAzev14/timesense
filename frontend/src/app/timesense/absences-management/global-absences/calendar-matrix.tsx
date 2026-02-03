import { useToast } from '@/components/toast-provider';
import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import {
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Typography,
    Box,
    Popover,
    Divider,
    Stack,
    Avatar,
    IconButton,
    FormControlLabel,
    Checkbox,
    styled,
    useTheme,
} from '@mui/material';
import { MouseEventHandler, useEffect, useState } from 'react';
import HolidayApi from '../../lib/holidays';
import { useSession } from 'next-auth/react';
import { Absence, AbsencesByDateMap, StatusCounts } from '../../lib/absences';
import PersonIcon from '@mui/icons-material/Person';
import format from '@datacentric/datacentric-ui/lib/formatter';
import { ChevronLeft, ChevronRight } from '@mui/icons-material';

interface CalendarMatrixProps {
    year: number;
    data: AbsencesByDateMap;
    showHeatMap?: boolean;
    totalUsers?: number;
    onFetchDetails?: (dateStr: Date) => Promise<Absence[]>;
    onYearChange?: (newYear: number) => void;
    onHeatMapChange?: (showHeatMap: boolean) => void;
}

const daysOfWeek = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
const repeatedDaysOfWeek = Array.from(
    { length: 37 },
    (_, i) => daysOfWeek[i % 7]
);

const getDaysInMonth = (year: number, month: number): number =>
    new Date(year, month + 1, 0).getDate();

const getWeekdayIndex = (year: number, month: number): number => {
    const day = new Date(year, month, 1).getDay(); // Sunday = 0
    return day === 0 ? 6 : day - 1; // Adjust to Monday = 0
};

const CalendarMatrixHeaderCell = styled(TableCell, {
    name: 'CalendarMatrix',
    slot: 'HeaderCell',
})(({ theme }) => ({
    textAlign: 'center',
    width: 50,
    height: 50,
    backgroundColor: theme.palette.tableHeader?.dark,
    borderColor: theme.palette.tableCell?.border,
}));

const CalendarMatrixBodyWeekendCell = styled(TableCell, {
    name: 'CalendarMatrix',
    slot: 'BodyWeekendCell',
})(({ theme }) => ({
    border: '0.5px solid',
    borderColor: theme.palette.tableCell?.border,
    backgroundImage: theme.palette.tableCell?.weekendGradient,
    backgroundColor: '#f0f0f0',
    borderRadius: '2px',
}));

const CalendarMatrixBodyWeekdayCell = styled(TableCell, {
    name: 'CalendarMatrix',
    slot: 'BodyWeekdayCell',
})(({ theme }) => ({
    border: '0.5px solid',
    borderColor: theme.palette.tableCell?.border,
    backgroundImage: 'none',
    backgroundColor: 'inherit',
    borderRadius: '2px',
}));

interface CalendarMatrixMonthDayCellProps {
    day: number;
    isWeekend: boolean;
    isHolidayDay: boolean;
    isSelected: boolean;
    statusLine: string;
    mainValue?: number;
    backgroundColor?: string;
    onClick?: MouseEventHandler<HTMLTableCellElement>;
    absences?: StatusCounts;
}

const CalendarMatrixMonthDayCell = (opts: CalendarMatrixMonthDayCellProps) => {
    const theme = useTheme();
    const backgroundImage = opts.isHolidayDay
        ? theme.palette.tableCell?.holiday
        : opts.isWeekend
            ? theme.palette.tableCell?.weekendGradient
            : 'none';
    return (
        <TableCell
            onClick={opts.onClick}
            sx={theme => ({
                border: '0.5px solid',
                verticalAlign: 'top',
                backgroundImage: backgroundImage,
                backgroundColor: opts.backgroundColor,
                borderRadius: '2px',
                borderWidth: opts.isSelected ? '4px' : '2px',
                borderColor: opts.isSelected
                    ? '#555'
                    : theme.palette.tableCell?.border,
                position: 'relative',
                padding: '1px',
                width: 50,
                height: 50,
                cursor: opts.absences && opts.onClick ? 'pointer' : 'default',
            })}>
            <Box
                sx={{
                    position: 'relative',
                    width: '100%',
                    height: '100%',
                }}>
                <Typography
                    variant="caption"
                    color="textSecondary"
                    sx={{
                        position: 'absolute',
                        top: 2,
                        left: 4,
                        fontSize: 10,
                    }}>
                    {opts.day}
                </Typography>

                {opts.mainValue && (
                    <Typography
                        fontWeight="bold"
                        variant="body2"
                        sx={{
                            position: 'absolute',
                            top: '50%',
                            right: 4,
                            transform: 'translateY(-50%)',
                            fontSize: 12,
                        }}>
                        {opts.mainValue}
                    </Typography>
                )}

                {opts.statusLine && (
                    <Typography
                        variant="caption"
                        color="textSecondary"
                        sx={{
                            position: 'absolute',
                            bottom: 2,
                            left: 4,
                            fontSize: 10,
                        }}>
                        {opts.statusLine}
                    </Typography>
                )}
            </Box>
        </TableCell>
    );
};

interface LegendEntry {
    color?: string;
    image?: string;
    label: string;
}

const CalendarMatrixLegend = ({ colorMap }: { colorMap: LegendEntry[] }) => {
    return (
        <Stack direction="row" spacing={2} sx={{ mt: 2 }}>
            {colorMap.map((item, index) => (
                <Box key={index} sx={{ display: 'flex', alignItems: 'center' }}>
                    <Box
                        sx={{
                            width: 16,
                            height: 16,
                            backgroundImage: item.image,
                            backgroundColor: item.color,
                            mr: 1,
                            border: '1px solid #ccc',
                        }}
                    />
                    <Typography variant="caption">{item.label}</Typography>
                </Box>
            ))}
        </Stack>
    );
};

export const CalendarMatrix: React.FC<CalendarMatrixProps> = ({
    year,
    data,
    showHeatMap,
    totalUsers,
    onFetchDetails,
    onYearChange,
    onHeatMapChange,
}) => {
    const months = [
        'January',
        'February',
        'March',
        'April',
        'May',
        'June',
        'July',
        'August',
        'September',
        'October',
        'November',
        'December',
    ];
    const { data: session } = useSession();
    const { t } = useTranslation(undefined, 'timesense');
    const theme = useTheme();
    const { toast } = useToast();
    const [holidaysDates, setHolidaysDates] = useState<Date[]>([]);

    useEffect(() => {
        const fetchHolidays = async () => {
            try {
                const holidayApi = new HolidayApi(session!);
                const holidays = await holidayApi.getHolidays();
                setHolidaysDates(holidays.map(h => new Date(h.holidayDate)));
            } catch {
                toast(t('Error fetching holidays'));
            }
        };
        fetchHolidays();
    }, []);

    const isHoliday = (date: Date) =>
        holidaysDates.some(
            holiday => holiday.toDateString() === date.toDateString()
        );

    // handle PopOver when click the table cells
    const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);
    const [popoverData, setPopoverData] = useState<{
        date: string;
        absences?: Absence[];
    }>({ date: '', absences: [] });

    const handleClick = async (
        event: React.MouseEvent<HTMLElement>,
        dateStr: string,
        currentDate: Date
    ) => {
        const e = event.currentTarget;
        setPopoverData({
            date: dateStr,
            absences: await onFetchDetails?.(currentDate),
        });
        setAnchorEl(e);
    };

    const handleClose = () => {
        setAnchorEl(null);
    };
    const open = Boolean(anchorEl);
    const id = open ? 'absence-popover' : undefined;

    const getCellColor = (
        entries: { status: string; type: string; count: number }[] | undefined,
        theme: any
    ) => {
        if (!entries || entries.length === 0) return undefined;

        const hasVacationDone = entries.some(
            e => e.type === 'VACATION' && e.status === 'DONE'
        );
        if (hasVacationDone) {
            return theme.palette.tableCell?.done;
        }
        return theme.palette.tableCell?.vacation;
    };

    const addPaddingCells = (cells: JSX.Element[], count: number) => {
        for (let i = 0; i < count; i++) {
            const index = cells.length;
            const isWeekend = index % 7 >= 5;
            cells.push(
                isWeekend ? (
                    <CalendarMatrixBodyWeekendCell key={`pad-${index}`} />
                ) : (
                    <CalendarMatrixBodyWeekdayCell key={`pad-${index}`} />
                )
            );
        }
        return cells;
    };

    return (
        <div>
            {(onYearChange || onHeatMapChange) && (
                <Stack
                    direction="row"
                    justifyContent="space-between"
                    alignItems="center"
                    mb={1}
                    spacing={2}>
                    {onHeatMapChange ? (
                        <FormControlLabel
                            control={
                                <Checkbox
                                    checked={showHeatMap}
                                    onChange={e =>
                                        onHeatMapChange(e.target.checked)
                                    }
                                    size="small"
                                />
                            }
                            label={t('Show HeatMap')}
                        />
                    ) : (
                        <Box></Box>
                    )}

                    {onYearChange && (
                        <Box display="flex" alignItems="center" gap={0.5}>
                            <IconButton
                                onClick={() => onYearChange(year - 1)}
                                size="small">
                                <ChevronLeft />
                            </IconButton>
                            <Typography variant="body2" fontWeight="bold">
                                {year}
                            </Typography>
                            <IconButton
                                onClick={() => onYearChange(year + 1)}
                                size="small">
                                <ChevronRight />
                            </IconButton>
                        </Box>
                    )}
                </Stack>
            )}
            <TableContainer
                className="shadow-lg"
                sx={{
                    width: '100%',
                    minWidth: '1550px',
                    overflowX: 'auto',
                }}>
                <Table stickyHeader size="small">
                    <TableHead>
                        <TableRow>
                            <CalendarMatrixHeaderCell
                                sx={_ => ({
                                    width: 90,
                                    maxWidth: 90,
                                })}></CalendarMatrixHeaderCell>
                            {repeatedDaysOfWeek.map((day, i) => (
                                <CalendarMatrixHeaderCell
                                    key={i}
                                    className="!font-bold !border">
                                    {t(day)}
                                </CalendarMatrixHeaderCell>
                            ))}
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {months.map((month, monthIndex) => {
                            const daysInMonth = getDaysInMonth(
                                year,
                                monthIndex
                            );
                            const startWeekday = getWeekdayIndex(
                                year,
                                monthIndex
                            );
                            const cells: JSX.Element[] = [];

                            addPaddingCells(cells, startWeekday);

                            for (let day = 1; day <= daysInMonth; day++) {
                                const currentDate = new Date(
                                    year,
                                    monthIndex,
                                    day
                                );
                                const dateStr = format(
                                    currentDate,
                                    'YYYY-MM-DD'
                                );
                                const absences = data?.[dateStr];
                                const total = absences?.total;
                                const isWeekend = cells.length % 7 >= 5;
                                const isHolidayDay = isHoliday(currentDate);
                                // heat map settings
                                const dayPercentage =
                                    absences && totalUsers
                                        ? (total / totalUsers) * 100
                                        : 0;

                                cells.push(
                                    <CalendarMatrixMonthDayCell
                                        key={day}
                                        day={day}
                                        isWeekend={isWeekend}
                                        isHolidayDay={isHolidayDay}
                                        isSelected={absences?.isSelected!}
                                        statusLine={getStatusIndicators(
                                            absences?.entries
                                        ).toString()}
                                        mainValue={total}
                                        backgroundColor={
                                            showHeatMap && absences
                                                ? getHeatMapColor(dayPercentage)
                                                : (getCellColor(
                                                    absences?.entries,
                                                    theme
                                                ) ??
                                                    (isWeekend
                                                        ? '#f0f0f0'
                                                        : 'inherit'))
                                        }
                                        onClick={
                                            absences && onFetchDetails
                                                ? e =>
                                                    handleClick(
                                                        e,
                                                        dateStr,
                                                        currentDate
                                                    )
                                                : undefined
                                        }
                                        absences={absences}
                                    />
                                );
                            }
                            addPaddingCells(cells, 37 - cells.length);

                            return (
                                <TableRow key={month}>
                                    <CalendarMatrixHeaderCell
                                        sx={_ => ({
                                            width: 90,
                                            maxWidth: 90,
                                            textAlign: 'left',
                                        })}>
                                        {t(month)}
                                    </CalendarMatrixHeaderCell>
                                    {cells}
                                </TableRow>
                            );
                        })}
                    </TableBody>
                </Table>
            </TableContainer>

            {/* Label Section */}
            {!showHeatMap ? (
                <CalendarMatrixLegend
                    colorMap={[
                        {
                            image: theme.palette.tableCell?.weekendGradient,
                            label: t('Weekend'),
                        },
                        {
                            image: theme.palette.tableCell?.holiday!,
                            label: t('Holiday'),
                        },
                        {
                            color: theme.palette.tableCell?.vacation!,
                            label: t('Vacations'),
                        },
                        {
                            color: theme.palette.tableCell?.vacation!,
                            label: t('Absences'),
                        },
                        {
                            color: theme.palette.tableCell?.done!,
                            label: t('Done Vacations'),
                        },
                    ]}
                />
            ) : (
                <CalendarMatrixLegend
                    colorMap={[
                        {
                            image: theme.palette.tableCell?.weekendGradient!,
                            label: t('Weekend'),
                        },
                        {
                            image: theme.palette.tableCell?.holiday!,
                            label: t('Holiday'),
                        },
                        {
                            color: '#b9d2e8',
                            label: t('0% - 25%'),
                        },
                        {
                            color: '#c1dbaf',
                            label: t('25% - 50%'),
                        },
                        {
                            color: '#f7df96',
                            label: t('50% - 75%'),
                        },
                        {
                            color: '#f8cbaa',
                            label: t('75% - 100%'),
                        },
                    ]}
                />
            )}

            <Popover
                id={id}
                open={open}
                anchorEl={anchorEl}
                onClose={handleClose}
                anchorOrigin={{
                    vertical: 'top',
                    horizontal: 'center',
                }}
                transformOrigin={{
                    vertical: 'bottom',
                    horizontal: 'left',
                }}>
                <Box p={2} minWidth={250} maxHeight={215} overflow="auto">
                    <Typography
                        variant="subtitle1"
                        fontWeight="bold"
                        gutterBottom>
                        {popoverData.date}
                    </Typography>
                    <Divider sx={{ mb: 1 }} />
                    <Stack spacing={1}>
                        {popoverData.absences?.map((absence, index) => (
                            <Box
                                key={index}
                                display="flex"
                                alignItems="center"
                                gap={2}
                                px={0.5}
                                py={0.25}
                                borderRadius={1}
                                sx={theme => ({
                                    bgcolor: theme.palette.background.paper,
                                })}
                                flexWrap="wrap">
                                <Avatar sx={{ width: 24, height: 24 }}>
                                    <PersonIcon fontSize="small" />
                                </Avatar>

                                <Typography
                                    variant="body2"
                                    sx={{ minWidth: 120 }}>
                                    {absence.user?.name ?? 'No user'}
                                </Typography>

                                <Typography
                                    variant="body2"
                                    color="text.secondary"
                                    sx={{ minWidth: 130 }}>
                                    {absence.type?.name}
                                    {absence.subType
                                        ? ` / ${absence.subType.name}`
                                        : ''}
                                    {absence.recordType
                                        ? ` / ${absence.recordType}`
                                        : ''}
                                </Typography>

                                <Typography
                                    variant="body2"
                                    color="text.secondary"
                                    sx={{ minWidth: 100 }}>
                                    {absence.status?.name}
                                </Typography>

                                <Typography
                                    variant="body2"
                                    color="text.secondary"
                                    sx={{ minWidth: 40 }}>
                                    {absence.businessYear}
                                </Typography>
                            </Box>
                        ))}
                    </Stack>
                </Box>
            </Popover>
        </div>
    );
};

function getHeatMapColor(percentage: number): string {
    const clamped = Math.max(0, Math.min(percentage, 100));

    let color: string;

    if (clamped < 25) {
        color = '#b9d2e8'; // Blue
    } else if (clamped < 50) {
        color = '#c1dbaf'; // Green
    } else if (clamped < 75) {
        color = '#f7df96'; // Yellow
    } else {
        color = '#f8cbaa'; // Red
    }

    return color;
}

function getStatusIndicators(
    entries: { status: string; type: string; count: number }[]
) {
    const statusSet = new Set<string>();
    entries?.forEach(e => {
        if (e.status === 'PENDING') statusSet.add('P');
        if (e.status === 'APPROVED') statusSet.add('A');
        if (e.status === 'DONE') statusSet.add('D');
    });
    return Array.from(statusSet);
}
