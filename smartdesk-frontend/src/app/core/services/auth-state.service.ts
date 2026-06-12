import { Injectable, computed, signal } from '@angular/core';
import { AbstractUser } from '../models';
import { ModelFactory } from '../utils/model-factory';
const REFRESH_STORAGE_KEY = 'smartdesk.refresh';
@Injectable({ providedIn: 'root' })
export class AuthStateService {
    private readonly userSignal = signal<AbstractUser | null>(this.readStoredUser());
    private readonly tokenSignal = signal<string | null>(localStorage.getItem('smartdesk.jwt'));
    private readonly refreshTokenSignal = signal<string | null>(localStorage.getItem(REFRESH_STORAGE_KEY));
    public readonly user = this.userSignal.asReadonly();
    public readonly isAuthenticated = computed(() => !!this.tokenSignal());
    public setSession(user: AbstractUser, token: string, refreshToken?: string | null): void {
        this.userSignal.set(user);
        this.tokenSignal.set(token);
        localStorage.setItem('smartdesk.jwt', token);
        if (refreshToken !== undefined) {
            if (refreshToken) {
                this.refreshTokenSignal.set(refreshToken);
                localStorage.setItem(REFRESH_STORAGE_KEY, refreshToken);
            }
            else {
                this.refreshTokenSignal.set(null);
                localStorage.removeItem(REFRESH_STORAGE_KEY);
            }
        }
        localStorage.setItem('smartdesk.user', JSON.stringify(this.serializeUser(user)));
    }
    public updateTokens(accessToken: string, newRefreshToken?: string): void {
        this.tokenSignal.set(accessToken);
        localStorage.setItem('smartdesk.jwt', accessToken);
        if (newRefreshToken !== undefined && newRefreshToken !== '') {
            this.refreshTokenSignal.set(newRefreshToken);
            localStorage.setItem(REFRESH_STORAGE_KEY, newRefreshToken);
        }
    }
    public refreshToken(): string | null {
        return this.refreshTokenSignal();
    }
    public updateUser(user: AbstractUser): void {
        this.userSignal.set(user);
        localStorage.setItem('smartdesk.user', JSON.stringify(this.serializeUser(user)));
    }
    public clearSession(): void {
        this.userSignal.set(null);
        this.tokenSignal.set(null);
        this.refreshTokenSignal.set(null);
        localStorage.removeItem('smartdesk.jwt');
        localStorage.removeItem(REFRESH_STORAGE_KEY);
        localStorage.removeItem('smartdesk.user');
    }
    public token(): string | null {
        return this.tokenSignal();
    }
    public currentUserSnapshot(): AbstractUser | null {
        return this.userSignal();
    }
    private readStoredUser(): AbstractUser | null {
        const raw = localStorage.getItem('smartdesk.user');
        if (!raw)
            return null;
        try {
            return ModelFactory.createUser(JSON.parse(raw));
        }
        catch {
            return null;
        }
    }
    private serializeUser(user: AbstractUser): Record<string, unknown> {
        const role = user.getRole();
        const base: Record<string, unknown> = {
            id: user.id,
            email: user.email,
            surname: user.surname,
            name: user.name,
            roleType: role,
            active: user.active
        };
        const roleUser = user as unknown as {
            description?: string;
            nameStructure?: string;
            specialisation?: string;
        };
        if (role === 'HOST') {
            base['nameStructure'] = roleUser.nameStructure ?? '';
            base['description'] = roleUser.description ?? '';
        }
        else if (role === 'TECHNICIAN') {
            base['specialisation'] = roleUser.specialisation ?? '';
        }
        else if (role === 'WORKER') {
            base['description'] = roleUser.description ?? '';
        }
        return base;
    }
}
