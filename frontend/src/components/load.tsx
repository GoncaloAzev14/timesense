import { Backdrop, LinearProgress, Stack } from '@mui/material';
import React from 'react';
import Logo from '@/components/logo';

export type LoadingProps = {
    fullPage?: boolean;
    backgroundColor?: string;
    progressBarBgColor?: string;
};

/**
 * Loading component to be used in a page while the data is being fetched.
 *
 * By default the loading component will take the full page and make the ui
 * unclickable.
 *
 * If you pass fullPage as false, the loading component will only show the
 * progress bar locally at that place to signify that that part of the ui is
 * still being loaded.
 */
export default function Loading(props: LoadingProps) {
    if (props.fullPage || props.fullPage === undefined) {
        return (
            <Backdrop
                sx={{ color: '#fff', zIndex: theme => theme.zIndex.drawer + 1 }}
                open={true}>
                <Stack>
                    <Logo mode="dark" />
                    <LinearProgress
                        sx={{
                            'backgroundColor': props.backgroundColor || 'gray',
                            '& .MuiLinearProgress-bar': {
                                backgroundColor:
                                    props.progressBarBgColor || '#fff',
                            },
                        }}
                    />
                </Stack>
            </Backdrop>
        );
    } else {
        return (
            <LinearProgress
                sx={{
                    'backgroundColor': props.backgroundColor || 'gray',
                    '& .MuiLinearProgress-bar': {
                        backgroundColor: props.progressBarBgColor || '#fff',
                    },
                }}
            />
        );
    }
}
