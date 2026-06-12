import { Component, computed, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { fromEvent, merge, of } from 'rxjs';
import { catchError, debounceTime, filter, startWith, switchMap } from 'rxjs/operators';
import { ItalianCountPhrase } from '../../../core/utils/italian-count.phrase';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { NotificationService } from '../../../core/services/notification.service';
import { roleNotificationsPath } from '../../role-navigation';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { SdIconComponent } from '../../icons/sd-icon/sd-icon.component';
@Component({
    selector: 'app-notification-bell',
    standalone: true,
    imports: [SdIconComponent, MdbRippleModule],
    templateUrl: './notification-bell.component.html',
    styleUrl: './notification-bell.component.scss'
})
export class NotificationBellComponent {
    private readonly notifications = inject(NotificationService);
    private readonly router = inject(Router);
    private readonly auth = inject(AuthStateService);
    protected readonly badgeCount = computed(() => this.notifications.unreadCount());
    constructor() {
        this.notifications.connectRealtimeStream();
        const visibleTab$ = fromEvent(document, 'visibilitychange').pipe(startWith(0), filter(() => !document.hidden));
        merge(this.notifications.refresh$, visibleTab$)
            .pipe(startWith(0), debounceTime(100), filter(() => !document.hidden), switchMap(() => this.notifications.getUnreadCount().pipe(catchError(() => of(0)))), takeUntilDestroyed())
            .subscribe((count) => this.notifications.setUnreadCount(count));
    }
    protected hasBadge(): boolean {
        return this.badgeCount() > 0;
    }
    protected badgeDisplay(): string {
        const n = this.badgeCount();
        return n > 99 ? '99+' : String(n);
    }
    protected bellAriaLabel(): string {
        const n = this.badgeCount();
        if (n > 0) {
            return `Apri notifiche, ${ItalianCountPhrase.format(n, 'non letta', 'non lette')}`;
        }
        return 'Apri notifiche';
    }
    protected openNotifications(): void {
        void this.router.navigate([roleNotificationsPath(this.auth.user()?.getRole())]);
    }
}
