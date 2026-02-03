import { Grid, Typography } from '@mui/material';


export function renderBasicField(title: string, value?: string) {
    return (
        <>
            <Grid item xs={3}>
                <Typography sx={{ fontWeight: 'bold' }}>{title}:</Typography>
            </Grid>
            <Grid item xs={9}>
                <Typography>{value || ''}</Typography>
            </Grid>
        </>
    );
}

export function renderStringListField(title: string, values?: string[]) {
    return (
        <>
            <Grid item xs={3}>
                <Typography sx={{ fontWeight: 'bold' }}>{title}:</Typography>
            </Grid>
            <Grid item xs={9}>
                <ul style={{ margin: 0, paddingLeft: '1rem' }}>
                    {values?.map((value, index) => (
                        <li key={index}>
                            <Typography>{value}</Typography>
                        </li>
                    ))}
                </ul>
            </Grid>
        </>
    );
}
