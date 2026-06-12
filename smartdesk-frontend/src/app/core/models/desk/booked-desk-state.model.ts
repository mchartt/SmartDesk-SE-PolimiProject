import { DeskStateCode } from './desk-state-code.type';
import { IDeskState } from './i-desk-state.model';
export class BookedDeskState implements IDeskState {
    public readonly code: DeskStateCode = 'BOOKED';
    public getName(): string {
        return 'Prenotata';
    }
    public canBeBooked(): boolean {
        return false;
    }
    public getBadgeClass(): string {
        return 'state-booked';
    }
}
