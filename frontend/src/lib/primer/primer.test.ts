import {describe, expect, it} from 'vitest';
import {get} from 'svelte/store';
import type {PrimerDesignDto, PrimerOligo} from '../api/types';
import {defaultPrimerSettings, pairSequences, reverseComplement} from './model';
import {parsePrimerDesign, parsePrimerSettings} from './validation';
import {parseProject} from '../project/projectValidation';
import {createProjectStore} from '../stores/projectStore';
import {exportEntries, fasta} from '../export/formats';

export function primerFixture(): PrimerDesignDto {
    const sequence = 'AACCGTTAGCGTACGATGCA'.repeat(10);
    const oligo = (start: number, end: number, reverse = false): PrimerOligo => ({
        start, end,
        sequence: reverse ? reverseComplement(sequence.slice(start, end)) : sequence.slice(start, end),
        strand: reverse ? 'REVERSE' : 'FORWARD', tm: 60, gc: 50, penalty: 0.6
    });
    return {
        id: 'primers',
        name: 'Primer design',
        version: '2.6.1',
        source: {id: 'source', name: 'Template', sequence, kind: 'DNA', marks: []},
        settings: {...defaultPrimerSettings(sequence.length, {start: 80, end: 100}), includeProbe: true},
        pairs: [{
            index: 0,
            left: oligo(2, 22),
            right: oligo(151, 171, true),
            probe: oligo(60, 80),
            productSize: 169,
            penalty: 1.2
        }],
        explanations: ['PAIR: considered 10, ok 1']
    };
}

describe('primer design persistence and exports', () => {
    it('validates probe and reverse primer coordinates', () => {
        expect(parsePrimerDesign(primerFixture())).toEqual(primerFixture());
        const bad = primerFixture();
        bad.pairs[0].right.start++;
        expect(() => parsePrimerDesign(bad)).toThrow();
        bad.pairs[0].probe = null;
        expect(() => parsePrimerDesign(bad)).toThrow();
    });
    it('rejects malformed settings, targets and non-finite values', () => {
        const settings = defaultPrimerSettings(200);
        expect(() => parsePrimerSettings({...settings, productMin: 250}, 200)).toThrow();
        expect(() => parsePrimerSettings({...settings, target: {start: 100, end: 10}}, 200)).toThrow();
        expect(() => parsePrimerSettings({...settings, saltMonovalent: NaN}, 200)).toThrow();
    });
    it('preserves design snapshots across project load and source deletion', () => {
        const design = primerFixture();
        const project = {name: 'Project', sequences: [design.source], alignments: [], entryOrder: ['source']};
        const store = createProjectStore(project);
        store.addPrimerDesignAfter('source', design);
        store.addPrimerDesignAfter('source', design);
        expect(get(store).entryOrder).toEqual(['source', 'primers']);
        store.removeEntries(['source']);
        expect(parseProject(JSON.parse(JSON.stringify(get(store)))).primerDesigns?.[0]).toEqual(design);
        store.removeEntries(['primers']);
        expect(get(store).entryOrder).toEqual([]);
    });
    it('exports all oligos in their synthesis direction and selects designs for print', () => {
        const design = primerFixture();
        const oligos = pairSequences(design, design.pairs[0]);
        expect(oligos).toHaveLength(3);
        expect(oligos[1].sequence).toBe(design.pairs[0].right.sequence);
        expect(fasta(oligos)).toContain('pair 1 Probe');
        const entries = exportEntries({
            name: 'Design',
            sequences: [],
            alignments: [],
            primerDesigns: [design],
            entryOrder: ['primers']
        }, []);
        expect(entries[0].type).toBe('primers');
    });
    it('preserves a successful empty design with explanations', () => {
        const design = {...primerFixture(), pairs: []};
        expect(parsePrimerDesign(design).pairs).toEqual([]);
    });
});
