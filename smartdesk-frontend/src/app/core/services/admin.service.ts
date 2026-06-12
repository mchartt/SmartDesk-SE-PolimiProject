import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Booking, SystemLog } from '../models';
import { ModelFactory } from '../utils/model-factory';
@Injectable({ providedIn: 'root' })
export class AdminService {
    private readonly http = inject(HttpClient);
    private readonly base = `${environment.apiUrl}/admin`;
    public getUsers(): Observable<Array<Record<string, unknown>>> {
        return this.http.get<Array<Record<string, unknown>>>(`${this.base}/users`).pipe(catchError(() => throwError(() => new Error('Impossibile caricare gli utenti.'))));
    }
    public moderateUser(userId: number, action: 'BAN' | 'REACTIVATE'): Observable<void> {
        return this.http
            .patch<void>(`${this.base}/users/${userId}?action=${encodeURIComponent(action)}`, {})
            .pipe(catchError(() => throwError(() => new Error('Impossibile moderare l\'utente.'))));
    }
    public getHosts(): Observable<Array<Record<string, unknown>>> {
        return this.http.get<Array<Record<string, unknown>>>(`${this.base}/hosts/pending`).pipe(catchError(() => throwError(() => new Error('Impossibile caricare gli host in attesa.'))));
    }
    public approveHost(hostId: number, approved: boolean): Observable<void> {
        const action = approved ? 'approve' : 'reject';
        return this.http
            .patch<void>(`${this.base}/hosts/${hostId}/${action}`, {})
            .pipe(catchError(() => throwError(() => new Error('Impossibile elaborare l\'approvazione dell\'host.'))));
    }
    public getSpaces(): Observable<Array<Record<string, unknown>>> {
        return this.http.get<Array<Record<string, unknown>>>(`${this.base}/spaces`).pipe(catchError(() => throwError(() => new Error('Impossibile caricare gli spazi.'))));
    }
    public getApprovedSpacesEnriched(): Observable<Array<Record<string, unknown>>> {
        return this.http.get<Array<Record<string, unknown>>>(`${this.base}/spaces/approved`).pipe(catchError(() => throwError(() => new Error('Impossibile caricare gli spazi approvati.'))));
    }
    public getPendingSpaces(): Observable<Array<Record<string, unknown>>> {
        return this.http.get<Array<Record<string, unknown>>>(`${this.base}/spaces/pending`).pipe(catchError(() => throwError(() => new Error('Impossibile caricare gli spazi in attesa.'))));
    }
    public approveSpace(spaceId: number, action: 'APPROVE' | 'REJECT' | 'FORCE_CLOSE'): Observable<void> {
        if (action === 'FORCE_CLOSE') {
            return this.http
                .delete<void>(`${this.base}/spaces/${spaceId}`)
                .pipe(catchError(() => throwError(() => new Error('Impossibile chiudere forzatamente lo spazio.'))));
        }
        const segment = action === 'APPROVE' ? 'approve' : 'reject';
        return this.http
            .patch<void>(`${this.base}/spaces/${spaceId}/${segment}`, {})
            .pipe(catchError(() => throwError(() => new Error('Impossibile elaborare la moderazione dello spazio.'))));
    }
    public getLogs(): Observable<SystemLog[]> {
        return this.http.get<SystemLog[]>(`${this.base}/logs`).pipe(catchError(() => throwError(() => new Error('Impossibile caricare i log di sistema.'))));
    }
    public getBookings(): Observable<Booking[]> {
        return this.http.get<Record<string, unknown>[]>(`${this.base}/bookings`).pipe(map((rows) => rows.map((row) => ModelFactory.createBooking({
            bookingID: Number(row['bookingID']),
            deskID: Number(row['deskID']),
            startTime: String(row['startTime'] ?? ''),
            endTime: String(row['endTime'] ?? ''),
            status: row['status'] != null ? String(row['status']) : undefined,
            bookedDay: row['bookedDay'] != null ? String(row['bookedDay']) : null,
            deskCode: row['deskCode'] != null ? String(row['deskCode']) : undefined,
            spaceName: row['spaceName'] != null ? String(row['spaceName']) : undefined,
            buildingName: row['buildingName'] != null ? String(row['buildingName']) : undefined,
            city: row['city'] != null ? String(row['city']) : undefined,
            bookingCode: row['bookingCode'] != null ? String(row['bookingCode']) : undefined,
            workerID: row['workerID'] != null ? Number(row['workerID']) : undefined,
            workerEmail: row['workerEmail'] != null ? String(row['workerEmail']) : undefined,
            workerName: row['workerName'] != null ? String(row['workerName']) : undefined
        }))), catchError(() => throwError(() => new Error('Impossibile caricare le prenotazioni.'))));
    }
    public cancelBooking(bookingId: number): Observable<void> {
        return this.http.delete<void>(`${this.base}/bookings/${bookingId}`).pipe(catchError(() => throwError(() => new Error('Impossibile annullare la prenotazione.'))));
    }
}
