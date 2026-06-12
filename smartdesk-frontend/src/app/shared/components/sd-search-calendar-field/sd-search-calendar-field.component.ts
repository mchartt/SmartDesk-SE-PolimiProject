import { CommonModule } from '@angular/common';
import { Component, input, model, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { SdIconComponent } from '../../icons/sd-icon/sd-icon.component';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
@Component({
    selector: 'app-sd-search-calendar-field',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        MdbFormsModule,
        SdIconComponent,
        MdbRippleModule
    ],
    templateUrl: './sd-search-calendar-field.component.html',
    styleUrl: './sd-search-calendar-field.component.scss'
})
export class SdSearchCalendarFieldComponent {
    readonly inputId = input.required<string>();
    readonly label = input('Giorno');
    readonly placeholder = input('Seleziona giorno');
    readonly selectedIso = model('');
    readonly highlightedDays = input<readonly string[]>([]);
    readonly closeOnSelect = input(true);
    protected readonly hasSelection = computed(() => this.selectedIso().trim().length > 0);
    protected clearDate(ev: Event): void {
        ev.preventDefault();
        ev.stopPropagation();
        this.selectedIso.set('');
    }
}
