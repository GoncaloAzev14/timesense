'use client';

import { SessionProvider } from 'next-auth/react';

type Props = {
    children?: React.ReactNode;
};

//TODO Add this to configuration file
const REFETCH_INTERVAL_MINUTES: number = 4;

export const AuthProvider = ({ children }: Props) => {
    return (
        <SessionProvider
            refetchInterval={60 * REFETCH_INTERVAL_MINUTES}
            refetchOnWindowFocus={true}>
            {children}
        </SessionProvider>
    );
};
