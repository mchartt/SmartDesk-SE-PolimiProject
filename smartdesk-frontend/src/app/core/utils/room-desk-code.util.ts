export function nextDeskCodeForRoom(roomCode: string, deskCodesInRoom: readonly string[]): string {
    const prefix = roomCode.trim();
    if (!prefix) {
        return '';
    }
    const suffix = new RegExp(`^${escapeRegExp(prefix)}(\\d+)$`, 'i');
    let max = 0;
    for (const code of deskCodesInRoom) {
        const match = suffix.exec(code.trim());
        if (match) {
            max = Math.max(max, Number.parseInt(match[1], 10));
        }
    }
    return `${prefix}${max + 1}`;
}
function escapeRegExp(value: string): string {
    return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
