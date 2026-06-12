import { Component, inject } from '@angular/core';
import { ToastService } from '../../../core/services/toast.service';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { SdIconComponent } from '../../icons/sd-icon/sd-icon.component';
@Component({
    selector: 'app-toast-container',
    standalone: true,
    imports: [SdIconComponent, MdbRippleModule],
    templateUrl: './toast-container.component.html',
    styleUrl: './toast-container.component.scss'
})
export class ToastContainerComponent {
    protected readonly toastService = inject(ToastService);
}
