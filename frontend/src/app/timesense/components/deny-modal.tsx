'use client';

import { useModal } from '@datacentric/datacentric-ui/lib/browser-utils';
import {
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogContentText,
    DialogTitle,
    Stack,
    TextField,
} from '@mui/material';
import React, { useState } from 'react';

interface CloneModalApi {
    ask: (
        title: string,
        question: string,
        okLabel: string,
        cancelLabel: string
    ) => Promise<string | null>;
    dialog: React.ReactElement;
}

/**
 * Setup a dialog utility that asks for the observation of the denied item and 
 * waits for the user input before continuing.
 * 
 * Returns the observation or null if the user cancels the operation.
 */
export default function useDenyModal(): CloneModalApi {
    const [isOpen, openDialog, closeDialog] = useModal();
    const [title, setTitle] = useState('');
    const [question, setQuestion] = useState('');
    const [cancelLabel, setCancelLabel] = useState<string | undefined>(
        undefined
    );
    const [okLabel, setOkLabel] = useState('');
    const [value, setValue] = useState('');
    const [observation, setObservation] = useState<
        ((result: string | null) => void) | null
    >(null);

    const ask = async (
        title: string,
        question: string,
        okLabel: string,
        cancelLabel: string
    ): Promise<string | null> => {
        setTitle(title);
        setQuestion(question);
        setCancelLabel(cancelLabel);
        setOkLabel(okLabel);
        setValue('');

        return new Promise<string | null>((resolve) => {
            setObservation(() => resolve);
            openDialog();
        });
    };

    const handleClose = (result: boolean) => {
        if (observation) {
            observation(result ? value : null);
        }
        setObservation(null);
        closeDialog(false);
    };

    return {
        ask,
        dialog: (
            <Dialog open={isOpen}>
                <DialogTitle>{title}</DialogTitle>
                <DialogContent>
                    <Stack spacing={2}>
                        <DialogContentText>{question}</DialogContentText>
                        <TextField
                            label="Observation"
                            variant="standard"
                            fullWidth
                            value={value}
                            onChange={(e) => setValue(e.target.value)}
                        />
                    </Stack>
                </DialogContent>
                <DialogActions>
                    {cancelLabel && (
                        <Button onClick={() => handleClose(false)}>
                            {cancelLabel}
                        </Button>
                    )}
                    {okLabel && (
                        <Button
                            onClick={() => handleClose(true)}
                            disabled={!value.trim()}
                        >
                            {okLabel}
                        </Button>
                    )}
                </DialogActions>
            </Dialog>
        ),
    };
}
