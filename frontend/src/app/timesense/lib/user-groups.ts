import { Session } from 'next-auth';
import ServiceBase, { ResponseEntity } from '@/lib/service-base';
import { Page } from '@datacentric/datacentric-ui/lib/spring-pagination';
import { Role } from './roles';

export type UserGroup = {
    id?: number;
    name?: string;
    tokenId?: string;
    roles?: Role[];
};

const BASE_URL = '/timesense/api/user-groups';
export default class UserGroupApi extends ServiceBase {
    constructor(session: Session) {
        super(session);
    }

    async getUserGroups(
        firstRow: number,
        numRows: number,
        sortBy?: string,
        filter?: string
    ) {
        const url =
            `${BASE_URL}?firstRow=${firstRow}&numRows=${numRows}` +
            (sortBy ? `&sort=${sortBy}` : '') +
            (filter ? `&filter=${filter}` : '');
        return await this.callJsonEndPoint<Page<UserGroup>>('GET', url);
    }

    async getUserGroup(id: number): Promise<UserGroup> {
        return await this.callJsonEndPoint('GET', `${BASE_URL}/${id}`);
    }

    async createUserGroup(
        userGroup: UserGroup
    ): Promise<ResponseEntity<UserGroup>> {
        return await this.callJsonEndPoint('POST', BASE_URL, userGroup);
    }

    async updateUserGroup(
        userGroup: UserGroup
    ): Promise<ResponseEntity<UserGroup>> {
        const url = `${BASE_URL}/${userGroup.id}`;
        return await this.callJsonEndPoint('PUT', url, userGroup);
    }

    async deleteUserGroup(id: number) {
        await this.callJsonEndPoint('DELETE', `${BASE_URL}/${id}`);
        return;
    }
}
