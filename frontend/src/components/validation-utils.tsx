export const validateNotEmpty = (value: string | null | undefined, setError: (arg0: boolean) => void) => {
    const isEmpty = value === null || value === undefined || value.trim() === '';
    setError(isEmpty);
    return !isEmpty;
};