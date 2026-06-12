import { AbstractUser } from './abstract-user.model';
import { UserRole } from './user-role.type';
export class Host extends AbstractUser {
    private _nameStructure: string;
    private _description: string;
    public constructor(id: number, email: string, surname: string, name: string, nameStructure: string, description: string, active = true) {
        super(id, email, surname, name, active);
        this._nameStructure = nameStructure;
        this._description = description;
    }
    public get nameStructure(): string {
        return this._nameStructure;
    }
    public set nameStructure(value: string) {
        this._nameStructure = value.trim();
    }
    public get description(): string {
        return this._description;
    }
    public set description(value: string) {
        this._description = value.trim();
    }
    public override getRole(): UserRole {
        return 'HOST';
    }
    public override getDashboardTitle(): string {
        const name = (this._nameStructure ?? '').trim();
        if (!name) {
            return 'Gestisci postazioni e recensioni.';
        }
        return `Gestisci postazioni e recensioni per ${name}.`;
    }
    public override canOpenTicket(): boolean {
        return false;
    }
}
