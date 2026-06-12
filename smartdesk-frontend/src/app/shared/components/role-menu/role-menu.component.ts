import { Component, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { SdIconComponent, SdIconName } from '../../icons/sd-icon/sd-icon.component';
@Component({
    selector: 'app-role-menu',
    standalone: true,
    imports: [RouterLink, RouterLinkActive, SdIconComponent, MdbRippleModule],
    templateUrl: './role-menu.component.html',
    styleUrl: './role-menu.component.scss'
})
export class RoleMenuComponent {
    public readonly items = input<Array<{
        label: string;
        path: string;
        icon: SdIconName;
    }>>([]);
}
