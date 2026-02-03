import { Session } from 'next-auth';
import ServiceBase, { ResponseEntity } from '@/lib/service-base';
import { Page } from '@datacentric/datacentric-ui/lib/spring-pagination';

export type AbsenceSubType = {
    id?: number;
    name: string;
    description?: string;
    updatedBy?: string;
};

const BASE_URL = '/timesense/api/absence-sub-types';
export default class AbsenceSubTypeApi extends ServiceBase {
    constructor(session: Session) {
        super(session);
    }

    async getAbsenceSubTypes(
        firstRow: number,
        numRows: number,
        sortBy?: string,
        filter?: string
    ) {
        const url =
            `${BASE_URL}?firstRow=${firstRow}&numRows=${numRows}` +
            (sortBy ? `&sort=${sortBy}` : '') +
            (filter ? `&filter=${filter}` : '');
        return await this.callJsonEndPoint<Page<AbsenceSubType>>('GET', url);
    }

    async createAbsenceSubType(
        absenceSubType: AbsenceSubType
    ): Promise<ResponseEntity<AbsenceSubType>> {
        return await this.callJsonEndPoint('POST', BASE_URL, absenceSubType);
    }

    async getAbsenceSubType(id: number): Promise<AbsenceSubType> {
        return await this.callJsonEndPoint<AbsenceSubType>(
            'GET',
            `${BASE_URL}/${id}`
        );
    }

    async updateAbsenceSubType(
        absenceSubType: AbsenceSubType
    ): Promise<ResponseEntity<AbsenceSubType>> {
        const url = `${BASE_URL}/${absenceSubType.id!}`;
        return await this.callJsonEndPoint('PUT', url, absenceSubType);
    }
}

