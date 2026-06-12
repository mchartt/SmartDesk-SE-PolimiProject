import { Component, computed, effect, ElementRef, input, model, output, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { TICKET_DESCRIPTION_MAX_LENGTH } from '../../../core/constants/ticket-form.constants';
import { buildTicketChatMessages, type TicketChatSource, type TicketChatViewer } from '../../../core/utils/ticket-chat.util';
import { formatShortDateTime } from '../../../core/utils/date.util';
import { SdIconComponent } from '../../icons/sd-icon/sd-icon.component';
@Component({
    selector: 'app-ticket-chat-panel',
    standalone: true,
    imports: [FormsModule, MdbFormsModule, MdbRippleModule, SdIconComponent],
    templateUrl: './ticket-chat-panel.component.html',
    styleUrl: './ticket-chat-panel.component.scss'
})
export class TicketChatPanelComponent {
    public readonly ticket = input.required<TicketChatSource>();
    public readonly viewer = input<TicketChatViewer>('worker');
    public readonly canComment = input(true);
    public readonly sending = input(false);
    public readonly commentError = input('');
    public readonly compact = input(false);
    public readonly relaxed = input(false);
    public readonly sectionLabel = input('Aggiornamenti');
    public readonly commentPlaceholder = input('Scrivi un aggiornamento…');
    public readonly emptyHint = input('Nessun messaggio ancora. Scrivi un aggiornamento per continuare la conversazione.');
    public readonly closedHint = input('Segnalazione chiusa: non è possibile aggiungere altri commenti.');
    public readonly commentDraft = model('');
    public readonly submitComment = output<void>();
    protected readonly maxLength = TICKET_DESCRIPTION_MAX_LENGTH;
    protected readonly messages = computed(() => buildTicketChatMessages(this.ticket(), this.viewer()));
    protected readonly messageCountLabel = computed(() => {
        const n = this.messages().length;
        return n === 1 ? '1 messaggio' : `${n} messaggi`;
    });
    private readonly chatLogEl = viewChild<ElementRef<HTMLElement>>('chatLog');
    public constructor() {
        effect(() => {
            const count = this.messages().length;
            if (count < 1)
                return;
            queueMicrotask(() => {
                const el = this.chatLogEl()?.nativeElement;
                if (el) {
                    el.scrollTop = el.scrollHeight;
                }
            });
        });
    }
    protected get commentLength(): number {
        return this.commentDraft().trim().length;
    }
    protected get canSubmit(): boolean {
        const len = this.commentLength;
        return len >= 1 && len <= this.maxLength && !this.sending();
    }
    protected formatWhen(iso: string | null | undefined): string {
        return formatShortDateTime(iso);
    }
    protected authorParts(label: string): {
        name: string;
        role: string;
    } | null {
        const idx = label.indexOf('·');
        if (idx < 0) {
            return null;
        }
        const name = label.slice(0, idx).trim();
        const role = label.slice(idx + 1).trim();
        if (!name || !role) {
            return null;
        }
        return { name, role };
    }
    protected onSubmit(): void {
        if (!this.canSubmit) {
            return;
        }
        this.submitComment.emit();
    }
}
