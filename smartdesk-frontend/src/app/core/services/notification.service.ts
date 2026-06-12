import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, Subject, catchError, map, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { NotificationModel } from '../models';
import { consumeNotificationSseBuffer, NotificationCreatedSsePayload } from '../utils/notification-sse.util';
import { AuthStateService } from './auth-state.service';
type NotificationApiRow = {
    notificationID?: number;
    id?: number;
    message?: string;
    title?: string;
    read?: boolean;
    createdAt?: string | null;
    kind?: string | null;
    actorName?: string | null;
    actorSurname?: string | null;
    actorEmail?: string | null;
    actorRating?: number | null;
};
@Injectable({ providedIn: 'root' })
export class NotificationService {
    private readonly http = inject(HttpClient);
    private readonly auth = inject(AuthStateService);
    private readonly baseUrl = `${environment.apiUrl}/profile/notifications`;
    public readonly refresh$ = new Subject<void>();
    public readonly notificationCreated$ = new Subject<NotificationModel>();
    public readonly notificationUpdated$ = new Subject<{
        id: number;
        read: boolean;
    }>();
    public readonly allNotificationsMarkedRead$ = new Subject<void>();
    private readonly unreadCountState = signal(0);
    private streamAbort: AbortController | null = null;
    private streamLoopActive = false;
    public readonly unreadCount = this.unreadCountState.asReadonly();
    public requestRefresh(): void {
        this.refresh$.next();
    }
    public resetLocalCount(): void {
        this.unreadCountState.set(0);
        this.refresh$.next();
    }
    public setUnreadCount(count: number): void {
        const safe = Number.isFinite(count) ? Math.max(0, Math.floor(count)) : 0;
        this.unreadCountState.set(safe);
    }
    public connectRealtimeStream(): void {
        if (this.streamLoopActive || !this.auth.isAuthenticated()) {
            return;
        }
        this.streamLoopActive = true;
        void this.runSseLoop();
    }
    public disconnectRealtimeStream(): void {
        this.streamLoopActive = false;
        this.streamAbort?.abort();
        this.streamAbort = null;
    }
    public getNotifications(): Observable<NotificationModel[]> {
        return this.http.get<NotificationApiRow[]>(this.baseUrl).pipe(
            map((rows) => this.mapNotificationRows(rows ?? [])),
            catchError(() => throwError(() => new Error('Impossibile caricare le notifiche.')))
        );
    }
    private mapNotificationRows(rows: NotificationApiRow[]): NotificationModel[] {
        return rows.map((row) => this.mapNotificationRow(row));
    }
    private mapNotificationRow(row: NotificationApiRow | NotificationCreatedSsePayload): NotificationModel {
        return new NotificationModel(row.notificationID ?? ('id' in row ? row.id : undefined) ?? 0, row.message ?? ('title' in row ? row.title : undefined) ?? '', row.read ?? false, row.createdAt ?? null, row.kind ?? null, row.actorName ?? null, row.actorSurname ?? null, row.actorEmail ?? null, row.actorRating ?? null);
    }
    public getUnreadCount(): Observable<number> {
        return this.http
            .get<number>(`${this.baseUrl}/unread-count`)
            .pipe(catchError(() => throwError(() => new Error('Impossibile caricare il conteggio delle notifiche non lette.'))));
    }
    public markAsRead(id: number): Observable<void> {
        return this.http.patch<void>(`${this.baseUrl}/${id}/read`, {}).pipe(catchError(() => throwError(() => new Error('Impossibile segnare la notifica come letta.'))));
    }
    public markAllAsRead(): Observable<void> {
        return this.http.patch<void>(`${this.baseUrl}/read-all`, {}).pipe(catchError(() => throwError(() => new Error('Impossibile segnare tutte le notifiche come lette.'))));
    }
    public clearHistory(): Observable<void> {
        return this.http.delete<void>(`${this.baseUrl}/history`).pipe(catchError(() => throwError(() => new Error('Impossibile svuotare lo storico notifiche.'))));
    }
    private async runSseLoop(): Promise<void> {
        while (this.streamLoopActive) {
            const token = this.auth.token();
            if (!token) {
                break;
            }
            const abort = new AbortController();
            this.streamAbort = abort;
            try {
                const response = await fetch(`${this.baseUrl}/stream`, {
                    method: 'GET',
                    headers: {
                        Accept: 'text/event-stream',
                        Authorization: `Bearer ${token}`
                    },
                    signal: abort.signal
                });
                if (!response.ok || !response.body) {
                    await this.sleep(5000, abort.signal);
                    continue;
                }
                const reader = response.body.getReader();
                const decoder = new TextDecoder();
                let buffer = '';
                while (this.streamLoopActive && !abort.signal.aborted) {
                    const { done, value } = await reader.read();
                    if (done) {
                        break;
                    }
                    buffer += decoder.decode(value, { stream: true });
                    const parsed = consumeNotificationSseBuffer(buffer);
                    buffer = parsed.remainder;
                    for (const count of parsed.unreadCounts) {
                        this.setUnreadCount(count);
                        this.refresh$.next();
                    }
                    for (const row of parsed.created) {
                        this.notificationCreated$.next(this.mapNotificationRow(row));
                        this.refresh$.next();
                    }
                    for (const update of parsed.updated) {
                        this.notificationUpdated$.next({ id: update.notificationID, read: update.read });
                        this.refresh$.next();
                    }
                    if (parsed.allMarkedRead) {
                        this.allNotificationsMarkedRead$.next();
                        this.refresh$.next();
                    }
                }
            }
            catch {
                if (abort.signal.aborted || !this.streamLoopActive) {
                    return;
                }
            }
            if (this.streamLoopActive) {
                await this.sleep(3000, abort.signal);
            }
        }
    }
    private sleep(ms: number, signal: AbortSignal): Promise<void> {
        if (signal.aborted) {
            return Promise.resolve();
        }
        return new Promise((resolve) => {
            const timer = window.setTimeout(() => resolve(), ms);
            signal.addEventListener('abort', () => {
                window.clearTimeout(timer);
                resolve();
            }, { once: true });
        });
    }
}
