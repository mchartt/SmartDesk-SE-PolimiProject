import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TicketNoteMessage, TicketResponse, TicketDto } from '../models';
export type ReportTicketPayload = {
    title: string;
    description: string;
    bookingID: number;
} | {
    title: string;
    description: string;
    deskCode: string;
};
@Injectable({ providedIn: 'root' })
export class TicketService {
    private readonly http = inject(HttpClient);
    private static mapHttpError(err: unknown, fallback: string): Error {
        if (err instanceof HttpErrorResponse) {
            const body = err.error;
            if (typeof body === 'string' && body.trim()) {
                return new Error(body);
            }
            if (body && typeof body === 'object') {
                const rec = body as Record<string, unknown>;
                const fieldErrors = rec['fieldErrors'];
                if (Array.isArray(fieldErrors) && fieldErrors.length > 0) {
                    const msgs = fieldErrors
                        .map((row) => {
                        if (row && typeof row === 'object' && 'message' in row) {
                            const m = (row as {
                                message?: unknown;
                            }).message;
                            return typeof m === 'string' ? m.trim() : '';
                        }
                        return '';
                    })
                        .filter(Boolean);
                    if (msgs.length) {
                        return new Error(msgs.join(' '));
                    }
                }
                const m = rec['message'];
                if (typeof m === 'string' && m.trim()) {
                    return new Error(m);
                }
            }
        }
        return new Error(fallback);
    }
    public reportIssue(payload: ReportTicketPayload): Observable<void> {
        return this.http.post<void>(`${environment.apiUrl}/workers/tickets`, payload).pipe(catchError((err) => throwError(() => TicketService.mapHttpError(err, 'Impossibile creare la segnalazione.'))));
    }
    public getMyTickets(): Observable<TicketResponse[]> {
        return this.http.get<TicketDto[]>(`${environment.apiUrl}/workers/tickets`).pipe(map((rows) => rows.map((row) => TicketService.mapRow(row))), catchError(() => throwError(() => new Error('Impossibile caricare le tue segnalazioni.'))));
    }
    public getTicketById(ticketId: number): Observable<TicketResponse> {
        return this.http.get<TicketDto>(`${environment.apiUrl}/workers/tickets/${ticketId}`).pipe(map((row) => TicketService.mapRow(row)), catchError(() => throwError(() => new Error('Impossibile caricare il dettaglio della segnalazione.'))));
    }
    public deleteTicket(ticketId: number): Observable<void> {
        return this.http
            .delete<void>(`${environment.apiUrl}/workers/tickets/${ticketId}`)
            .pipe(catchError(() => throwError(() => new Error('Impossibile eliminare la segnalazione.'))));
    }
    public addComment(ticketId: number, body: string): Observable<TicketResponse> {
        return this.http
            .post<TicketDto>(`${environment.apiUrl}/workers/tickets/${ticketId}/comments`, { body })
            .pipe(map((row) => TicketService.mapRow(row)), catchError((err) => throwError(() => TicketService.mapHttpError(err, 'Impossibile inviare il commento.'))));
    }
    private static mapNoteHistory(rows: TicketDto['technicianNoteHistory'], defaultLabel = 'Tecnico'): TicketNoteMessage[] {
        if (!rows?.length) {
            return [];
        }
        return rows
            .map((r) => {
            const body = (r.body ?? '').trim();
            if (!body) {
                return null;
            }
            return new TicketNoteMessage(body, r.createdAt ?? null, (r.authorLabel ?? defaultLabel).trim() || defaultLabel);
        })
            .filter((m): m is TicketNoteMessage => m != null);
    }
    private static mapRow(row: TicketDto): TicketResponse {
        return new TicketResponse(row.ticketID, row.ticketCode ?? null, row.title ?? null, row.deskCode ?? null, row.deskID ?? null, row.description ?? null, row.technicianNote ?? null, TicketService.mapNoteHistory(row.technicianNoteHistory, 'Tecnico'), TicketService.mapNoteHistory(row.workerNoteHistory, 'Tu'), TicketService.mapNoteHistory(row.hostNoteHistory, 'Host'), row.status, row.assignedTechID === null || row.assignedTechID === undefined ? null : row.assignedTechID, row.resolution === null || row.resolution === undefined ? null : row.resolution, row.createdAt ?? '', row.severity ?? null, row.resolvedAt ?? null, row.estimatedResolutionAt ?? null);
    }
}
