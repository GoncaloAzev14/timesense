import { Session } from 'next-auth';
import ServiceBase from './service-base';
import { Role } from '@/app/timesense/lib/roles';

export default class UsersService extends ServiceBase {
    constructor(token: Session) {
        super(token);
    }

    fetchUsers() {
        return this.callEndPoint('GET', '/api/users');
    }

    /**
     * Retrieves the list of internal permissions of user (Admin, Manager, User)
     * @returns Array of internal user permissions
     */
    async fetchInternalRoles(): Promise<Role[] | undefined> {
        const response = await this.callEndPoint(
            'GET',
            '/timesense/api/user-roles/user/internal'
        );
        if (response.status === 200) {
            const data = await response.json();
            return data;
        }
    }

    addUser(newUserName: string) {
        return this.callEndPoint('POST', '/api/users', { name: newUserName });
    }
}
