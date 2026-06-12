import { computed, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AuthStateService } from '../../core/services/auth-state.service';
export abstract class RoleRouteTitleBase {
    protected readonly route = inject(ActivatedRoute);
    protected readonly authState = inject(AuthStateService);
    protected readonly title = computed(() => (this.route.snapshot.data['title'] as string) ?? this.defaultRouteTitle());
    protected readonly dashboardTitle = computed(() => this.resolveDashboardTitle());
    protected resolveDashboardTitle(): string {
        return this.authState.user()?.getDashboardTitle() ?? this.defaultDashboardTitle();
    }
    protected abstract defaultRouteTitle(): string;
    protected abstract defaultDashboardTitle(): string;
}
