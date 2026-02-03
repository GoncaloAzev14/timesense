/**
 * Payload for the simple return of a regular operation
 */
export type Message = {
    data?: {
        message_code: string;
        message_args: string[];
    };
    messageCode: string;
    message: string;
};
