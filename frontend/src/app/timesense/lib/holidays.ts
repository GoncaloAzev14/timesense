import { Session } from 'next-auth';
import ServiceBase, { ResponseEntity } from '@/lib/service-base';
import { Page } from '@datacentric/datacentric-ui/lib/spring-pagination';
import { Message } from './messages';

export type Holiday = {
    id?: number;
    holidayDate: Date;
    name: string;
};

const BASE_URL = '/timesense/api/holidays';
export default class HolidayApi extends ServiceBase {
    constructor(session: Session) {
        super(session);
    }

    async getHolidays(filter?: string) {
        const url = `${BASE_URL}` + (filter ? `&filter=${filter}` : '');
        return await this.callJsonEndPoint<Holiday[]>('GET', url);
    }

    async getHolidaysPage(
        firstRow: number,
        numRows: number,
        sortBy?: string,
        filter?: string
    ) {
        const url =
            `${BASE_URL}/HolidaysPage?firstRow=${firstRow}&numRows=${numRows}` +
            (sortBy ? `&sort=${sortBy}` : '') +
            (filter ? `&filter=${filter}` : '');
        return await this.callJsonEndPoint<Page<Holiday>>('GET', url);
    }

    async importHolidays(file: FormData): Promise<Message> {
        return await this.callMessageJsonEndPoint(
            'POST',
            `${BASE_URL}/import`,
            file
        );
    }

    async createHolidays(holiday: Holiday): Promise<ResponseEntity<Holiday>> {
        return await this.callJsonEndPoint('POST', BASE_URL, holiday);
    }

    async getHoliday(id: number): Promise<Holiday> {
        return await this.callJsonEndPoint<Holiday>('GET', `${BASE_URL}/${id}`);
    }

    async updateHoliday(holiday: Holiday): Promise<ResponseEntity<Holiday>> {
        const url = `${BASE_URL}/${holiday.id}`;

        return await this.callJsonEndPoint('PUT', url, holiday);
    }

    async deleteHoliday(id: number): Promise<void> {
        await this.callJsonEndPoint('DELETE', `${BASE_URL}/${id}`);
        return;
    }
}
