import { AbstractUser } from './abstract-user.model';
export class UserSession {
    public buildWelcomeMessage(user: AbstractUser): string {
        return `${user.getRole()} | ${user.getDashboardTitle()}`;
    }
}
