import React from "react";
import {
    Autocomplete,
    TextField,
    Checkbox,
    CircularProgress,
} from "@mui/material";
import { CheckBox, CheckBoxOutlineBlank } from "@mui/icons-material";

interface DcAutocomplete<T> {
    label: string;
    options: T[];
    getOptionLabel: (option: T) => string;
    onChange: (newValue: T[]) => void;
    loading?: boolean;
    width?: number;
    listboxHeight?: number;
    multiple?: boolean;
    disableCloseOnSelect?: boolean;
}

const icon = <CheckBoxOutlineBlank fontSize="small" />;
const checkedIcon = <CheckBox fontSize="small" />;

export function DcAutocomplete<T>({
    label,
    options,
    getOptionLabel,
    onChange,
    loading = false,
    width = 400,
    listboxHeight = 170,
    multiple = true,
    disableCloseOnSelect = true,
}: DcAutocomplete<T>) {
    return (
        <Autocomplete
            size="small"
            multiple={multiple}
            disableCloseOnSelect={disableCloseOnSelect}
            options={options}
            getOptionLabel={getOptionLabel}
            isOptionEqualToValue={(option, val) =>
                getOptionLabel(option) === getOptionLabel(val)
            }
            onChange={(_, newValue) => onChange((newValue as T[]) || [])}
            ListboxProps={{
                style: {
                    maxHeight: listboxHeight,
                },
            }}
            renderOption={(props, option, { selected }) => {
                const { key, ...optionProps } = props;
                return (
                    <li key={key} {...optionProps}>
                        <Checkbox
                            icon={icon}
                            checkedIcon={checkedIcon}
                            style={{ marginRight: 8 }}
                            checked={selected}
                        />
                        {getOptionLabel(option)}
                    </li>
                );
            }}
            sx={{ width }}
            renderInput={(params) => (
                <TextField
                    {...params}
                    label={label}
                    placeholder={label}
                    InputProps={{
                        ...params.InputProps,
                        sx: {
                            maxHeight: 100,
                            overflowY: "auto",
                        },
                        endAdornment: (
                            <>
                                {loading ? (
                                    <CircularProgress color="inherit" size={16} />
                                ) : null}
                                {params.InputProps.endAdornment}
                            </>
                        ),
                    }}
                />
            )}
        />
    );
}
