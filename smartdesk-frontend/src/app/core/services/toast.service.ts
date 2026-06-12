import { Injectable, signal } from '@angular/core';
export type ToastKind = 'success' | 'error';
export interface ToastMessage {
    id: number;
    message: string;
    kind: ToastKind;
}
@Injectable({ providedIn: 'root' })
export class ToastService {
    private nextId = 0;
    private readonly timers = new Map<number, ReturnType<typeof setTimeout>>();
    public readonly toasts = signal<ToastMessage[]>([]);
    public success(message: string, durationMs = 4500): void {
        this.show(message, 'success', durationMs);
    }
    public error(message: string, durationMs = 5500): void {
        this.show(message, 'error', durationMs);
    }
    public dismiss(id: number): void {
        const timer = this.timers.get(id);
        if (timer) {
            clearTimeout(timer);
            this.timers.delete(id);
        }
        this.toasts.update((list) => list.filter((t) => t.id !== id));
    }
    private show(message: string, kind: ToastKind, durationMs: number): void {
        const trimmed = message.trim();
        if (!trimmed) {
            return;
        }
        const currentToasts = this.toasts();
        const existing = currentToasts.find((t) => t.message === trimmed && t.kind === kind);
        if (existing) {
            const oldTimer = this.timers.get(existing.id);
            if (oldTimer) {
                clearTimeout(oldTimer);
            }
            const timer = setTimeout(() => this.dismiss(existing.id), durationMs);
            this.timers.set(existing.id, timer);
            return;
        }
        const id = ++this.nextId;
        this.toasts.update((list) => {
            const newList = [...list, { id, message: trimmed, kind }];
            if (newList.length > 3) {
                const oldest = newList[0];
                const timer = this.timers.get(oldest.id);
                if (timer) {
                    clearTimeout(timer);
                    this.timers.delete(oldest.id);
                }
                return newList.slice(1);
            }
            return newList;
        });
        const timer = setTimeout(() => this.dismiss(id), durationMs);
        this.timers.set(id, timer);
    }
}
