import type {AlignmentDto, ProjectDto, SequenceDto, SequenceLogoDto, PrimerDesignDto, GuideTreeDto} from '../api/types';

export type ExportEntry = { type: 'sequence'; value: SequenceDto } | { type: 'alignment'; value: AlignmentDto } | {
    type: 'logo';
    value: SequenceLogoDto
} | { type: 'primers'; value: PrimerDesignDto } | { type: 'tree'; value: GuideTreeDto };
export type ExportScope = 'auto' | 'all';

export function exportEntries(project: ProjectDto, selectedIds: string[], scope: ExportScope = 'auto'): ExportEntry[] {
    const entries = new Map<string, ExportEntry>();
    project.sequences.forEach(value => entries.set(value.id!, {type: 'sequence', value}));
    project.alignments.forEach(value => entries.set(value.id, {type: 'alignment', value}));
    project.logos?.forEach(value => entries.set(value.id, {type: 'logo', value}));
    project.primerDesigns?.forEach(value => entries.set(value.id, {type: 'primers', value}));
    project.trees?.forEach(value => entries.set(value.id, {type: 'tree', value}));
    const selection = new Set(selectedIds);
    return project.entryOrder
        .filter(id => scope === 'all' || selection.size === 0 || selection.has(id))
        .flatMap(id => entries.has(id) ? [entries.get(id)!] : []);
}

export function filenameStem(name: string): string {
    const stem = name.normalize('NFKD').replace(/[\u0300-\u036f]/g, '')
        .replace(/[^a-zA-Z0-9_-]+/g, '-').replace(/^-+|-+$/g, '').slice(0, 80) || 'genewarrior';
    return /^(con|prn|aux|nul|com[0-9]|lpt[0-9])$/i.test(stem) ? `_${stem}` : stem;
}

export function blocks(length: number, width = 60): { start: number; end: number }[] {
    return Array.from({length: Math.ceil(length / width)}, (_, index) => ({
        start: index * width, end: Math.min((index + 1) * width, length)
    }));
}

export function fasta(sequences: SequenceDto[]): string {
    return sequences.map(sequence => {
        const name = sequence.name.replace(/[\s\u0000-\u001f\u007f]+/g, ' ').trim() || 'Sequence';
        return `>${name}\n${blocks(sequence.sequence.length, 70)
            .map(block => sequence.sequence.slice(block.start, block.end)).join('\n')}\n`;
    }).join('');
}

function uniqueName(base: string, used: Set<string>): string {
    let candidate = base;
    for (let suffix = 2; used.has(candidate.toLowerCase()); suffix++) candidate = `${base}_${suffix}`;
    used.add(candidate.toLowerCase());
    return candidate;
}

export function clustal(alignment: AlignmentDto): string {
    const used = new Set<string>();
    const names = alignment.sequences.map((sequence, index) => uniqueName(
        sequence.name.replace(/[^a-zA-Z0-9_.-]+/g, '_').replace(/^_+|_+$/g, '') || `sequence_${index + 1}`, used));
    const padding = Math.max(...names.map(name => name.length)) + 2;
    const output = blocks(alignment.sequences[0]?.value.length ?? 0).map(block => {
        const rows = alignment.sequences.map((sequence, index) =>
            names[index].padEnd(padding) + sequence.value.slice(block.start, block.end));
        // Preserve column positions, including trailing spaces in the consensus.
        rows.push(' '.repeat(padding) + alignment.consensus.slice(block.start, block.end).replace(/[^*:. ]/g, ' '));
        return rows.join('\n');
    });
    return `CLUSTAL multiple sequence alignment\n\n${output.join('\n\n')}\n\n`;
}

export function clustalFiles(alignments: AlignmentDto[]): { name: string; content: string }[] {
    const used = new Set<string>();
    return alignments.map(alignment => ({
        name: `${uniqueName(filenameStem(alignment.name), used)}.aln`, content: clustal(alignment)
    }));
}
