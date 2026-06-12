import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { MdbModalRef } from 'mdb-angular-ui-kit/modal';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { SdIconComponent, SdIconName } from '../../icons/sd-icon/sd-icon.component';
export type ConfirmVariant = 'primary' | 'danger' | 'warning' | 'success' | 'info';
@Component({
    standalone: true,
    imports: [CommonModule, SdIconComponent, MdbRippleModule],
    templateUrl: './confirm-modal-content.component.html',
    styleUrl: './confirm-modal-content.component.scss'
})
export class ConfirmModalContentComponent {
    public title = 'Conferma operazione';
    public message = 'Continuare?';
    public confirmLabel = 'Conferma';
    public cancelLabel = 'Annulla';
    public alertMode = false;
    public variant: ConfirmVariant = 'primary';
    public iconOverride: SdIconName | null = null;
    private static readonly DEFAULT_ICON: Record<ConfirmVariant, SdIconName> = {
        primary: 'exclamation',
        danger: 'exclamation',
        warning: 'exclamation',
        success: 'circle-check',
        info: 'exclamation'
    };
    public constructor(public readonly modalRef: MdbModalRef<ConfirmModalContentComponent>) { }
    protected get iconName(): SdIconName {
        return this.iconOverride ?? ConfirmModalContentComponent.DEFAULT_ICON[this.variant];
    }
    protected get headerIconClass(): string {
        switch (this.variant) {
            case 'danger':
                return 'bg-danger-subtle text-danger';
            case 'warning':
                return 'bg-warning-subtle text-warning-emphasis';
            case 'success':
                return 'bg-success-subtle text-success';
            case 'info':
                return 'bg-info-subtle text-info-emphasis';
            default:
                return 'bg-primary-subtle text-primary';
        }
    }
    protected get confirmBtnClass(): string {
        switch (this.variant) {
            case 'danger':
                return 'btn-danger';
            case 'warning':
                return 'btn-warning text-dark';
            case 'success':
                return 'btn-success';
            case 'info':
                return 'btn-info text-dark';
            default:
                return 'btn-primary';
        }
    }
    protected close(): void {
        this.modalRef.close(false);
    }
    protected confirm(): void {
        this.modalRef.close(true);
    }
}
