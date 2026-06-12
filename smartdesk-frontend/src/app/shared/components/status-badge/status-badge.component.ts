import { NgClass } from '@angular/common';
import { Component, Input } from '@angular/core';
@Component({
    selector: 'app-status-badge',
    standalone: true,
    imports: [NgClass],
    templateUrl: './status-badge.component.html',
    styleUrl: './status-badge.component.scss'
})
export class StatusBadgeComponent {
    @Input({ required: true })
    public status = '';
    protected badgeClass(): string {
        const value = this.status.toUpperCase();
        if (value.includes('RESOLVED') || value.includes('APPROVED') || value.includes('ACTIVE')) {
            return 'badge-success';
        }
        if (value.includes('PENDING') || value.includes('OPEN') || value.includes('IN_PROGRESS')) {
            return 'badge-warning text-dark';
        }
        return 'badge-secondary';
    }
    protected displayLabel(): string {
        const value = this.status.toUpperCase();
        if (value.includes('APPROVED'))
            return 'Approvato';
        if (value.includes('PENDING'))
            return 'In attesa';
        if (value.includes('RESOLVED'))
            return 'Risolto';
        if (value.includes('OPEN'))
            return 'Aperta';
        if (value.includes('IN_PROGRESS'))
            return 'In corso';
        if (value.includes('ACTIVE'))
            return 'Attivo';
        return this.status;
    }
}
