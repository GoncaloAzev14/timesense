import { Session } from 'next-auth';
import ServiceBase, { ResponseEntity } from '@/lib/service-base';
import { Page } from '@datacentric/datacentric-ui/lib/spring-pagination';
import { AbsenceType } from './absence-types';
import { User } from './users';
import { Status } from './status';
import { AbsenceSubType } from './absence-sub-types';
import { Message } from './messages';

export type AbsenceEntry = {
    status: string; //"PENDING", "DONE", "APPROVED"
    type: string; //"VACATION", "ABSENCES"
    count: number;
};

export type StatusCounts = {
    entries: AbsenceEntry[];
    total: number;
    isSelected?: boolean;
};

export type AbsencesByDateMap = {
    [date: string]: StatusCounts;
};

export type AbsenceAttachment = {
    id: number;
    absenceId: number;
    originalFileName: string;
}

export type Absence = {
    id?: number;
    name?: string;
    type?: AbsenceType;
    subType?: AbsenceSubType;
    recordType?: string;
    absenceHours?: number;
    user?: User;
    startDate: Date;
    endDate: Date;
    approvedDate?: Date;
    approver?: User;
    approvedBy?: User;
    status?: Status;
    reason?: string;
    workDays?: number;
    businessYear?: string;
    observations?: string;
    hasAttachments?: boolean;
};

const BASE_URL = '/timesense/api/absences';
export default class AbsenceApi extends ServiceBase {
    constructor(session: Session) {
        super(session);
    }

    async getAllAbsences(
        firstRow: number,
        numRows: number,
        sortBy?: string,
        filter?: string
    ) {
        const url =
            `${BASE_URL}?firstRow=${firstRow}&numRows=${numRows}` +
            (sortBy ? `&sort=${sortBy}` : '') +
            (filter ? `&filter=${filter}` : '');
        return await this.callJsonEndPoint<Page<Absence>>('GET', url);
    }

    async getUserAbsences(
        firstRow?: number,
        numRows?: number,
        sortBy?: string,
        filter?: string
    ) {
        const url =
            `${BASE_URL}/user?firstRow=${firstRow}&numRows=${numRows}` +
            (sortBy ? `&sort=${sortBy}` : '') +
            (filter ? `&filter=${filter}` : '');
        return await this.callJsonEndPoint<Page<Absence>>('GET', url);
    }

    async createAbsence(absence: Absence): Promise<ResponseEntity<Absence>> {
        return await this.callMessageJsonEndPoint('POST', BASE_URL, absence);
    }

    async getAbsence(id: number): Promise<Absence> {
        return await this.callJsonEndPoint<Absence>('GET', `${BASE_URL}/${id}`);
    }

    async importAbsences(file: FormData): Promise<Message> {
        return await this.callMessageJsonEndPoint(
            'POST',
            `${BASE_URL}/import`,
            file
        );
    }

    async updateAbsence(
        absence: Absence,
        manage?: string
    ): Promise<ResponseEntity<Absence>> {
        const url =
            `${BASE_URL}/${absence.id!}` + (manage ? `?manage=${manage}` : '');
        return await this.callMessageJsonEndPoint('PUT', url, absence);
    }

    async deleteAbsence(id: number): Promise<void> {
        await this.callJsonEndPoint('DELETE', `${BASE_URL}/${id}`);
        return;
    }

    async getAbsencesByDate(
        year: string,
        userFilter?: number[],
        statusFilter?: number[],
        typeFilter?: number[],
        businessYearFilter?: string[],
        scope: string = "SCOPE-TEAM"
    ): Promise<ResponseEntity<AbsencesByDateMap>> {
        const url =
            `${BASE_URL}/byDate/${year}` +
            (userFilter ? `?userFilter=${userFilter}` : ' ') +
            (statusFilter ? `&statusFilter=${statusFilter}` : ' ') +
            (typeFilter ? `&typeFilter=${typeFilter}` : ' ') +
            (businessYearFilter
                ? `&businessYearFilter=${businessYearFilter}`
                : ' ') +
            (`&scope=${scope}`);
        return await this.callJsonEndPoint('GET', url);
    }

    async getAbsencesDetailsByDate(
        date: string,
        userFilter?: number[],
        statusFilter?: number[],
        typeFilter?: number[],
        businessYearFilter?: string[],
        scope: string = "SCOPE-TEAM"
    ): Promise<ResponseEntity<Absence[]>> {
        const url =
            `${BASE_URL}/byDateDetails?date=${date}` +
            (userFilter ? `&userFilter=${userFilter}` : ' ') +
            (statusFilter ? `&statusFilter=${statusFilter}` : ' ') +
            (typeFilter ? `&typeFilter=${typeFilter}` : ' ') +
            (businessYearFilter
                ? `&businessYearFilter=${businessYearFilter}`
                : ' ') +
            (`&scope=${scope}`);
        return await this.callJsonEndPoint('GET', url);
    }

    async approveAbsences(ids: number[]): Promise<void> {
        return await this.callJsonEndPoint('PATCH', `${BASE_URL}`, [
            {
                command: 'approve',
                data: {
                    ids: ids,
                },
            },
        ]);
    }

    async draftAbsences(ids: number[]): Promise<void> {
        return await this.callJsonEndPoint('PATCH', `${BASE_URL}`, [
            {
                command: 'pending',
                data: {
                    ids: ids,
                },
            },
        ]);
    }

    async denyAbsences(ids: number[], reason: string): Promise<void> {
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

    async uploadAttachment(absenceId: number, files: FormData): Promise<Message> {
        return await this.callMessageJsonEndPoint(
            'POST',
            `${BASE_URL}/${absenceId}/attachments`,
            files
        );
    }

    async deleteAttachment(absenceId: number, attachmentId: number): Promise<boolean> {
        try {
            await this.callJsonEndPoint(
                'DELETE',
                `${BASE_URL}/${absenceId}/attachments/${attachmentId}`
            );
            return true;
        } catch (error) {
            console.error(`Failed to delete attachment ${attachmentId}:`, error);
            return false;
        }
    }

    async getAttachmentsByAbsenceId(absenceId: number): Promise<AbsenceAttachment[]> {
        const response = await this.callJsonEndPoint<ResponseEntity<AbsenceAttachment[]>>(
            'GET',
            `${BASE_URL}/${absenceId}/attachments`
        );
        return response.data || [];
    }

    async exportAbsenceAttachments(absenceId: number): Promise<void> {
        await this.callExportEndPoint('GET', `${BASE_URL}/${absenceId}/attachments/downloadAll`,
            'absence-' + absenceId + '-attachments.zip'
        );
    }

    async donwloadAttachment(absenceId: number, attachmentId: number, fileName: string): Promise<void> {
        await this.callExportEndPoint('GET', `${BASE_URL}/${absenceId}/attachments/${attachmentId}`,
            fileName
        );
    }
}
