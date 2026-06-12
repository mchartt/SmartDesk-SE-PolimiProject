import { AbstractUser } from './abstract-user.model';
import { UserRole } from './user-role.type';
export class SysAdmin extends AbstractUser {
    public constructor(id: number, email: string, surname: string, name: string, active = true) {
        super(id, email, surname, name, active);
    }
    public override getRole(): UserRole {
        return 'SYS_ADMIN';
    }
    public override getDashboardTitle(): string {
        return 'Supervisione utenti, moderazione e log di sistema.';
    }
    public override canOpenTicket(): boolean {
        return false;
    }
}
