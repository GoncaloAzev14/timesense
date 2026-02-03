import {
    Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions, Button, List, ListItem
} from '@mui/material';
import { useTranslation } from '@datacentric/datacentric-ui/lib/i18n-client';

type ErrorModalProps = {
    open: boolean;
    onClose: () => void;
    message: React.ReactNode;
    details: string[];
    title: string
};

export const ErrorMessageModal = ({ open, onClose, message, details, title }: ErrorModalProps) => {
    const { t } = useTranslation(undefined, 'timesense');
    return (
        <Dialog open={open} onClose={onClose}>
            <DialogTitle>{title}</DialogTitle>
            <DialogContent>
                <DialogContentText sx={{ whiteSpace: 'pre-line' }}>
                    {message}  {/* aqui respeita os \n */}
                </DialogContentText>
                {details.length > 0 && (
                    <List>
                        {details.map((detail, index) => (
                            <ListItem key={index}>{detail}</ListItem>
                        ))}
                    </List>
                )}
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>{t('Close')}</Button>
            </DialogActions>
        </Dialog>
    );
};
