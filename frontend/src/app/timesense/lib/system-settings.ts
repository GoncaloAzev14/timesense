import { Session } from 'next-auth';
import ServiceBase, { ResponseEntity } from '@/lib/service-base';
import { Page } from '@datacentric/datacentric-ui/lib/spring-pagination';

export type SystemSetting = {
    id?: number;
    name: string;
    value?: string;
    updatedBy?: string;
    userEditable?: boolean;
};

const BASE_URL = '/timesense/api/system-settings';
export default class SystemSettingApi extends ServiceBase {
    constructor(session: Session) {
        super(session);
    }

    async getSystemSettings(
        firstRow: number,
        numRows: number,
        sortBy?: string,
        filter?: string
    ) {
        const url =
            `${BASE_URL}?firstRow=${firstRow}&numRows=${numRows}` +
            (sortBy ? `&sort=${sortBy}` : '') +
            (filter ? `&filter=${filter}` : '');
        return await this.callJsonEndPoint<Page<SystemSetting>>('GET', url);
    }

    async createSystemSetting(
        systemSetting: SystemSetting
    ): Promise<ResponseEntity<SystemSetting>> {
        return await this.callJsonEndPoint('POST', BASE_URL, systemSetting);
    }

    async getSystemSetting(id: number): Promise<SystemSetting> {
        return await this.callJsonEndPoint<SystemSetting>(
            'GET',
            `${BASE_URL}/${id}`
        );
    }

    async getSystemSettingByName(name: string): Promise<SystemSetting> {
        return await this.callJsonEndPoint<SystemSetting>(
            'GET',
            `${BASE_URL}/byName/${name}`
        );
    }

    async updateSystemSetting(
        systemSetting: SystemSetting
    ): Promise<ResponseEntity<SystemSetting>> {
        const url = `${BASE_URL}/${systemSetting.id!}`;
        return await this.callJsonEndPoint('PUT', url, systemSetting);
    }

    async closeBusinessYear(): Promise<ResponseEntity<string>> {
        return await this.callJsonEndPoint(
            'POST',
            `${BASE_URL}/closeBusinessYear`);
    }
}
