export type TicketNoteMessageDto = {
    body?: string | null;
    createdAt?: string | null;
    authorLabel?: string | null;
};
export type TicketDto = {
    ticketID: number;
    ticketCode?: string | null;
    title?: string | null;
    deskID?: number | null;
    deskCode?: string | null;
    spaceID?: number | null;
    spaceName?: string | null;
    officeCode?: string | null;
    description?: string | null;
    technicianNote?: string | null;
    technicianNoteHistory?: TicketNoteMessageDto[] | null;
    workerNoteHistory?: TicketNoteMessageDto[] | null;
    hostNoteHistory?: TicketNoteMessageDto[] | null;
    status: string;
    assignedTechID?: number | null;
    assignedTechName?: string | null;
    assignedTechSurname?: string | null;
    resolution?: string | null;
    createdAt?: string;
    severity?: string;
    resolvedAt?: string | null;
    estimatedResolutionAt?: string | null;
    workerID?: number | null;
    workerName?: string | null;
    workerSurname?: string | null;
};
export class TicketNoteMessage {
    public constructor(public readonly body: string, public readonly createdAt: string | null, public readonly authorLabel: string) { }
}
export class TicketResponse {
    public constructor(public readonly ticketID: number, public readonly ticketCode: string | null, public readonly title: string | null, public readonly deskCode: string | null, public readonly deskID: number | null, public readonly description: string | null, public readonly technicianNote: string | null, public readonly technicianNoteHistory: TicketNoteMessage[], public readonly workerNoteHistory: TicketNoteMessage[], public readonly hostNoteHistory: TicketNoteMessage[] = [], public readonly status: string, public readonly assignedTechID: number | null, public readonly resolution: string | null, public readonly createdAt: string, public readonly severity: string | null, public readonly resolvedAt: string | null, public readonly estimatedResolutionAt: string | null) { }
    public get isResolved(): boolean {
        return (this.status || '').toUpperCase() === 'RESOLVED';
    }
    public get displayTicketLabel(): string {
        return this.ticketCode ? `ticket#${this.ticketCode}` : `Ticket #${this.ticketID}`;
    }
    public get displayTicketHeading(): string {
        const t = (this.title ?? '').trim();
        if (t)
            return t;
        return this.displayTicketLabel;
    }
    public get displayDeskLabel(): string {
        return this.deskCode ? `desk#${this.deskCode}` : this.deskID != null ? `Desk #${this.deskID}` : 'Desk';
    }
}
