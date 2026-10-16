import {describe, expect, it} from 'vitest';
import type {AlignmentDto, ProjectDto, SequenceDto} from '../api/types';
import {blocks, clustal, clustalFiles, exportEntries, fasta, filenameStem} from './formats';
import {annotationFragments, highlightAt} from './printLayout';

const dna: SequenceDto = {id: 'dna', name: 'DNA sample', kind: 'DNA', sequence: 'ACGT'.repeat(37), marks: []};
const protein: SequenceDto = {id: 'protein', name: 'Protein', kind: 'PROTEIN', sequence: 'MAKX*', marks: []};
const alignment: AlignmentDto = {
    id: 'alignment', name: 'Alignment', kind: 'DNA', score: 0, marks: [],
    sequences: [
        {name: 'Same name', value: 'A-CG'.repeat(32)},
        {name: 'Same name', value: 'ATCG'.repeat(32)},
        {name: 'Same_name_2', value: 'ATCG'.repeat(32)}
    ], consensus: '* **'.repeat(32)
};
const project: ProjectDto = {
    name: 'Test',
    sequences: [dna, protein],
    alignments: [alignment],
    entryOrder: ['protein', 'alignment', 'dna']
};

describe('export scope', () => {
    it('uses workspace order, not selection order, without mutating the project', () => {
        const original = JSON.stringify(project);
        expect(exportEntries(project, ['dna', 'protein']).map(entry => entry.value.id)).toEqual(['protein', 'dna']);
        expect(JSON.stringify(project)).toBe(original);
    });
    it('defaults to all entries and can explicitly override selected entries', () => {
        expect(exportEntries(project, []).map(entry => entry.value.id)).toEqual(project.entryOrder);
        expect(exportEntries(project, ['dna'], 'all').map(entry => entry.value.id)).toEqual(project.entryOrder);
    });
    it('does not fall back to unrelated entries for a stale selection or an empty workspace', () => {
        expect(exportEntries(project, ['missing'])).toEqual([]);
        expect(exportEntries({...project, sequences: [], alignments: [], entryOrder: []}, [])).toEqual([]);
    });
});

describe('FASTA', () => {
    it('preserves names, complete residues and ordering, and wraps at 70 characters', () => {
        const result = fasta([protein, dna]).split('\n');
        expect(result.slice(0, 3)).toEqual(['>Protein', 'MAKX*', '>DNA sample']);
        expect(result.slice(3, 6).map(row => row.length)).toEqual([70, 70, 8]);
        expect(result.slice(3, 6).join('')).toBe(dna.sequence);
        expect(result.at(-1)).toBe('');
    });
    it('prevents sequence names from injecting new FASTA records', () => {
        expect(fasta([{
            ...protein,
            name: '  name\r\n>injected\t\u0000 title  '
        }])).toBe('>name >injected title\nMAKX*\n');
        expect(fasta([])).toBe('');
    });
});

describe('CLUSTAL', () => {
    it('writes aligned blocks, unique identifiers, gaps and a correctly padded consensus', () => {
        const result = clustal(alignment);
        expect(result).toMatch(/^CLUSTAL .*\n\n/);
        const sections = result.trimEnd().split('\n\n').slice(1);
        expect(sections).toHaveLength(3);
        const names = ['Same_name', 'Same_name_2', 'Same_name_2_2'];
        names.forEach((name, index) => {
            const rows = sections.map(section => section.split('\n')[index]);
            expect(rows.every(row => row.startsWith(name + ' '))).toBe(true);
            expect(rows.map(row => row.slice(15)).join('')).toBe(alignment.sequences[index].value);
        });
        expect(sections.map(section => section.split('\n')[3].slice(15)).join('')).toBe(alignment.consensus);
        expect(sections[0].split('\n')[0].slice(15)).toHaveLength(60);
    });
    it('creates distinct safe filenames even for duplicate names and case collisions', () => {
        const files = clustalFiles([alignment, alignment, {...alignment, name: 'ALIGNMENT'}, {
            ...alignment,
            name: '../Alignment_2'
        }]);
        expect(files.map(file => file.name)).toEqual(['Alignment.aln', 'Alignment_2.aln', 'ALIGNMENT_3.aln', 'Alignment_2_2.aln']);
    });
});

describe('filenames and print layout', () => {
    it('removes paths, control characters, reserved filenames and unsafe extensions', () => {
        expect(filenameStem('../../My project\n<script>.html')).toBe('My-project-script-html');
        expect(filenameStem('CON')).toBe('_CON');
        expect(filenameStem('..')).toBe('genewarrior');
        expect(filenameStem('a'.repeat(200))).toHaveLength(80);
    });
    it('splits annotation arrows across lines while keeping direction and boundaries', () => {
        const mark = {
            start: 50,
            end: 125,
            color: '#ffcc00',
            annotation: 'Domain',
            type: 'ANNOTATION' as const,
            direction: 'REVERSE' as const
        };
        const fragments = blocks(150).flatMap(block => annotationFragments([mark], block.start, block.end));
        expect(fragments.map(({start, end, continuesBefore, continuesAfter}) => ({
            start,
            end,
            continuesBefore,
            continuesAfter
        }))).toEqual([
            {start: 50, end: 60, continuesBefore: false, continuesAfter: true},
            {start: 0, end: 60, continuesBefore: true, continuesAfter: true},
            {start: 0, end: 5, continuesBefore: true, continuesAfter: false}
        ]);
        expect(fragments.every(fragment => fragment.mark.direction === 'REVERSE')).toBe(true);
    });
    it('preserves background highlights without treating arrows as background colors', () => {
        expect(highlightAt([{start: 0, end: 2, color: '#ffcc00'}], 1)).toBe('#ffcc00');
        expect(highlightAt([{start: 0, end: 2, color: '#ffcc00'}], 2)).toBeUndefined();
        expect(highlightAt([{start: 0, end: 2, color: '#ffcc00', type: 'ANNOTATION'}], 1)).toBeUndefined();
        expect(highlightAt([{start: 0, end: 2, color: 'red;display:none'}], 1)).toBe('#d9e2e2');
    });
});
