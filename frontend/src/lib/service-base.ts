import { Session } from 'next-auth';

// Extend the Session with the access token that is populated by our
// frontend server. This access token allows us to make authenticated
// calls to our applicational backend server
declare module 'next-auth' {
    interface Session {
        accessToken: string;
    }
}

export type ResponseEntity<T> = {
    messageCode: string;
    message: string;
    data: T;
};

export class ServiceError extends Error {
    statusCode: number;
    errorContent: any;

    constructor(
        message: string,
        status_code: number,
        errorContent: ReadableStream<Uint8Array> | string | Array<any> | null
    ) {
        super(message);
        this.statusCode = status_code;
        this.errorContent = errorContent;
    }

    getContentAsJson(): any {
        return this.errorContent;
    }
}

const ISO_DATE_FORMAT =
    /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d{3}[+-]\d{2}:\d{2}$/;

export default class ServiceBase {
    private token: string;
    constructor(session: Session) {
        this.token = session ? session.accessToken : '';
    }

    async callEndPoint(
        method: string,
        endpoint: string,
        body?: object,
        signal?: AbortSignal,
        timeout_sec: number = 30000
    ): Promise<Response> {
        const headers: HeadersInit = {
            Authorization: `Bearer ${this.token}`,
        };

        if (body && !(body instanceof FormData)) {
            headers['Content-Type'] = 'application/json';
        }

        const controller = new AbortController();
        const id = setTimeout(() => controller.abort("Timeout"), timeout_sec);

        const fetchOptions: RequestInit = {
            method,
            headers,
            body: body ? (body instanceof FormData ? body : JSON.stringify(body)) : undefined,
            signal: signal || controller.signal,
        };

        try {
            return await fetch(endpoint, fetchOptions);
        } catch (error: any) {
            const runned_signal = signal || controller.signal;
            if (runned_signal.aborted) {
                if (runned_signal.reason == 'Timeout') {
                    console.error("Request aborted due to timeout");
                    throw new Error("Request aborted due to timeout");
                } else {
                    console.error("Request aborted:", runned_signal.reason || "Unknown reason");
                    throw new Error(runned_signal.reason || "Request aborted");
                }
            }
            throw new ServiceError('Failed to call endpoint', error?.status ?? 500, null);
        } finally {
            clearTimeout(id);
        }
    }

    async callExportEndPoint(
        method: string,
        endpoint: string,
        fileName: string,
        body?: object,
        signal?: AbortSignal
    ): Promise<void> {
        var response = await this.callEndPoint(method, endpoint, body, signal);

        if (!response.ok) {
            throw new Error('Failed to export!');
        }

        // convert http backend response into a blob object that contains the xml file
        // generate url for it to be treated as a "real" file that can be accessed and downloaded
        // create an invisible anchor element to trigger the file download
        // release memory by revoking the blob url
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = fileName;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
    }

    async callJsonEndPoint<T>(
        method: string,
        endpoint: string,
        body?: object,
        signal?: AbortSignal
    ): Promise<T> {
        var response = await this.callEndPoint(method, endpoint, body, signal);

        if (response.status === 401) {
            throw new ServiceError(
                'EndpointAccessTokenError',
                response.status,
                null
            );
        }

        if (response.ok) {
            try {
                let result = (await response.json()) as T;
                this.fixDates(result);
                return result;
            } catch (e) {
                throw new ServiceError(
                    'Failed to parse response' + e,
                    response.status,
                    null
                );
            }
        } else {
            throw new ServiceError(
                'Failed to call endpoint',
                await response.status,
                await response.text()
            );
        }
    }

    async callMessageJsonEndPoint<T>(
        method: string,
        endpoint: string,
        body?: object,
        signal?: AbortSignal
    ): Promise<T> {
        var response = await this.callEndPoint(method, endpoint, body, signal);

        if (response.status === 401) {
            throw new ServiceError(
                'EndpointAccessTokenError',
                response.status,
                null
            );
        }

        try {
            let result = (await response.json()) as T;
            this.fixDates(result);
            return result;
        } catch (e) {
            throw new ServiceError(
                'Failed to parse response' + e,
                response.status,
                null
            );
        }
    }

    private fixDates(data: any) {
        if (data === null || data === undefined || typeof data !== 'object') {
            return;
        }

        for (const key of Object.keys(data)) {
            const value = data[key];
            if (
                value &&
                typeof value === 'string' &&
                ISO_DATE_FORMAT.test(value)
            ) {
                data[key] = new Date(value);
            } else if (typeof value === 'object') {
                this.fixDates(value);
            }
        }
    }
}
