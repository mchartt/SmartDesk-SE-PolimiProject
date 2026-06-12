import { Component, DestroyRef, ElementRef, HostListener, computed, inject, input, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter, map, startWith } from 'rxjs/operators';
import { MdbCollapseModule } from 'mdb-angular-ui-kit/collapse';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { UserRole } from '../../../core/models';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ROLE_LABEL, ROLE_QUICK_LINKS, roleAccountMenuItems, roleDashboardPath } from '../../role-navigation';
import type { MenuItem } from '../../role-menus';
import { NotificationBellComponent } from '../notification-bell/notification-bell.component';
import { SdIconComponent } from '../../icons/sd-icon/sd-icon.component';
import { SdBrandMarkComponent } from '../sd-brand-mark/sd-brand-mark.component';
@Component({
    selector: 'app-navbar',
    standalone: true,
    imports: [
        RouterLink,
        RouterLinkActive,
        NotificationBellComponent,
        MdbCollapseModule,
        MdbRippleModule,
        CommonModule,
        SdIconComponent,
        SdBrandMarkComponent
    ],
    templateUrl: './navbar.component.html',
    styleUrl: './navbar.component.scss'
})
export class NavbarComponent {
    readonly menuItems = input<MenuItem[]>([]);
    readonly role = input<UserRole>('WORKER');
    protected readonly auth = inject(AuthStateService);
    private readonly authService = inject(AuthService);
    private readonly toast = inject(ToastService);
    private readonly notifications = inject(NotificationService);
    private readonly destroyRef = inject(DestroyRef);
    private readonly router = inject(Router);
    private readonly mobileCollapse = viewChild<{
        hide: () => void;
        toggle: () => void;
    }>('mobileCollapse');
    private readonly accountMenuRoot = viewChild<ElementRef<HTMLElement>>('accountMenuRoot');
    protected readonly scrolled = signal(false);
    protected readonly mobileNavOpen = signal(false);
    protected readonly accountMenuOpen = signal(false);
    protected readonly pageTitle = signal<string | null>(null);
    protected readonly brandLink = computed(() => roleDashboardPath(this.role()));
    protected readonly roleLabel = computed(() => ROLE_LABEL[this.role()]);
    protected readonly quickLinks = computed(() => ROLE_QUICK_LINKS[this.role()]);
    protected readonly accountMenuItems = computed(() => roleAccountMenuItems(this.role()));
    protected readonly unreadNotifications = computed(() => this.notifications.unreadCount());
    protected readonly accountRestricted = computed(() => {
        const u = this.auth.user();
        return !!(u && !u.active);
    });
    protected readonly userInitials = computed(() => {
        const u = this.auth.user();
        if (!u) {
            return 'U';
        }
        const d = u.displayName.trim();
        const parts = d.split(/\s+/).filter(Boolean);
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
        }
        if (parts.length === 1 && parts[0].length >= 2) {
            return parts[0].slice(0, 2).toUpperCase();
        }
        if (parts.length === 1 && parts[0].length === 1) {
            return parts[0].toUpperCase();
        }
        return 'U';
    });
    constructor() {
        this.router.events
            .pipe(filter((event) => event instanceof NavigationEnd), startWith(null), map(() => this.resolvePageTitle()), takeUntilDestroyed())
            .subscribe((title) => {
            this.pageTitle.set(title);
            this.closeMobileNav();
            this.accountMenuOpen.set(false);
        });
    }
    @HostListener('document:click', ['$event'])
    protected onDocumentClick(event: MouseEvent): void {
        if (!this.accountMenuOpen()) {
            return;
        }
        const root = this.accountMenuRoot()?.nativeElement;
        if (root && !root.contains(event.target as Node)) {
            this.accountMenuOpen.set(false);
        }
    }
    @HostListener('window:scroll')
    protected onWindowScroll(): void {
        this.scrolled.set(window.scrollY > 4);
    }
    protected toggleMobileNav(): void {
        this.mobileCollapse()?.toggle();
        this.mobileNavOpen.update((open) => !open);
    }
    protected closeMobileNav(): void {
        this.mobileCollapse()?.hide();
        this.mobileNavOpen.set(false);
    }
    protected toggleAccountMenu(event: Event): void {
        event.stopPropagation();
        this.accountMenuOpen.update((open) => !open);
    }
    protected closeAccountMenu(): void {
        this.accountMenuOpen.set(false);
    }
    protected unreadBadgeLabel(count: number): string {
        return count > 99 ? '99+' : String(count);
    }
    protected logout(): void {
        this.authService
            .logout()
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: () => {
                void this.router.navigate(['/login']).then((ok) => {
                    if (ok) {
                        this.toast.success('Disconnessione completata.');
                    }
                });
            }
        });
    }
    private resolvePageTitle(): string | null {
        let route = this.router.routerState.root;
        while (route.firstChild) {
            route = route.firstChild;
        }
        return (route.snapshot.data['title'] as string | undefined) ?? null;
    }
}
