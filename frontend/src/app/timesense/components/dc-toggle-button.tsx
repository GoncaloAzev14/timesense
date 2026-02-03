import { ToggleButton, ToggleButtonGroup } from "@mui/material";
import React from "react";

export interface DCToggleOption {
    value: string;
    label: React.ReactNode;
}

interface DCToggleButtonProps {
    value: string;
    options: DCToggleOption[];
    onChange: (value: string) => void;
    sx?: any;
}

const DCToggleButton: React.FC<DCToggleButtonProps> = ({
    value,
    options,
    onChange,
    sx
}) => {
    return (
        <>
            <ToggleButtonGroup
                value={value}
                exclusive
                onChange={(e, newValue) => {
                    if (newValue !== null) {
                        onChange(newValue);
                    }
                }}
                sx={(theme) => ({
                    ...sx,
                    backgroundColor: theme.palette.background.paper,
                    borderRadius: 3,
                    p: 0.5,
                    boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
                    '& .MuiToggleButtonGroup-grouped': {
                        border: 0,
                        mx: 0.3,
                        borderRadius: 2,
                        textTransform: 'none',
                        px: 3,
                        py: 0.8,
                        fontSize: '0.9rem',
                        fontWeight: 500,
                        transition: 'all 0.25s ease',
                        '&.Mui-selected': {
                            backgroundColor: theme.palette.info.main,
                            color: '#fff',
                            boxShadow: '0 2px 6px rgba(0,0,0,0.15)',
                            '&:hover': {
                                backgroundColor: theme.palette.info.dark,
                            }
                        }
                    },
                })}
            >
                {options.map((opt) => (
                    <ToggleButton key={opt.value} value={opt.value}>
                        {opt.label}
                    </ToggleButton>
                ))}
            </ToggleButtonGroup>
        </>
    );
};

export default DCToggleButton;