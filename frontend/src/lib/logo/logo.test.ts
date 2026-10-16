import {describe, expect, it} from 'vitest';
import {get} from 'svelte/store';
import type {SequenceLogoDto} from '../api/types';
import {logoLayout} from './layout';
import {glyphs} from './glyphs';
import {decodeProject, parseLogo, parseProject} from '../project/projectValidation';
import {createProjectStore, exampleProject} from '../stores/projectStore';
import {exportEntries} from '../export/formats';

function fixture(): SequenceLogoDto {
    return {
        id: 'logo-test', name: 'Logo', sourceAlignmentId: 'example-alignment', kind: 'DNA', mode: 'FREQUENCY',
        method: 'uniform-uncorrected-v1', sequenceCount: 4,
        columns: [{
            position: 1, observations: 4, gaps: 0, ignored: 0, information: 2,
            symbols: [...'ACGT'].map(residue => ({
                residue, count: residue === 'A' ? 4 : 0,
                frequency: residue === 'A' ? 1 : 0, bits: residue === 'A' ? 2 : 0
            }))
        }]
    };
}

describe('logo layout', () => {
    it('renders conserved columns with bounded positive geometry without mutating data', () => {
        const logo = fixture();
        const before = JSON.stringify(logo);
        const line = logoLayout(logo)[0];
        expect(line.maximum).toBe(1);
        expect(line.columns[0].glyphs).toEqual([expect.objectContaining({residue: 'A', height: 140, y: 28})]);
        expect(logoLayout({...logo, mode: 'INFORMATION'})[0].maximum).toBe(2);
        expect(JSON.stringify(logo)).toBe(before);
    });
    it('leaves zero-information and all-gap columns empty, while retaining positions and coverage', () => {
        const logo = fixture();
        logo.columns[0] = {
            position: 1, observations: 4, gaps: 0, ignored: 0, information: 0,
            symbols: [...'ACGT'].map(residue => ({residue, count: 1, frequency: .25, bits: 0}))
        };
        expect(logoLayout({...logo, mode: 'INFORMATION'})[0].columns[0].glyphs).toEqual([]);
        expect(logoLayout(logo)[0].columns[0].glyphs).toHaveLength(4);
        logo.columns[0] = {
            ...logo.columns[0], observations: 0, gaps: 4,
            symbols: logo.columns[0].symbols.map(symbol => ({...symbol, count: 0, frequency: 0}))
        };
        const result = logoLayout(logo)[0].columns[0];
        expect(result.position).toBe(1);
        expect(result.coverage).toBe(0);
        expect(result.glyphs).toEqual([]);
    });
    it('wraps on exact column boundaries and includes outlines for all canonical residues', () => {
        const logo = fixture();
        logo.columns = Array.from({length: 21}, (_, index) => ({...logo.columns[0], position: index + 1}));
        expect(logoLayout(logo, 10).map(line => line.columns.map(column => column.position))).toEqual([
            [1, 2, 3, 4, 5, 6, 7, 8, 9, 10], [11, 12, 13, 14, 15, 16, 17, 18, 19, 20], [21]
        ]);
        for (const residue of 'ACDEFGHIKLMNPQRSTVWY') expect(glyphs[residue]).toMatch(/^M/);
    });
});

describe('logo persistence', () => {
    it('migrates old projects without logos and preserves logos after source deletion', () => {
        const original = exampleProject();
        delete original.logos;
        expect(parseProject(original).logos).toEqual([]);
        const store = createProjectStore(original);
        store.addLogoAfter('example-alignment', fixture());
        expect(get(store).entryOrder.slice(-2)).toEqual(['example-alignment', 'logo-test']);
        store.replaceLogo({...fixture(), mode: 'INFORMATION'});
        store.removeEntries(['example-alignment']);
        const restored = decodeProject(JSON.stringify(get(store)));
        expect(restored.logos?.[0].mode).toBe('INFORMATION');
        expect(exportEntries(restored, ['logo-test'])[0].type).toBe('logo');
        store.removeEntries(['logo-test']);
        expect(get(store).logos).toEqual([]);
    });
    it('rejects unsafe, nonfinite, inconsistent and oversized logo data', () => {
        expect(parseLogo(fixture())).toEqual(fixture());
        const badValues = [NaN, Infinity, -1, .5];
        for (const value of badValues) {
            const logo = fixture();
            logo.columns[0].symbols[0].frequency = value;
            expect(() => parseLogo(logo)).toThrow();
        }
        expect(() => parseLogo({...fixture(), method: 'unknown'})).toThrow();
        const logo = fixture();
        logo.columns[0].symbols[0].residue = '<svg>';
        expect(() => parseLogo(logo)).toThrow();
        const project = exampleProject();
        project.logos = [{...fixture(), id: project.alignments[0].id}];
        expect(() => parseProject(project)).toThrow('Duplicate');
    });
});
