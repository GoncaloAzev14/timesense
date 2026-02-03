import React from "react";
import {
    TableContainer,
    Table,
    TableHead,
    TableRow,
    TableCell,
    TableBody,
    Box,
    Typography,
    useTheme,
} from "@mui/material";
import { format, isWithinInterval, addDays, startOfDay } from "date-fns";

/**
 * Represents the Object to include on each row of the Gantt Chart
 * For example Users, Projects, Taks,...
 */
export type Row = {
    /**
     * Numeric id of the row
     */
    id: number;
    /**
     * Text to show on the first column of the chart
     */
    name: string
};

/**
 * Represents the Object used to fill the Gantt chart
 */
export type GanttData = {
    /**
     * Numeric id to match with the id of the row object that will map the chart content
     */
    rowId: number;
    /**
     * Start Date to map with the respective column to mark the beggining of the chart boxes
     */
    start: Date;
    /**
     * End Date to map with the respective column to mark the end of the chart boxes
     */
    end: Date;
    /**
     * Optional label to include inside the chart boxes
     * For example allocation percentage, task executed, entity name,... 
     */
    label1?: string;
    /**
     * Optional label to include inside the chart boxes
     * For example allocation percentage, task executed, entity name,... 
     */
    label2?: string;
    /**
     * Text value to include into the optional column cells
     * This value will only be visible if the chart prop 'showOptionalColumn' is set to true
     */
    optionalCol?: string;
    /**
     * Number representing the id of the current row, and use on the onClick action
     */
    onClickId?: number;
};

type GanttChartProps = {
    /**
     * Label that represents the name of the chart first column
     */
    rowLabel: string;
    /**
     * List of elements to fill on the chart first column
     */
    rows: Row[];
    /**
     * List of Dates to fill into the remaining chart columns
     */
    columns: Date[];
    /**
     * List of elements to populate the chart
     */
    data: GanttData[];
    /**
     * Optional value to show or not the optional column, default is false
     */
    showOptionalColumn?: boolean;
    /**
     * Optional column name
     */
    optionalColumnLabel?: string;
    /**
     * Defines the chart data mode
     * if "daily" expects the columns list to represent a list of consecutive days
     * if "weekly" expects the columns list to represent a list of days representing 
     *      the beggining of consecutive weeks
     */
    columnMode: "daily" | "weekly";
    /**
     * Action to perform when clicking some row
     */
    onClickAction?: (id: number | undefined) => void;
};

/**
 * The Gantt chart component retreives a versatile component displaying a Gantt chart representation
 * of the data passed as input.
 * It is initialized by providing the first column label, the content of the first column, the list
 *      of dates to show on the other columns, the chart data, 2 optional vars that represent the
 *      optional column and column mode meaning that the data intervals will be daily or weekly
 * 
 * @example Weekly Gant chart that represents the users allocation time, allocation % and their 
 *      tasks dedicated to a project:
 * <GanttChart
 *    rowLabel={'User'}
 *    rows={ { id: 1, name: "Tiago Gouveia" }, { id: 2, name: "Luis Passos" }}
 *    showOptionalColumn={false}
 *    columns={[new Date(2025, 3, 7),new Date(2025, 3, 14),new Date(2025, 3, 21)]}
 *    data={{ rowId: 1, start: new Date(2025, 3, 7), end: new Date(2025, 3, 14), label1: "Analysis", label2: '100%' },}
 *    columnMode="weekly"
 *  />
 */
export const GanttChart: React.FC<GanttChartProps> = ({
    rowLabel,
    rows,
    columns,
    data,
    showOptionalColumn = false,
    optionalColumnLabel,
    columnMode,
    onClickAction,
}) => {
    const theme = useTheme();

    // Helper function to check if a column date is within a date range
    const isDateInRange = (columnDate: Date, start: Date, end: Date, mode: "daily" | "weekly") => {
        const col = startOfDay(columnDate);
        const startDay = startOfDay(start);
        const endDay = startOfDay(end);

        if (mode === "daily") {
            return isWithinInterval(col, { start: startDay, end: endDay });
        } else {
            // For weekly mode, check if the week starting at columnDate overlaps with the task range
            const weekEnd = addDays(columnDate, 6); // Assume week ends 6 days after start
            return (
                (columnDate <= end && weekEnd >= start) ||
                isWithinInterval(col, { start: startDay, end: endDay })
            );
        }
    };

    // Helper function to format date column headers
    const formatColumnHeader = (date: Date, mode: "daily" | "weekly") => {
        if (mode === "daily") {
            return format(date, "dd/MM/yyyy");
        } else {
            return `${format(date, "dd/MM")}`;
        }
    };

    return (
        <TableContainer
            sx={{
                width: "100%",
                minWidth: "1830px",
                overflowX: "auto",
                border: `1px solid ${theme.palette.divider}`,
                borderRadius: 2,
                boxShadow: "0 2px 8px rgba(0,0,0,0.1)",
            }}
        >
            <Table stickyHeader size="small">
                <TableHead>
                    <TableRow>
                        <TableCell
                            sx={{
                                backgroundColor: theme.palette.tableHeader?.main,
                                fontWeight: "bold",
                                width: 130,
                                borderRight: `1px solid ${theme.palette.divider}`,
                                position: "sticky",
                                left: 0,
                            }}
                        >
                            {rowLabel}
                        </TableCell>
                        {showOptionalColumn && (
                            <TableCell
                                sx={{
                                    backgroundColor: theme.palette.tableHeader?.main,
                                    fontWeight: "bold",
                                    width: 130,
                                    borderRight: `1px solid ${theme.palette.divider}`,
                                    zIndex: 2,
                                }}
                            >
                                {optionalColumnLabel}
                            </TableCell>
                        )}
                        {columns.map((col) => (
                            <TableCell
                                key={col.toDateString()}
                                sx={{
                                    backgroundColor: theme.palette.tableHeader?.light,
                                    textAlign: "center",
                                    fontWeight: "medium",
                                    width: 80,
                                    borderRight: `1px solid ${theme.palette.divider}`,
                                    color: theme.palette.text.primary,
                                }}
                            >
                                {formatColumnHeader(col, columnMode)}
                            </TableCell>
                        ))}
                    </TableRow>
                </TableHead>
                <TableBody>
                    {rows.map((user) => {
                        const userGanttDatas = data.filter((a) => a.rowId === user.id);

                        return userGanttDatas.map((allocation, index) => {
                            const isFirstRowForUser = index === 0;

                            return (
                                <TableRow
                                    key={`${user.id}-${index}`}
                                    sx={{
                                        "&:hover": {
                                            backgroundColor: theme.palette.action.hover,
                                        },
                                    }}
                                >
                                    {isFirstRowForUser && (
                                        <TableCell
                                            sx={{
                                                backgroundColor: theme.palette.tableHeader?.light,
                                                fontWeight: "bold",
                                                borderRight: `1px solid ${theme.palette.divider}`,
                                                left: 0,
                                                zIndex: 1,
                                            }}
                                            rowSpan={userGanttDatas.length}
                                        >
                                            {user.name}
                                        </TableCell>
                                    )}
                                    {showOptionalColumn && (
                                        <TableCell
                                            sx={{
                                                backgroundColor: theme.palette.tableHeader?.light,
                                                fontStyle: "italic",
                                                borderRight: `1px solid ${theme.palette.divider}`,
                                                position: "sticky",
                                                zIndex: 1,
                                            }}
                                        >
                                            {allocation.optionalCol || "-"}
                                        </TableCell>
                                    )}
                                    {columns.map((columnDate, colIndex) => {
                                        const col = startOfDay(columnDate);
                                        const startDay = startOfDay(allocation.start);
                                        const endDay = startOfDay(allocation.end);

                                        const isInRange = isDateInRange(col, startDay, endDay, columnMode);
                                        const isStart = col.getTime() === startDay.getTime();
                                        const isEnd = col.getTime() === endDay.getTime();
                                        return (
                                            <TableCell
                                                key={columnDate.toDateString()}
                                                sx={{
                                                    padding: 0,
                                                    height: 50,
                                                    borderRight: `1px solid ${theme.palette.divider}`,
                                                    position: "relative",
                                                    backgroundColor: isInRange
                                                        ? "transparent"
                                                        : theme.palette.background.paper,
                                                    cursor: onClickAction && isInRange ? 'pointer' : 'default'
                                                }}
                                                onClick={
                                                    onClickAction && isInRange
                                                        ? () => onClickAction(allocation.onClickId)
                                                        : undefined
                                                }
                                            >
                                                {isInRange && (
                                                    <Box
                                                        sx={{
                                                            position: "absolute",
                                                            top: 4,
                                                            bottom: 4,
                                                            left: isStart ? "10%" : 0,
                                                            right: isEnd ? "10%" : 0,
                                                            background: `linear-gradient(to right, ${theme.palette.primary.light}, ${theme.palette.primary.main})`,
                                                            borderRadius: isStart && isEnd ? 4 : isStart ? "4px 0 0 4px" : isEnd ? "0 4px 4px 0" : 0,
                                                            display: "flex",
                                                            alignItems: "center",
                                                            justifyContent: "center",
                                                            transition: "transform 0.2s",
                                                            "&:hover": {
                                                                transform: "scale(1.05)",
                                                                boxShadow: "0 2px 4px rgba(0,0,0,0.2)",
                                                            },
                                                        }}
                                                    >
                                                        {isStart && (
                                                            <Box
                                                                sx={{
                                                                    display: "flex",
                                                                    flexDirection: "column",
                                                                    alignItems: "center",
                                                                    justifyContent: "center",
                                                                    color: theme.palette.primary.contrastText,
                                                                    fontSize: 11,
                                                                    fontWeight: "medium",
                                                                    textAlign: "center",
                                                                    width: "100%",
                                                                    height: "100%",
                                                                    padding: "0 4px",
                                                                }}
                                                            >
                                                                <Typography variant="caption" noWrap>
                                                                    {allocation.label1}
                                                                </Typography>
                                                                <Typography variant="caption" noWrap>
                                                                    {allocation.label2}
                                                                </Typography>
                                                            </Box>
                                                        )}
                                                    </Box>
                                                )}
                                            </TableCell>
                                        );
                                    })}
                                </TableRow>
                            );
                        });
                    })}
                </TableBody>
            </Table>
        </TableContainer>
    );
};