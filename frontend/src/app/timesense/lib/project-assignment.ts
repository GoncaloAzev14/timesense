import { Session } from 'next-auth';
import ServiceBase, { ResponseEntity } from '@/lib/service-base';
import { Page } from '@datacentric/datacentric-ui/lib/spring-pagination';
import { Project } from './projects';
import { User } from './users';

export type ProjectAssignment = {
    id?: number;
    user?: User;
    project?: Project;
    allocation?: number;
    description?: string;
    startDate?: Date;
    endDate?: Date;
    createdBy?: string;
    createdAt?: Date;
    updatedBy?: string;
    updatedAt?: Date;
};

const BASE_URL = '/timesense/api/project-assignments';
export default class ProjectAssignmentApi extends ServiceBase {
    constructor(session: Session) {
        super(session);
    }

    async getProjectAssignments(
        firstRow: number,
        numRows: number,
        sortBy?: string,
        filter?: string
    ) {
        const url =
            `${BASE_URL}?firstRow=${firstRow}&numRows=${numRows}` +
            (sortBy ? `&sort=${sortBy}` : '') +
            (filter ? `&filter=${filter}` : '');
        return await this.callJsonEndPoint<Page<ProjectAssignment>>('GET', url);
    }

    async createProjectAssignment(
        projectAssignment: ProjectAssignment
    ): Promise<ResponseEntity<ProjectAssignment>> {
        return await this.callJsonEndPoint('POST', BASE_URL, projectAssignment);
    }

    async getProjectAssignment(id: number): Promise<ProjectAssignment> {
        return await this.callJsonEndPoint<ProjectAssignment>(
            'GET',
            `${BASE_URL}/${id}`
        );
    }

    async updateProjectAssignment(
        projectAssignment: ProjectAssignment
    ): Promise<ResponseEntity<ProjectAssignment>> {
        const url = `${BASE_URL}/${projectAssignment.id!}`;
        return await this.callJsonEndPoint('PUT', url, projectAssignment);
    }

    async deleteProjectAssignment(id: number): Promise<void> {
        await this.callJsonEndPoint('DELETE', `${BASE_URL}/${id}`);
        return;
    }
}
