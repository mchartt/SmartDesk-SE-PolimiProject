import { Component, computed, input } from '@angular/core';
import { SdIconComponent, type SdIconName } from '../sd-icon/sd-icon.component';
function iconForAmenityTag(tag: string): SdIconName | null {
    switch (tag.toUpperCase()) {
        case 'WIFI':
        case 'FAST_INTERNET':
            return 'wifi';
        case 'DOUBLE_MONITOR':
        case 'ULTRAWIDE_MONITOR':
            return 'monitor';
        case 'ERGONOMIC_CHAIR':
            return 'chair';
        case 'POWER_OUTLET':
        case 'DOCKING_STATION':
            return 'outlet';
        case 'STANDING_DESK':
            return 'standing-desk';
        case 'NATURAL_LIGHT':
            return 'sun';
        case 'SILENT_MOUSE':
            return 'tag';
        default:
            return null;
    }
}
@Component({
    selector: 'app-amenity-icon',
    standalone: true,
    imports: [SdIconComponent],
    styleUrl: './amenity-icon.component.scss',
    template: `    @if (resolved(); as icon) {      <span class="sd-amenity-icon-wrap me-1" aria-hidden="true">        <app-sd-icon [name]="icon" [size]="iconSize()" />      </span>    }  `,
    styles: [
        `      .sd-amenity-icon-wrap {        display: inline-flex;        vertical-align: middle;      }    `,
    ],
})
export class AmenityIconComponent {
    readonly tag = input.required<string>();
    readonly iconSize = input(14);
    readonly resolved = computed(() => iconForAmenityTag(this.tag()));
}
