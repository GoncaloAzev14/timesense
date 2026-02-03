'use client';

import React from 'react';
import { Box, Typography, useTheme, Link, Theme } from '@mui/material';
import imagesUrls from '@/lib/image-urls';


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
                variant="body1"
                gutterBottom
                sx={{
                    maxWidth: '600px',
                    color:
                        theme.palette.mode === 'dark'
                            ? 'rgba(255,255,255,1)'
                            : 'rgba(0,0,0,1)',
                }}>
                Welcome to the Timesense Platform. Feel free to
                manage your vacation days, absences and record
                the time you invested in each of your work projects.
            </Typography>
        </>
    );
}

export default function Page() {
    const theme = useTheme();

    return (
        <div style={retrieveBackgroundStyle(theme)}>
            <Box
                sx={{
                    textAlign: 'center',
                    marginBottom: '40px',
                }}>
            </Box>
            {retrieveStandardText(theme)}
        </div>
    );
}
