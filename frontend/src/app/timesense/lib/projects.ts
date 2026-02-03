import { Session } from 'next-auth';
import ServiceBase, { ResponseEntity } from '@/lib/service-base';
import { Page } from '@datacentric/datacentric-ui/lib/spring-pagination';
import { ProjectType } from './project-types';
import { User } from './users';
import { Client } from './clients';
import { Status } from './status';
import { ProjectAssignment } from './project-assignment';
import { Message } from './messages';
import { ProjectTask } from './project-tasks';

export type Project = {
    id?: number;
    name: string;
    description?: string;
    type?: ProjectType;
    tasks?: ProjectTask[];
    manager?: User;
    client?: Client;
    startDate?: Date;
    expectedDueDate?: Date;
    endDate?: Date;
    status?: Status;
    realBudget?: number;
    createdBy?: string;
    createdAt?: Date;
    updatedBy?: string;
    updatedAt?: Date;
};

export type ProjectCostByWeekMap = {
    user?: User;
    startWeek: string;
    month?: string;
    start_date?: string;
    hours: number;
    cost: number;
};

const BASE_URL = '/timesense/api/projects';
export default class ProjectApi extends ServiceBase {
    constructor(session: Session) {
        super(session);
    }

    async getProjects(
        firstRow: number,
        numRows: number,
        sortBy?: string,
        filter?: string,
        statusFilter?: number[],
        clientFilter?: number[],
        managerFilter?: number[],
        scope: string = 'SCOPE-COMPANY'
    ) {
        const url =
            `${BASE_URL}?firstRow=${firstRow}&numRows=${numRows}` +
            (sortBy ? `&sort=${sortBy}` : '') +
            (filter ? `&filter=${filter}` : '') +
            (statusFilter ? `&statusFilter=${statusFilter}` : '') +
            (clientFilter ? `&clientFilter=${clientFilter}` : '') +
            (managerFilter ? `&managerFilter=${managerFilter}` : '') +
            `&scope=${scope}`;
        return await this.callJsonEndPoint<Page<Project>>('GET', url);
    }

    async createProject(project: Project): Promise<ResponseEntity<Project>> {
        return await this.callJsonEndPoint('POST', BASE_URL, project);
    }

    async getProject(id: number): Promise<Project> {
        return await this.callJsonEndPoint<Project>('GET', `${BASE_URL}/${id}`);
    }

    async getProjectTasksByProjectId(
        id: number
    ): Promise<ResponseEntity<ProjectTask[]>> {
        return await this.callJsonEndPoint(
            'GET',
            `${BASE_URL}/${id}/projectTasks`
        );
    }

    async getProjectByCode(code: string): Promise<Project | null> {
        let response = await this.callJsonEndPoint<Page<Project>>(
            'GET',
            `${BASE_URL}?filter=name=${code}`
        );
        if (response.totalElements > 0) {
            return response.content[0];
        } else {
            return null;
        }
    }

    async getProjectCostByWeek(
        id: number,
        includeReporter?: boolean
    ): Promise<ProjectCostByWeekMap[]> {
        return await this.callJsonEndPoint<ProjectCostByWeekMap[]>(
            'GET',
            `${BASE_URL}/${id}/costByWeek` +
                (includeReporter ? `?includeReporter=true` : '')
        );
    }

    async getProjectCostByWeekPage(
        id: number,
        firstRow: number,
        numRows: number,
        sortBy?: string,
        reporterFilter?: number[],
        startDateFilter?: string,
        endDateFilter?: string
    ) {
        return await this.callJsonEndPoint<Page<ProjectCostByWeekMap>>(
            'GET',
            `${BASE_URL}/${id}/costByWeekPage` +
                `?firstRow=${firstRow * numRows}&numRows=${numRows}` +
                (sortBy ? `&sort=${sortBy}` : '') +
                (reporterFilter ? `&reporterFilter=${reporterFilter}` : '') +
                (startDateFilter ? `&startDateFilter=${startDateFilter}` : '') +
                (endDateFilter ? `&endDateFilter=${endDateFilter}` : '')
        );
    }

    async getProjectCostByMonthPage(
        id: number,
        firstRow: number,
        numRows: number,
        sortBy?: string,
        reporterFilter?: number[],
        startDateFilter?: string,
        endDateFilter?: string
    ) {
        return await this.callJsonEndPoint<Page<ProjectCostByWeekMap>>(
            'GET',
            `${BASE_URL}/${id}/costByMonthPage` +
                `?firstRow=${firstRow * numRows}&numRows=${numRows}` +
                (sortBy ? `&sort=${sortBy}` : '') +
                (reporterFilter ? `&reporterFilter=${reporterFilter}` : '') +
                (startDateFilter ? `&startDateFilter=${startDateFilter}` : '') +
                (endDateFilter ? `&endDateFilter=${endDateFilter}` : '')
        );
    }

    async getProjectCostByDay(
        id: number,
        firstRow: number,
        numRows: number,
        sortBy?: string,
        reporterFilter?: number[],
        startDateFilter?: string,
        endDateFilter?: string
    ) {
        return await this.callJsonEndPoint<Page<ProjectCostByWeekMap>>(
            'GET',
            `${BASE_URL}/${id}/costByDay` +
                `?firstRow=${firstRow * numRows}&numRows=${numRows}` +
                (sortBy ? `&sort=${sortBy}` : '') +
                (reporterFilter ? `&reporterFilter=${reporterFilter}` : '') +
                (startDateFilter ? `&startDateFilter=${startDateFilter}` : '') +
                (endDateFilter ? `&endDateFilter=${endDateFilter}` : '')
        );
    }

    async exportProjectTimeRecords(
        id: number,
        projName?: string,
        exportDate?: string
    ): Promise<void> {
        await this.callExportEndPoint(
            'POST',
            `${BASE_URL}/${id}/costByDay/export`,
            `timesense_proj_` +
                (projName ? `${projName}` : '') +
                `_time_records` +
                (exportDate ? `_${exportDate}` : '_') +
                `.csv`
        );
    }

    async getProjectBudget(id: number): Promise<number> {
        return await this.callJsonEndPoint('GET', `${BASE_URL}/${id}/budget`);
    }

    async getProjectUserAllocations(id: number): Promise<ProjectAssignment[]> {
        return await this.callJsonEndPoint(
            'GET',
            `${BASE_URL}/${id}/user-allocations`
        );
    }

    async getLastUsedProjectsByUser(): Promise<Project[]> {
        return await this.callJsonEndPoint<Project[]>(
            'GET',
            `${BASE_URL}/last-used`
        );
    }

    async updateProject(project: Project): Promise<ResponseEntity<Project>> {
        const url = `${BASE_URL}/${project.id!}`;
        return await this.callJsonEndPoint('PUT', url, project);
    }

    async closeProject(id: number): Promise<void> {
        const url = `${BASE_URL}/${id}/close`;
        return await this.callJsonEndPoint('PATCH', url);
    }

    async deleteProject(id: number): Promise<void> {
        await this.callJsonEndPoint('DELETE', `${BASE_URL}/${id}`);
        return;
    }

    async importProjects(file: FormData): Promise<Message> {
        return await this.callMessageJsonEndPoint(
            'POST',
            `${BASE_URL}/import`,
            file
        );
    }
}
