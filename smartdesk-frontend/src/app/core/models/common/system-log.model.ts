export interface SystemLog {
    logID: number;
    severity: 'CRITICAL' | 'ERROR' | 'WARN' | 'INFO' | 'DEBUG' | 'AUDIT';
    action: string;
    timestamp: string;
    ipAddress: string;
    actorRole: string;
}
