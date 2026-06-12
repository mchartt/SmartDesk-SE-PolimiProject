import { CommonModule } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { SdIconComponent, SdIconName } from '../../icons/sd-icon/sd-icon.component';
export type SdModalHeaderVariant = 'primary' | 'warning' | 'danger' | 'success' | 'info' | 'secondary';
@Component({
    selector: 'app-sd-modal-header',
    standalone: true,
    imports: [CommonModule, SdIconComponent, MdbRippleModule],
    templateUrl: './sd-modal-header.component.html',
    styleUrl: './sd-modal-header.component.scss'
})
export class SdModalHeaderComponent {
    readonly title = input.required<string>();
    readonly subtitle = input<string | null>(null);
    readonly eyebrow = input<string | null>(null);
    readonly icon = input<SdIconName | null>(null);
    readonly iconVariant = input<SdModalHeaderVariant>('primary');
    readonly titleId = input<string | null>(null);
    readonly closeLabel = input('Chiudi');
    readonly showClose = input(true);
    readonly close = output<void>();
    protected readonly iconCircleClass = computed(() => {
        switch (this.iconVariant()) {
            case 'danger':
                return 'bg-danger-subtle text-danger';
            case 'warning':
                return 'bg-warning-subtle text-warning';
            case 'success':
                return 'bg-success-subtle text-success';
            case 'info':
                return 'bg-info-subtle text-info-emphasis';
            case 'secondary':
                return 'bg-secondary-subtle text-secondary';
            default:
                return 'bg-primary-subtle text-primary';
        }
    });
}
