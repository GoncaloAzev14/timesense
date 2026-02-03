'use client';

import {
    Avatar,
    Button,
    Grid,
    IconButton,
    Stack,
    Tooltip,
    Typography,
    useTheme,
} from '@mui/material';
import { getSession, signIn, signOut, useSession } from 'next-auth/react';
import { Session } from 'next-auth';
import { PropsWithoutRef, useState } from 'react';
import { openPopupWindow, useInterval } from '@datacentric/datacentric-ui/lib/browser-utils';
import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import LogoutIcon from '@mui/icons-material/Logout';
import SettingsIcon from '@mui/icons-material/Settings';
import Loading from './load';

type Props = {
    loginPopupUrl: string;
    settingsPopupUrl?: string;
};

export default function LoginButton({
    loginPopupUrl,
    settingsPopupUrl,
}: PropsWithoutRef<Props>) {
    const theme = useTheme();
    const { t } = useTranslation();
    const [pollRate, setPollRate] = useState(0);
    const { data: session, status } = useSession();
    const [pollStatus, setPollStatus] = useState('loading');
    const [pollSession, setPollSession] = useState<Session | null>(null);

    // Polling session data at a specified rate
    useInterval(async () => {
        const session = await getSession();
        const status = session ? 'authenticated' : 'unauthenticated';
        setPollSession(session);
        setPollStatus(status);
        if (status === 'authenticated') {
            setPollRate(0);
        }
    }, pollRate);

    function handleSignIn() {
        if (loginPopupUrl) {
            openPopupWindow(
                window,
                loginPopupUrl,
                t('login'),
                400,
                400,
                'center'
            );
            // Keep updating every second until the login in completed in the
            // front-end
            setPollRate(1000);
        } else {
            signIn();
        }
    }

    function handleOpenSettings() {
        if (settingsPopupUrl) {
            openPopupWindow(
                window,
                settingsPopupUrl,
                t('settings'),
                400,
                400,
                'center'
            );
        }
    }

    if (status === 'loading' && pollStatus === 'loading') {
        return <Loading />;
    }

    if (status !== 'authenticated' && pollStatus !== 'authenticated') {
        return (
            <div className="items-center text-center w-full">
                <Button
                    variant="contained"
                    color="info"
                    sx={{
                        'boxShadow': 'rgba(0, 0, 0, 0.35) 0px 5px 15px',
                        'borderRadius': 50,
                        'width': '100%',
                        'backgroundColor': theme.palette.background.default, // Dynamic background
                        '&:hover': {
                            backgroundColor: theme.palette.background.paper,
                            boxShadow: 'rgba(0, 0, 0, 0.25) 0px 5px 15px',
                        },
                        'color': theme.palette.info.main,
                        'fontSize': '20px',
                        'textTransform': 'capitalize',
                    }}
                    onClick={handleSignIn}>
                    {t('login')}
                </Button>
            </div>
        );
    }

    const sessionToUse = session ?? pollSession;
    const firstname = sessionToUse?.user?.name || t('unknown');
    const avatarUrl = sessionToUse?.user?.image || '';

    const stringToColor = (string: string) => {
        let hash = 0;
        for (let i = 0; i < string.length; i++) {
            hash = string.charCodeAt(i) + ((hash << 5) - hash);
        }
        return `#${[0, 1, 2]
            .map(i => ((hash >> (i * 8)) & 0xff).toString(16).padStart(2, '0'))
            .join('')}`;
    };

    const stringAvatar = (name: string) => ({
        sx: { bgcolor: stringToColor(name) },
        children: name[0]?.toUpperCase() || '',
    });

    return (
        <Grid
            container
            alignItems="center"
            sx={{
                width: '100%',
                borderRadius: '8px',
            }}>
            {/* Avatar Zone */}
            <Grid item>
                <Avatar
                    {...stringAvatar(firstname)}
                    src={avatarUrl}
                    alt={firstname} // Add alt text for accessibility
                    sx={{
                        border: '2px solid #fff',
                        boxShadow: '0 2px 4px rgba(0, 0, 0, 0.15)',
                    }}
                />
            </Grid>

            {/* Name Zone */}
            {/* <Grid item xs p={2} sx={{ maxWidth: '200px', minWidth: 0 }}> */}
            <Grid xs p={2} sx={{ maxWidth: '200px', minWidth: 0 }}>
                <Tooltip
                    title={firstname}
                    arrow
                    slotProps={{
                        tooltip: {
                            sx: {
                                backgroundColor:
                                    theme.palette.mode === 'dark'
                                        ? 'rgba(254, 254, 254, 0.85)' // Dark background
                                        : 'rgba(0, 0, 0, 0.85)',
                                color:
                                    theme.palette.mode === 'dark'
                                        ? 'rgba(0, 0, 0, 0.85)'
                                        : 'rgba(254, 254, 254, 0.85)', // Dark background
                                fontSize: '14px', // Slightly larger text
                                fontWeight: 500,
                                padding: '8px 12px', // Adds space inside
                                borderRadius: '8px', // Rounded corners
                                boxShadow: '0px 4px 10px rgba(0, 0, 0, 0.3)', // Soft shadow
                            },
                        },
                        arrow: {
                            sx: {
                                color:
                                    theme.palette.mode === 'dark'
                                        ? 'rgba(254, 254, 254, 0.85)' // Dark background
                                        : 'rgba(0, 0, 0, 0.85)', // Match the background color
                            },
                        },
                    }}>
                    <Typography
                        sx={{
                            fontWeight: 700,
                            fontSize: '18px',
                            color: theme.typography.button.color,
                            letterSpacing: '0.5px',
                            overflow: 'hidden', // Hide overflowing text
                            whiteSpace: 'nowrap', // Prevent text from wrapping
                            textOverflow: 'ellipsis', // Show "..." when text is too long
                            maxWidth: '100%', // Prevent expansion
                            display: 'block',
                            cursor: 'pointer', // Indicate interactivity
                        }}>
                        {firstname}
                    </Typography>
                </Tooltip>
            </Grid>

            {/* Button Zone */}
            <Grid item>
                <Stack direction="row" spacing={1}>
                    {settingsPopupUrl && (
                        <IconButton
                            aria-label="settings"
                            onClick={handleOpenSettings}
                            sx={{
                                'color': theme.typography.button.color,
                                '&:hover': {
                                    backgroundColor: 'rgba(255, 255, 255, 0.1)',
                                    transform: 'scale(1.1)', // Slightly enlarge on hover
                                    transition: 'transform 0.2s ease',
                                },
                            }}>
                            <SettingsIcon />
                        </IconButton>
                    )}
                    <IconButton
                        aria-label="logout"
                        onClick={() => signOut({ callbackUrl: '/' })}
                        sx={{
                            'color': theme.typography.button.color,
                            '&:hover': {
                                backgroundColor: 'rgba(255, 255, 255, 0.1)',
                                transform: 'scale(1.1)', // Slightly enlarge on hover
                                transition: 'transform 0.2s ease',
                            },
                        }}>
                        <LogoutIcon />
                    </IconButton>
                </Stack>
            </Grid>
        </Grid>
    );
}
