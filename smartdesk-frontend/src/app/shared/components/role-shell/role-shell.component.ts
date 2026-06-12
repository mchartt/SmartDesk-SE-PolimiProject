import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, RouterOutlet } from '@angular/router';
import { NavbarComponent } from '../navbar/navbar.component';
import { RoleMenuComponent } from '../role-menu/role-menu.component';
import { ROLE_MENUS, type MenuItem } from '../../role-menus';
import { UserRole } from '../../../core/models';
import { AuthStateService } from '../../../core/services/auth-state.service';
@Component({
    selector: 'app-role-shell',
    standalone: true,
    imports: [NavbarComponent, RoleMenuComponent, RouterOutlet],
    templateUrl: './role-shell.component.html',
    styleUrl: './role-shell.component.scss'
})
export class RoleShellComponent {
    private readonly route = inject(ActivatedRoute);
    private readonly authState = inject(AuthStateService);
    protected readonly shellLayout = computed(() => this.route.snapshot.data['shellLayout'] === true);
    protected readonly role = computed(() => (this.route.snapshot.data['role'] as UserRole | undefined) ?? 'WORKER');
    protected readonly menuItems = computed<MenuItem[]>(() => ROLE_MENUS[this.role()]);
    protected readonly accountRestricted = computed(() => {
        const u = this.authState.user();
        return !!(u && !u.active);
    });
}
