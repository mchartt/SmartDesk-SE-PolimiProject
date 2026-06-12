import { OpeningHoursDayPayload, WorkerSpaceClosure } from '../../../core/services/booking.service';
import { localCalendarDateIsoFromDate } from '../../../core/utils/date.util';
import {
    SLOT_COUNT,
    SLOT_LABELS,
    minutesToHm,
    parseHmToMins
} from './desk-slot-grid.util';

export const OPENING_HOURS_DAY_KEYS = [
    'SUNDAY',
    'MONDAY',
    'TUESDAY',
    'WEDNESDAY',
    'THURSDAY',
    'FRIDAY',
    'SATURDAY'
] as const;

export type OpeningHoursDayKey = (typeof OPENING_HOURS_DAY_KEYS)[number];

export type OfficeHoursBannerViewModel = {
    show: boolean;
    variant: 'open' | 'closed' | 'default';
    kicker: string;
    range: string;
    sub: string;
};

export type BookingHoursBlockedNoticeViewModel = {
    show: boolean;
    title: string;
    detail: string;
};

const HIDDEN_BANNER: OfficeHoursBannerViewModel = {
    show: false,
    variant: 'open',
    kicker: '',
    range: '',
    sub: ''
};

const HIDDEN_NOTICE: BookingHoursBlockedNoticeViewModel = {
    show: false,
    title: '',
    detail: ''
};

export function dayOfWeekIndexFromIsoDate(iso: string): number {
    const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(iso ?? '').trim());
    if (!m) {
        return new Date().getDay();
    }
    const y = Number(m[1]);
    const mo = Number(m[2]);
    const d = Number(m[3]);
    if (![y, mo, d].every((n) => Number.isFinite(n))) {
        return new Date().getDay();
    }
    return new Date(y, mo - 1, d, 12, 0, 0).getDay();
}

export function openingHoursDayKeyForTargetDate(targetDate: string): OpeningHoursDayKey {
    return OPENING_HOURS_DAY_KEYS[dayOfWeekIndexFromIsoDate(targetDate)];
}

export function nowMinutesLocal(now: Date): number {
    return now.getHours() * 60 + now.getMinutes();
}

export function targetDateDayHuman(targetDate: string): string {
    const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(targetDate ?? '').trim());
    const date = m
        ? new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]), 12, 0, 0)
        : new Date();
    const dowLong = date.toLocaleDateString('it-IT', { weekday: 'long' });
    return `${dowLong.charAt(0).toUpperCase()}${dowLong.slice(1)}`;
}

export function officeOpeningBoundsForTargetDate(
    openingHours: Record<string, OpeningHoursDayPayload> | undefined,
    targetDate: string
): [number, number] | null {
    const day = openingHours?.[openingHoursDayKeyForTargetDate(targetDate)] as {
        closed?: boolean;
        open?: string;
        close?: string;
    } | undefined;
    if (day?.closed) {
        return null;
    }
    const openRaw = parseHmToMins(day?.open ?? '');
    const closeRaw = parseHmToMins(day?.close ?? '');
    const open = Math.max(8 * 60, Number.isFinite(openRaw) ? openRaw : 8 * 60);
    const close = Math.min(20 * 60, Number.isFinite(closeRaw) ? closeRaw : 20 * 60);
    return close - open >= 30 ? [open, close] : null;
}

export function effectiveBookingBoundsForTargetDate(
    openingHours: Record<string, OpeningHoursDayPayload> | undefined,
    targetDate: string,
    now: Date
): [number, number] | null {
    const bounds = officeOpeningBoundsForTargetDate(openingHours, targetDate);
    if (!bounds) {
        return null;
    }
    const [open, close] = bounds;
    if (targetDate !== localCalendarDateIsoFromDate(now)) {
        return bounds;
    }
    const nowMins = nowMinutesLocal(now);
    if (nowMins < open || nowMins >= close) {
        return null;
    }
    const nextFutureGrid = Math.floor(nowMins / 30) * 30 + 30;
    const start = Math.max(open, nextFutureGrid);
    return close - start >= 30 ? [start, close] : null;
}

export function computeOfficeClosedMask(
    openingHours: Record<string, OpeningHoursDayPayload> | undefined,
    targetDate: string
): boolean[] {
    if (!openingHours) {
        return Array(SLOT_COUNT).fill(false);
    }
    const dayKey = openingHoursDayKeyForTargetDate(targetDate);
    const day = openingHours[dayKey] as {
        closed?: boolean;
        open?: string;
        close?: string;
    } | undefined;
    if (!day) {
        return Array(SLOT_COUNT).fill(false);
    }
    if (day.closed) {
        return Array(SLOT_COUNT).fill(true);
    }
    const openRaw = parseHmToMins(day.open ?? '');
    const closeRaw = parseHmToMins(day.close ?? '');
    const open = Number.isFinite(openRaw) ? openRaw : 8 * 60;
    const close = Number.isFinite(closeRaw) ? closeRaw : 20 * 60;
    return SLOT_LABELS.map((label) => {
        const slotStart = parseHmToMins(label);
        const slotEnd = slotStart + 30;
        if (!Number.isFinite(slotStart)) {
            return true;
        }
        return slotStart < open || slotEnd > close;
    });
}

export function bookingHoursBlockedNotice(params: {
    step2Done: boolean;
    selectedClosure: WorkerSpaceClosure | null;
    openingHours: Record<string, OpeningHoursDayPayload> | undefined;
    targetDate: string;
    now: Date;
    bookingHoursAvailable: boolean;
}): BookingHoursBlockedNoticeViewModel {
    const { step2Done, selectedClosure, openingHours, targetDate, now, bookingHoursAvailable } = params;
    if (!step2Done) {
        return HIDDEN_NOTICE;
    }
    if (selectedClosure) {
        const reason = (selectedClosure.reason ?? '').trim();
        return {
            show: true,
            title: 'Ufficio chiuso in questa data',
            detail: reason
                ? `Chiusura straordinaria: ${reason}. Scegli un altro giorno per prenotare.`
                : 'Chiusura straordinaria per questa sede. Scegli un altro giorno per prenotare.'
        };
    }
    const dayKey = openingHoursDayKeyForTargetDate(targetDate);
    const dayHuman = targetDateDayHuman(targetDate);
    const day = openingHours?.[dayKey] as {
        closed?: boolean;
        open?: string;
        close?: string;
    } | undefined;
    if (day?.closed) {
        return {
            show: true,
            title: 'Ufficio chiuso',
            detail: `${dayHuman} la sede non accetta prenotazioni. Seleziona un altro giorno nel calendario.`
        };
    }
    const officeBounds = officeOpeningBoundsForTargetDate(openingHours, targetDate);
    if (!officeBounds) {
        return {
            show: true,
            title: 'Ufficio chiuso',
            detail: `${dayHuman} non è possibile prenotare in questa sede. Scegli un altro giorno.`
        };
    }
    const [open, close] = officeBounds;
    const openStr = minutesToHm(open);
    const closeStr = minutesToHm(close);
    if (targetDate === localCalendarDateIsoFromDate(now)) {
        const nowMins = nowMinutesLocal(now);
        const nowStr = minutesToHm(nowMins);
        if (nowMins < open) {
            return {
                show: true,
                title: 'Ufficio ancora chiuso',
                detail: `Sono le ${nowStr}: la sede apre alle ${openStr} e chiude alle ${closeStr}. Le prenotazioni per oggi saranno possibili dall’orario di apertura.`
            };
        }
        if (nowMins >= close) {
            return {
                show: true,
                title: 'Ufficio chiuso per oggi',
                detail: `Sono le ${nowStr}: la sede ha chiuso alle ${closeStr} (orario ${openStr}–${closeStr}). Non è possibile prenotare per oggi: scegli un altro giorno.`
            };
        }
        if (!effectiveBookingBoundsForTargetDate(openingHours, targetDate, now)) {
            return {
                show: true,
                title: 'Nessuna fascia prenotabile',
                detail: `Mancano meno di 30 minuti alla chiusura (${closeStr}). Non è possibile avviare una nuova prenotazione per oggi.`
            };
        }
    }
    if (!bookingHoursAvailable) {
        return {
            show: true,
            title: 'Prenotazione non disponibile',
            detail: `Per ${dayHuman} non ci sono fasce orarie prenotabili in questa sede (${openStr}–${closeStr}).`
        };
    }
    return HIDDEN_NOTICE;
}

export function officeHoursBanner(params: {
    step2Done: boolean;
    hasSelectedSpace: boolean;
    openingHours: Record<string, OpeningHoursDayPayload> | undefined;
    targetDate: string;
}): OfficeHoursBannerViewModel {
    const { step2Done, hasSelectedSpace, openingHours, targetDate } = params;
    if (!step2Done || !hasSelectedSpace) {
        return HIDDEN_BANNER;
    }
    const dayKey = openingHoursDayKeyForTargetDate(targetDate);
    const dayHuman = targetDateDayHuman(targetDate);
    if (!openingHours) {
        return {
            show: true,
            variant: 'default',
            kicker: 'Fascia prenotabile',
            range: '08:00 – 20:00',
            sub: 'Orari sede non impostati · limite standard piattaforma'
        };
    }
    const day = openingHours[dayKey];
    if (!day) {
        return {
            show: true,
            variant: 'default',
            kicker: 'Fascia prenotabile',
            range: '08:00 – 20:00',
            sub: `${dayHuman} · orario sede non definito per questo giorno`
        };
    }
    if (day.closed) {
        return {
            show: true,
            variant: 'closed',
            kicker: 'Sede chiusa',
            range: '',
            sub: dayHuman
        };
    }
    const openMins = day.open ? parseHmToMins(day.open) : 8 * 60;
    const closeMins = day.close ? parseHmToMins(day.close) : 20 * 60;
    const openOk = Number.isFinite(openMins);
    const closeOk = Number.isFinite(closeMins);
    const openStr = openOk ? minutesToHm(openMins) : '08:00';
    const closeStr = closeOk ? minutesToHm(closeMins) : '20:00';
    return {
        show: true,
        variant: 'open',
        kicker: 'Orario sede',
        range: `${openStr} – ${closeStr}`,
        sub: dayHuman
    };
}
