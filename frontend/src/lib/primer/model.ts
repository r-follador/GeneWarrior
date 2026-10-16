import type {PrimerDesignDto, PrimerPair, PrimerSettings, SequenceDto} from '../api/types';

export function defaultPrimerSettings(length: number, target: PrimerSettings['target'] = null): PrimerSettings {
    const targetLength = target ? target.end - target.start : 0;
    return {
        productMin: Math.min(length, Math.max(100, targetLength + 40)),
        productMax: Math.min(length, Math.max(300, targetLength + 120)),
        numReturn: 5,
        primers: {minSize: 18, optSize: 20, maxSize: 25, minTm: 57, optTm: 60, maxTm: 63, minGc: 20, maxGc: 80},
        includeProbe: false,
        probe: {minSize: 18, optSize: 20, maxSize: 27, minTm: 60, optTm: 65, maxTm: 70, minGc: 20, maxGc: 80},
        target,
        saltMonovalent: 50,
        saltDivalent: 1.5,
        dntp: 0.6,
        dnaConcentration: 50
    };
}

export function pairSequences(design: PrimerDesignDto, pair: PrimerPair): SequenceDto[] {
    return ([['Forward', pair.left], ['Reverse', pair.right], ['Probe', pair.probe]] as const)
        .flatMap(([label, oligo]) => oligo ? [{
            name: `${design.name.slice(0, 200)} pair ${pair.index + 1} ${label}`,
            kind: 'DNA' as const, sequence: oligo.sequence, marks: []
        }] : []);
}

export function allPrimerSequences(design: PrimerDesignDto) {
    return design.pairs.flatMap(pair => pairSequences(design, pair));
}

export function reverseComplement(value: string) {
    return [...value].reverse().map(letter => ({A: 'T', C: 'G', G: 'C', T: 'A'}[letter])).join('');
}
