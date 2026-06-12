import { Component, OnInit, computed, DestroyRef, inject, ChangeDetectorRef, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { MdbValidationModule } from 'mdb-angular-ui-kit/validation';
import { SdIconComponent } from '../../../../shared/icons/sd-icon/sd-icon.component';
import { WorkerService } from '../../../../core/services/worker.service';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { ModelFactory } from '../../../../core/utils/model-factory';
import { ConfirmModalService } from '../../../../shared/services/confirm-modal.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { filter, switchMap } from 'rxjs/operators';
@Component({
    standalone: true,
    imports: [CommonModule, SdIconComponent, FormsModule, MdbFormsModule, MdbValidationModule, MdbRippleModule],
    templateUrl: './worker-profile-page.component.html',
    styleUrl: './worker-profile-page.component.scss'
})
export class WorkerProfilePageComponent implements OnInit {
    private readonly route = inject(ActivatedRoute);
    private readonly workerService = inject(WorkerService);
    private readonly authState = inject(AuthStateService);
    private readonly router = inject(Router);
    private readonly confirmModal = inject(ConfirmModalService);
    private readonly notifications = inject(NotificationService);
    private readonly cdr = inject(ChangeDetectorRef);
    private readonly destroyRef = inject(DestroyRef);
    protected readonly title = computed(() => (this.route.snapshot.data['title'] as string) ?? 'Profilo');
    protected loading = signal(true);
    protected name = '';
    protected surname = '';
    protected email = '';
    protected currentPassword = '';
    protected newPassword = '';
    protected errorMsg = '';
    protected successMsg = '';
    public ngOnInit(): void {
        this.loadProfile();
    }
    protected loadProfile(): void {
        this.loading.set(true);
        this.errorMsg = '';
        this.successMsg = '';
        this.workerService
            .getProfile()
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: (profile) => {
                this.name = profile.name ?? '';
                this.surname = profile.surname ?? '';
                this.email = profile.email ?? '';
                this.loading.set(false);
                this.cdr.markForCheck();
            },
            error: (err: Error) => {
                this.errorMsg = err.message;
                this.loading.set(false);
                this.cdr.markForCheck();
            }
        });
    }
    protected updateProfile(): void {
        this.errorMsg = '';
        this.successMsg = '';
        const name = this.name.trim();
        const surname = this.surname.trim();
        const email = this.email.trim();
        if (!name || !surname || !email) {
            this.errorMsg = 'Nome, cognome ed email sono obbligatori.';
            return;
        }
        this.workerService
            .updateProfile({ name, surname, email })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: () => {
                this.successMsg = 'Profilo aggiornato.';
                this.notifications.requestRefresh();
                const current = this.authState.currentUserSnapshot();
                const token = this.authState.token();
                if (current && token) {
                    const updated = ModelFactory.createUser({
                        id: current.id,
                        email,
                        password: '',
                        surname,
                        name,
                        active: current.active,
                        roleType: current.getRole()
                    });
                    this.authState.setSession(updated, token);
                }
                this.cdr.markForCheck();
            },
            error: (err: Error) => {
                this.errorMsg = err.message;
                this.cdr.markForCheck();
            }
        });
    }
    protected changePassword(): void {
        this.errorMsg = '';
        this.successMsg = '';
        if (!this.currentPassword || !this.newPassword) {
            this.errorMsg = 'Entrambe le password sono obbligatorie.';
            return;
        }
        this.workerService
            .changePassword(this.currentPassword, this.newPassword)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: () => {
                this.successMsg = 'Password aggiornata.';
                this.currentPassword = '';
                this.newPassword = '';
                this.cdr.markForCheck();
            },
            error: (err: Error) => {
                this.errorMsg = err.message;
                this.cdr.markForCheck();
            }
        });
    }
    protected deleteAccount(): void {
        this.confirmModal
            .confirm({
            title: 'Elimina account',
            message: 'Sei sicuro di voler eliminare il tuo account? Tutti i dati associati verranno rimossi e l’azione non può essere annullata.',
            confirmLabel: 'Elimina account',
            cancelLabel: 'Annulla',
            variant: 'danger'
        })
            .pipe(takeUntilDestroyed(this.destroyRef), filter((confirmed): confirmed is true => confirmed === true), switchMap(() => {
            this.errorMsg = '';
            this.successMsg = '';
            return this.workerService.deleteAccount();
        }))
            .subscribe({
            next: () => {
                this.authState.clearSession();
                this.router.navigate(['/login']);
            },
            error: (err: Error) => {
                this.errorMsg = err.message;
                this.cdr.markForCheck();
            }
        });
    }
}
