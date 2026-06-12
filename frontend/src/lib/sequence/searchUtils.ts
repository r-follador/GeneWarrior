import {reverseComplement} from './sequenceUtils';

export interface SearchHit {
    entryId: string;
    start: number;
    end: number;
    strand?: 'forward' | 'reverse';
    row?: number;
}

const dnaQueryMatches: Record<string, Set<string>> = {
    A: new Set(['A']),
    C: new Set(['C']),
    G: new Set(['G']),
    T: new Set(['T']),
    U: new Set(['T']),
    R: new Set(['G', 'A']),
    Y: new Set(['T', 'C']),
    K: new Set(['G', 'T']),
    M: new Set(['A', 'C']),
    S: new Set(['G', 'C']),
    W: new Set(['A', 'T']),
    B: new Set(['G', 'T', 'C', 'K', 'S', 'Y']),
    D: new Set(['G', 'A', 'T', 'R', 'K', 'W']),
    H: new Set(['A', 'T', 'C', 'W', 'M', 'Y']),
    V: new Set(['G', 'A', 'C', 'R', 'S', 'M']),
    N: new Set(['A', 'C', 'G', 'T', 'R', 'Y', 'K', 'M', 'S', 'W', 'B', 'D', 'H', 'V', 'N'])
};

const proteinQueryMatches: Record<string, Set<string>> = {
    B: new Set(['N', 'D']),
    Z: new Set(['Q', 'E']),
    J: new Set(['I', 'L'])
};

export function sanitizeDnaQuery(query: string) {
    return query.replace(/[\s\-.0-9]/g, '').toUpperCase().replaceAll('U', 'T').replace(/[^ACGTRYSWKMBDHVN]/g, 'N');
}

export function sanitizeProteinQuery(query: string) {
    return query.replace(/[\s\-.0-9]/g, '').toUpperCase().replace(/[^ABCDEFGHIKLMNPQRSTVWYZ*]/g, 'X');
}

export function findDnaHits(entryId: string, sequence: string, query: string, bothStrands: boolean): SearchHit[] {
    const sanitizedQuery = sanitizeDnaQuery(query);
    if (!sanitizedQuery) return [];
    const hits = findHits(sequence.toUpperCase().replaceAll('U', 'T'), sanitizedQuery, dnaMatches).map((start) => ({
        entryId,
        start,
        end: start + sanitizedQuery.length - 1,
        strand: 'forward' as const
    }));
    if (!bothStrands) return hits;
    const reverseQuery = reverseComplement(sanitizedQuery);
    if (reverseQuery === sanitizedQuery) return hits;
    return [
        ...hits,
        ...findHits(sequence.toUpperCase().replaceAll('U', 'T'), reverseQuery, dnaMatches).map((start) => ({
            entryId,
            start,
            end: start + sanitizedQuery.length - 1,
            strand: 'reverse' as const
        }))
    ];
}

export function findProteinHits(entryId: string, sequence: string, query: string): SearchHit[] {
    const sanitizedQuery = sanitizeProteinQuery(query);
    if (!sanitizedQuery) return [];
    return findHits(sequence.toUpperCase(), sanitizedQuery, proteinMatches).map((start) => ({
        entryId,
        start,
        end: start + sanitizedQuery.length - 1
    }));
}

function findHits(sequence: string, query: string, matcher: (queryChar: string, targetChar: string) => boolean) {
    if (query.length > sequence.length) return [];
    const hits: number[] = [];
    for (let start = 0; start <= sequence.length - query.length; start += 1) {
        let matched = true;
        for (let offset = 0; offset < query.length; offset += 1) {
            if (!matcher(query[offset], sequence[start + offset])) {
                matched = false;
                break;
            }
        }
        if (matched) hits.push(start);
    }
    return hits;
}

function dnaMatches(queryChar: string, targetChar: string) {
    return queryChar === targetChar || (dnaQueryMatches[queryChar]?.has(targetChar) ?? queryChar === 'N');
}

function proteinMatches(queryChar: string, targetChar: string) {
    if (queryChar === targetChar) return true;
    if (queryChar === 'X') return targetChar !== '-' && targetChar !== '*';
    return proteinQueryMatches[queryChar]?.has(targetChar) ?? false;
}
