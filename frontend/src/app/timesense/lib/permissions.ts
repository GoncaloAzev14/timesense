import { Session } from 'next-auth';
import ServiceBase from '@/lib/service-base';

export const PERMISSIONS_SUBJECT_TYPES: (keyof Permissions)[] = [
    'user',
    'group',
    'role',
];

export interface Subject {
    id: number;
    name: string;
}

export interface Permissions {
    user: Subject[];
    group: Subject[];
    role: Subject[];
}

export interface PermissionsMap extends Map<string, Permissions> { }

export function emptyPermissions(): Permissions {
    return {
        user: [],
        group: [],
        role: [],
    };
}

export interface ResourcePermission {
    id: number,
    resourceType: string,
    resourceId: number,
    accessType: string,
    subjectType: string,
    subject: number,
}

const BASE_URL = '/timesense/api/permissions';
export default class PermissionsApi extends ServiceBase {
    constructor(session: Session) {
        super(session);
    }

    async getPermissionsForResource(
        resourceType: string,
        resourceId: number
    ): Promise<PermissionsMap> {
        const url = `${BASE_URL}/${resourceType}/${resourceId}`;
        return new Map(
            Object.entries(await this.callJsonEndPoint<Object>('GET', url))
        );
    }

    async getSystemPermissions(): Promise<string[]> {
        const url = `${BASE_URL}/System/0/user`;
        return await this.callJsonEndPoint<string[]>('GET', url);
    }

    async getUserPermissionsForResource(
        resourceType: string,
        resourceId: number
    ): Promise<string[]> {
        const url = `${BASE_URL}/${resourceType}/${resourceId}/user`;
        return await this.callJsonEndPoint<string[]>('GET', url);
    }

    async savePermissionsForResource(
        resourceType: string,
        resourceId: number,
        permissions: PermissionsMap
    ): Promise<void> {
        const url = `${BASE_URL}/${resourceType}/${resourceId}`;
        return await this.callJsonEndPoint(
            'POST',
            url,
            Object.fromEntries(permissions)
        );
    }

    async getUserPermissions(): Promise<ResourcePermission[]> {
        const url = `${BASE_URL}/user`;
        return await this.callJsonEndPoint<ResourcePermission[]>('GET', url);
    }
}
