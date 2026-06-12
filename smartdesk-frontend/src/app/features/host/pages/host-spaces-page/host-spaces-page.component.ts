import { CommonModule } from '@angular/common';
import { Component, ElementRef, HostListener, ViewChild, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { take } from 'rxjs';
import { StatusBadgeComponent } from '../../../../shared/components/status-badge/status-badge.component';
import { SdIconComponent } from '../../../../shared/icons/sd-icon/sd-icon.component';
import { HostService } from '../../../../core/services/host.service';
import { ItalianComuniService, ItalianComuneRecord } from '../../../../core/services/italian-comuni.service';
import { Space } from '../../../../core/models';
import { ConfirmModalService } from '../../../../shared/services/confirm-modal.service';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { MdbCollapseModule } from 'mdb-angular-ui-kit/collapse';
import { mergeOpeningHoursFromSpace, openingHoursSummary, type OpeningHoursRow } from './host-spaces.util';
import { HOST_SPACE_DESCRIPTION_MAX_LEN, HostSpacesModalStore } from './host-spaces-modal.store';
import { ClosureCalendarComponent } from './components/closure-calendar/closure-calendar.component';
import { OpeningHoursEditorComponent } from './components/opening-hours-editor/opening-hours-editor.component';

@Component({
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        SdIconComponent,
        StatusBadgeComponent,
        MdbRippleModule,
        MdbCollapseModule,
        OpeningHoursEditorComponent,
        ClosureCalendarComponent
    ],
    providers: [HostSpacesModalStore],
    templateUrl: './host-spaces-page.component.html',
    styleUrl: './host-spaces-page.component.scss'
})
export class HostSpacesPageComponent implements OnInit {
    @ViewChild('cityCombo')
    protected cityComboRef?: ElementRef<HTMLElement>;
    @ViewChild(OpeningHoursEditorComponent)
    private openingHoursEditor?: OpeningHoursEditorComponent;
    @ViewChild(ClosureCalendarComponent)
    private closureCalendar?: ClosureCalendarComponent;

    private readonly route = inject(ActivatedRoute);
    private readonly destroyRef = inject(DestroyRef);
    private readonly hostService = inject(HostService);
    private readonly confirmService = inject(ConfirmModalService);
    private readonly italianComuni = inject(ItalianComuniService);
    private readonly modal = inject(HostSpacesModalStore);
    private modalCityBaseline = '';
    protected cityPanelOpen = false;
    protected citySuggestions: ItalianComuneRecord[] = [];
    protected cityHighlightIdx = -1;
    protected readonly title = computed(() => (this.route.snapshot.data['title'] as string) ?? 'Spazi');
    protected spaces = signal<Space[]>([]);
    protected loading = signal(false);
    protected errorMsg = '';
    protected successMsg = '';
    protected readonly descriptionMaxLen = HOST_SPACE_DESCRIPTION_MAX_LEN;
    protected readonly isModalOpen = this.modal.isModalOpen;

    protected get editingId(): number | null {
        return this.modal.editingId;
    }
    protected get name(): string {
        return this.modal.name;
    }
    protected set name(value: string) {
        this.modal.name = value;
    }
    protected get address(): string {
        return this.modal.address;
    }
    protected set address(value: string) {
        this.modal.address = value;
    }
    protected get city(): string {
        return this.modal.city;
    }
    protected set city(value: string) {
        this.modal.city = value;
    }
    protected get description(): string {
        return this.modal.description;
    }
    protected set description(value: string) {
        this.modal.description = value;
    }
    protected get openingHoursRows(): OpeningHoursRow[] {
        return this.modal.openingHoursRows;
    }
    protected set openingHoursRows(rows: OpeningHoursRow[]) {
        this.modal.openingHoursRows = rows;
    }
    protected get hoursQuickAction(): 'weekdays' | 'allDays' | null {
        return this.modal.hoursQuickAction;
    }
    protected set hoursQuickAction(value: 'weekdays' | 'allDays' | null) {
        this.modal.hoursQuickAction = value;
    }

    public ngOnInit(): void {
        this.modal.bindHost({
            onError: (message) => {
                this.errorMsg = message;
            },
            onSuccess: (message) => {
                this.successMsg = message;
            },
            onSpacesListReload: () => this.load(),
            validateCitySelection: (cityTrim) => this.validateCitySelection(cityTrim),
            confirmRemoveClosures: (message) =>
                this.confirmService.confirm({
                    title: 'Rimuovi chiusure',
                    message,
                    confirmLabel: 'Rimuovi',
                    cancelLabel: 'Annulla',
                    variant: 'info'
                })
        });
        this.load();
    }

    @HostListener('document:keydown.escape')
    protected onEscapeCloseModal(): void {
        if (this.cityPanelOpen) {
            this.cityPanelOpen = false;
            return;
        }
        if (this.openingHoursEditor?.closeTimePickerIfOpen()) {
            return;
        }
        if (this.closureCalendar?.closeCalMenusIfOpen()) {
            return;
        }
        if (this.isModalOpen()) {
            this.closeModal();
        }
    }

    @HostListener('document:click', ['$event'])
    protected onDocumentClick(ev: MouseEvent): void {
        if (!this.isModalOpen()) {
            return;
        }
        const t = ev.target;
        if (!(t instanceof Node)) {
            return;
        }
        if (!this.cityPanelOpen) {
            return;
        }
        const el = this.cityComboRef?.nativeElement;
        if (el && el.contains(t)) {
            return;
        }
        this.cityPanelOpen = false;
    }

    protected load(): void {
        this.errorMsg = '';
        this.loading.set(true);
        this.hostService.getSpaces().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
            next: (rows) => {
                this.spaces.set(rows);
                this.loading.set(false);
            },
            error: (err: Error) => {
                this.spaces.set([]);
                this.errorMsg = err.message;
                this.loading.set(false);
            }
        });
    }

    protected openModal(space?: Space): void {
        this.errorMsg = '';
        this.successMsg = '';
        this.cityPanelOpen = false;
        this.citySuggestions = [];
        this.cityHighlightIdx = -1;
        this.prefetchComuniCatalogue();
        if (space) {
            this.modalCityBaseline = space.city.trim();
            this.modal.openForEdit(space);
        }
        else {
            this.modalCityBaseline = '';
            this.modal.openForCreate();
        }
    }

    protected closeModal(): void {
        this.openingHoursEditor?.closeTimePickerIfOpen();
        this.closureCalendar?.closeCalMenusIfOpen();
        this.modal.close();
    }

    protected onModalBackdropClick(): void {
        this.openingHoursEditor?.closeTimePickerIfOpen();
        if (!this.modal.isDirty()) {
            this.closeModal();
        }
    }

    protected onModalDialogClick(event: MouseEvent): void {
        event.stopPropagation();
        this.openingHoursEditor?.closeTimePickerIfOpen();
    }

    protected saveSpace(): void {
        this.errorMsg = '';
        this.successMsg = '';
        const trySave = (): void => {
            const result = this.modal.saveSpace();
            if (!result.ok && result.error) {
                this.errorMsg = result.error;
            }
        };
        if (this.italianComuni.isLoaded()) {
            trySave();
            return;
        }
        this.italianComuni
            .ensureLoaded()
            .pipe(take(1), takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: () => trySave(),
            error: (err: Error) => (this.errorMsg = err.message?.trim() || 'Impossibile caricare l’elenco dei comuni. Riprova.')
        });
    }

    protected editSpace(space: Space): void {
        this.openModal(space);
    }

    protected deleteSpace(spaceId: number): void {
        const target = this.spaces().find((s) => s.spaceID === spaceId);
        const label = target?.name ? `“${target.name}”` : 'questo spazio';
        this.confirmService
            .confirm({
            title: 'Elimina spazio',
            message: `Sei sicuro di voler eliminare ${label}? Verranno rimosse anche tutte le postazioni associate. L’azione non può essere annullata.`,
            confirmLabel: 'Elimina spazio',
            cancelLabel: 'Annulla',
            variant: 'danger'
        })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((confirmed) => {
            if (!confirmed) {
                return;
            }
            this.errorMsg = '';
            this.hostService
                .deleteSpace(spaceId)
                .pipe(takeUntilDestroyed(this.destroyRef))
                .subscribe({
                next: () => {
                    this.spaces.update((prev) => prev.filter((s) => s.spaceID !== spaceId));
                    if (this.modal.matchesEditingSpace(spaceId)) {
                        this.closeModal();
                    }
                },
                error: (err: Error) => (this.errorMsg = err.message)
            });
        });
    }

    protected openingHoursSummary(space: Space): string | null {
        return openingHoursSummary(space);
    }

    protected openingHoursGrid(space: Space): OpeningHoursRow[] {
        return mergeOpeningHoursFromSpace(space);
    }

    protected onOpeningHoursRowsChange(rows: OpeningHoursRow[]): void {
        this.openingHoursRows = rows;
    }

    protected onOpeningHoursEdited(): void {
        this.modal.touchOpeningHoursCalendar();
    }

    protected onCityFocus(): void {
        this.cityPanelOpen = true;
        this.withComuniLoaded(() => {
            this.refreshCitySuggestions();
            if (this.citySuggestions.length && this.cityHighlightIdx < 0) {
                this.cityHighlightIdx = 0;
            }
        });
    }

    protected onCityQueryChange(value: string): void {
        this.city = value;
        this.cityPanelOpen = true;
        this.withComuniLoaded(() => {
            this.refreshCitySuggestions();
            this.cityHighlightIdx = this.citySuggestions.length ? 0 : -1;
        });
    }

    protected onCityBlur(): void {
        queueMicrotask(() => {
            const resolved = this.italianComuni.tryResolveUniqueNome(this.city);
            if (resolved) {
                this.city = resolved;
            }
        });
    }

    protected onCityKeydown(ev: KeyboardEvent): void {
        if (ev.key === 'ArrowDown' || ev.key === 'ArrowUp') {
            if (!this.cityPanelOpen) {
                this.cityPanelOpen = true;
                this.withComuniLoaded(() => {
                    this.refreshCitySuggestions();
                    if (this.citySuggestions.length) {
                        this.cityHighlightIdx = 0;
                    }
                });
            }
        }
        if (!this.cityPanelOpen) {
            return;
        }
        if (ev.key === 'ArrowDown') {
            ev.preventDefault();
            if (!this.citySuggestions.length) {
                return;
            }
            this.cityHighlightIdx = (this.cityHighlightIdx + 1) % this.citySuggestions.length;
        }
        else if (ev.key === 'ArrowUp') {
            ev.preventDefault();
            if (!this.citySuggestions.length) {
                return;
            }
            this.cityHighlightIdx =
                this.cityHighlightIdx <= 0 ? this.citySuggestions.length - 1 : this.cityHighlightIdx - 1;
        }
        else if (ev.key === 'Enter') {
            const pick = this.citySuggestions[this.cityHighlightIdx];
            if (pick) {
                ev.preventDefault();
                this.selectCitySuggestion(pick);
            }
        }
        else if (ev.key === 'Escape') {
            ev.preventDefault();
            this.cityPanelOpen = false;
        }
    }

    protected selectCitySuggestion(row: ItalianComuneRecord): void {
        this.city = row.label;
        this.cityPanelOpen = false;
        this.citySuggestions = [];
        this.cityHighlightIdx = -1;
    }

    protected comuniCatalogReady(): boolean {
        return this.italianComuni.isLoaded();
    }

    protected readonly cityListboxId = 'host-space-city-listbox';

    private prefetchComuniCatalogue(): void {
        if (this.italianComuni.isLoaded()) {
            return;
        }
        this.italianComuni
            .ensureLoaded()
            .pipe(take(1), takeUntilDestroyed(this.destroyRef))
            .subscribe({ error: () => undefined });
    }

    private withComuniLoaded(done: () => void): void {
        if (this.italianComuni.isLoaded()) {
            done();
            return;
        }
        this.italianComuni
            .ensureLoaded()
            .pipe(take(1), takeUntilDestroyed(this.destroyRef))
            .subscribe({
            next: () => done(),
            error: (err: Error) => (this.errorMsg = err.message?.trim() || 'Impossibile caricare l’elenco dei comuni.')
        });
    }

    private refreshCitySuggestions(): void {
        this.citySuggestions = this.italianComuni.filter(this.city, 14);
        if (this.cityHighlightIdx >= this.citySuggestions.length) {
            this.cityHighlightIdx = Math.max(0, this.citySuggestions.length - 1);
        }
    }

    private validateCitySelection(cityTrim: string): boolean {
        if (this.italianComuni.hasExactLabel(cityTrim)) {
            return true;
        }
        const uniq = this.italianComuni.tryResolveUniqueNome(cityTrim);
        if (uniq) {
            this.city = uniq;
            return true;
        }
        if (this.editingId !== null && cityTrim === this.modalCityBaseline) {
            return true;
        }
        this.errorMsg = 'Seleziona una città dall’elenco dei comuni italiani.';
        return false;
    }
}
