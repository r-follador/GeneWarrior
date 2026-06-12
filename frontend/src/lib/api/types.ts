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

export interface ProjectDto {
    id?: string;
    name: string;
    sequences: SequenceDto[];
    createdAt?: string;
    updatedAt?: string;
}

export interface ToolJobDto {
    id: string;
    tool: string;
    status: 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED';
    createdAt: string;
    finishedAt?: string;
    result?: unknown;
    error?: string;
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
