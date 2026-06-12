import type {SequenceDto} from '../api/types';

const complementMap: Record<string, string> = {
    A: 'T',
    T: 'A',
    U: 'A',
    G: 'C',
    C: 'G',
    R: 'Y',
    Y: 'R',
    S: 'S',
    W: 'W',
    K: 'M',
    M: 'K',
    B: 'V',
    D: 'H',
    H: 'D',
    V: 'B',
    N: 'N'
};

export function normalizeSequence(sequence: string): string {
    return sequence.replace(/[\s\-.0-9]/g, '').toUpperCase().replaceAll('U', 'T');
}

export function reverseComplement(sequence: string): string {
    return [...normalizeSequence(sequence)]
        .reverse()
        .map((base) => complementMap[base] ?? 'N')
        .join('');
}

export function fastaFor(sequence: SequenceDto): string {
    return `>${sequence.name}\n${sequence.sequence.match(/.{1,80}/g)?.join('\n') ?? ''}\n`;
}
