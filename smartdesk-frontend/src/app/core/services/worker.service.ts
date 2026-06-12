import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
export type UserProfileDto = {
    userID: number;
    name: string;
    surname?: string;
    email: string;
    role: string;
    description?: string;
    nameStructure?: string;
    status?: string;
    approved?: boolean;
    registeredAt?: string | null;
};
@Injectable({ providedIn: 'root' })
export class WorkerService {
    private readonly http = inject(HttpClient);
    private readonly base = `${environment.apiUrl}/profile`;
    public getProfile(): Observable<UserProfileDto> {
        return this.http.get<UserProfileDto>(this.base).pipe(catchError(() => throwError(() => new Error('Impossibile caricare il profilo.'))));
    }
    public updateProfile(payload: {
        name: string;
        surname: string;
        email: string;
    }): Observable<UserProfileDto> {
        return this.http.put<UserProfileDto>(this.base, payload).pipe(catchError(() => throwError(() => new Error('Impossibile aggiornare il profilo.'))));
    }
    public changePassword(currentPassword: string, newPassword: string): Observable<void> {
        return this.http
            .put<void>(`${this.base}/password`, { currentPassword, newPassword })
            .pipe(catchError(() => throwError(() => new Error('Impossibile cambiare la password.'))));
    }
    public deleteAccount(): Observable<void> {
        return this.http.delete<void>(this.base).pipe(catchError(() => throwError(() => new Error('Impossibile eliminare l\'account.'))));
    }
}
