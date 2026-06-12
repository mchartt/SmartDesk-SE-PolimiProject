import { IResolvable } from './i-resolvable.model';
import { TicketStatus } from './ticket-status.type';
export class Ticket implements IResolvable {
    private _ticketID: number;
    private _deskID: number;
    private _description: string;
    private _status: TicketStatus;
    private _technicianNote: string;
    public constructor(ticketID: number, deskID: number, description: string, status: TicketStatus = 'OPEN', technicianNote = '') {
        this._ticketID = ticketID;
        this._deskID = deskID;
        this._description = description;
        this._status = status;
        this._technicianNote = technicianNote;
    }
    public get ticketID(): number {
        return this._ticketID;
    }
    public set ticketID(value: number) {
        this._ticketID = value;
    }
    public get deskID(): number {
        return this._deskID;
    }
    public set deskID(value: number) {
        this._deskID = value;
    }
    public get description(): string {
        return this._description;
    }
    public set description(value: string) {
        this._description = value.trim();
    }
    public get status(): TicketStatus {
        return this._status;
    }
    public set status(value: TicketStatus) {
        this._status = value;
    }
    public get technicianNote(): string {
        return this._technicianNote;
    }
    public set technicianNote(value: string) {
        this._technicianNote = value.trim();
    }
    public resolve(note = 'Issue solved by technician.'): void {
        this._status = 'RESOLVED';
        this._technicianNote = note;
    }
}
