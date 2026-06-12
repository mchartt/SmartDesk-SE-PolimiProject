import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AbstractUser } from '../models';
import { ModelFactory } from '../utils/model-factory';
import { AuthStateService } from './auth-state.service';
import { NotificationService } from './notification.service';
type AuthResponseDto = {
    accessToken: string;
    refreshToken?: string;
    tokenType?: string;
    expiresIn?: string | null;
    userID: number;
    role: string;
};
type UserProfileDto = {
    userID: number;
    name: string;
    surname?: string;
    email: string;
    role: string;
    status?: string;
    approved?: boolean;
    description?: string;
    nameStructure?: string;
    registeredAt?: string | null;
};
type RegisterRole = 'WORKER' | 'HOST' | 'TECHNICIAN' | 'SYS_ADMIN';
@Injectable({ providedIn: 'root' })
export class AuthService {
    private readonly http = inject(HttpClient);
    private readonly authState = inject(AuthStateService);
    private readonly notifications = inject(NotificationService);
    private readonly baseUrl = `${environment.apiUrl}/auth`;
    public login(email: string, password: string): Observable<AbstractUser> {
        return this.http.post<AuthResponseDto>(`${this.baseUrl}/login`, { email, password }).pipe(map((response) => {
            const normalizedRole = this.normalizeRole(response.role);
            const placeholder = ModelFactory.createUser({
                id: response.userID,
                email,
                password: '',
                surname: '',
                name: email.split('@')[0],
                roleType: normalizedRole
            });
            this.authState.setSession(placeholder, response.accessToken, response.refreshToken ?? null);
            this.notifications.connectRealtimeStream();
            return placeholder;
        }), tap(() => {
            this.fetchCurrentProfile().subscribe();
        }), catchError((error: HttpErrorResponse) => {
            let message = 'Accesso non riuscito. Controlla email e password.';
            if (error.status === 401)
                message = 'Email o password non validi.';
            else if (error.status === 403)
                message = 'Account bloccato: contatta un amministratore.';
            return throwError(() => new Error(message));
        }));
    }
    public fetchCurrentProfile(): Observable<AbstractUser | null> {
        return this.http.get<UserProfileDto>(`${this.baseUrl}/me`).pipe(map((profile) => this.profileToUser(profile)), tap((user) => {
            if (user) {
                this.authState.updateUser(user);
            }
        }), catchError(() => of(null)));
    }
    public logout(): Observable<void> {
        const user = this.authState.currentUserSnapshot();
        const token = this.authState.token();
        this.notifications.disconnectRealtimeStream();
        this.authState.clearSession();
        if (user?.id && token) {
            this.http
                .delete<void>(`${this.baseUrl}/logout/${user.id}`, {
                headers: { Authorization: `Bearer ${token}` }
            })
                .subscribe({ error: () => undefined });
        }
        return of(void 0);
    }
    public registerUser(payload: {
        email: string;
        password: string;
        name: string;
        surname: string;
        description: string;
        role: RegisterRole;
    }): Observable<AbstractUser> {
        return this.http.post<AuthResponseDto>(`${this.baseUrl}/register`, payload).pipe(map((response) => ModelFactory.createUser({
            id: response.userID,
            email: payload.email,
            surname: payload.surname,
            name: payload.name,
            roleType: this.normalizeRole(response.role),
            password: '',
            active: true,
            description: payload.description
        })), catchError((error: HttpErrorResponse) => {
            let message = 'Registrazione non riuscita.';
            if (error.status === 409)
                message = 'Email già registrata.';
            else if (error.error?.message)
                message = error.error.message;
            return throwError(() => new Error(message));
        }));
    }
    public registerWorker(payload: {
        email: string;
        password: string;
        name: string;
        surname: string;
        description: string;
    }): Observable<AbstractUser> {
        return this.registerUser({ ...payload, role: 'WORKER' });
    }
    public registerHost(payload: {
        email: string;
        password: string;
        name: string;
        surname: string;
        vatNumber: string;
        nameStructure: string;
        description: string;
    }): Observable<AbstractUser> {
        return this.http.post<AuthResponseDto>(`${this.baseUrl}/register/host`, payload).pipe(map((response) => ModelFactory.createUser({
            id: response.userID,
            email: payload.email,
            surname: payload.surname,
            name: payload.name,
            roleType: this.normalizeRole(response.role),
            password: '',
            active: true,
            description: payload.description,
            nameStructure: payload.nameStructure
        })), catchError((error: HttpErrorResponse) => {
            let message = 'Registrazione host non riuscita.';
            if (error.status === 409)
                message = 'Email già registrata.';
            else if (error.error?.message)
                message = error.error.message;
            return throwError(() => new Error(message));
        }));
    }
    public getUserProfile(userId: number): Observable<AbstractUser> {
        return this.http.get<UserProfileDto>(`${this.baseUrl}/users/${userId}/profile`).pipe(map((profile) => this.profileToUser(profile)), catchError(() => throwError(() => new Error('Impossibile caricare il profilo utente.'))));
    }
    private profileToUser(profile: UserProfileDto): AbstractUser {
        const role = this.normalizeRole(profile.role);
        return ModelFactory.createUser({
            id: profile.userID,
            email: profile.email,
            password: '',
            surname: profile.surname ?? '',
            name: profile.name,
            roleType: role,
            active: profile.status ? profile.status.toUpperCase() === 'ACTIVE' : true,
            description: profile.description ?? '',
            nameStructure: profile.nameStructure ?? ''
        });
    }
    private normalizeRole(role: string): RegisterRole {
        const normalized = role.trim().toUpperCase();
        if (normalized === 'ADMIN')
            return 'SYS_ADMIN';
        if (normalized === 'WORKER' || normalized === 'HOST' || normalized === 'TECHNICIAN' || normalized === 'SYS_ADMIN') {
            return normalized;
        }
        return 'WORKER';
    }
}
