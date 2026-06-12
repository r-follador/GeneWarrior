import {describe, expect, it} from 'vitest';
import {findDnaHits, findProteinHits, sanitizeDnaQuery, sanitizeProteinQuery} from './searchUtils';

describe('searchUtils', () => {
    it('sanitizes legacy DNA and protein queries', () => {
        expect(sanitizeDnaQuery('acg u?')).toBe('ACGTN');
        expect(sanitizeProteinQuery('bjz?*')).toBe('BXZX*');
    });

    it('finds DNA hits with degenerate query bases', () => {
        expect(findDnaHits('s1', 'AAACCCGGGTTT', 'NNN', false).map((hit) => hit.start)).toContain(0);
        expect(findDnaHits('s1', 'AAACCCGGGTTT', 'RYK', false)).toEqual([{
            entryId: 's1',
            start: 8,
            end: 10,
            strand: 'forward'
        }]);
    });

    it('finds reverse strand DNA hits', () => {
        expect(findDnaHits('s1', 'AAACCC', 'GGG', true)).toEqual([{
            entryId: 's1',
            start: 3,
            end: 5,
            strand: 'reverse'
        }]);
    });

    it('finds protein hits with degenerate query symbols', () => {
        expect(findProteinHits('p1', 'MNDQEILV', 'BZJ')).toEqual([{entryId: 'p1', start: 2, end: 4}]);
        expect(findProteinHits('p1', 'MND*', 'X')).toEqual([
            {entryId: 'p1', start: 0, end: 0},
            {entryId: 'p1', start: 1, end: 1},
            {entryId: 'p1', start: 2, end: 2}
        ]);
    });

    it('allows callers to add alignment row metadata', () => {
        expect(findProteinHits('alignment', 'GAVFHK', 'VF').map((hit) => ({...hit, row: 1}))).toEqual([
            {entryId: 'alignment', start: 2, end: 3, row: 1}
        ]);
    });
});
