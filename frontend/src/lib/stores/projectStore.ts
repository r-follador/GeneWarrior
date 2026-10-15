import {writable} from 'svelte/store';
import type {AlignmentDto, ProjectDto, SequenceDto, SequenceLogoDto} from '../api/types';

const exampleAlignment: AlignmentDto = {
    id: 'example-alignment',
    name: 'LacZ protein alignment',
    kind: 'PROTEIN',
    sequences: [
        {
            name: 'L. delbrueckii',
            value: 'GSLLEVSRQEVGFRNFELKDGIMYLNGQRIVFKGANRHEFDSKLGRAITEEDMIWDIKTMKRSNINAVRCSHYPNQSLFYRLCDKYGLYVIDEANLESHGTWEKVGGHEDP'
        },
        {
            name: 'L. lactis',
            value: 'GAVFHKETKKIGIRKIAIEKGQLKINGKALLVRGVNKHEFTPEHGYVVSEEVMIKDIKLMKEHNFNAVRCSHYPNDSRWYELCDEYGLYVMDEANIETHGM---------T'
        },
        {
            name: 'E. coli',
            value: 'GTLIEAEACDVGFREVRIENGLLLLNGKPLLIRGVNRHEHHPLHGQVMDEQTMVQDILLMKQNNFNAVRCSHYPNHPLWYTLCDRYGLYVVDEANIETHGM---------V'
        }
    ],
    consensus: '* :  .  .:*:*:. ::.* : :**: ::..*.*.**. .  * .: *: *:.**  **  *:********** . :* *** *****:****:*:**            ',
    score: 0,
    marks: []
};

export const exampleProject = (): ProjectDto => ({
    name: 'LacZ comparison workspace',
    alignments: [structuredClone(exampleAlignment)],
    logos: [],
    entryOrder: ['example-ecoli', 'example-lactis', 'example-protein', exampleAlignment.id],
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

function insertAfter(order: string[], sourceId: string, ids: string[]) {
    const remaining = order.filter((id) => !ids.includes(id));
    const source = remaining.indexOf(sourceId);
    const index = source === -1 ? remaining.length : source + 1;
    return [...remaining.slice(0, index), ...ids, ...remaining.slice(index)];
}

export function createProjectStore(initial: ProjectDto = exampleProject()) {
    const {subscribe, set, update} = writable<ProjectDto>(initial);
    const edit = (change: (project: ProjectDto) => ProjectDto) => update((project) => {
        const now = new Date().toISOString();
        return {...change(project), createdAt: project.createdAt ?? now, updatedAt: now};
    });
    const withIds = (sequences: SequenceDto[]) => sequences.map((sequence) => ({
        ...sequence, id: sequence.id || crypto.randomUUID()
    }));

    return {
        subscribe,
        set,
        reset: () => set(exampleProject()),
        renameProject: (name: string) => edit((project) => ({...project, name})),
        addSequences: (sequences: SequenceDto[]) => {
            const added = withIds(sequences);
            edit((project) => ({
                ...project, sequences: [...project.sequences, ...added],
                entryOrder: [...project.entryOrder, ...added.map((sequence) => sequence.id)]
            }));
        },
        addSequencesAfter: (sourceId: string, sequences: SequenceDto[]) => {
            const added = withIds(sequences);
            edit((project) => ({
                ...project, sequences: [...project.sequences, ...added],
                entryOrder: insertAfter(project.entryOrder, sourceId, added.map((sequence) => sequence.id))
            }));
        },
        addAlignmentAfter: (sourceId: string, alignment: AlignmentDto) => edit((project) => project.entryOrder.includes(alignment.id) ? project : ({
            ...project, alignments: [...project.alignments, alignment],
            entryOrder: insertAfter(project.entryOrder, sourceId, [alignment.id])
        })),
        addLogoAfter: (sourceId: string, logo: SequenceLogoDto) => edit((project) => ({
            ...project, logos: [...(project.logos ?? []), logo],
            entryOrder: insertAfter(project.entryOrder, sourceId, [logo.id])
        })),
        replaceLogo: (logo: SequenceLogoDto) => edit((project) => ({
            ...project, logos: (project.logos ?? []).map(existing => existing.id === logo.id ? logo : existing)
        })),
        insertEntriesAfter: (sourceId: string, ids: string[]) => edit((project) => ({
            ...project, entryOrder: insertAfter(project.entryOrder, sourceId, ids)
        })),
        moveEntry: (sourceId: string, targetId: string, after: boolean) => edit((project) => {
            if (sourceId === targetId || !project.entryOrder.includes(sourceId) || !project.entryOrder.includes(targetId)) return project;
            const order = project.entryOrder.filter((id) => id !== sourceId);
            order.splice(order.indexOf(targetId) + Number(after), 0, sourceId);
            return {...project, entryOrder: order};
        }),
        removeEntries: (ids: string[]) => edit((project) => ({
            ...project,
            sequences: project.sequences.filter((sequence) => !ids.includes(sequence.id ?? '')),
            alignments: project.alignments.filter((alignment) => !ids.includes(alignment.id)),
            logos: (project.logos ?? []).filter(logo => !ids.includes(logo.id)),
            entryOrder: project.entryOrder.filter((id) => !ids.includes(id))
        })),
        replaceSequence: (sequence: SequenceDto) => edit((project) => ({
            ...project,
            sequences: project.sequences.map((existing) => existing.id === sequence.id ? sequence : existing)
        })),
        replaceAlignment: (alignment: AlignmentDto) => edit((project) => ({
            ...project,
            alignments: project.alignments.map((existing) => existing.id === alignment.id ? alignment : existing)
        }))
    };
}

export const projectStore = createProjectStore();
