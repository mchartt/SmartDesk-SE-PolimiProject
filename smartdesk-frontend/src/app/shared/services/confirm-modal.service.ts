import { Injectable, inject } from '@angular/core';
import { Observable, finalize, map } from 'rxjs';
import { MdbModalRef, MdbModalService } from 'mdb-angular-ui-kit/modal';
import { ConfirmModalContentComponent, ConfirmVariant } from '../components/confirm-modal-content/confirm-modal-content.component';
import { SdIconName } from '../icons/sd-icon/sd-icon.component';
import { ScrollLockService } from './scroll-lock.service';
export interface ConfirmOptions {
    title: string;
    message: string;
    confirmLabel?: string;
    cancelLabel?: string;
    variant?: ConfirmVariant;
    icon?: SdIconName;
}
export interface AlertOptions {
    title: string;
    message: string;
    okLabel?: string;
    variant?: ConfirmVariant;
    icon?: SdIconName;
}
@Injectable({ providedIn: 'root' })
export class ConfirmModalService {
    private readonly modal = inject(MdbModalService);
    private readonly scrollLock = inject(ScrollLockService);
    public confirm(options: ConfirmOptions): Observable<boolean> {
        this.scrollLock.acquire();
        const ref = this.openModalShell();
        const inst = ref.component;
        inst.title = options.title;
        inst.message = options.message;
        inst.confirmLabel = options.confirmLabel ?? 'Conferma';
        inst.cancelLabel = options.cancelLabel ?? 'Annulla';
        inst.variant = options.variant ?? 'primary';
        inst.iconOverride = options.icon ?? null;
        inst.alertMode = false;
        return ref.onClose.pipe(map((result) => result === true), finalize(() => this.scrollLock.release()));
    }
    public alert(options: AlertOptions): Observable<boolean> {
        this.scrollLock.acquire();
        const ref = this.openModalShell();
        const inst = ref.component;
        inst.title = options.title;
        inst.message = options.message;
        inst.confirmLabel = options.okLabel ?? 'Ho capito';
        inst.variant = options.variant ?? 'primary';
        inst.iconOverride = options.icon ?? null;
        inst.alertMode = true;
        return ref.onClose.pipe(map((result) => result === true), finalize(() => this.scrollLock.release()));
    }
    private openModalShell(): MdbModalRef<ConfirmModalContentComponent> {
        return this.modal.open(ConfirmModalContentComponent, {
            modalClass: 'modal-dialog-centered modal-dialog-scrollable',
            backdrop: true,
            keyboard: true,
            ignoreBackdropClick: false
        });
    }
}
