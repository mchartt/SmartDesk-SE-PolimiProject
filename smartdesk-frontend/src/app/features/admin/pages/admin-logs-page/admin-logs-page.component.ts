import { CommonModule } from '@angular/common';
import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { SdIconComponent } from '../../../../shared/icons/sd-icon/sd-icon.component';
import { AdminService } from '../../../../core/services/admin.service';
import { SystemLog } from '../../../../core/models';
@Component({
    standalone: true,
    imports: [CommonModule, FormsModule, EmptyStateComponent, SdIconComponent],
    templateUrl: './admin-logs-page.component.html',
    styleUrl: './admin-logs-page.component.scss'
})
export class AdminLogsPageComponent implements OnInit {
    private readonly route = inject(ActivatedRoute);
    private readonly destroyRef = inject(DestroyRef);
    private readonly adminService = inject(AdminService);
    protected readonly title = computed(() => (this.route.snapshot.data['title'] as string) ?? 'Log di sistema');
    protected logs = signal<SystemLog[]>([]);
    protected errorMsg = '';
    protected filterSeverity = signal<string>('ALL');
    protected filterAction = signal<string>('');
    protected readonly filteredLogs = computed(() => {
        return this.logs().filter(log => {
            const sevOk = this.filterSeverity() === 'ALL' || log.severity === this.filterSeverity();
            const actOk = !this.filterAction() ||
                log.action.toLowerCase().includes(this.filterAction().toLowerCase());
            return sevOk && actOk;
        });
    });
    public ngOnInit(): void {
        this.adminService
            .getLogs()
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: (rows) => this.logs.set(rows),
            error: (err: Error) => {
                this.logs.set([]);
                this.errorMsg = err.message;
            }
        });
    }
}
