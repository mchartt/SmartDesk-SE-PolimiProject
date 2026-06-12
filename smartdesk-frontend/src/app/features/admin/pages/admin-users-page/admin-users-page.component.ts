import { CommonModule } from '@angular/common';
import { Component, DestroyRef, HostListener, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { filter, switchMap } from 'rxjs/operators';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { SdModalHeaderComponent } from '../../../../shared/components/sd-modal-header/sd-modal-header.component';
import type { SdIconName } from '../../../../shared/icons/sd-icon/sd-icon.component';
import { SdIconComponent } from '../../../../shared/icons/sd-icon/sd-icon.component';
import { AdminService } from '../../../../core/services/admin.service';
import { ConfirmModalService } from '../../../../shared/services/confirm-modal.service';
import { AuthStateService } from '../../../../core/services/auth-state.service';
type UserRow = {
    userID: number;
    name: string;
    surname: string;
    email: string;
    role: string;
    status: string;
};
const ROLE_SECTION_ORDER = ['SYS_ADMIN', 'HOST', 'WORKER', 'TECHNICIAN'] as const;
type KnownRoleKey = (typeof ROLE_SECTION_ORDER)[number];
const ROLE_SECTION_LABELS: Record<KnownRoleKey, string> = {
    SYS_ADMIN: 'Amministratori di sistema',
    HOST: 'Host',
    WORKER: 'Lavoratori',
    TECHNICIAN: 'Tecnici'
};
const ROLE_SECTION_ICONS: Record<KnownRoleKey, SdIconName> = {
    SYS_ADMIN: 'shield',
    HOST: 'host-venue',
    WORKER: 'users',
    TECHNICIAN: 'wrench'
};
function normalizeAdminUsersSearch(value: string): string {
    return value
        .toLowerCase()
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '');
}
function matchesNameSearch(user: UserRow, rawQuery: string): boolean {
    const q = rawQuery.trim();
    if (!q) {
        return true;
    }
    const tokens = normalizeAdminUsersSearch(q)
        .split(/\s+/)
        .filter(Boolean);
    if (!tokens.length) {
        return true;
    }
    const target = normalizeAdminUsersSearch(`${user.name} ${user.surname}`.trim() || user.email);
    return tokens.every((t) => target.includes(t));
}
function compareUsersByName(a: UserRow, b: UserRow): number {
    const s = a.surname.localeCompare(b.surname, 'it', { sensitivity: 'base' });
    if (s !== 0) {
        return s;
    }
    return a.name.localeCompare(b.name, 'it', { sensitivity: 'base' });
}
@Component({
    standalone: true,
    imports: [CommonModule, FormsModule, MdbFormsModule, EmptyStateComponent, MdbRippleModule, SdModalHeaderComponent, SdIconComponent],
    templateUrl: './admin-users-page.component.html',
    styleUrl: './admin-users-page.component.scss'
})
export class AdminUsersPageComponent implements OnInit {
    private readonly route = inject(ActivatedRoute);
    private readonly destroyRef = inject(DestroyRef);
    private readonly adminService = inject(AdminService);
    private readonly confirmService = inject(ConfirmModalService);
    private readonly authState = inject(AuthStateService);
    protected readonly title = computed(() => (this.route.snapshot.data['title'] as string) ?? 'Utenti');
    protected users = signal<UserRow[]>([]);
    protected errorMsg = '';
    protected readonly modalRoleKey = signal<KnownRoleKey | null>(null);
    protected readonly roleModalOpen = signal(false);
    protected readonly modalSearchQuery = signal('');
    protected readonly roleSectionTiles = computed(() => ROLE_SECTION_ORDER.map((roleKey) => ({
        roleKey,
        label: ROLE_SECTION_LABELS[roleKey],
        icon: ROLE_SECTION_ICONS[roleKey],
        count: this.users().filter((u) => u.role === roleKey).length
    })));
    protected readonly modalUsersInRoleUnfiltered = computed(() => {
        const key = this.modalRoleKey();
        if (!key) {
            return [];
        }
        return this.users().filter((u) => u.role === key).sort(compareUsersByName);
    });
    protected readonly modalFilteredUsers = computed(() => this.modalUsersInRoleUnfiltered().filter((u) => matchesNameSearch(u, this.modalSearchQuery())));
    public ngOnInit(): void {
        this.load();
    }
    protected load(): void {
        this.errorMsg = '';
        this.adminService
            .getUsers()
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: (rows) => {
                const mapped = (rows as Record<string, unknown>[]).map((raw) => ({
                    userID: Number(raw['userID'] ?? 0),
                    name: String(raw['name'] ?? '').trim(),
                    surname: String(raw['surname'] ?? '').trim(),
                    email: String(raw['email'] ?? ''),
                    role: String(raw['role'] ?? ''),
                    status: String(raw['status'] ?? '')
                }));
                this.users.set(mapped);
            },
            error: (err: Error) => {
                this.users.set([]);
                this.errorMsg = err.message;
            }
        });
    }
    protected openRoleModal(roleKey: KnownRoleKey): void {
        this.modalRoleKey.set(roleKey);
        this.modalSearchQuery.set('');
        this.roleModalOpen.set(true);
    }
    protected closeRoleModal(): void {
        this.roleModalOpen.set(false);
    }
    protected roleModalTitle(roleKey: KnownRoleKey): string {
        return ROLE_SECTION_LABELS[roleKey];
    }
    protected roleModalIcon(roleKey: KnownRoleKey): SdIconName {
        return ROLE_SECTION_ICONS[roleKey];
    }
    protected displayName(user: UserRow): string {
        const full = `${user.name} ${user.surname}`.trim();
        return full || user.email;
    }
    protected avatarInitial(user: UserRow): string {
        const base = user.name.trim() || user.surname.trim() || user.email;
        return base.charAt(0).toUpperCase();
    }
    protected isCurrentAdmin(user: UserRow): boolean {
        return Number(user?.userID) === this.authState.user()?.id;
    }
    protected moderate(user: UserRow, action: 'BAN' | 'REACTIVATE'): void {
        if (action === 'BAN' && this.isCurrentAdmin(user)) {
            return;
        }
        this.confirmService
            .confirm({
            title: `${action === 'BAN' ? 'Blocca' : 'Riattiva'} utente`,
            message: `Sei sicuro di voler ${action === 'BAN' ? 'bloccare' : 'riattivare'} l’utente ${this.displayName(user)} (${user.email})?`,
            confirmLabel: action === 'BAN' ? 'Blocca' : 'Riattiva',
            cancelLabel: 'Annulla',
            variant: action === 'BAN' ? 'danger' : 'success'
        })
            .pipe(takeUntilDestroyed(this.destroyRef), filter((confirmed): confirmed is true => confirmed === true), switchMap(() => {
            this.errorMsg = '';
            return this.adminService.moderateUser(user.userID, action);
        }))
            .subscribe({
            next: () => this.load(),
            error: (err: Error) => (this.errorMsg = err.message)
        });
    }
    @HostListener('document:keydown.escape')
    protected onEscape(): void {
        if (this.roleModalOpen()) {
            this.closeRoleModal();
        }
    }
}
