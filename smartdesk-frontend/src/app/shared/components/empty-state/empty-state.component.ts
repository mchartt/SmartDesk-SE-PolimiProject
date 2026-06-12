import { Component, input } from '@angular/core';
import { SdIconComponent, SdIconName } from '../../icons/sd-icon/sd-icon.component';
@Component({
    selector: 'app-empty-state',
    standalone: true,
    imports: [SdIconComponent],
    templateUrl: './empty-state.component.html',
    styleUrl: './empty-state.component.scss'
})
export class EmptyStateComponent {
    public readonly message = input('Nessun dato disponibile.');
    public readonly icon = input<SdIconName>('search');
}
