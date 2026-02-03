import { Session } from 'next-auth';
import ServiceBase, { ResponseEntity } from '@/lib/service-base';
import { Page } from '@datacentric/datacentric-ui/lib/spring-pagination';

export type Role = {
    id?: number;
    name?: string;
    parentRole?: Role;
};

const BASE_URL = '/timesense/api/user-roles';
export default class RolesApi extends ServiceBase {
    constructor(session: Session) {
        super(session);
    }

    async getRoles(
        firstRow: number,
        numRows: number,
        sortBy?: string,
        filter?: string
    ) {
        const url =
            `${BASE_URL}?firstRow=${firstRow}&numRows=${numRows}` +
            (sortBy ? `&sort=${sortBy}` : '') +
            (filter ? `&filter=${filter}` : '');
        return await this.callJsonEndPoint<Page<Role>>('GET', url);
    }

    async createRole(role: Role): Promise<ResponseEntity<Role>> {
        return await this.callJsonEndPoint('POST', BASE_URL, role);
    }

    async getRole(id: number): Promise<Role> {
        return await this.callJsonEndPoint<Role>('GET', `${BASE_URL}/${id}`);
    }

    async updateRole(role: Role): Promise<ResponseEntity<Role>> {
        const url = `${BASE_URL}/${role.id!}`;
        return await this.callJsonEndPoint('PUT', url, role);
    }

    async deleteRole(id: number): Promise<void> {
        await this.callJsonEndPoint('DELETE', `${BASE_URL}/${id}`);
        return;
    }
}
