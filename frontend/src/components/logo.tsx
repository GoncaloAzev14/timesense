'use client';

import imagesUrls from '@/lib/image-urls';
import { useTheme } from '@mui/material';
import { useState, CSSProperties } from 'react';

interface ILogo extends Omit<CSSProperties, 'mode'> {
    mode?: 'dark' | 'light';
}

export default function Logo(props: ILogo) {
    const theme = useTheme();
    const [isError, setIsError] = useState(false);

    // Determine the theme mode: use the provided prop or fallback to the theme's mode
    const defineTheme = props.mode || theme.palette.mode;
    const src = imagesUrls[defineTheme]?.logo;
    const alt = 'Datacentric';

    // Extract mode to avoid passing it to styles
    const { mode, ...styleProps } = props;

    // Ensure TypeScript-friendly CSS properties
    const properties: CSSProperties = {
        width: props.width || '128px',
        height: props.height || '128px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: props.backgroundColor ?? 'transparent',
        color: props.color ?? 'inherit',
        fontSize: props.fontSize ?? '16px',
        fontWeight: props.fontWeight ?? 'bold',
        textAlign: (props.textAlign as CSSProperties['textAlign']) ?? 'center', // Explicit cast
        ...styleProps, // Apply remaining styles
    };

    return isError ? (
        <span style={properties}>{alt}</span> // Fallback text with same size
    ) : (
        <img
            src={src}
            alt={alt}
            loading="lazy"
            onError={() => setIsError(true)} // Switch to text when image fails
            style={properties}
        />
    );
}
