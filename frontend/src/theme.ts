'use client';

import { Lato } from 'next/font/google';
import { createTheme } from '@mui/material/styles';

export type AvailableThemes = 'light' | 'dark' | 'default';

const lato = Lato({
    weight: ['300', '400', '700'],
    subsets: ['latin'],
    display: 'swap',
});

declare module '@mui/material/styles' {
    interface Palette {
        tableHeader?: Palette['primary'];
        tableCell?: {
            default?: string;
            dark?: string;
            weekend?: string;
            approved?: string;
            pending?: string;
            denied?: string;
            draft?: string;
            holiday?: string;
            done?: string;
            vacation?: string;
            weekendGradient?: string;
            border?: string;
        };
    }
    interface PaletteOptions {
        tableHeader?: PaletteOptions['primary'];
        tableCell?: {
            default?: string;
            dark?: string;
            weekend?: string;
            approved?: string;
            pending?: string;
            denied?: string;
            draft?: string;
            holiday?: string;
            done?: string;
            vacation?: string;
            weekendGradient?: string;
            border?: string;
        };
    }
}

export const lightTheme = createTheme({
    palette: {
        mode: 'light',
        background: { default: '#ffffff', paper: '#f5f5f5' },
        primary: { main: '#009502', light: '#009502', contrastText: '#fff' },
        secondary: { main: '#000295', light: '#000295', contrastText: '#fff' },
        error: { main: '#950002', light: '#950002', contrastText: '#fff' },
        warning: { main: '#000295', light: '#000295', contrastText: '#fff' },
        info: { main: '#01579b', light: '#01579b', contrastText: '#fff' },
        success: { main: '#009502', light: '#009502', contrastText: '#fff' },
        tableHeader: { main: '#e1e6ed', light: '#f5f5f5', dark: '#e5e7eb' },
        tableCell: {
            default: "#ffffff",
            weekend: "#f5f5f5",
            approved: '#90EE90',
            dark: "#fafafa",
            pending: "rgb(202, 229, 255)",
            draft: "#fff3cd",
            holiday: "repeating-linear-gradient(135deg, #ffeaea, #ffeaea 5px, #ffdcdc 5px, #ffdcdc 10px)",
            done: '#cfe6cf',
            vacation: "rgb(202, 229, 255)",
            weekendGradient: "repeating-linear-gradient(45deg, #f0f0f0, #f0f0f0 5px, #e0e0e0 5px, #e0e0e0 10px)",
            border: "#e0e0e0"
        },
    },
    typography: {
        fontFamily: lato.style.fontFamily,
        button: { color: '#000' },
    },
    components: {
        MuiListItemButton: {
            styleOverrides: {
                root: {
                    '&.Mui-selected': {
                        'backgroundColor': 'rgba(224, 224, 224)',
                        '&:hover': { backgroundColor: 'rgba(240, 240, 240)' },
                    },
                    '&:hover': 'rgba(0,0,0,0.1)',
                },
            },
        },
        MuiAlert: {
            styleOverrides: {
                root: ({ ownerState }) => ({
                    ...(ownerState.severity === 'info' && {
                        backgroundColor: '#60a5fa',
                    }),
                }),
            },
        },
    },
});

export const darkTheme = createTheme({
    palette: {
        mode: 'dark',
        background: { default: '#1E1E2F', paper: '#3A3A50' },
        primary: { main: '#00ff00', light: '#00ff00', contrastText: '#000' },
        secondary: { main: '#0000ff', light: '#0000ff', contrastText: '#000' },
        error: { main: '#ff0000', light: '#ff0000', contrastText: '#000' },
        warning: { main: '#ffcc00', light: '#ffcc00', contrastText: '#000' },
        info: { main: '#33b5e5', light: '#33b5e5', contrastText: '#000' },
        success: { main: '#00e676', light: '#00e676', contrastText: '#000' },
        tableHeader: { main: '#2A2A40', light: '#3A3A50', dark: '#2A2A40' },
        tableCell: {
            default: "#1E1E2F",
            weekend: "#2A2A40",
            approved: '#02a958ff',    //CHANGE THIS
            dark: '#3A3A50',
            pending: "rgba(97, 111, 125, 1)",
            draft: "#957a34ff",
            holiday: "repeating-linear-gradient(135deg, #ff4d6d, #ff4d6d 5px, #3d3d55 5px, #3d3d55 10px)",
            done: "#00c853",
            vacation: "#00bcd4",
            weekendGradient: "repeating-linear-gradient(45deg, #2A2A40, #2A2A40 5px, #3d3d55 5px, #3d3d55 10px)",
            border: '#3A3A50'
        },
    },
    typography: {
        fontFamily: lato.style.fontFamily,
        button: { color: '#FFF' },
    },
    components: {
        MuiSwitch: {
            styleOverrides: {
                root: {
                    '& .MuiSwitch-switchBase.Mui-checked': {
                        '& .MuiSwitch-thumb': {
                            backgroundColor: 'rgb(144, 202, 249)',
                        },
                    },
                    '& .MuiSwitch-switchBase.Mui-checked + .MuiSwitch-track': {
                        backgroundColor: 'rgb(144, 202, 249)',
                    },
                },
            },
        },
        // MuiCssBaseline: {
        //     styleOverrides: (theme) => ({
        //       body: {
        //         backgroundColor: `${theme.palette.background.default} !important`,
        //         color: `${theme.palette.text.primary} !important`,
        //       },
        //     }),
        //   },
        MuiTablePagination: {
            styleOverrides: {
                actions: {
                    // Targets the container of the pagination buttons
                    root: {
                        gap: '12px', // Adds space between buttons
                        padding: '0 12px', // Adds padding around buttons
                    },
                },
            },
        },
        MuiListItemButton: {
            styleOverrides: {
                root: {
                    '&.Mui-selected': {
                        'backgroundColor': 'rgb(42, 42, 64)',
                        '&:hover': {
                            backgroundColor: 'rgba(58, 58, 80)',
                        },
                    },
                    '&:hover': 'rgba(255,255,255,0.1)',
                },
            },
        },
    },
});

// TODO: Improve themes and set this into dark and light themes
export const colors = {
    gold: "#FFD700",       // gold
    green: "#90EE90",  // light green
    homeOffice: "#ADD8E6",     // light blue
    businessTrip: "#fbbabb",

    // doneVacationCell: "#d9f1d7",
    doneVacationCell: "#cfe6cf",

    vacationCell: "rgb(202, 229, 255)",
    sicknessCell: "rgb(202, 229, 255)",
    licenseCell: "rgb(202, 229, 255)",
    holidayCell: "repeating-linear-gradient(135deg, #ffeaea, #ffeaea 5px, #ffdcdc 5px, #ffdcdc 10px)",
    weekendCell: "repeating-linear-gradient(45deg, #f0f0f0, #f0f0f0 5px, #e0e0e0 5px, #e0e0e0 10px)",

}
