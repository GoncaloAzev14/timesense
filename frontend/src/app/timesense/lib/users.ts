import { Session } from 'next-auth';
import ServiceBase, { ResponseEntity } from '@/lib/service-base';
import { Page } from '@datacentric/datacentric-ui/lib/spring-pagination';
import { Role } from './roles';
import { UserGroup } from './user-groups';
import { JobTitle } from './job-titles';

export type User = {
    id: number;
    name: string;
    birthdate?: Date;
    email?: string;
    lineManager?: User;
    lineManagerId?: number;
    jobTitle?: JobTitle;
    admissionDate?: Date;
    exitDate?: Date;
    currentYearVacationDays?: number;
    prevYearVacationDays?: number;
    userRoles?: Role[];
    userGroups?: UserGroup[];
};

const BASE_URL = '/timesense/api/users';
export default class UserApi extends ServiceBase {
    constructor(session: Session) {
        super(session);
    }

    /**
     * Return users who's name matches the given query
     */
    async getUsers(
        firstRow: number,
        numRows: number,
        sortBy?: string,
        query?: string,
        scope: string = 'SCOPE-TEAM'
    ) {
        const url =
            `${BASE_URL}?firstRow=${firstRow}&numRows=${numRows}` +
            (sortBy ? `&sort=${sortBy}` : '') +
            (query ? `&filter=${query}` : '') +
            `&scope=${scope}`;
        return await this.callJsonEndPoint<Page<User>>('GET', url);
    }

    async getUser(id: number): Promise<User> {
        return await this.callJsonEndPoint('GET', `${BASE_URL}/${id}`);
    }

    async getUserInfo(): Promise<User> {
        return await this.callJsonEndPoint('GET', `${BASE_URL}/info`);
    }

    async createUser(user: User): Promise<ResponseEntity<User>> {
        return await this.callJsonEndPoint('POST', BASE_URL, user);
    }

    async updateUser(user: User): Promise<ResponseEntity<User>> {
        return await this.callJsonEndPoint(
            'PUT',
            `${BASE_URL}/${user.id}`,
            user
        );
    }

    async deleteUser(id: number) {
        await this.callJsonEndPoint('DELETE', `${BASE_URL}/${id}`);
    }

    async synchronizeUsers() {
        await this.callJsonEndPoint('POST', `${BASE_URL}/synchronize`);
    }
}
