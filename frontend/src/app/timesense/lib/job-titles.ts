import { Session } from 'next-auth';
import ServiceBase, { ResponseEntity } from '@/lib/service-base';
import { Page } from '@datacentric/datacentric-ui/lib/spring-pagination';

export type JobTitle = {
    id?: number;
    name: string;
    rate: number;
    startDate?: Date;
    endDate?: Date;
};

const BASE_URL = '/timesense/api/job-titles';
export default class JobTitleApi extends ServiceBase {
    constructor(session: Session) {
        super(session);
    }

    async getJobTitles(
        firstRow: number,
        numRows: number,
        sortBy?: string,
        filter?: string
    ) {
        const url =
            `${BASE_URL}?firstRow=${firstRow}&numRows=${numRows}` +
            (sortBy ? `&sort=${sortBy}` : '') +
            (filter ? `&filter=${filter}` : '');
        return await this.callJsonEndPoint<Page<JobTitle>>('GET', url);
    }

    async createJobTitle(
        jobTitle: JobTitle
    ): Promise<ResponseEntity<JobTitle>> {
        return await this.callJsonEndPoint('POST', BASE_URL, jobTitle);
    }

    async getJobTitle(id: number): Promise<JobTitle> {
        return await this.callJsonEndPoint<JobTitle>(
            'GET',
            `${BASE_URL}/${id}`
        );
    }

    async updateJobTitle(
        jobTitle: JobTitle
    ): Promise<ResponseEntity<JobTitle>> {
        const url = `${BASE_URL}/${jobTitle.id!}`;
        return await this.callJsonEndPoint('PUT', url, jobTitle);
    }

    async deleteJobTitle(id: number): Promise<void> {
        await this.callJsonEndPoint('DELETE', `${BASE_URL}/${id}`);
        return;
    }
}

