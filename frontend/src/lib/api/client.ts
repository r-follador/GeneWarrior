import type {
    AlignmentDto,
    ApiErrorDto,
    ProjectDto,
    SequenceDto,
    SequenceLogoDto,
    ToolJobDto,
    TranslationFramesDto,
    TcoffeeCapabilities
} from './types';

const jsonHeaders = {'Content-Type': 'application/json'};

export class ApiError extends Error {
    constructor(public readonly response: ApiErrorDto) {
        super(response.message);
    }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
    const response = await fetch(path, {signal: AbortSignal.timeout(15000), ...init});
    if (!response.ok) {
        throw new ApiError((await response.json()) as ApiErrorDto);
    }
    return (await response.json()) as T;
}

export const apiClient = {
    tcoffeeCapabilities(): Promise<TcoffeeCapabilities> {
        return request('/api/tools/tcoffee');
    },
    submitAlignment(requestId: string, sequences: SequenceDto[]): Promise<ToolJobDto> {
        return request('/api/alignments', {
            method: 'POST',
            headers: jsonHeaders,
            body: JSON.stringify({requestId, sequences})
        });
    },
    cancelJob(id: string): Promise<ToolJobDto> {
        return request(`/api/jobs/${encodeURIComponent(id)}`, {method: 'DELETE'});
    },
    consensus(alignment: AlignmentDto): Promise<SequenceDto> {
        return request('/api/alignments/consensus', {
            method: 'POST', headers: jsonHeaders,
            body: JSON.stringify({name: alignment.name, kind: alignment.kind, sequences: alignment.sequences})
        });
    },
    logo(alignment: AlignmentDto): Promise<SequenceLogoDto> {
        return request('/api/logos', {
            method: 'POST', headers: jsonHeaders,
            body: JSON.stringify({
                sourceAlignmentId: alignment.id, name: `${alignment.name.slice(0, 240)} [Logo]`,
                kind: alignment.kind, mode: 'FREQUENCY', sequences: alignment.sequences
            })
        });
    },
    async importSequences(text: string): Promise<SequenceDto[]> {
        const body = await request<{ sequences: SequenceDto[] }>('/api/sequences/import', {
            method: 'POST',
            headers: jsonHeaders,
            body: JSON.stringify({text})
        });
        return body.sequences;
    },

    transform(sequence: SequenceDto, transform: 'REVERSE' | 'COMPLEMENT' | 'REVERSE_COMPLEMENT'): Promise<SequenceDto> {
        return request('/api/transforms', {
            method: 'POST',
            headers: jsonHeaders,
            body: JSON.stringify({sequence, transform})
        });
    },

    translate(sequence: SequenceDto, frame = 1, translationTable = 1): Promise<SequenceDto> {
        return request('/api/translations', {
            method: 'POST',
            headers: jsonHeaders,
            body: JSON.stringify({sequence, frame, translationTable})
        });
    },

    translateBestOrf(sequence: SequenceDto, translationTable = 1): Promise<SequenceDto> {
        return request('/api/translations/best-orf', {
            method: 'POST',
            headers: jsonHeaders,
            body: JSON.stringify({sequence, translationTable})
        });
    },

    translationFrames(sequence: SequenceDto, translationTable = 1): Promise<TranslationFramesDto> {
        return request('/api/translations/frames', {
            method: 'POST',
            headers: jsonHeaders,
            body: JSON.stringify({sequence, translationTable})
        });
    },

    pairwiseAlignment(sequences: SequenceDto[], mode: 'CFE' | 'LOCAL'): Promise<AlignmentDto> {
        return request('/api/alignments/pairwise', {
            method: 'POST',
            headers: jsonHeaders,
            body: JSON.stringify({sequences, mode})
        });
    },

    translateRange(sequence: SequenceDto, start: number, end: number, translationTable = 1): Promise<SequenceDto> {
        return request('/api/translations/range', {
            method: 'POST',
            headers: jsonHeaders,
            body: JSON.stringify({sequence, start, end, translationTable})
        });
    },

    saveProject(project: ProjectDto): Promise<{ id: string; shareKey: string }> {
        return request('/api/projects', {
            method: 'POST',
            headers: jsonHeaders,
            body: JSON.stringify({project})
        });
    },

    loadProject(shareKey: string): Promise<ProjectDto> {
        return request(`/api/projects/${encodeURIComponent(shareKey)}`);
    },

    getJob(id: string): Promise<ToolJobDto> {
        return request(`/api/jobs/${encodeURIComponent(id)}`);
    }
};
