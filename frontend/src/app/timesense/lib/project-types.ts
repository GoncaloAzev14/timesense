import { Session } from 'next-auth';
import ServiceBase, { ResponseEntity } from '@/lib/service-base';
import { Page } from '@datacentric/datacentric-ui/lib/spring-pagination';

export type ProjectType = {
    id?: number;
    name: string;
    description?: string;
    lineManager?: boolean;
};

const BASE_URL = '/timesense/api/project-types';
export default class ProjectTypeApi extends ServiceBase {
    constructor(session: Session) {
        super(session);
    }

    async getProjectTypes(
        firstRow: number,
        numRows: number,
        sortBy?: string,
        filter?: string
    ) {
        const url =
            `${BASE_URL}?firstRow=${firstRow}&numRows=${numRows}` +
            (sortBy ? `&sort=${sortBy}` : '') +
            (filter ? `&filter=${filter}` : '');
        return await this.callJsonEndPoint<Page<ProjectType>>('GET', url);
    }

    async createProjectType(
        projectType: ProjectType
    ): Promise<ResponseEntity<ProjectType>> {
        return await this.callJsonEndPoint('POST', BASE_URL, projectType);
    }

    async getProjectType(id: number): Promise<ProjectType> {
        return await this.callJsonEndPoint<ProjectType>(
            'GET',
            `${BASE_URL}/${id}`
        );
    }

    async updateProjectType(
        projectType: ProjectType
    ): Promise<ResponseEntity<ProjectType>> {
        const url = `${BASE_URL}/${projectType.id!}`;
        return await this.callJsonEndPoint('PUT', url, projectType);
    }

    async deleteProjectType(id: number): Promise<void> {
        await this.callJsonEndPoint('DELETE', `${BASE_URL}/${id}`);
        return;
    }
}

