'use client';
import React, { useEffect, useState } from 'react';

import './globals.css';
import Logo from '@/components/logo';
import { Button, Box, Typography, useTheme, Theme } from '@mui/material';
import { getProviders, signIn, useSession } from 'next-auth/react';
import Loading from '@/components/load';
import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';
import imagesUrls from '@/lib/image-urls';

type AuthenticatedProps = {
    theme: Theme;
};

type UnAuthenticatedProps = {
    provider: string;
    theme: Theme;
};

function retrieveBackgroundImage(theme: Theme) {

    switch (theme.palette.mode) {
        case 'dark':
            return `url(${imagesUrls.dark.background})`;
        case 'light':
            return `url(${imagesUrls.light.background})`;
        default:
            return `url(${imagesUrls.default.background})`;
    }
}

function retrieveBackgroundStyle(theme: Theme): React.CSSProperties {
    return {
        position: 'relative',
        margin: '0',
        height: '100vh',
        overflow: 'hidden',
        width: '100%',
        top: '0',
        bottom: '0',
        left: '0',
        right: '0',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundImage: retrieveBackgroundImage(theme),
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        boxShadow:
            theme.palette.mode === 'dark'
                ? '10px 5px rgba(30, 30, 47, 0.5)'
                : '10px 5px rgba(255, 255, 255, 0.5)',
    };
}

function retrieveStandardText(theme: Theme): React.JSX.Element {
    return (
        <>
            <Typography
                variant="h3"
                component="h1"
                gutterBottom
                sx={{
                    color:
                        theme.palette.mode === 'dark'
                            ? 'rgba(255,255,255,1)'
                            : 'rgba(0,0,0,1)',
                }}>
                Welcome to DataCentric Platform
            </Typography>
            <Typography
                variant="h5"
                gutterBottom
                sx={{
                    color:
                        theme.palette.mode === 'dark'
                            ? 'rgba(255,255,255,1)'
                            : 'rgba(0,0,0,1)',
                }}>
                Harnessing the Power of Data
            </Typography>
            <Typography
                variant="body1"
                gutterBottom
                sx={{
                    maxWidth: '600px',
                    color:
                        theme.palette.mode === 'dark'
                            ? 'rgba(255,255,255,1)'
                            : 'rgba(0,0,0,1)',
                }}>
                At DataCentric, we prioritize your data needs with innovative
                solutions and insights. Our mission is to empower businesses
                through data-driven decisions, ensuring you stay ahead in a
                competitive landscape.
            </Typography>
        </>
    );
}

/**
 * Page to be rendered when user is authenticated
 * @returns
 */
function Authenticated({ theme }: AuthenticatedProps) {
    return (
        <div style={retrieveBackgroundStyle(theme)}>
            <Box
                sx={{
                    textAlign: 'center',
                    marginBottom: '40px',
                }}>
                <Logo />
            </Box>
            {retrieveStandardText(theme)}
        </div>
    );
}

/**
 * Page to be rendered when user is not authenticated
 * @returns ReactNode
 */
function UnAuthenticated({ provider, theme }: UnAuthenticatedProps) {
    const { t } = useTranslation();
    return (
        <div style={retrieveBackgroundStyle(theme)}>
            <Box
                sx={{
                    textAlign: 'center',
                    marginBottom: '40px',
                }}>
                <Logo />
            </Box>
            {retrieveStandardText(theme)}
            <Box
                sx={{
                    textAlign: 'center',
                    marginTop: '40px',
                }}>
                <Button
                    sx={{
                        'padding': '10px 30px',
                        'fontWeight': 'bold',
                        'background':
                            theme.palette.mode === 'dark'
                                ? 'rgba(255,255,255,1)'
                                : 'rgb(0, 0, 0)',
                        'color':
                            theme.palette.mode === 'dark'
                                ? 'rgba(0,0,0,1)'
                                : 'rgba(255,255,255,1)',
                        '&:hover': {
                            background:
                                theme.palette.mode === 'dark'
                                    ? 'rgba(255,255,255,0.80)'
                                    : 'rgba(0, 0, 0,0.80)',
                            color:
                                theme.palette.mode === 'dark'
                                    ? 'rgba(0,0,0,0.80)'
                                    : 'rgba(255, 255, 255, 0.80)',
                        },
                    }}
                    onClick={async () => signIn(provider)}>
                    {t('login')}
                </Button>
            </Box>
        </div>
    );
}

/**
 * Initial page of platform
 * @returns ReactNode
 */
export default function HomePage() {
    const { status } = useSession();
    const [isLoading, setLoading] = useState(true);
    const [provider, setProvider] = useState<string>('');
    const theme = useTheme();

    useEffect(() => {
        (async () => {
            const providers = await getProviders();
            if (providers) {
                const provider = providers[Object.keys(providers)[0]];
                setProvider(provider.id);
                setLoading(false);
            }
        })();
    }, []);

    switch (status) {
        case 'authenticated':
            return <Authenticated theme={theme} />;
        case 'unauthenticated':
            return isLoading ? (
                <Loading />
            ) : (
                <UnAuthenticated provider={provider} theme={theme} />
            );
        default:
            return <Loading />;
    }
}
