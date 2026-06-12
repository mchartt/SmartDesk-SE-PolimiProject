import { ChangeDetectionStrategy, Component, inject, input, output, TemplateRef, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbModalModule, MdbModalRef, MdbModalService } from 'mdb-angular-ui-kit/modal';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { Desk } from '../../../../../../core/models';
import { WorkerSpace } from '../../../../../../core/services/booking.service';
import { SdModalHeaderComponent } from '../../../../../../shared/components/sd-modal-header/sd-modal-header.component';
import { SdIconComponent } from '../../../../../../shared/icons/sd-icon/sd-icon.component';

@Component({
    selector: 'app-scope-selector',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        FormsModule,
        MdbFormsModule,
        MdbModalModule,
        MdbRippleModule,
        SdIconComponent,
        SdModalHeaderComponent
    ],
    templateUrl: './scope-selector.component.html',
    styleUrl: './scope-selector.component.scss'
})
export class ScopeSelectorComponent {
    readonly spaces = input<WorkerSpace[]>([]);
    readonly cities = input<string[]>([]);
    readonly officeDesks = input<Desk[]>([]);
    readonly selectedCity = input('');
    readonly selectedSpaceId = input<number | null>(null);
    readonly selectedRoomKey = input<number | null>(null);
    readonly step1Done = input(false);
    readonly step2Done = input(false);
    readonly isLoading = input(false);
    readonly slotsLoading = input(false);

    readonly cityChosen = output<string>();
    readonly officeChosen = output<number>();
    readonly roomChosen = output<number | null>();

    @ViewChild('cityModal')
    private cityModal!: TemplateRef<unknown>;
    @ViewChild('officeModal')
    private officeModal!: TemplateRef<unknown>;
    @ViewChild('roomModal')
    private roomModal!: TemplateRef<unknown>;

    protected citySearch = '';
    protected officeSearch = '';
    protected roomSearch = '';

    private cityModalRef: MdbModalRef<unknown> | null = null;
    private officeModalRef: MdbModalRef<unknown> | null = null;
    private roomModalRef: MdbModalRef<unknown> | null = null;

    private readonly modalService = inject(MdbModalService);

    protected get filteredSpaces(): WorkerSpace[] {
        if (!this.selectedCity()) {
            return [];
        }
        return this.spaces().filter((space) => space.city === this.selectedCity());
    }

    protected getSpaceRating(spaceID: number | null): number | null {
        if (spaceID === null) {
            return null;
        }
        return this.spaces().find((s) => s.spaceID === spaceID)?.averageReviewRating ?? null;
    }

    protected hasSpaceReviewAverage(rating: number | null | undefined): boolean {
        return rating != null && Number.isFinite(rating);
    }

    protected formatSpaceReviewAverage(rating: number): string {
        return rating.toFixed(1);
    }

    protected spaceReviewRatingCaption(rating: number): string {
        return `${this.formatSpaceReviewAverage(rating)} / 5 - Media Recensioni`;
    }

    private deskRoomKey(desk: Desk): number {
        return desk.roomID ?? 0;
    }

    protected get roomsInOffice(): Array<{
        roomKey: number;
        label: string;
        deskCount: number;
    }> {
        const map = new Map<number, {
            label: string;
            count: number;
        }>();
        for (const d of this.officeDesks()) {
            const key = this.deskRoomKey(d);
            const label = key === 0
                ? 'Area generale'
                : `${d.roomName || 'Sala'}${d.roomCode ? ` · ${d.roomCode}` : ''}`;
            const prev = map.get(key);
            if (prev) {
                prev.count++;
            }
            else {
                map.set(key, { label, count: 1 });
            }
        }
        return Array.from(map.entries())
            .map(([roomKey, v]) => ({ roomKey, label: v.label, deskCount: v.count }))
            .sort((a, b) => a.label.localeCompare(b.label, 'it'));
    }

    protected get filteredRoomsForModal(): Array<{
        roomKey: number;
        label: string;
        deskCount: number;
    }> {
        const q = this.roomSearch.trim().toLowerCase();
        const rows = this.roomsInOffice;
        if (!q) {
            return rows;
        }
        return rows.filter((r) => r.label.toLowerCase().includes(q));
    }

    protected get selectedRoomLabel(): string {
        if (this.selectedRoomKey() === null) {
            return 'Tutte le sale';
        }
        const hit = this.roomsInOffice.find((r) => r.roomKey === this.selectedRoomKey());
        return hit?.label ?? 'Sala';
    }

    protected get filteredCities(): string[] {
        const q = this.citySearch.trim().toLowerCase();
        if (!q) {
            return this.cities();
        }
        return this.cities().filter((city) => city.toLowerCase().includes(q));
    }

    protected get filteredOfficesForModal(): WorkerSpace[] {
        const q = this.officeSearch.trim().toLowerCase();
        const spaces = this.filteredSpaces;
        if (!q) {
            return spaces;
        }
        return spaces.filter((s) => `${s.name} ${s.city} ${s.officeCode ?? ''}`.toLowerCase().includes(q));
    }

    protected openCityModal(): void {
        this.citySearch = '';
        this.cityModalRef = this.modalService.open(this.cityModal, {
            modalClass: 'modal-dialog-centered'
        });
    }

    protected closeCityModal(): void {
        this.cityModalRef?.close();
        this.cityModalRef = null;
    }

    protected chooseCity(city: string): void {
        this.closeCityModal();
        this.cityChosen.emit(city);
    }

    protected openOfficeModal(): void {
        if (!this.step1Done()) {
            return;
        }
        this.officeSearch = '';
        this.officeModalRef = this.modalService.open(this.officeModal, {
            modalClass: 'modal-dialog-centered modal-lg'
        });
    }

    protected closeOfficeModal(): void {
        this.officeModalRef?.close();
        this.officeModalRef = null;
    }

    protected chooseOffice(spaceId: number): void {
        this.closeOfficeModal();
        this.officeChosen.emit(spaceId);
    }

    protected openRoomModal(): void {
        if (!this.step2Done()) {
            return;
        }
        this.roomSearch = '';
        this.roomModalRef = this.modalService.open(this.roomModal, {
            modalClass: 'modal-dialog-centered modal-lg'
        });
    }

    protected closeRoomModal(): void {
        this.roomModalRef?.close();
        this.roomModalRef = null;
    }

    protected chooseRoom(roomKey: number | null): void {
        this.closeRoomModal();
        this.roomChosen.emit(roomKey);
    }
}
