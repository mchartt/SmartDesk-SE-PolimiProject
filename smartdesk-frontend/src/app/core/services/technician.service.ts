import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Desk } from '../models';
import { ModelFactory } from '../utils/model-factory';
import type { TicketNoteMessageDto } from '../models';
export interface TechnicianAssignedSpaceRow {
    spaceID: number;
    name: string;
    officeCode?: string | null;
}
export interface TechnicianTicketRow {
    ticketID: number;
    ticketCode?: string | null;
    title?: string | null;
    deskID?: number | null;
    deskCode?: string | null;
    spaceID?: number | null;
    spaceName?: string | null;
    officeCode?: string | null;
    description?: string | null;
    status: string;
    severity?: string | null;
    technicianNote?: string | null;
    technicianNoteHistory?: TicketNoteMessageDto[] | null;
    workerNoteHistory?: TicketNoteMessageDto[] | null;
    resolution?: string | null;
    createdAt?: string | null;
    resolvedAt?: string | null;
    workerID?: number | null;
    workerName?: string | null;
    workerSurname?: string | null;
    workerEmail?: string | null;
    estimatedResolutionAt?: string | null;
}
@Injectable({ providedIn: 'root' })
export class TechnicianService {
    private readonly http = inject(HttpClient);
    private readonly base = `${environment.apiUrl}/technicians`;
    private static mapHttpError(err: unknown, fallback: string): Error {
        if (err instanceof HttpErrorResponse) {
            const body = err.error;
            if (typeof body === 'string' && body.trim()) {
                return new Error(body);
            }
            if (body && typeof body === 'object') {
                const rec = body as Record<string, unknown>;
                const m = rec['message'];
                if (typeof m === 'string' && m.trim()) {
                    return new Error(m);
                }
            }
        }
        return new Error(fallback);
    }
    private static mapTicketRow(row: TechnicianTicketRow): TechnicianTicketRow {
        return {
            ...row,
            technicianNoteHistory: row.technicianNoteHistory ?? [],
            workerNoteHistory: row.workerNoteHistory ?? []
        };
    }
    public getDesks(spaceId: number): Observable<Desk[]> {
        return this.http
            .get<Array<Record<string, unknown>>>(`${this.base}/spaces/${spaceId}/desks`)
            .pipe(map((rows) => rows.map((row) => ModelFactory.createDesk(row as never))))
            .pipe(catchError(() => throwError(() => new Error('Impossibile caricare le postazioni assegnate.'))));
    }
    public getAssignedSpaces(): Observable<TechnicianAssignedSpaceRow[]> {
        return this.http
            .get<TechnicianAssignedSpaceRow[]>(`${this.base}/spaces`)
            .pipe(catchError(() => throwError(() => new Error('Impossibile caricare gli uffici assegnati.'))));
    }
    public getPendingTickets(): Observable<TechnicianTicketRow[]> {
        return this.http
            .get<TechnicianTicketRow[]>(`${this.base}/tickets/pending`)
            .pipe(map((rows) => rows.map((row) => TechnicianService.mapTicketRow(row))), catchError(() => throwError(() => new Error('Impossibile caricare i ticket in attesa.'))));
    }
    public getAssignedTickets(): Observable<TechnicianTicketRow[]> {
        return this.http
            .get<TechnicianTicketRow[]>(`${this.base}/tickets/assigned`)
            .pipe(map((rows) => rows.map((row) => TechnicianService.mapTicketRow(row))), catchError(() => throwError(() => new Error('Impossibile caricare i ticket assegnati.'))));
    }
    public getTicketById(ticketId: number): Observable<TechnicianTicketRow> {
        return this.http
            .get<TechnicianTicketRow>(`${this.base}/tickets/${ticketId}`)
            .pipe(map((row) => TechnicianService.mapTicketRow(row)), catchError(() => throwError(() => new Error('Impossibile caricare il dettaglio del ticket.'))));
    }
    public addComment(ticketId: number, body: string): Observable<TechnicianTicketRow> {
        return this.http
            .post<TechnicianTicketRow>(`${this.base}/tickets/${ticketId}/comments`, { body })
            .pipe(map((row) => TechnicianService.mapTicketRow(row)), catchError((err) => throwError(() => TechnicianService.mapHttpError(err, 'Impossibile inviare il commento.'))));
    }
    public updateStatus(ticketId: number, status: 'IN_PROGRESS' | 'VERIFYING' | 'RESOLVED', note = '', resolution?: string, severity?: string, estimatedResolutionAt?: string): Observable<TechnicianTicketRow> {
        const body: {
            status: string;
            note?: string;
            severity?: string;
            resolution?: string;
            estimatedResolutionAt?: string;
        } = { status, note };
        if (severity) {
            body.severity = severity;
        }
        if (resolution !== undefined) {
            body.resolution = resolution;
        }
        if (estimatedResolutionAt) {
            body.estimatedResolutionAt = estimatedResolutionAt;
        }
        return this.http
            .patch<TechnicianTicketRow>(`${this.base}/tickets/${ticketId}`, body)
            .pipe(map((row) => TechnicianService.mapTicketRow(row)), catchError(() => throwError(() => new Error('Impossibile aggiornare lo stato del ticket.'))));
    }
    public setMaintenance(deskId: number): Observable<void> {
        return this.http
            .patch<void>(`${this.base}/desks/${deskId}/maintenance`, {})
            .pipe(catchError(() => throwError(() => new Error('Impossibile impostare la postazione in manutenzione.'))));
    }
    public revertMaintenance(deskId: number): Observable<void> {
        return this.http
            .delete<void>(`${this.base}/desks/${deskId}/maintenance`)
            .pipe(catchError(() => throwError(() => new Error('Impossibile ripristinare la postazione.'))));
    }
    public clearResolvedHistory(): Observable<{
        deleted: number;
    }> {
        return this.http.delete<{
            deleted: number;
        }>(`${this.base}/tickets/resolved-history`).pipe(catchError((err) => throwError(() => TechnicianService.mapHttpError(err, 'Impossibile svuotare lo storico risolti.'))));
    }
}
