import { AbstractUser } from './abstract-user.model';
import { UserRole } from './user-role.type';
export class Technician extends AbstractUser {
    private _specialisation: string;
    public constructor(id: number, email: string, surname: string, name: string, specialisation: string, active = true) {
        super(id, email, surname, name, active);
        this._specialisation = specialisation;
    }
    public get specialisation(): string {
        return this._specialisation;
    }
    public set specialisation(value: string) {
        this._specialisation = value.trim();
    }
    public override getRole(): UserRole {
        return 'TECHNICIAN';
    }
    public override getDashboardTitle(): string {
        return `Manutenzione nel tuo ambito: ${this._specialisation}.`;
    }
    public override canOpenTicket(): boolean {
        return false;
    }
}
