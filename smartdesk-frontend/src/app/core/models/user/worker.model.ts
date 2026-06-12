import { AbstractUser } from './abstract-user.model';
import { UserRole } from './user-role.type';
export class Worker extends AbstractUser {
    private _description: string;
    public constructor(id: number, email: string, surname: string, name: string, description: string, active = true) {
        super(id, email, surname, name, active);
        this._description = description;
    }
    public get description(): string {
        return this._description;
    }
    public set description(value: string) {
        this._description = value.trim();
    }
    public override getRole(): UserRole {
        return 'WORKER';
    }
    public override getDashboardTitle(): string {
        const who = this.displayName.trim() || this.email;
        return `Benvenuto ${who}, hai intenzione di prenotare oggi?`;
    }
    public override canOpenTicket(): boolean {
        return this.active;
    }
}
