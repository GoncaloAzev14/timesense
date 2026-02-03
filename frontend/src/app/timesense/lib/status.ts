import { Session } from 'next-auth';
import ServiceBase, { ResponseEntity } from '@/lib/service-base';
import { Page } from '@datacentric/datacentric-ui/lib/spring-pagination';

export type Status = {
    id?: number;
    name: string;
};

const BASE_URL = '/timesense/api/status';
export default class StatusApi extends ServiceBase {
    constructor(session: Session) {
        super(session);
    }

    async getAllStatus(
        firstRow: number,
        numRows: number,
        sortBy?: string,
        filter?: string,
    ) {
        const url =
            `${BASE_URL}?firstRow=${firstRow}&numRows=${numRows}` +
            (sortBy ? `&sort=${sortBy}` : '') +
            (filter ? `&filter=${filter}` : '');
        return await this.callJsonEndPoint<Page<Status>>('GET', url);
    }

    async createStatus(status: Status): Promise<ResponseEntity<Status>> {
        return await this.callJsonEndPoint('POST', BASE_URL, status);
    }

    async getStatus(id: number): Promise<Status> {
        return await this.callJsonEndPoint<Status>('GET', `${BASE_URL}/${id}`);
    }

    async updateStatus(status: Status): Promise<ResponseEntity<Status>> {
        const url = `${BASE_URL}/${status.id!}`;
        return await this.callJsonEndPoint('PUT', url, status);
    }

    async deleteStatus(id: number): Promise<void> {
        await this.callJsonEndPoint('DELETE', `${BASE_URL}/${id}`);
        return;
    }
}

