import { Session } from 'next-auth';
import ServiceBase, { ResponseEntity } from '@/lib/service-base';
import { Page } from '@datacentric/datacentric-ui/lib/spring-pagination';
import { ProjectType } from './project-types';

export type ProjectTask = {
    id?: number;
    name: string;
    description?: string;
    projectTypes?: ProjectType[];
    updatedBy?: string;
};

const BASE_URL = '/timesense/api/project-tasks';
export default class ProjectTaskApi extends ServiceBase {
    constructor(session: Session) {
        super(session);
    }

    async getProjectTasks(
        firstRow: number,
        numRows: number,
        sortBy?: string,
        filter?: string
    ) {
        const url =
            `${BASE_URL}?firstRow=${firstRow}&numRows=${numRows}` +
            (sortBy ? `&sort=${sortBy}` : '') +
            (filter ? `&filter=${filter}` : '');
        return await this.callJsonEndPoint<Page<ProjectTask>>('GET', url);
    }

    async createProjectTask(
        projectTask: ProjectTask
    ): Promise<ResponseEntity<ProjectTask>> {
        return await this.callJsonEndPoint('POST', BASE_URL, projectTask);
    }

    async getProjectTask(id: number): Promise<ProjectTask> {
        return await this.callJsonEndPoint<ProjectTask>(
            'GET',
            `${BASE_URL}/${id}`
        );
    }

    async updateProjectTask(
        projectTask: ProjectTask
    ): Promise<ResponseEntity<ProjectTask>> {
        const url = `${BASE_URL}/${projectTask.id!}`;
        return await this.callJsonEndPoint('PUT', url, projectTask);
    }

    async getProjectTypeTasks(typeId: number): Promise<ResponseEntity<ProjectTask[]>> {
        const url = `${BASE_URL}/${typeId!}/byType`;
        return await this.callJsonEndPoint('GET', url);
    }
}

