'use client';

import { Snackbar } from '@mui/material';
import { PropsWithChildren, createContext, useContext, useState } from 'react';

export const ToastContext = createContext({ toast: (message: string) => { } });

export const ToastContextProvider = ({ children }: PropsWithChildren) => {
    const [isToastVisible, setToastVisible] = useState(false);
    const [toastMessage, setToastMessage] = useState('');

    const toast = (message: string) => {
        setToastMessage(message);
        setToastVisible(true);
    };

    return (
        <ToastContext.Provider value={{ toast }}>
            {children}
            <Snackbar
                anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
                open={isToastVisible}
                autoHideDuration={1000}
                onClose={() => setToastVisible(false)}
                message={toastMessage}
            />
        </ToastContext.Provider>
    );
};

export const useToast = () => {
    return useContext(ToastContext);
};
