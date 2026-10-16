import type {AlignmentDto, GuideTreeDto} from '../api/types';

function check(value: unknown, message = 'Invalid guide tree'): asserts value {
    if (!value) throw new Error(message);
}

function object(value: unknown): Record<string, unknown> {
    check(value !== null && typeof value === 'object' && !Array.isArray(value));
    return value as Record<string, unknown>;
}

function text(value: unknown, max = 255): string {
    check(typeof value === 'string' && value.trim() && value.length <= max);
    return value;
}

function id(value: unknown): string {
    const result = text(value, 160);
    check(/^[a-zA-Z0-9_-]+$/.test(result));
    return result;
}

export function parseGuideTree(value: unknown): GuideTreeDto {
    const raw = object(value);
    check(raw.kind === 'DNA' || raw.kind === 'PROTEIN');
    check(raw.method === 'tcoffee-nj-guide-v1');
    const version = text(raw.version, 100);
    check(/^[0-9][A-Za-z0-9.]*$/.test(version));
    check(Array.isArray(raw.sources) && raw.sources.length >= 2 && raw.sources.length <= 20);
    const sources = raw.sources.map(value => {
        const source = object(value), sha256 = text(source.sha256, 64);
        check(/^[a-f0-9]{64}$/.test(sha256));
        return {id: source.id == null ? undefined : id(source.id), name: text(source.name), sha256};
    });
    check(Array.isArray(raw.nodes) && raw.nodes.length >= 3 && raw.nodes.length <= 40);
    const childCounts = new Array<number>(raw.nodes.length).fill(0), leaves = new Set<number>();
    const nodes = raw.nodes.map((value, index) => {
        const node = object(value);
        check(node.id === index);
        if (index === 0) check(node.parentId === null && node.length === null);
        else {
            check(Number.isInteger(node.parentId) && (node.parentId as number) >= 0 && (node.parentId as number) < index);
            childCounts[node.parentId as number]++;
            check(typeof node.length === 'number' && Number.isFinite(node.length) && Math.abs(node.length) <= 1000000);
        }
        check(typeof node.name === 'string');
        if (node.sourceIndex != null) {
            check(Number.isInteger(node.sourceIndex) && (node.sourceIndex as number) >= 0 && (node.sourceIndex as number) < sources.length);
            check(!leaves.has(node.sourceIndex as number) && sources[node.sourceIndex as number].name === node.name);
            leaves.add(node.sourceIndex as number);
        } else check(node.name === '');
        return {
            id: index, parentId: node.parentId as number | null, length: node.length as number | null,
            name: node.name, sourceIndex: node.sourceIndex == null ? null : node.sourceIndex as number
        };
    });
    nodes.forEach(node => check(node.sourceIndex === null ? childCounts[node.id] >= 2 : childCounts[node.id] === 0));
    check(leaves.size === sources.length);
    return {
        id: id(raw.id), name: text(raw.name), sourceAlignmentId: id(raw.sourceAlignmentId),
        kind: raw.kind, method: raw.method, version, sources, nodes
    };
}

export function treeEligible(alignment: AlignmentDto) {
    const rows = alignment.sequences;
    if (rows.length < 3 || rows.length > 20 || rows[0].value.length > 10000) return false;
    const alphabet = alignment.kind === 'DNA' ? /^[ACGT]+$/i : /^[ACDEFGHIKLMNPQRSTVWY]+$/i;
    const sequences = rows.map(row => row.value.replace(/-/g, ''));
    return rows.every(row => row.value.length === rows[0].value.length)
        && sequences.every(sequence => alphabet.test(sequence) && sequence.length <= 2000)
        && sequences.reduce((sum, sequence) => sum + sequence.length, 0) <= 10000;
}

export function treeNewick(tree: GuideTreeDto): string {
    const children = tree.nodes.map(() => [] as number[]);
    tree.nodes.forEach(node => {
        if (node.parentId !== null) children[node.parentId].push(node.id);
    });
    const write = (index: number): string => {
        const node = tree.nodes[index];
        // Quote every label, doubling embedded quotes as required by Newick.
        const label = node.sourceIndex === null ? `(${children[index].map(write).join(',')})` : `'${node.name.replace(/[\r\n\t]/g, ' ').replace(/'/g, "''")}'`;
        return label + (node.length === null ? '' : ':' + node.length);
    };
    return write(0) + ';\n';
}
