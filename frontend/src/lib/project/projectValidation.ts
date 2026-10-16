import type {AlignmentDto, ProjectDto, SequenceDto, SequenceKind, SequenceMarkDto, SequenceLogoDto} from '../api/types';

import {parsePrimerDesign} from '../primer/validation';
import {parseGuideTree} from '../tree/model';

export const MAX_PROJECT_BYTES = 5_000_000;

function requireValue(condition: unknown, message: string): asserts condition {
    if (!condition) throw new Error(message);
}

function object(value: unknown): Record<string, unknown> {
    requireValue(value !== null && typeof value === 'object' && !Array.isArray(value), 'Invalid project object');
    return value as Record<string, unknown>;
}

function text(value: unknown, field: string, max = 255): string {
    requireValue(typeof value === 'string' && value.trim().length > 0 && value.length <= max, 'Invalid ' + field);
    return value;
}

function id(value: unknown): string {
    const result = text(value, 'entry ID', 160);
    requireValue(/^[a-zA-Z0-9_-]+$/.test(result), 'Invalid entry ID');
    return result;
}

function kind(value: unknown): SequenceKind {
    requireValue(value === 'DNA' || value === 'PROTEIN', 'Invalid sequence kind');
    return value;
}

function sequenceText(value: unknown, sequenceKind: SequenceKind): string {
    const result = text(value, 'sequence', MAX_PROJECT_BYTES);
    requireValue((sequenceKind === 'DNA' ? /^[ACGTURYSWKMBDHVN-]+$/i : /^[A-Z*-]+$/i).test(result), 'Invalid sequence characters');
    return result;
}

function list(value: unknown): unknown[] {
    requireValue(Array.isArray(value) && value.length <= 10000, 'Invalid project list');
    return value;
}

function marks(value: unknown, length: number, alignment = false): SequenceMarkDto[] {
    return list(value).map((item) => {
        const mark = object(item);
        requireValue(Number.isInteger(mark.start) && Number.isInteger(mark.end)
            && (mark.start as number) >= 0 && (mark.end as number) > (mark.start as number)
            && (mark.end as number) <= length, 'Invalid annotation coordinates');
        requireValue(typeof mark.color === 'string' && /^#[0-9a-f]{6}$/i.test(mark.color), 'Invalid annotation color');
        requireValue(mark.type === 'ANNOTATION' || (!alignment && (mark.type === 'HIGHLIGHT' || mark.type == null)), 'Invalid annotation type');
        requireValue(mark.direction == null || mark.direction === 'FORWARD' || mark.direction === 'REVERSE', 'Invalid annotation direction');
        const annotation = mark.annotation == null ? undefined : text(mark.annotation, 'annotation label', 100);
        requireValue(mark.type !== 'ANNOTATION' || annotation, 'Annotation label is required');
        return {
            start: mark.start as number, end: mark.end as number, color: mark.color,
            type: mark.type as SequenceMarkDto['type'], direction: mark.direction as SequenceMarkDto['direction'],
            annotation
        };
    });
}

function timestamp(value: unknown): string | undefined {
    if (value == null) return undefined;
    requireValue(typeof value === 'string' && Number.isFinite(Date.parse(value)), 'Invalid project timestamp');
    return value;
}

export function parseProject(value: unknown): ProjectDto {
    const project = object(value);
    const sequences: SequenceDto[] = list(project.sequences).map((item) => {
        const entry = object(item);
        const entryKind = kind(entry.kind);
        const sequence = sequenceText(entry.sequence, entryKind);
        return {
            id: id(entry.id),
            name: text(entry.name, 'sequence name'),
            kind: entryKind,
            sequence,
            marks: marks(entry.marks, sequence.length)
        };
    });
    const alignments: AlignmentDto[] = list(project.alignments).map((item) => {
        const entry = object(item);
        const entryKind = kind(entry.kind);
        const rows = list(entry.sequences).map((item) => {
            const row = object(item);
            return {name: text(row.name, 'alignment row name'), value: sequenceText(row.value, entryKind)};
        });
        requireValue(rows.length >= 2 && rows.every((row) => row.value.length === rows[0].value.length), 'Alignment rows must have equal lengths');
        requireValue(typeof entry.consensus === 'string' && /^[*:. ]*$/.test(entry.consensus)
            && entry.consensus.length === rows[0].value.length, 'Invalid alignment consensus');
        requireValue(entry.score == null || Number.isInteger(entry.score), 'Invalid alignment score');
        let provenance: AlignmentDto['provenance'];
        if (entry.provenance != null) {
            const raw = object(entry.provenance);
            const sources = list(raw.sources).map(item => {
                const source = object(item);
                const sha256 = text(source.sha256, 'sequence digest', 64);
                requireValue(/^[a-f0-9]{64}$/.test(sha256), 'Invalid sequence digest');
                return {
                    id: source.id == null ? undefined : id(source.id),
                    name: text(source.name, 'source name'),
                    sha256
                };
            });
            requireValue(sources.length >= 2 && sources.length <= 20, 'Invalid alignment sources');
            provenance = {
                tool: text(raw.tool, 'tool', 100), version: text(raw.version, 'version', 100),
                method: text(raw.method, 'method', 100), sources
            };
        }
        return {
            id: id(entry.id), name: text(entry.name, 'alignment name'), kind: entryKind,
            sequences: rows, consensus: entry.consensus, score: entry.score == null ? null : entry.score as number,
            ...(provenance ? {provenance} : {}),
            marks: marks(entry.marks, rows[0].value.length, true)
        };
    });
    // Additive migration: earlier workspace snapshots have no logo collection.
    const logos = project.logos === undefined ? [] : list(project.logos).map(parseLogo);
    const primerDesigns = project.primerDesigns == null ? [] : list(project.primerDesigns).map(parsePrimerDesign);
    const trees = project.trees == null ? [] : list(project.trees).map(parseGuideTree);
    const ids = [...sequences.map((entry) => entry.id!), ...alignments.map((entry) => entry.id), ...logos.map(entry => entry.id), ...primerDesigns.map(entry => entry.id), ...trees.map(entry => entry.id)];
    requireValue(new Set(ids).size === ids.length, 'Duplicate entry IDs');
    const order = list(project.entryOrder).map(id);
    requireValue(order.length === ids.length && new Set(order).size === order.length
        && order.every((entryId) => ids.includes(entryId)), 'Invalid workspace entry order');
    return {
        id: project.id == null ? undefined : text(project.id, 'project ID', 160),
        name: text(project.name, 'project name'),
        sequences, alignments, logos, entryOrder: order,
        ...(primerDesigns.length ? {primerDesigns} : {}),
        ...(trees.length ? {trees} : {}),
        createdAt: timestamp(project.createdAt), updatedAt: timestamp(project.updatedAt)
    };
}

export function parseLogo(value: unknown): SequenceLogoDto {
    const logo = object(value);
    const entryKind = kind(logo.kind);
    const alphabet = entryKind === 'DNA' ? 'ACGT' : 'ACDEFGHIKLMNPQRSTVWY';
    const integer = (v: unknown, max: number) => {
        requireValue(Number.isInteger(v) && (v as number) >= 0 && (v as number) <= max, 'Invalid logo count');
        return v as number;
    };
    const near = (v: unknown, expected: number) => {
        requireValue(typeof v === 'number' && Number.isFinite(v) && v >= 0 && Math.abs(v - expected) < 1e-9, 'Invalid logo statistics');
        return v;
    };
    requireValue(logo.mode === 'FREQUENCY' || logo.mode === 'INFORMATION', 'Invalid logo mode');
    requireValue(logo.method === 'uniform-uncorrected-v1', 'Invalid logo method');
    const sequenceCount = integer(logo.sequenceCount, 10000);
    const rawColumns = list(logo.columns);
    requireValue(sequenceCount > 0 && rawColumns.length > 0 && rawColumns.length <= 2000
        && rawColumns.length * sequenceCount <= 2_000_000, 'Invalid logo dimensions');
    const columns = rawColumns.map((item, index) => {
        const column = object(item);
        requireValue(column.position === index + 1, 'Invalid logo position');
        const rawSymbols = list(column.symbols);
        requireValue(rawSymbols.length === alphabet.length, 'Invalid logo alphabet');
        const symbols = rawSymbols.map((item, i) => {
            const symbol = object(item);
            requireValue(symbol.residue === alphabet[i], 'Invalid logo residue');
            return {
                residue: alphabet[i],
                count: integer(symbol.count, sequenceCount),
                frequency: symbol.frequency,
                bits: symbol.bits
            };
        });
        const n = symbols.reduce((sum, symbol) => sum + symbol.count, 0);
        const gaps = integer(column.gaps, sequenceCount);
        const ignored = integer(column.ignored, sequenceCount);
        requireValue(n === column.observations && n + gaps + ignored === sequenceCount, 'Invalid logo coverage');
        const information = n === 0 ? 0 : Math.max(0, Math.log2(alphabet.length) + symbols.reduce((sum, symbol) => {
            const p = symbol.count / n;
            return sum + (p ? p * Math.log2(p) : 0);
        }, 0));
        return {
            position: index + 1, observations: n, gaps, ignored, information: near(column.information, information),
            symbols: symbols.map(symbol => {
                const p = n ? symbol.count / n : 0;
                return {
                    residue: symbol.residue,
                    count: symbol.count,
                    frequency: near(symbol.frequency, p),
                    bits: near(symbol.bits, p * information)
                };
            })
        };
    });
    return {
        id: id(logo.id), name: text(logo.name, 'logo name'), sourceAlignmentId: id(logo.sourceAlignmentId),
        kind: entryKind, mode: logo.mode, method: logo.method, sequenceCount, columns
    };
}

export function decodeProject(json: string): ProjectDto {
    if (new TextEncoder().encode(json).length > MAX_PROJECT_BYTES) throw new Error('Project exceeds 5 MB');
    return parseProject(JSON.parse(json));
}
