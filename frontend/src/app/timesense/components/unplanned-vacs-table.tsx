import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography } from "@mui/material";
import { User } from "../lib/users";
import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import { useEffect, useState } from "react";
import SystemSettingApi from "../lib/system-settings";
import { useSession } from "next-auth/react";

interface UnplannedVacsTableProps {
    users: User[];
}

export const UnplannedVacsTable: React.FC<UnplannedVacsTableProps> = ({ users }) => {
    const { t } = useTranslation(undefined, 'timesense');
    const { data: session } = useSession();
    const [yearSetting, setYearSetting] = useState<string>();
    const [currentYear, setCurrentYear] = useState<string>();
    const [prevYear, setPrevYear] = useState<string>();

    useEffect(() => {
        const fetchYearSetting = async () => {
            try {
                const sysSettingsApi = new SystemSettingApi(session!);
                const setting = await sysSettingsApi.getSystemSettingByName('current_year');

                const value = setting?.value;
                if (value) {
                    setYearSetting(value);
                    setCurrentYear(value);

                    const prev = (parseInt(value, 10) - 1).toString();
                    setPrevYear(prev);
                }
            } catch (error) {
                console.error('Error fetching current_year setting:', error);
            }
        };

        fetchYearSetting();
    }, [session]);

    return (
        <div>
            {/* <Typography variant="subtitle1" sx={{ mt: 1, mb: 1 }}>
                Unplanned Vacation Days
            </Typography> */}
            <TableContainer
                sx={{
                    width: "360px",
                    minWidth: "100px",
                    minHeight: "110px",
                    overflowX: "auto",
                    maxHeight: "180px"
                }}
                className="shadow-lg"
            >
                <Table stickyHeader size="small">
                    <TableHead>
                        <TableRow>
                            <TableCell
                                sx={(theme) => ({
                                    backgroundColor: theme.palette.tableHeader?.main,
                                    width: 100,
                                })}
                            >
                            </TableCell>
                            <TableCell
                                sx={(theme) => ({
                                    alignItems: "center",
                                    backgroundColor: theme.palette.tableHeader?.main,
                                    width: 30,
                                    fontWeight: "bold",
                                })}
                            >
                                {t('Unplanned\n' + currentYear)}
                            </TableCell>
                            <TableCell
                                sx={(theme) => ({
                                    alignItems: "center",
                                    backgroundColor: theme.palette.tableHeader?.main,
                                    width: 30,
                                    fontWeight: "bold",
                                })}
                            >
                                {t('Unplanned\n' + prevYear)}
                            </TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {users.map((user) => (
                            <TableRow key={user.id}>
                                <TableCell align="left">
                                    {user.name}
                                </TableCell>
                                <TableCell align="center">
                                    {user.currentYearVacationDays}
                                </TableCell>
                                <TableCell align="center">
                                    {user.prevYearVacationDays}
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
        </div>
    );

}
