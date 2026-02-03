import ServiceBase, { ResponseEntity } from '@/lib/service-base';
import { Project } from './projects';
import { Status } from './status';
import { User } from './users';
import { Page } from '@datacentric/datacentric-ui/lib/spring-pagination';
import { Session } from 'next-auth';
import { ProjectTask } from './project-tasks';

export type TimeRecord = {
    id?: number;
    user?: User;
    project: Project;
    task?: ProjectTask;
    hours: number;
    description?: string;
    status?: Status;
    startDate: Date;
    endDate: Date;
    approvedAt?: Date;
    approvedBy?: User;
    createdAt?: Date;
    updatedAt?: Date;
    reason?: string;
};

export type FilteredTimeRecord = {
    id?: number;
    userName: string;
    projectName: string;
    taskName?: string;
    hours: number;
    description?: string;
    statusName?: string;
    startDate: Date;
    endDate: Date;
    // approvedAt?: Date;
    // approvedBy?: User;
    // createdAt?: Date;
    // updatedAt?: Date;
};

const BASE_URL = '/timesense/api/time-records';
export default class TimeRecordApi extends ServiceBase {
    constructor(session: Session) {
        super(session);
    }

    async getFilteredTimeRecords(
        firstRow: number,
        numRows: number,
        sortBy?: string,
        projectFilter?: number[],
        reporterFilter?: number[],
        startDateFilter?: string,
        endDateFilter?: string
    ) {
        return await this.callJsonEndPoint<Page<FilteredTimeRecord>>(
            'GET',
            `${BASE_URL}/filtered` +
                `?firstRow=${firstRow * numRows}&numRows=${numRows}` +
                (sortBy ? `&sort=${sortBy}` : '') +
                (projectFilter ? `&projectFilter=${projectFilter}` : '') +
                (reporterFilter ? `&reporterFilter=${reporterFilter}` : '') +
                (startDateFilter ? `&startDateFilter=${startDateFilter}` : '') +
                (endDateFilter ? `&endDateFilter=${endDateFilter}` : '')
        );
    }

    async exportTimeRecords(
        scope: string = 'SCOPE-TEAM',
        projectFilter?: number[],
        reporterFilter?: number[],
        startDateFilter?: string,
        endDateFilter?: string,
        exportDate?: string
    ): Promise<void> {
        await this.callExportEndPoint(
            'POST',
            `${BASE_URL}/export` +
                `?scope=${scope}` +
                (projectFilter ? `&projectFilter=${projectFilter}` : '') +
                (reporterFilter ? `&reporterFilter=${reporterFilter}` : '') +
                (startDateFilter ? `&startDateFilter=${startDateFilter}` : '') +
                (endDateFilter ? `&endDateFilter=${endDateFilter}` : ''),
            `timesense_time_records_` +
                (exportDate ? `_${exportDate}` : '_') +
                `.csv`
        );
    }

    async getTimeRecords(
        firstRow: number,
        numRows: number,
        sortBy?: string,
        query?: string,
        scope?: 'my_teams' | 'my_projects' | 'company'
    ) {
        const url =
            `${BASE_URL}?firstRow=${firstRow}&numRows=${numRows}` +
            (sortBy ? `&sort=${sortBy}` : '') +
            (query ? `&filter=${query}` : '') +
            (scope ? `&scope=${scope}` : '');
        return await this.callJsonEndPoint<Page<TimeRecord>>('GET', url);
    }

    async getUserTimeRecords(
        firstRow: number,
        numRows: number,
        startDate: string,
        sortBy?: string
    ) {
        const url =
            `${BASE_URL}/user-page?firstRow=${firstRow}&numRows=${numRows}` +
            (sortBy ? `&sort=${sortBy}` : '') +
            `&startDate=${startDate}`;
        return await this.callJsonEndPoint<Page<TimeRecord>>('GET', url);
    }

    async getTimeRecord(id: number): Promise<TimeRecord> {
        return await this.callJsonEndPoint('GET', `${BASE_URL}/${id}`);
    }

    async getTimeRecordsByUserAndDate(
        startDate: string,
        endDate: string
    ): Promise<ResponseEntity<TimeRecord[]>> {
        const url = `${BASE_URL}/byUser?startDate=${startDate}&endDate=${endDate}`;
        return await this.callJsonEndPoint('GET', url);
    }

    async createTimeRecord(
        timeRecord: TimeRecord
    ): Promise<ResponseEntity<TimeRecord>> {
        return await this.callJsonEndPoint('POST', BASE_URL, timeRecord);
    }

    async createTimeRecordsDraft(
        timeRecords: TimeRecord[]
    ): Promise<ResponseEntity<TimeRecord[]>> {
        return await this.callJsonEndPoint(
            'POST',
            `${BASE_URL}/bulk`,
            timeRecords
        );
    }

    async createTimeRecordsSubmit(
        timeRecords: TimeRecord[]
    ): Promise<ResponseEntity<TimeRecord[]>> {
        return await this.callJsonEndPoint(
            'POST',
            `${BASE_URL}/bulk?submit=true`,
            timeRecords
        );
    }

    async updateTimeRecord(
        timeRecord: TimeRecord,
        manage?: string
    ): Promise<ResponseEntity<TimeRecord>> {
        const url =
            `${BASE_URL}/${timeRecord.id!}` +
            (manage ? `?manage=${manage}` : '');
        return await this.callJsonEndPoint('PUT', url, timeRecord);
    }

    async updateProjectAndTask(
        timeRecordIds: number[],
        projectId: number,
        taskId: number
    ): Promise<ResponseEntity<TimeRecord>> {
        return await this.callJsonEndPoint('PUT', `${BASE_URL}/batch`, {
            projectId,
            taskId,
            timeRecordIds,
        });
    }

    async deleteTimeRecord(id: number) {
        await this.callJsonEndPoint('DELETE', `${BASE_URL}/${id}`);
    }

    async approveTimeRecords(ids: number[]): Promise<void> {
        return await this.callJsonEndPoint('PATCH', `${BASE_URL}`, [
            {
                command: 'approve',
                data: {
                    ids: ids,
                },
            },
        ]);
    }

    async denyTimeRecords(ids: number[], reason: string): Promise<void> {
        return await this.callJsonEndPoint('PATCH', `${BASE_URL}`, [
            {
                command: 'deny',
                data: {
                    ids: ids,
                    reason: reason,
                },
            },
        ]);
    }

    async draftTimeRecords(ids: number[]): Promise<void> {
        return await this.callJsonEndPoint('PATCH', `${BASE_URL}`, [
            {
                command: 'draft',
                data: {
                    ids: ids,
                },
            },
        ]);
    }
}
