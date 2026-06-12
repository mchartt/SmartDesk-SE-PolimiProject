import {
    AbstractUser,
    Booking,
    BookingRequest,
    Desk,
    DeskStateCode,
    Host,
    OpeningHoursDay,
    Review,
    SearchCriteria,
    Space,
    SysAdmin,
    Technician,
    Ticket,
    TicketStatus,
    UserRole,
    Worker,
} from '../models';
type UserJson = {
    id: number;
    email: string;
    surname: string;
    name: string;
    active?: boolean;
    roleType: UserRole;
    description?: string;
    nameStructure?: string;
    specialisation?: string;
};
type DeskJson = {
    id?: number;
    deskID?: number;
    code?: string;
    building: string;
    roomID?: number;
    roomCode?: string;
    roomName?: string;
    amenities: string[];
    stateCode?: DeskStateCode;
    currentState?: string;
    spaceID?: number;
    spaceAverageRating?: number | null;
};
type BookingJson = {
    bookingID: number;
    deskID: number;
    version?: number;
    deskCode?: string;
    spaceName?: string;
    city?: string;
    buildingName?: string;
    bookingCode?: string;
    workerID?: number;
    workerEmail?: string;
    workerName?: string;
    startTime: string;
    endTime: string;
    status?: string;
    bookedDay?: string | null;
};
type TicketJson = {
    ticketID?: number;
    deskID: number;
    description: string;
    status?: TicketStatus;
    technicianNote?: string;
};
type ReviewJson = {
    reviewID?: number;
    bookingID?: number;
    bookingCode?: string;
    hostID?: number;
    workerID?: number;
    rating: number;
    spaceID: number;
    comment: string;
    spaceName?: string;
    city?: string;
    spaceOfficeCode?: string;
    createdAt?: string | null;
    workerGivenName?: string;
    workerFamilyName?: string;
    workerEmail?: string;
    seenByHost?: boolean;
};
type SpaceJson = {
    spaceID: number;
    description: string;
    hostID: number;
    name: string;
    city?: string;
    address?: string;
    approved?: boolean;
    officeCode?: string;
    openingHours?: Record<string, {
        closed?: boolean;
        open?: string | null;
        close?: string | null;
    }>;
    desks?: DeskJson[];
};
export class ModelFactory {
    public static createUser(payload: unknown): AbstractUser {
        const rawUser = payload as UserJson;
        const roleType = ModelFactory.normalizeRole(rawUser.roleType);
        switch (roleType) {
            case 'HOST':
                return new Host(rawUser.id, rawUser.email, rawUser.surname, rawUser.name, rawUser.nameStructure ?? '', rawUser.description ?? '', rawUser.active ?? true);
            case 'TECHNICIAN':
                return new Technician(rawUser.id, rawUser.email, rawUser.surname, rawUser.name, rawUser.specialisation ?? '', rawUser.active ?? true);
            case 'SYS_ADMIN':
                return new SysAdmin(rawUser.id, rawUser.email, rawUser.surname, rawUser.name, rawUser.active ?? true);
            default:
                return new Worker(rawUser.id, rawUser.email, rawUser.surname, rawUser.name, rawUser.description ?? '', rawUser.active ?? true);
        }
    }
    private static normalizeRole(roleType: UserRole | string): UserRole {
        const normalized = String(roleType ?? '')
            .trim()
            .toUpperCase();
        if (normalized === 'ADMIN')
            return 'SYS_ADMIN';
        if (normalized === 'HOST' || normalized === 'TECHNICIAN' || normalized === 'SYS_ADMIN') {
            return normalized;
        }
        return 'WORKER';
    }
    public static createDesk(payload: DeskJson): Desk {
        const deskId = (payload.id ?? payload.deskID ?? 0) as number;
        const code = payload.code ?? '';
        const stateCode = (payload.stateCode ?? payload.currentState ?? 'AVAILABLE') as DeskStateCode;
        const rid = payload.roomID;
        const room = rid != null && rid > 0
            ? {
                id: rid,
                code: payload.roomCode ?? '',
                name: payload.roomName ?? ''
            }
            : null;
        return new Desk(deskId, code, payload.building, payload.amenities ?? [], stateCode, room, payload.spaceID ?? null, payload.spaceAverageRating ?? null);
    }
    public static createSearchCriteria(payload: Partial<SearchCriteria>): SearchCriteria {
        const p = payload as Partial<{
            targetDate: string;
            requiredAmenities: string[];
            includeMaintenance: boolean;
            slotStart: string | null;
            slotEnd: string | null;
        }>;
        return new SearchCriteria(p.targetDate ?? '', p.requiredAmenities ?? [], p.includeMaintenance ?? false, p.slotStart ?? null, p.slotEnd ?? null);
    }
    public static createBookingRequest(payload: Partial<BookingRequest>): BookingRequest {
        return new BookingRequest(payload.deskID ?? 0, payload.end ?? '');
    }
    public static createBooking(payload: BookingJson): Booking {
        return new Booking(payload.bookingID, payload.deskID, payload.startTime, payload.endTime, payload.status, payload.bookedDay ?? undefined, payload.deskCode ?? '', payload.spaceName ?? '', payload.buildingName ?? '', payload.city ?? '', payload.bookingCode ?? '', payload.workerID ?? null, payload.workerEmail ?? '', payload.workerName ?? '', payload.version ?? 0);
    }
    public static createTicket(payload: TicketJson): Ticket {
        return new Ticket(payload.ticketID ?? 0, payload.deskID, payload.description, payload.status ?? 'OPEN', payload.technicianNote ?? '');
    }
    public static createReview(payload: ReviewJson): Review {
        return new Review(payload.reviewID ?? 0, payload.bookingID ?? 0, payload.hostID ?? 0, payload.workerID ?? 0, payload.rating, payload.spaceID, payload.comment, payload.spaceName ?? '', payload.city ?? '', payload.spaceOfficeCode ?? '', payload.createdAt ?? null, payload.workerGivenName ?? '', payload.workerFamilyName ?? '', payload.workerEmail ?? '', payload.seenByHost === true, payload.bookingCode ?? '');
    }
    public static createSpace(payload: SpaceJson): Space {
        const desks = (payload.desks ?? []).map((desk) => ModelFactory.createDesk(desk));
        const openingHours = ModelFactory.parseSpaceOpeningHours(payload.openingHours);
        return new Space(payload.spaceID, payload.description, payload.hostID, payload.name, payload.city ?? '', payload.address ?? '', payload.approved ?? false, desks, payload.officeCode ?? '', openingHours);
    }
    private static parseSpaceOpeningHours(raw: SpaceJson['openingHours']): Record<string, OpeningHoursDay> | null {
        if (!raw || typeof raw !== 'object')
            return null;
        const out: Record<string, OpeningHoursDay> = {};
        for (const [dayKey, value] of Object.entries(raw)) {
            if (!value || typeof value !== 'object')
                continue;
            out[dayKey] = {
                closed: !!value.closed,
                open: String(value.open ?? '').trim(),
                close: String(value.close ?? '').trim()
            };
        }
        return Object.keys(out).length ? out : null;
    }
}
