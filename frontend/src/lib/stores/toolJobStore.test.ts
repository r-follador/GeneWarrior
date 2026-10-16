import {afterEach, describe, expect, it, vi} from 'vitest';
import {get} from 'svelte/store';
import {createToolJobStore} from './toolJobStore';
import {createProjectStore} from './projectStore';
import {apiClient} from '../api/client';
import type {AlignmentDto, ProjectDto, ToolJobDto} from '../api/types';

const project: ProjectDto = {
    name: 'Jobs', sequences: [
        {id: 'one', name: 'First', kind: 'DNA', sequence: 'ACGT', marks: []},
        {id: 'two', name: 'Second', kind: 'DNA', sequence: 'ACT', marks: []}
    ], alignments: [], entryOrder: ['one', 'two']
};
const alignment: AlignmentDto = {
    id: 'result', name: 'T-Coffee alignment', kind: 'DNA', score: null,
    sequences: [{name: 'First', value: 'ACGT'}, {name: 'Second', value: 'AC-T'}], marks: [], consensus: '** *'
};
const job = (id: string, status: ToolJobDto['status']): ToolJobDto => ({
    id, status, tool: 'TCOFFEE', createdAt: new Date().toISOString(),
    ...(status === 'SUCCEEDED' ? {result: alignment} : {})
});

function memoryStorage(): Storage {
    const data = new Map<string, string>();
    return {
        getItem: key => data.get(key) ?? null, setItem: (key, value) => {
            data.set(key, value);
        },
        removeItem: key => {
            data.delete(key);
        }, clear: () => data.clear(), key: () => null, get length() {
            return data.size;
        }
    };
}

afterEach(() => vi.useRealTimers());

describe('durable workspace jobs', () => {
    it('resumes after refresh and inserts exactly once below the source, without sharing job metadata', async () => {
        vi.useFakeTimers();
        const store = createProjectStore(structuredClone(project));
        const storage = memoryStorage();
        const api = {
            ...apiClient,
            submitAlignment: vi.fn(async id => job(id, 'QUEUED')),
            getJob: vi.fn(async id => job(id, 'SUCCEEDED'))
        };
        const tracker = createToolJobStore(store, api);
        const stop = tracker.start(storage, null);
        await tracker.submit(project.sequences);
        await vi.advanceTimersByTimeAsync(1100);
        stop();
        const resumed = createToolJobStore(store, api);
        const stopAgain = resumed.start(storage, null);
        await vi.advanceTimersByTimeAsync(1100);
        expect(get(store).entryOrder).toEqual(['one', 'result', 'two']);
        expect(get(store).alignments).toHaveLength(1);
        expect(JSON.stringify(get(store))).not.toContain('QUEUED');
        store.removeEntries(['result']);
        await vi.advanceTimersByTimeAsync(2000);
        expect(get(store).alignments).toHaveLength(0);
        stopAgain();
    });

    it('keeps scopes separate and requires explicit insertion after source deletion', async () => {
        vi.useFakeTimers();
        const store = createProjectStore(structuredClone(project));
        const storage = memoryStorage();
        const api = {
            ...apiClient,
            submitAlignment: vi.fn(async id => job(id, 'QUEUED')),
            getJob: vi.fn(async id => job(id, 'SUCCEEDED'))
        };
        const tracker = createToolJobStore(store, api);
        const stop = tracker.start(storage, 'shared');
        await tracker.submit(project.sequences);
        store.removeEntries(['one']);
        await vi.advanceTimersByTimeAsync(2100);
        expect(get(store).alignments).toHaveLength(0);
        tracker.insert(get(tracker)[0].id);
        expect(get(store).entryOrder).toEqual(['two', 'result']);
        stop();
        const local = createToolJobStore(store, api);
        const stopLocal = local.start(storage, null);
        expect(get(local)).toHaveLength(0);
        stopLocal();
    });

    it('reuses submission ID after a lost response and retries failures with a new ID', async () => {
        vi.useFakeTimers();
        const store = createProjectStore(structuredClone(project));
        const api = {
            ...apiClient, submitAlignment: vi.fn().mockRejectedValueOnce(new Error('Offline'))
                .mockImplementation(async id => job(id, 'FAILED'))
        };
        const tracker = createToolJobStore(store, api);
        const stop = tracker.start(memoryStorage(), null);
        await tracker.submit(project.sequences);
        await vi.advanceTimersByTimeAsync(4000);
        const id = get(tracker)[0].id;
        expect(api.submitAlignment.mock.calls.map(call => call[0])).toEqual([id, id]);
        await tracker.retry(id);
        await vi.advanceTimersByTimeAsync(2000);
        expect(get(tracker)[0].id).not.toBe(id);
        stop();
    });

    it('cancels uncertain submissions and does not insert cancelled work', async () => {
        vi.useFakeTimers();
        const store = createProjectStore(structuredClone(project));
        const api = {
            ...apiClient, submitAlignment: vi.fn(async id => job(id, 'QUEUED')),
            cancelJob: vi.fn(async id => job(id, 'CANCELLED')), getJob: vi.fn(async id => job(id, 'CANCELLED'))
        };
        const tracker = createToolJobStore(store, api);
        const stop = tracker.start(memoryStorage(), null);
        await tracker.submit(project.sequences);
        const id = get(tracker)[0].id;
        await tracker.cancel(id);
        await vi.advanceTimersByTimeAsync(3000);
        expect(api.cancelJob).toHaveBeenCalledWith(id);
        expect(get(tracker)[0].job?.status).toBe('CANCELLED');
        expect(get(store).alignments).toHaveLength(0);
        stop();
    });
});
