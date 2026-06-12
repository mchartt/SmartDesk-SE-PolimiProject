import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthStateService } from './auth-state.service';
import { Desk, Review, Space } from '../models';
import { ModelFactory } from '../utils/model-factory';
export interface HostSpaceOpeningHoursDayPayload {
    closed: boolean;
    open?: string;
    close?: string;
}
export interface HostSpaceUpsertPayload {
    name: string;
    address: string;
    city: string;
    description: string;
    openingHours: Record<string, HostSpaceOpeningHoursDayPayload>;
}
export interface HostSpaceClosureDto {
    id: number;
    spaceID: number;
    closedDate: string;
    reason: string;
}
export interface HostAmenityPresetDto {
    presetID?: number;
    spaceID?: number;
    label: string;
    hint?: string | null;
    amenities: string[];
}
export interface HostRoom {
    roomID: number;
    spaceID: number;
    name: string;
    code: string;
}
export interface HostTechnicianAssignedSpaceDto {
    spaceID: number;
    name: string;
    officeCode?: string | null;
}
export interface HostTechnicianDto {
    technicianID: number;
    technicianCode?: string | null;
    name: string;
    email: string;
    specialization: string;
    registeredAt?: string | null;
    assignedSpaces?: HostTechnicianAssignedSpaceDto[];
    profileVersion?: number | null;
}
@Injectable({ providedIn: 'root' })
export class HostService {
    private readonly http = inject(HttpClient);
    private readonly authState = inject(AuthStateService);
    private readonly hostsBase = `${environment.apiUrl}/hosts`;
    private readonly desksBase = `${environment.apiUrl}/desks`;
    private get hostID(): number {
        const user = this.authState.currentUserSnapshot();
        if (!user)
            throw new Error('Sessione host assente.');
        return user.id;
    }
    public getSpaces(): Observable<Space[]> {
        try {
            return this.http
                .get<Array<Record<string, unknown>>>(`${this.hostsBase}/${this.hostID}/spaces`)
                .pipe(map((rows) => rows.map((row) => ModelFactory.createSpace(row as never))))
                .pipe(catchError(() => throwError(() => new Error('Impossibile caricare gli spazi.'))));
        }
        catch (e) {
            return throwError(() => e);
        }
    }
    public createSpace(payload: HostSpaceUpsertPayload): Observable<Space> {
        return this.http.post<Space>(`${this.hostsBase}`, payload).pipe(catchError(() => throwError(() => new Error('Impossibile creare lo spazio.'))));
    }
    public updateSpace(spaceId: number, payload: HostSpaceUpsertPayload): Observable<Space> {
        return this.http
            .put<Space>(`${this.hostsBase}/spaces/${spaceId}`, payload)
            .pipe(catchError(() => throwError(() => new Error('Impossibile aggiornare lo spazio.'))));
    }
    public deleteSpace(spaceId: number): Observable<void> {
        return this.http.delete<void>(`${this.hostsBase}/spaces/${spaceId}`).pipe(catchError(() => throwError(() => new Error('Impossibile eliminare lo spazio.'))));
    }
    public getSpaceClosures(spaceId: number): Observable<HostSpaceClosureDto[]> {
        return this.http
            .get<HostSpaceClosureDto[]>(`${this.hostsBase}/spaces/${spaceId}/closures`)
            .pipe(catchError(() => throwError(() => new Error('Impossibile caricare le chiusure.'))));
    }
    public createSpaceClosures(spaceId: number, body: {
        dates: string[];
        reason: string;
    }): Observable<HostSpaceClosureDto[]> {
        return this.http
            .post<HostSpaceClosureDto[]>(`${this.hostsBase}/spaces/${spaceId}/closures`, body)
            .pipe(catchError(() => throwError(() => new Error('Impossibile salvare le chiusure.'))));
    }
    public deleteSpaceClosure(spaceId: number, closureId: number): Observable<void> {
        return this.http
            .delete<void>(`${this.hostsBase}/spaces/${spaceId}/closures/${closureId}`)
            .pipe(catchError(() => throwError(() => new Error('Impossibile rimuovere la chiusura.'))));
    }
    public getRooms(spaceId: number): Observable<HostRoom[]> {
        return this.http
            .get<HostRoom[]>(`${this.hostsBase}/spaces/${spaceId}/rooms`)
            .pipe(catchError(() => throwError(() => new Error('Impossibile caricare le stanze.'))));
    }
    public createRoom(spaceId: number, body: {
        name: string;
        code: string;
    }): Observable<HostRoom> {
        return this.http
            .post<HostRoom>(`${this.hostsBase}/spaces/${spaceId}/rooms`, body)
            .pipe(catchError(() => throwError(() => new Error('Impossibile creare la stanza.'))));
    }
    public updateRoom(spaceId: number, roomId: number, body: {
        name: string;
        code: string;
    }): Observable<HostRoom> {
        return this.http
            .put<HostRoom>(`${this.hostsBase}/spaces/${spaceId}/rooms/${roomId}`, body)
            .pipe(catchError(() => throwError(() => new Error('Impossibile aggiornare la stanza.'))));
    }
    public deleteRoom(spaceId: number, roomId: number): Observable<void> {
        return this.http
            .delete<void>(`${this.hostsBase}/spaces/${spaceId}/rooms/${roomId}`)
            .pipe(catchError(() => throwError(() => new Error('Impossibile eliminare la stanza.'))));
    }
    public listAmenityPresets(spaceId: number): Observable<HostAmenityPresetDto[]> {
        return this.http
            .get<HostAmenityPresetDto[]>(`${this.hostsBase}/spaces/${spaceId}/amenity-presets`)
            .pipe(catchError(() => throwError(() => new Error('Impossibile caricare i set di dotazioni.'))));
    }
    public createAmenityPreset(spaceId: number, body: Pick<HostAmenityPresetDto, 'label' | 'hint' | 'amenities'>): Observable<HostAmenityPresetDto> {
        const payload = {
            label: body.label,
            hint: body.hint ?? undefined,
            amenities: body.amenities
        };
        return this.http
            .post<HostAmenityPresetDto>(`${this.hostsBase}/spaces/${spaceId}/amenity-presets`, payload)
            .pipe(catchError(() => throwError(() => new Error('Impossibile creare il set di dotazioni.'))));
    }
    public updateAmenityPreset(spaceId: number, presetId: number, body: Pick<HostAmenityPresetDto, 'label' | 'hint' | 'amenities'>): Observable<HostAmenityPresetDto> {
        const payload = {
            label: body.label,
            hint: body.hint ?? undefined,
            amenities: body.amenities
        };
        return this.http
            .put<HostAmenityPresetDto>(`${this.hostsBase}/spaces/${spaceId}/amenity-presets/${presetId}`, payload)
            .pipe(catchError(() => throwError(() => new Error('Impossibile aggiornare il set di dotazioni.'))));
    }
    public deleteAmenityPreset(spaceId: number, presetId: number): Observable<void> {
        return this.http
            .delete<void>(`${this.hostsBase}/spaces/${spaceId}/amenity-presets/${presetId}`)
            .pipe(catchError(() => throwError(() => new Error('Impossibile eliminare il set di dotazioni.'))));
    }
    public getDesks(spaceId: number): Observable<Desk[]> {
        return this.http
            .get<Array<Record<string, unknown>>>(`${this.desksBase}?spaceId=${spaceId}`)
            .pipe(map((rows) => rows.map((row) => ModelFactory.createDesk(row as never))))
            .pipe(catchError(() => throwError(() => new Error('Impossibile caricare le postazioni.'))));
    }
    public createDesk(payload: {
        roomID: number;
        code?: string;
        amenities: string[];
        spaceID: number;
    }): Observable<Desk> {
        return this.http
            .post<Array<Record<string, unknown>> | Record<string, unknown>>(`${this.hostsBase}/desks`, payload)
            .pipe(map((row) => {
            const desk = Array.isArray(row) ? row[0] : row;
            return ModelFactory.createDesk(desk as never);
        }))
            .pipe(catchError(() => throwError(() => new Error('Impossibile creare la postazione.'))));
    }
    public updateDesk(deskId: number, payload: {
        roomID: number;
        code: string;
        amenities: string[];
        spaceID: number;
    }): Observable<Desk> {
        return this.http
            .put<Array<Record<string, unknown>> | Record<string, unknown>>(`${this.hostsBase}/desks/${deskId}`, payload)
            .pipe(map((row) => {
            const desk = Array.isArray(row) ? row[0] : row;
            return ModelFactory.createDesk(desk as never);
        }))
            .pipe(catchError(() => throwError(() => new Error('Impossibile aggiornare la postazione.'))));
    }
    public deleteDesk(deskId: number): Observable<void> {
        return this.http.delete<void>(`${this.hostsBase}/desks/${deskId}`).pipe(catchError(() => throwError(() => new Error('Impossibile eliminare la postazione.'))));
    }
    public approveInspection(deskId: number): Observable<Desk> {
        return this.http
            .patch<Record<string, unknown>>(`${this.hostsBase}/desks/${deskId}/inspect`, {})
            .pipe(map((row) => ModelFactory.createDesk(row as never)), catchError(() => throwError(() => new Error('Impossibile approvare l\'ispezione.'))));
    }
    public decommissionDesk(deskId: number): Observable<Desk> {
        return this.http
            .patch<Record<string, unknown>>(`${this.hostsBase}/desks/${deskId}/decommission`, {})
            .pipe(map((row) => ModelFactory.createDesk(row as never)), catchError(() => throwError(() => new Error('Impossibile dismettere la postazione.'))));
    }
    public rejectInspection(deskId: number): Observable<Desk> {
        return this.http
            .patch<Record<string, unknown>>(`${this.hostsBase}/desks/${deskId}/maintenance`, {})
            .pipe(map((row) => ModelFactory.createDesk(row as never)), catchError(() => throwError(() => new Error('Impossibile rimettere la postazione in manutenzione.'))));
    }
    public getDeskTickets(deskId: number): Observable<Array<Record<string, unknown>>> {
        return this.http
            .get<Array<Record<string, unknown>>>(`${this.hostsBase}/desks/${deskId}/tickets`)
            .pipe(catchError(() => throwError(() => new Error('Impossibile caricare le segnalazioni della postazione.'))));
    }
    public getResolvedTickets(limit = 80): Observable<Array<Record<string, unknown>>> {
        return this.http
            .get<Array<Record<string, unknown>>>(`${this.hostsBase}/resolved-tickets`, {
            params: { limit: String(limit) }
        })
            .pipe(catchError(() => throwError(() => new Error('Impossibile caricare lo storico delle segnalazioni risolte.'))));
    }
    public clearResolvedTickets(): Observable<{
        deleted: number;
    }> {
        return this.http.delete<{
            deleted: number;
        }>(`${this.hostsBase}/resolved-tickets`).pipe(catchError((err: unknown) => {
            if (err instanceof HttpErrorResponse) {
                const body = err.error as {
                    message?: string;
                } | null;
                const msg = typeof body?.message === 'string' ? body.message.trim() : '';
                if (msg)
                    return throwError(() => new Error(msg));
            }
            return throwError(() => new Error('Impossibile pulire lo storico delle segnalazioni risolte.'));
        }));
    }
    public getReviews(): Observable<Review[]> {
        return this.http
            .get<Array<Record<string, unknown>>>(`${this.hostsBase}/${this.hostID}/reviews`)
            .pipe(map((rows) => rows.map((row) => ModelFactory.createReview(row as never))))
            .pipe(catchError(() => throwError(() => new Error('Impossibile caricare le recensioni.'))));
    }
    public getSpaceReviews(spaceID: number): Observable<Review[]> {
        return this.http
            .get<Array<Record<string, unknown>>>(`${this.hostsBase}/spaces/${spaceID}/reviews`)
            .pipe(map((rows) => rows.map((row) => ModelFactory.createReview(row as never))))
            .pipe(catchError(() => throwError(() => new Error('Impossibile caricare le recensioni.'))));
    }
    public markReviewSeenByHost(spaceID: number, reviewID: number): Observable<Review> {
        return this.http
            .patch<Record<string, unknown>>(`${this.hostsBase}/spaces/${spaceID}/reviews/${reviewID}/seen`, {})
            .pipe(map((row) => ModelFactory.createReview(row as never)))
            .pipe(catchError(() => throwError(() => new Error('Impossibile aggiornare lo stato della recensione.'))));
    }
    public patchHostReviewNote(spaceID: number, reviewID: number, note: string): Observable<Review> {
        return this.http
            .patch<Record<string, unknown>>(`${this.hostsBase}/spaces/${spaceID}/reviews/${reviewID}/host-note`, { note })
            .pipe(map((row) => ModelFactory.createReview(row as never)))
            .pipe(catchError(() => throwError(() => new Error('Impossibile salvare la risposta.'))));
    }
    public deleteHostReviewNote(spaceID: number, reviewID: number): Observable<Review> {
        return this.http
            .delete<Record<string, unknown>>(`${this.hostsBase}/spaces/${spaceID}/reviews/${reviewID}/host-note`)
            .pipe(map((row) => ModelFactory.createReview(row as never)))
            .pipe(catchError(() => throwError(() => new Error('Impossibile eliminare la risposta.'))));
    }
    public getTechniciansForSpace(spaceId: number): Observable<HostTechnicianDto[]> {
        return this.http
            .get<HostTechnicianDto[]>(`${this.hostsBase}/spaces/${spaceId}/technicians`)
            .pipe(catchError(() => throwError(() => new Error('Impossibile caricare i tecnici.'))));
    }
    public getAllTechnicians(): Observable<HostTechnicianDto[]> {
        return this.http
            .get<HostTechnicianDto[]>(`${this.hostsBase}/technicians`)
            .pipe(catchError(() => throwError(() => new Error('Impossibile caricare i tecnici.'))));
    }
    public updateTechnician(technicianId: number, body: {
        name: string;
        email: string;
        specialization: string;
        password?: string;
        profileVersion: number;
    }): Observable<HostTechnicianDto> {
        return this.http.put<HostTechnicianDto>(`${this.hostsBase}/technicians/${technicianId}`, body).pipe(catchError((err: unknown) => {
            if (err instanceof HttpErrorResponse && err.status === 409) {
                const payload = err.error as {
                    message?: string;
                } | null;
                const apiMsg = typeof payload?.message === 'string' ? payload.message : '';
                return throwError(() => new Error(apiMsg || 'I dati sono stati aggiornati altrove. Ricarica la pagina e riprova.'));
            }
            return throwError(() => new Error('Impossibile aggiornare il tecnico.'));
        }));
    }
    public deleteTechnician(technicianId: number): Observable<void> {
        return this.http
            .delete<void>(`${this.hostsBase}/technicians/${technicianId}`)
            .pipe(catchError(() => throwError(() => new Error('Impossibile eliminare il tecnico.'))));
    }
    public assignTechnician(spaceId: number, technicianId: number): Observable<void> {
        return this.http
            .post<void>(`${this.hostsBase}/spaces/${spaceId}/technicians/${technicianId}`, {})
            .pipe(catchError((err: unknown) => {
            if (err instanceof HttpErrorResponse) {
                const body = err.error as {
                    message?: string;
                } | null;
                const msg = typeof body?.message === 'string' ? body.message.trim() : '';
                if (msg)
                    return throwError(() => new Error(msg));
            }
            return throwError(() => new Error('Impossibile assegnare il tecnico allo spazio.'));
        }));
    }
    public assignTechnicianToTicket(ticketId: number, technicianId: number, severity?: string): Observable<Record<string, unknown>> {
        return this.http
            .post<Record<string, unknown>>(`${this.hostsBase}/tickets/${ticketId}/technicians/${technicianId}`, { severity })
            .pipe(catchError((err: unknown) => {
            if (err instanceof HttpErrorResponse) {
                const body = err.error as {
                    message?: string;
                } | null;
                const msg = typeof body?.message === 'string' ? body.message.trim() : '';
                if (msg)
                    return throwError(() => new Error(msg));
            }
            return throwError(() => new Error('Impossibile assegnare il tecnico alla segnalazione.'));
        }));
    }
    public approveTicket(ticketId: number): Observable<Record<string, unknown>> {
        return this.http
            .post<Record<string, unknown>>(`${this.hostsBase}/tickets/${ticketId}/approve`, {})
            .pipe(catchError(() => throwError(() => new Error('Impossibile approvare la segnalazione.'))));
    }
    public rejectTicket(ticketId: number, body: {
        newTechnicianId?: number;
        reason?: string;
    }): Observable<Record<string, unknown>> {
        return this.http
            .post<Record<string, unknown>>(`${this.hostsBase}/tickets/${ticketId}/reject`, body)
            .pipe(catchError(() => throwError(() => new Error('Impossibile respingere la segnalazione.'))));
    }
    public addHostComment(ticketId: number, comment: string): Observable<Record<string, unknown>> {
        return this.http
            .post<Record<string, unknown>>(`${this.hostsBase}/tickets/${ticketId}/comments`, { body: comment })
            .pipe(catchError(() => throwError(() => new Error('Impossibile aggiungere il commento.'))));
    }
    public dismissDeskTicket(ticketId: number): Observable<Record<string, unknown>> {
        return this.http
            .post<Record<string, unknown>>(`${this.hostsBase}/tickets/${ticketId}/dismiss-desk`, {})
            .pipe(catchError(() => throwError(() => new Error('Impossibile dismettere la postazione.'))));
    }
    public getHostBookings(): Observable<Record<string, unknown>[]> {
        return this.http
            .get<Record<string, unknown>[]>(`${this.hostsBase}/bookings`)
            .pipe(catchError(() => throwError(() => new Error('Impossibile caricare le prenotazioni.'))));
    }
    public unassignTechnician(spaceId: number, technicianId: number): Observable<void> {
        return this.http
            .delete<void>(`${this.hostsBase}/spaces/${spaceId}/technicians/${technicianId}`)
            .pipe(catchError(() => throwError(() => new Error('Impossibile rimuovere l\'assegnazione del tecnico.'))));
    }
    public createTechnician(payload: {
        name: string;
        email: string;
        password: string;
        specialization: string;
    }): Observable<HostTechnicianDto> {
        return this.http.post<HostTechnicianDto>(`${this.hostsBase}/technicians`, payload).pipe(catchError((err: unknown) => {
            if (err instanceof HttpErrorResponse && err.status === 400) {
                const body = err.error as {
                    code?: string;
                    message?: string;
                } | null;
                if (body?.code === 'VALIDATION_ERROR') {
                    return throwError(() => err);
                }
                const msg = typeof body?.message === 'string' ? body.message : 'Richiesta non valida.';
                return throwError(() => new Error(msg));
            }
            return throwError(() => new Error('Impossibile creare il tecnico.'));
        }));
    }
}
