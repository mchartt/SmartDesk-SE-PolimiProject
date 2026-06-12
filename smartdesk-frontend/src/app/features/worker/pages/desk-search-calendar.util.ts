const IT_DOW_UPPER = ['DOM', 'LUN', 'MAR', 'MER', 'GIO', 'VEN', 'SAB'] as const;
export interface CalendarDay {
    isoString: string;
    dayNum: string;
    dowIt: string;
    isPast: boolean;
    isToday: boolean;
    isBeyondHorizon: boolean;
    selectable: boolean;
}
function startOfDay(date: Date): Date {
    const d = new Date(date);
    d.setHours(0, 0, 0, 0);
    return d;
}
function mondayOfWeekContaining(date: Date): Date {
    const d = startOfDay(date);
    const day = d.getDay();
    const diff = day === 0 ? -6 : 1 - day;
    d.setDate(d.getDate() + diff);
    return d;
}
function maxBookableDay(from: Date): Date {
    const t = startOfDay(from);
    t.setDate(t.getDate() + 7);
    return t;
}
export function buildCalendarDays(todayIso: string): CalendarDay[] {
    const [y, m, d] = todayIso.split('-').map((v) => Number(v));
    const today = startOfDay(new Date(y, m - 1, d));
    const horizon = maxBookableDay(today);
    const mon = mondayOfWeekContaining(today);
    const days: CalendarDay[] = [];
    for (let i = 0; i < 14; i++) {
        const cell = new Date(mon);
        cell.setDate(mon.getDate() + i);
        const year = cell.getFullYear();
        const month = String(cell.getMonth() + 1).padStart(2, '0');
        const dayStr = String(cell.getDate()).padStart(2, '0');
        const isoString = `${year}-${month}-${dayStr}`;
        const isPast = cell.getTime() < today.getTime();
        const isToday = isoString === todayIso;
        const isBeyondHorizon = cell.getTime() > horizon.getTime();
        days.push({
            isoString,
            dayNum: String(cell.getDate()),
            dowIt: IT_DOW_UPPER[cell.getDay()],
            isPast,
            isToday,
            isBeyondHorizon,
            selectable: !isPast && !isBeyondHorizon
        });
    }
    return days;
}
