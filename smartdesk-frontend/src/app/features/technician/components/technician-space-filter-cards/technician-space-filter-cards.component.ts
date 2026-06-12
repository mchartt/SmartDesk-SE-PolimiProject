import { CommonModule } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { TechnicianAssignedSpaceRow, TechnicianTicketRow } from '../../../../core/services/technician.service';
import type { TechnicianSpaceFilterValue } from '../../../../core/utils/technician-ticket-list.util';
export type { TechnicianSpaceFilterValue };
import { SdIconComponent } from '../../../../shared/icons/sd-icon/sd-icon.component';
@Component({
    selector: 'app-technician-space-filter-cards',
    standalone: true,
    imports: [CommonModule, MdbRippleModule, SdIconComponent],
    templateUrl: './technician-space-filter-cards.component.html',
    styleUrl: './technician-space-filter-cards.component.scss'
})
export class TechnicianSpaceFilterCardsComponent {
    readonly spaces = input<TechnicianAssignedSpaceRow[]>([]);
    readonly tickets = input<TechnicianTicketRow[]>([]);
    readonly selectedFilter = input<TechnicianSpaceFilterValue>('all');
    readonly selectedFilterChange = output<TechnicianSpaceFilterValue>();
    readonly assignedIds = computed(() => new Set(this.spaces().map((s) => s.spaceID)));
    readonly outOfScopeCount = computed(() => {
        const ids = this.assignedIds();
        return this.tickets().filter((t) => {
            const sid = t.spaceID ?? null;
            return sid == null || !ids.has(sid);
        }).length;
    });
    readonly filterOrder = computed((): TechnicianSpaceFilterValue[] => {
        const nums = this.spaces().map((s) => s.spaceID);
        const tail: TechnicianSpaceFilterValue[] = this.outOfScopeCount() > 0 ? ['out-of-scope'] : [];
        return ['all', ...nums, ...tail];
    });
    readonly showScopeHint = computed(() => this.spaces().length === 0 && this.tickets().length > 0);
    pick(value: TechnicianSpaceFilterValue): void {
        this.selectedFilterChange.emit(value);
    }
    isActive(value: TechnicianSpaceFilterValue): boolean {
        return this.selectedFilter() === value;
    }
    countAll(): number {
        return this.tickets().length;
    }
    countSpace(spaceId: number): number {
        return this.tickets().filter((t) => t.spaceID === spaceId).length;
    }
    onToolbarKeydown(ev: KeyboardEvent): void {
        const order = this.filterOrder();
        if (order.length <= 1) {
            return;
        }
        let idx = order.indexOf(this.selectedFilter());
        if (idx < 0) {
            idx = 0;
        }
        if (ev.key === 'ArrowRight' || ev.key === 'ArrowDown') {
            ev.preventDefault();
            this.pick(order[Math.min(order.length - 1, idx + 1)]);
        }
        else if (ev.key === 'ArrowLeft' || ev.key === 'ArrowUp') {
            ev.preventDefault();
            this.pick(order[Math.max(0, idx - 1)]);
        }
        else if (ev.key === 'Home') {
            ev.preventDefault();
            this.pick(order[0]);
        }
        else if (ev.key === 'End') {
            ev.preventDefault();
            this.pick(order[order.length - 1]);
        }
    }
    spaceHeadline(s: TechnicianAssignedSpaceRow): string {
        return (s.name ?? '').trim() || `Spazio #${s.spaceID}`;
    }
    spaceMeta(s: TechnicianAssignedSpaceRow): string {
        const code = (s.officeCode ?? '').trim();
        return code ? `Codice ufficio ${code}` : `ID ${s.spaceID}`;
    }
}
