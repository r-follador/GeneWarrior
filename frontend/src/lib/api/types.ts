export type SequenceKind = 'DNA' | 'PROTEIN';

export interface SequenceMarkDto {
    start: number;
    end: number;
    color: string;
    annotation?: string;
    type?: 'HIGHLIGHT' | 'ANNOTATION';
    direction?: 'FORWARD' | 'REVERSE';
}

export interface SequenceDto {
    id?: string;
    name: string;
    kind: SequenceKind;
    sequence: string;
    marks: SequenceMarkDto[];
}

export interface AlignmentSequenceDto {
    name: string;
    value: string;
}

export interface AlignmentDto {
    id: string;
    name: string;
    kind: SequenceKind;
    sequences: AlignmentSequenceDto[];
    consensus: string;
    score: number | null;
    marks: SequenceMarkDto[];
    provenance?: {
        tool: string;
        version: string;
        method: string;
        sources: { id?: string; name: string; sha256: string }[]
    } | null;
}

export interface ProjectDto {
    id?: string;
    name: string;
    sequences: SequenceDto[];
    alignments: AlignmentDto[];
    logos?: SequenceLogoDto[];
    entryOrder: string[];
    createdAt?: string;
    updatedAt?: string;
}

export interface SequenceLogoDto {
    id: string;
    name: string;
    sourceAlignmentId: string;
    kind: SequenceKind;
    mode: 'FREQUENCY' | 'INFORMATION';
    method: 'uniform-uncorrected-v1';
    sequenceCount: number;
    columns: {
        position: number;
        observations: number;
        gaps: number;
        ignored: number;
        information: number;
        symbols: { residue: string; count: number; frequency: number; bits: number }[];
    }[];
}

export interface ToolJobDto {
    id: string;
    tool: string;
    status: 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED';
    createdAt: string;
    finishedAt?: string;
    result?: unknown;
    error?: string;
    errorCode?: string;
    startedAt?: string;
    expiresAt?: string;
}

export interface TcoffeeCapabilities {
    available: boolean;
    version?: string;
    maxSequences: number;
    maxLength: number;
    maxResidues: number;
}

export interface TranslationFramesDto {
    sequence: string;
    frame1: string;
    frame2: string;
    frame3: string;
    markers1: string;
    markers2: string;
    markers3: string;
}

export interface ApiErrorDto {
    timestamp: string;
    status: number;
    error: string;
    message: string;
    fields: Record<string, string>;
}
