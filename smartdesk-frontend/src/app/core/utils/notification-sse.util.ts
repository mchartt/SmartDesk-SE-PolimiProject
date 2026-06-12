export type UnreadCountSsePayload = {
    unreadCount: number;
};
export type NotificationCreatedSsePayload = {
    notificationID?: number;
    message?: string;
    kind?: string | null;
    actorName?: string | null;
    actorSurname?: string | null;
    actorEmail?: string | null;
    actorRating?: number | null;
    read?: boolean;
    createdAt?: string | null;
};
export type NotificationUpdatedSsePayload = {
    notificationID: number;
    read: boolean;
};
export type NotificationSseParseResult = {
    unreadCounts: number[];
    created: NotificationCreatedSsePayload[];
    updated: NotificationUpdatedSsePayload[];
    allMarkedRead: boolean;
    remainder: string;
};
export function consumeNotificationSseBuffer(buffer: string): NotificationSseParseResult {
    const unreadCounts: number[] = [];
    const created: NotificationCreatedSsePayload[] = [];
    const updated: NotificationUpdatedSsePayload[] = [];
    let allMarkedRead = false;
    const blocks = buffer.split(/\r?\n\r?\n/);
    const remainder = blocks.pop() ?? '';
    for (const block of blocks) {
        const parsed = parseSseBlock(block);
        if (!parsed) {
            continue;
        }
        if (parsed.event === 'unread-count' && parsed.unreadCount !== null) {
            unreadCounts.push(parsed.unreadCount);
        }
        else if (parsed.event === 'notification-created' && parsed.created) {
            created.push(parsed.created);
        }
        else if (parsed.event === 'notification-updated' && parsed.updated) {
            updated.push(parsed.updated);
        }
        else if (parsed.event === 'notifications-all-read') {
            allMarkedRead = true;
        }
    }
    return { unreadCounts, created, updated, allMarkedRead, remainder };
}
export function consumeUnreadCountSseBuffer(buffer: string): {
    unreadCounts: number[];
    remainder: string;
} {
    const result = consumeNotificationSseBuffer(buffer);
    return { unreadCounts: result.unreadCounts, remainder: result.remainder };
}
type ParsedSseBlock = {
    event: 'unread-count';
    unreadCount: number | null;
    created?: undefined;
    updated?: undefined;
} | {
    event: 'notification-created';
    created: NotificationCreatedSsePayload;
    unreadCount?: undefined;
    updated?: undefined;
} | {
    event: 'notification-updated';
    updated: NotificationUpdatedSsePayload;
    unreadCount?: undefined;
    created?: undefined;
} | {
    event: 'notifications-all-read';
};
function parseSseBlock(block: string): ParsedSseBlock | null {
    const lines = block.split(/\r?\n/);
    let eventName: string | null = null;
    const dataLines: string[] = [];
    for (const line of lines) {
        if (line.startsWith(':')) {
            continue;
        }
        if (line.startsWith('event:')) {
            eventName = line.slice('event:'.length).trim();
            continue;
        }
        if (line.startsWith('data:')) {
            dataLines.push(line.slice('data:'.length).trimStart());
        }
    }
    const raw = dataLines.join('\n').trim();
    if (!raw) {
        return null;
    }
    try {
        const json = JSON.parse(raw) as Record<string, unknown>;
        if (eventName === 'notification-created') {
            return { event: 'notification-created', created: json as NotificationCreatedSsePayload };
        }
        if (eventName === 'notification-updated') {
            const id = Number(json['notificationID']);
            const read = json['read'] === true;
            if (!Number.isFinite(id)) {
                return null;
            }
            return { event: 'notification-updated', updated: { notificationID: id, read } };
        }
        if (eventName === 'notifications-all-read') {
            return { event: 'notifications-all-read' };
        }
        if (eventName === 'unread-count' || eventName === null) {
            const n = Number((json as UnreadCountSsePayload).unreadCount);
            return {
                event: 'unread-count',
                unreadCount: Number.isFinite(n) ? Math.max(0, Math.floor(n)) : null
            };
        }
        return null;
    }
    catch {
        return null;
    }
}
