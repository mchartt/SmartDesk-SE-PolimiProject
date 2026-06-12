export function shouldAttachJwtToRequest(reqUrl: string, apiUrl: string): boolean {
    const trimmedApi = apiUrl.trim().replace(/\/+$/, '');
    if (!trimmedApi) {
        return false;
    }
    if (reqUrl.startsWith(trimmedApi)) {
        return true;
    }
    let apiParsed: URL;
    try {
        apiParsed = new URL(trimmedApi);
    }
    catch {
        return false;
    }
    const apiOrigin = apiParsed.origin;
    const apiPathBase = apiParsed.pathname.replace(/\/+$/, '') || '/';
    const pathOnly = (reqUrl.split('?')[0] ?? reqUrl).trim();
    try {
        const reqParsed = new URL(pathOnly);
        if (reqParsed.origin !== apiOrigin) {
            return false;
        }
        const p = reqParsed.pathname.replace(/\/+$/, '') || '/';
        return p === apiPathBase || p.startsWith(`${apiPathBase}/`);
    }
    catch {
        const p = pathOnly.startsWith('/') ? pathOnly.split('?')[0].replace(/\/+$/, '') || '/' : `/${pathOnly}`;
        return p === apiPathBase || p.startsWith(`${apiPathBase}/`);
    }
}
export function extractRequestPathname(reqUrl: string): string {
    const raw = (reqUrl.split('?')[0] ?? '').trim();
    try {
        return new URL(raw).pathname.replace(/\/+$/, '') || '/';
    }
    catch {
        const p = raw.startsWith('/') ? raw : `/${raw}`;
        return p.replace(/\/+$/, '') || '/';
    }
}
export function isPublicAuthRequestUrl(reqUrl: string): boolean {
    const path = extractRequestPathname(reqUrl);
    return (/\/auth\/login$/i.test(path) ||
        /\/auth\/register\/host$/i.test(path) ||
        /\/auth\/register$/i.test(path) ||
        /\/auth\/refresh$/i.test(path));
}
export function isAuthEndpointUrl(reqUrl: string): boolean {
    try {
        const u = new URL(reqUrl);
        return u.pathname.includes('/auth/');
    }
    catch {
        return reqUrl.includes('/auth/');
    }
}
