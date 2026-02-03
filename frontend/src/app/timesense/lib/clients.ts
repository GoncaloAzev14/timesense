import { Session } from 'next-auth';
import ServiceBase, { ResponseEntity } from '@/lib/service-base';
import { Page } from '@datacentric/datacentric-ui/lib/spring-pagination';

export type Client = {
    id?: number;
    name: string;
    clientTicker?: string;
};

const BASE_URL = '/timesense/api/clients';
export default class ClientApi extends ServiceBase {
    constructor(session: Session) {
        super(session);
    }

    async getClients(
        firstRow: number,
        numRows: number,
        sortBy?: string,
        filter?: string,
    ) {
        const url =
            `${BASE_URL}?firstRow=${firstRow}&numRows=${numRows}` +
            (sortBy ? `&sort=${sortBy}` : '') +
            (filter ? `&filter=${filter}` : '');
        return await this.callJsonEndPoint<Page<Client>>('GET', url);
    }

    async createClient(client: Client): Promise<ResponseEntity<Client>> {
        return await this.callJsonEndPoint('POST', BASE_URL, client);
    }

    async getClient(id: number): Promise<Client> {
        return await this.callJsonEndPoint<Client>('GET', `${BASE_URL}/${id}`);
    }

    async updateClient(client: Client): Promise<ResponseEntity<Client>> {
        const url = `${BASE_URL}/${client.id!}`;
        return await this.callJsonEndPoint('PUT', url, client);
    }

    async deleteClient(id: number): Promise<void> {
        await this.callJsonEndPoint('DELETE', `${BASE_URL}/${id}`);
        return;
    }
}

