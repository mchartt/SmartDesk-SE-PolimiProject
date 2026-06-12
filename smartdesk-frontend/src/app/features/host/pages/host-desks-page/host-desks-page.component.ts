import { Component, DestroyRef, computed, effect, inject, signal, OnInit, HostListener } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { SdIconComponent } from '../../../../shared/icons/sd-icon/sd-icon.component';
import { Desk, Space } from '../../../../core/models';
import { HostRoom, HostService } from '../../../../core/services/host.service';
import { ConfirmModalService } from '../../../../shared/services/confirm-modal.service';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import {
    buildDeskSections,
    canManageDesks as utilCanManageDesks,
    DeskRoomSection,
    maintenanceDesksFrom,
    sectionKey as utilSectionKey
} from './host-desks.util';
import { HostDesksModalStore } from './host-desks-modal.store';
import { DeskFormModalComponent } from './components/desk-form-modal/desk-form-modal.component';
import { RoomFormModalComponent } from './components/room-form-modal/room-form-modal.component';
import { HostPresetCreateModalComponent } from './components/host-preset-create-modal/host-preset-create-modal.component';
import { HostPresetLibraryModalComponent } from './components/host-preset-library-modal/host-preset-library-modal.component';
import { RoomBrowseModalComponent } from './components/room-browse-modal/room-browse-modal.component';

export type { AmenityPreset, DeskRoomSection, HostCustomAmenityPreset } from './host-desks.util';

@Component({
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        MdbRippleModule,
        EmptyStateComponent,
        SdIconComponent,
        RouterLink,
        DeskFormModalComponent,
        RoomFormModalComponent,
        HostPresetCreateModalComponent,
        HostPresetLibraryModalComponent,
        RoomBrowseModalComponent
    ],
    providers: [HostDesksModalStore],
    templateUrl: './host-desks-page.component.html',
    styleUrl: './host-desks-page.component.scss'
})
export class HostDesksPageComponent implements OnInit {
    private readonly route = inject(ActivatedRoute);
    private readonly destroyRef = inject(DestroyRef);
    private readonly hostService = inject(HostService);
    private readonly confirmService = inject(ConfirmModalService);
    private readonly modal = inject(HostDesksModalStore);

    protected title = computed(() => (this.route.snapshot.data['title'] as string) ?? 'Postazioni');
    protected spaces = signal<Space[]>([]);
    protected desks = signal<Desk[]>([]);
    protected rooms = signal<HostRoom[]>([]);
    protected selectedSpaceId = signal<number | null>(null);
    protected loadingSpaces = signal(false);
    protected loadingDesks = signal(false);
    protected maintenanceDesks = signal<Desk[]>([]);
    protected deskActionInProgress = signal<number | null>(null);
    protected errorMsg = '';

    protected readonly selectedSpace = computed(
        () => this.spaces().find((s) => s.spaceID === this.selectedSpaceId()) || null
    );
    protected readonly deskSections = computed(() => buildDeskSections(this.rooms(), this.desks()));
    protected readonly canManageDesks = computed(() =>
        utilCanManageDesks(this.selectedSpace()?.approved, this.rooms().length)
    );

    protected readonly hostCustomPresets = this.modal.hostCustomPresets;
    protected readonly loadingPresets = this.modal.loadingPresets;
    protected readonly presetsMutating = this.modal.presetsMutating;
    protected readonly presetSectionError = this.modal.presetSectionError;
    protected readonly hostPresetSummaryText = this.modal.hostPresetSummaryText;
    protected readonly anyModalOpen = this.modal.anyModalOpen;

    constructor() {
        effect(() => {
            const space = this.selectedSpace();
            if (!space?.approved) {
                this.desks.set([]);
                this.rooms.set([]);
                return;
            }
            this.reloadSpaceContext(space.spaceID);
        });
    }

    public ngOnInit(): void {
        this.modal.bindHost({
            desks: this.desks,
            rooms: this.rooms,
            selectedSpaceId: this.selectedSpaceId,
            selectedSpace: () => this.selectedSpace(),
            canManageDesks: () => this.canManageDesks(),
            deskSections: () => this.deskSections(),
            pageError: () => this.errorMsg,
            onPageError: (message) => {
                this.errorMsg = message;
            },
            reloadSpaceContext: (spaceId) => this.reloadSpaceContext(spaceId)
        });
        this.loadingSpaces.set(true);
        this.hostService
            .getSpaces()
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (spaces) => {
                    this.spaces.set(spaces);
                    this.loadingSpaces.set(false);
                    if (spaces.length && this.selectedSpaceId() === null) {
                        this.selectedSpaceId.set(spaces[0].spaceID);
                    }
                },
                error: (err: Error) => {
                    this.errorMsg = err.message;
                    this.loadingSpaces.set(false);
                }
            });
    }

    @HostListener('document:keydown.escape')
    protected onEscapeCloseModals(): void {
        this.modal.onEscapeCloseModals();
    }

    private reloadSpaceContext(spaceId: number): void {
        this.errorMsg = '';
        this.loadingDesks.set(true);
        const requested = spaceId;
        this.loadDesksByState(requested);
        this.hostService
            .getRooms(requested)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (rooms) => {
                    if (this.selectedSpaceId() !== requested) {
                        return;
                    }
                    this.rooms.set(rooms);
                    this.loadingDesks.set(false);
                },
                error: (err: Error) => {
                    if (this.selectedSpaceId() !== requested) {
                        return;
                    }
                    this.rooms.set([]);
                    this.errorMsg = err.message;
                    this.loadingDesks.set(false);
                }
            });
    }

    private loadDesksByState(spaceId: number): void {
        this.hostService
            .getDesks(spaceId)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (desks) => {
                    this.desks.set(desks);
                    this.maintenanceDesks.set(maintenanceDesksFrom(desks));
                },
                error: (err: Error) => {
                    this.errorMsg = err.message;
                }
            });
    }

    protected decommissionMaintenanceDesk(desk: Desk): void {
        if (this.deskActionInProgress() !== null) {
            return;
        }
        this.confirmService
            .confirm({
                title: 'Dismetti postazione',
                message: `Sei sicuro di voler dismettere definitivamente la postazione ${desk.code}?`,
                confirmLabel: 'Dismetti',
                cancelLabel: 'Annulla',
                variant: 'danger'
            })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((ok) => {
                if (!ok) {
                    return;
                }
                this.deskActionInProgress.set(desk.id);
                this.hostService
                    .decommissionDesk(desk.id)
                    .pipe(takeUntilDestroyed(this.destroyRef))
                    .subscribe({
                        next: () => {
                            this.deskActionInProgress.set(null);
                            const spaceId = this.selectedSpaceId();
                            if (spaceId != null) {
                                this.reloadSpaceContext(spaceId);
                            }
                        },
                        error: (err: Error) => {
                            this.errorMsg = err.message;
                            this.deskActionInProgress.set(null);
                        }
                    });
            });
    }

    protected openDeskModal(): void {
        this.modal.openDeskModal();
    }

    protected openRoomModal(room?: HostRoom): void {
        this.modal.openRoomModal(room);
    }

    protected openRoomBrowseModal(section: DeskRoomSection): void {
        this.modal.openRoomBrowseModal(section);
    }

    protected openHostPresetLibraryModal(): void {
        this.modal.openHostPresetLibraryModal();
    }

    protected openHostPresetCreateModal(): void {
        this.modal.openHostPresetCreateModal();
    }

    protected deleteRoom(room: HostRoom): void {
        this.modal.deleteRoom(room);
    }

    protected trackRoomSection(_idx: number, item: DeskRoomSection): string {
        return utilSectionKey(item);
    }
}
