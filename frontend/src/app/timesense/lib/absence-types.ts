import { Session } from 'next-auth';
import ServiceBase, { ResponseEntity } from '@/lib/service-base';
import { Page } from '@datacentric/datacentric-ui/lib/spring-pagination';

export type AbsenceType = {
    id?: number;
    name: string;
};

const BASE_URL = '/timesense/api/absence-types';
export default class AbsenceTypeApi extends ServiceBase {
    constructor(session: Session) {
        super(session);
    }

    async getAbsenceTypes(
        firstRow: number,
        numRows: number,
        sortBy?: string,
        filter?: string
    ) {
        const url =
            `${BASE_URL}?firstRow=${firstRow}&numRows=${numRows}` +
            (sortBy ? `&sort=${sortBy}` : '') +
            (filter ? `&filter=${filter}` : '');
        return await this.callJsonEndPoint<Page<AbsenceType>>('GET', url);
    }

    async createAbsenceType(
        absenceType: AbsenceType
    ): Promise<ResponseEntity<AbsenceType>> {
        return await this.callJsonEndPoint('POST', BASE_URL, absenceType);
    }

    async getAbsenceType(id: number): Promise<AbsenceType> {
        return await this.callJsonEndPoint<AbsenceType>(
            'GET',
            `${BASE_URL}/${id}`
        );
    }

    async updateAbsenceType(
        absenceType: AbsenceType
    ): Promise<ResponseEntity<AbsenceType>> {
        const url = `${BASE_URL}/${absenceType.id!}`;
        return await this.callJsonEndPoint('PUT', url, absenceType);
    }

    async deleteAbsenceType(id: number): Promise<void> {
        await this.callJsonEndPoint('DELETE', `${BASE_URL}/${id}`);
        return;
    }
}

