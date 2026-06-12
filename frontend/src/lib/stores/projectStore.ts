import {writable} from 'svelte/store';
import type {ProjectDto, SequenceDto} from '../api/types';

const emptyProject = (): ProjectDto => ({
    name: 'LacZ comparison workspace',
    sequences: [
        {
            id: 'example-ecoli',
            name: 'lacZ beta-galactosidase, E. coli',
            kind: 'DNA',
            sequence:
                'ATGACCATGATTACGGATTCACTGGCCGTCGTTTTACAACGTCGTGACTGGGAAAACCCTGGCGTTACCCAACTTAATCGCCTTGCAGCACATCCCCCTTTCGCCAGCTGGCGTAATAGCGAAGAGGCCCGCACCGATCGCCCTTCCCAACAGTTGCGCAGCCTGAATGGCGAATGGCGCTTTGCCTGGTTTCCGGCACCAGAAGCGGTGCCGGAAAGCTGGCTGGAGTGCGATCTTCCTGAGGCCGATACTGTCGTCGTCCCCTCAAACTGGCAGATGCACGGTTACGATGCGCCCATCTACACCAACGTGACCTATCCCATTACGGTCAATCCGCCGTTTGTTCCCACGGAGAATCCGACGGGTTGTTACTCGCTCACATTTAATGTTGATGAAAGCTGGCTACAGGAAGGCC',
            marks: []
        },
        {
            id: 'example-lactis',
            name: 'lacZ beta-galactosidase, L. lactis',
            kind: 'DNA',
            sequence:
                'AAAATTGTTTTATTCAACCCCCTTTTTATGCTAATTTTGGTGGAAAAAATGTTCATTTTGGAACAGGGATTTATGCTAATTTTAATCTAACATTGGTTGACGATACAGATATTTTTGTAGGAAATCATGTCATGTTTGGACCAAATGTTACGATTGACACGGCGACACATCCTGTCTCGCTTGACTTACGTAAACGAGGGGCTCAATATAATAAAAAAGTTTATATTGAAGAAAATGTTTGGCTAGGTGCTGGTGTAATTGTGTTACCTGGTGTTAGAATTGGGAAAAATAGTGTGATTGGCGCAGGGAGTCTTGTAACTAAAGACATTCCAGATAATGTGGTTGCTTTTGGAACTCCTTGTATGGTTAAAAGAAAAATCAATGATTCTGACTTTAAAACTTATGATCATGGAAAAA',
            marks: [
                {start: 28, end: 92, color: '#f4c95d', type: 'HIGHLIGHT'},
                {
                    start: 143,
                    end: 352,
                    color: '#9bc7b5',
                    annotation: 'beta-galactosidase CDS',
                    type: 'ANNOTATION',
                    direction: 'FORWARD'
                }
            ]
        },
        {
            id: 'example-protein',
            name: 'Polyubiquitin-B precursor, Homo sapiens',
            kind: 'PROTEIN',
            sequence:
                'MQIFVKTLTGKTITLEVEPSDTIENVKAKIQDKEGIPPDQQRLIFAGKQLEDGRTLSDYNIQKESTLHLVLRLRGGMQIFVKTLTGKTITLEVEPSDTIENVKAKIQDKEGIPPDQQRLIFAGKQLEDGRTLSDYNIQKESTLHLVLRLRGGMQIFVKTLTGKTITLEVEPSDTIENVKAKIQDKEGIPPDQQRLIFAGKQLEDGRTLSDYNIQKESTLHLVLRLRGGC',
            marks: []
        }
    ]
});

function createProjectStore() {
    const {subscribe, set, update} = writable<ProjectDto>(emptyProject());

    return {
        subscribe,
        set,
        reset: () => set(emptyProject()),
        renameProject: (name: string) =>
            update((project) => ({...project, name})),
        addSequences: (sequences: SequenceDto[]) =>
            update((project) => ({...project, sequences: [...project.sequences, ...sequences]})),
        addSequencesAfter: (sourceId: string, sequences: SequenceDto[]) =>
            update((project) => {
                const sourceIndex = project.sequences.findIndex((sequence) => sequence.id === sourceId);
                if (sourceIndex === -1) return {...project, sequences: [...project.sequences, ...sequences]};
                return {
                    ...project,
                    sequences: [
                        ...project.sequences.slice(0, sourceIndex + 1),
                        ...sequences,
                        ...project.sequences.slice(sourceIndex + 1)
                    ]
                };
            }),
        removeSequences: (ids: string[]) =>
            update((project) => ({
                ...project,
                sequences: project.sequences.filter((sequence) => !sequence.id || !ids.includes(sequence.id))
            })),
        replaceSequence: (sequence: SequenceDto) =>
            update((project) => ({
                ...project,
                sequences: project.sequences.map((existing) => (existing.id === sequence.id ? sequence : existing))
            }))
    };
}

export const projectStore = createProjectStore();
