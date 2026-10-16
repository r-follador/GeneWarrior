import {get, writable} from 'svelte/store';
import {apiClient, ApiError} from '../api/client';
import type {AlignmentDto, SequenceDto, ToolJobDto, TreeInput} from '../api/types';
import {parseGuideTree} from '../tree/model';
import {parseProject} from '../project/projectValidation';
import {workspaceKey} from '../project/workspacePersistence';
import type {PrimerSettings} from '../api/types';
import {parsePrimerDesign} from '../primer/validation';
import {projectStore, type createProjectStore} from './projectStore';

export interface WorkspaceJob {
    id: string;
    sourceId: string;
    sequences: SequenceDto[];
    tool?: 'TCOFFEE' | 'PRIMER3' | 'GUIDE_TREE';
    tree?: TreeInput;
    settings?: PrimerSettings;
    job?: ToolJobDto;
    applied?: boolean;
    problem?: string;
    abandoned?: boolean;
}

export function createToolJobStore(project: ReturnType<typeof createProjectStore>, api = apiClient) {
    const state = writable<WorkspaceJob[]>([]);
    let storage: Storage | undefined;
    let key = '';
    let stopped = true;
    let timer: ReturnType<typeof setTimeout> | undefined;
    let processing = false;
    let delay = 1000;
    const notice = writable('');

    function save(jobs: WorkspaceJob[]) {
        if (!storage) throw new Error('Browser storage is unavailable; tool jobs cannot be resumed.');
        storage.setItem(key, JSON.stringify(jobs));
        state.set(jobs);
    }

    function change(id: string, update: (job: WorkspaceJob) => WorkspaceJob) {
        save(get(state).map(job => job.id === id ? update(job) : job));
    }

    function accept(id: string, job: ToolJobDto) {
        change(id, current => ({...current, job, problem: undefined}));
        const current = get(state).find(item => item.id === id)!;
        if (job.status === 'SUCCEEDED' && !current.applied && get(project).entryOrder.includes(current.sourceId)) insert(id);
    }

    function insert(id: string) {
        const current = get(state).find(item => item.id === id);
        if (!current?.job?.result || current.applied) return;
        // Validate server results like shared data before adding them to a persisted workspace.
        if (current.tool === 'GUIDE_TREE') {
            project.addTreeAfter(current.sourceId, parseGuideTree(current.job.result));
        } else if (current.tool === 'PRIMER3') {
            project.addPrimerDesignAfter(current.sourceId, parsePrimerDesign(current.job.result));
        } else {
            const raw = current.job.result as AlignmentDto;
            const result = parseProject({
                name: 'Result',
                sequences: [],
                alignments: [raw],
                entryOrder: [raw.id]
            }).alignments[0];
            project.addAlignmentAfter(current.sourceId, result);
        }
        change(id, item => ({...item, applied: true}));
    }

    async function poll() {
        if (stopped || processing) return;
        processing = true;
        let failed = false;
        try {
            for (const current of get(state)) {
                if (stopped) break;
                if (current.abandoned || current.applied || ['FAILED', 'CANCELLED'].includes(current.job?.status ?? '')) continue;
                if (current.job?.status === 'SUCCEEDED') continue;
                try {
                    const job = current.job
                        ? await api.getJob(current.id)
                        : await send(current);
                    if (!stopped) accept(current.id, job);
                } catch (error) {
                    if (stopped) break;
                    failed = true;
                    const terminal = error instanceof ApiError && [400, 404, 409, 413].includes(error.response.status);
                    change(current.id, item => ({
                        ...item, abandoned: terminal,
                        problem: terminal && error instanceof ApiError && error.response.status === 404
                            ? 'Job has expired or is no longer available.' : message(error)
                    }));
                }
            }
        } catch (error) {
            failed = true;
            notice.set(message(error));
        } finally {
            processing = false;
            delay = failed ? Math.min(delay * 2, 15000) : 1000;
            if (!stopped) timer = setTimeout(poll, delay);
        }
    }

    function start(store: Storage | undefined, shareKey: string | null) {
        storage = store;
        key = workspaceKey(shareKey) + '.jobs.v1';
        stopped = false;
        try {
            const raw = storage?.getItem(key);
            if (raw) {
                if (raw.length > 2_000_000) throw new Error('Saved job data is too large');
                const jobs: WorkspaceJob[] = JSON.parse(raw);
                if (!Array.isArray(jobs) || jobs.length > 50 || jobs.some(job =>
                    !/^[a-f0-9-]{36}$/.test(job.id) || typeof job.sourceId !== 'string' || !Array.isArray(job.sequences)))
                    throw new Error('Saved job data is invalid');
                state.set(jobs);
            }
            void poll();
        } catch (error) {
            stopped = true;
            notice.set(message(error));
        }
        return () => {
            stopped = true;
            clearTimeout(timer);
        };
    }

    function send(current: WorkspaceJob) {
        if (current.tool === 'GUIDE_TREE') return api.submitTree(current.id, current.tree!);
        return current.tool === 'PRIMER3'
            ? api.submitPrimers(current.id, current.sequences[0], current.settings!)
            : api.submitAlignment(current.id, current.sequences);
    }

    async function enqueue(item: Omit<WorkspaceJob, 'id'>) {
        if (stopped) throw new Error('Job tracking is unavailable');
        const id = crypto.randomUUID();
        const current = get(state);
        const pending = current.filter(item => !item.applied && !item.abandoned && !['FAILED', 'CANCELLED'].includes(item.job?.status ?? ''));
        if (pending.length >= 8) throw new Error('Eight tool jobs are already pending');
        save([...current.slice(-49), {id, ...structuredClone(item)}]);
        // The durable local record precedes the request; a lost response is retried with the same ID.
        clearTimeout(timer);
        void poll();
    }

    async function submit(sequences: SequenceDto[], settings?: PrimerSettings) {
        return enqueue({
            sourceId: sequences[0].id!, sequences: sequences.map(sequence => ({...sequence, marks: []})),
            tool: settings ? 'PRIMER3' : 'TCOFFEE', ...(settings ? {settings} : {})
        });
    }

    async function submitTree(alignment: AlignmentDto) {
        return enqueue({
            sourceId: alignment.id, sequences: [], tool: 'GUIDE_TREE',
            tree: {
                sourceAlignmentId: alignment.id,
                name: alignment.name,
                kind: alignment.kind,
                sequences: alignment.sequences
            }
        });
    }

    async function cancel(id: string) {
        try {
            const current = get(state).find(item => item.id === id);
            if (!current) return;
            // An uncertain submission must be resolved before cancelling, or it could keep running remotely.
            if (!current.job) accept(id, await send(current));
            accept(id, await api.cancelJob(id));
        } catch (error) {
            notice.set(message(error));
        }
    }

    async function retry(id: string) {
        const current = get(state).find(item => item.id === id);
        if (!current) return;
        try {
            if (current.tool === 'GUIDE_TREE') await enqueue({
                sourceId: current.sourceId,
                sequences: [],
                tool: 'GUIDE_TREE',
                tree: current.tree
            });
            else await submit(current.sequences, current.settings);
            save(get(state).filter(item => item.id !== id));
        } catch (error) {
            notice.set(message(error));
        }
    }

    return {
        subscribe: state.subscribe, notice, start, submit, submitTree, cancel, retry, insert,
        dismiss: (id: string) => {
            try {
                save(get(state).filter(item => item.id !== id));
            } catch (error) {
                notice.set(message(error));
            }
        }
    };
}

function message(error: unknown) {
    return error instanceof Error ? error.message : 'Job request failed';
}

export const toolJobStore = createToolJobStore(projectStore);
