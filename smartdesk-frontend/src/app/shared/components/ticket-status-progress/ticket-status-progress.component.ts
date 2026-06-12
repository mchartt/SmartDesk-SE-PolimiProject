import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { ticketStatusLabel } from '../../../core/utils/ticket-status-display.util';
import { SdIconComponent } from '../../icons/sd-icon/sd-icon.component';
export type TicketStatusValue = 'OPEN' | 'IN_PROGRESS' | 'VERIFYING' | 'RESOLVED' | 'CLOSED' | string;
export type TicketProgressTone = 'yellow' | 'orange' | 'green' | 'purple' | 'cyan';
interface Step {
    key: string;
    labelLines: readonly string[];
    tone: TicketProgressTone;
    state: 'done' | 'current' | 'pending';
    current: boolean;
}
const TICKET_PROGRESS_STEPS: ReadonlyArray<{
    key: string;
    labelLines: readonly string[];
    tone: TicketProgressTone;
    index: number;
}> = [
    { key: 'OPEN', labelLines: ['In attesa'], tone: 'yellow', index: 1 },
    { key: 'IN_PROGRESS', labelLines: ['In lavorazione'], tone: 'cyan', index: 2 },
    { key: 'TECH_RESOLVED', labelLines: ['Riparato'], tone: 'purple', index: 3 },
    { key: 'VERIFYING', labelLines: ['Verifica host'], tone: 'orange', index: 4 },
    { key: 'CONFIRMED', labelLines: ['Risoluzione', 'confermata'], tone: 'green', index: 5 }
] as const;
const STEP_COUNT = TICKET_PROGRESS_STEPS.length;
@Component({
    selector: 'app-ticket-status-progress',
    standalone: true,
    imports: [CommonModule, SdIconComponent],
    templateUrl: './ticket-status-progress.component.html',
    styleUrl: './ticket-status-progress.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class TicketStatusProgressComponent {
    public readonly status = input.required<TicketStatusValue>();
    public readonly compact = input<boolean>(false);
    public readonly cardSide = input<boolean>(false);
    public readonly detail = input<boolean>(false);
    private readonly normalizedStatus = computed(() => (this.status() ?? '').toUpperCase());
    private readonly allDone = computed(() => {
        const u = this.normalizedStatus();
        return u === 'RESOLVED' || u === 'CLOSED';
    });
    private readonly currentIndex = computed(() => {
        const u = this.normalizedStatus();
        if (u === 'RESOLVED' || u === 'CLOSED') {
            return STEP_COUNT + 1;
        }
        if (u === 'VERIFYING') {
            return 4;
        }
        if (u === 'IN_PROGRESS') {
            return 2;
        }
        return 1;
    });
    protected readonly railColor = computed(() => {
        const tone = this.steps().find((s) => s.current)?.tone ??
            this.steps()
                .filter((s) => s.state === 'done')
                .at(-1)?.tone ??
            'orange';
        const map: Record<TicketProgressTone, string> = {
            yellow: '#ca8a04',
            orange: '#ea580c',
            green: '#16a34a',
            purple: '#9333ea',
            cyan: '#0891b2'
        };
        return map[tone];
    });
    protected readonly trackFillPercent = computed(() => {
        if (this.allDone()) {
            return 100;
        }
        const idx = this.currentIndex();
        const active = Math.min(Math.max(idx, 1), STEP_COUNT);
        if (active <= 1) {
            return 0;
        }
        return ((active - 1) / (STEP_COUNT - 1)) * 100;
    });
    protected readonly statusCaption = computed(() => ticketStatusLabel(this.status()));
    protected readonly steps = computed<Step[]>(() => {
        const current = this.currentIndex();
        const done = this.allDone();
        return TICKET_PROGRESS_STEPS.map(({ key, labelLines, tone, index }) => ({
            key,
            labelLines,
            tone,
            state: done ? 'done' : index < current ? 'done' : index === current ? 'current' : 'pending',
            current: !done && index === current
        }));
    });
}
