import {describe, expect, it, vi} from 'vitest';
import {get} from 'svelte/store';
import {createProjectStore, exampleProject} from '../stores/projectStore';
import {decodeProject, parseProject} from './projectValidation';
import {restoreWorkspace, shareUrl, workspaceKey} from './workspacePersistence';

function memoryStorage() {
    const values = new Map<string, string>();
    return {
        getItem: (key: string) => values.get(key) ?? null,
        setItem: (key: string, value: string) => {
            values.set(key, value);
        }
    };
}

describe('complete workspace persistence', () => {
    it('validates and round-trips the example workspace including alignments', () => {
        const project = exampleProject();
        expect(decodeProject(JSON.stringify(project))).toEqual(parseProject(project));
    });

    it('restores renamed, reordered, annotated entries and preserves deletions', async () => {
        const storage = memoryStorage();
        const store = createProjectStore();
        const stop = await restoreWorkspace(store, storage, null, vi.fn(), vi.fn());
        const initial = get(store);
        store.renameProject('My project');
        store.replaceSequence({...initial.sequences[0], name: 'Renamed DNA'});
        store.replaceAlignment({
            ...initial.alignments[0], marks: [
                {start: 2, end: 20, color: '#5a88b2', annotation: 'Domain', type: 'ANNOTATION', direction: 'REVERSE'}
            ]
        });
        store.moveEntry('example-alignment', 'example-ecoli', false);
        store.removeEntries(['example-protein']);
        stop();
        const restored = createProjectStore();
        const unsubscribe = await restoreWorkspace(restored, storage, null, vi.fn(), vi.fn());
        expect(get(restored)).toEqual(parseProject(get(store)));
        expect(get(restored).entryOrder[0]).toBe('example-alignment');
        expect(get(restored).sequences).toHaveLength(2);
        unsubscribe();
    });

    it('persists an empty workspace without resurrecting example entries', async () => {
        const storage = memoryStorage();
        const store = createProjectStore();
        const stop = await restoreWorkspace(store, storage, null, vi.fn(), vi.fn());
        store.removeEntries(get(store).entryOrder);
        stop();
        const restored = createProjectStore();
        const unsubscribe = await restoreWorkspace(restored, storage, null, vi.fn(), vi.fn());
        expect(get(restored).entryOrder).toEqual([]);
        expect(get(restored).alignments).toEqual([]);
        expect(get(restored).sequences).toEqual([]);
        unsubscribe();
    });

    it('inserts derived sequences and alignments adjacent to their source in persisted order', () => {
        const store = createProjectStore();
        const initial = get(store);
        store.addAlignmentAfter('example-ecoli', {...initial.alignments[0], id: 'new-alignment'});
        store.addSequencesAfter('new-alignment', [{...initial.sequences[0], id: 'derived'}]);
        expect(get(store).entryOrder.slice(0, 3)).toEqual(['example-ecoli', 'new-alignment', 'derived']);
        expect(() => parseProject(get(store))).not.toThrow();
    });

    it('isolates shared drafts from the local workspace and restores their edits on refresh', async () => {
        const storage = memoryStorage();
        const local = {...exampleProject(), name: 'Local'};
        storage.setItem(workspaceKey(null), JSON.stringify(local));
        const snapshot = {...exampleProject(), name: 'Shared snapshot'};
        const load = vi.fn().mockResolvedValue(snapshot);
        const store = createProjectStore();
        const stop = await restoreWorkspace(store, storage, 'share-key', load, vi.fn());
        expect(load).toHaveBeenCalledWith('share-key');
        store.renameProject('Edited shared draft');
        stop();
        const restored = createProjectStore();
        const unsubscribe = await restoreWorkspace(restored, storage, 'share-key', load, vi.fn());
        expect(get(restored).name).toBe('Edited shared draft');
        expect(load).toHaveBeenCalledTimes(1);
        expect(JSON.parse(storage.getItem(workspaceKey(null))!).name).toBe('Local');
        expect(snapshot.name).toBe('Shared snapshot');
        unsubscribe();
    });

    it('does not overwrite a corrupt draft or a failed shared-project load', async () => {
        const storage = memoryStorage();
        storage.setItem(workspaceKey(null), '{bad json');
        const store = createProjectStore();
        const report = vi.fn();
        const stop = await restoreWorkspace(store, storage, null, vi.fn(), report);
        store.renameProject('Changed');
        expect(storage.getItem(workspaceKey(null))).toBe('{bad json');
        expect(report).toHaveBeenCalled();
        stop();
        await expect(restoreWorkspace(store, storage, 'missing', vi.fn().mockRejectedValue(new Error('Project not found')), report)).rejects.toThrow('Project not found');
        expect(storage.getItem(workspaceKey('missing'))).toBeNull();
    });

    it('reports quota failures without discarding the in-memory workspace', async () => {
        const storage = {
            getItem: () => null, setItem: () => {
                throw new Error('QuotaExceeded');
            }
        };
        const report = vi.fn();
        const store = createProjectStore();
        const stop = await restoreWorkspace(store, storage, null, vi.fn(), report);
        store.renameProject('Unsaved edit');
        expect(get(store).name).toBe('Unsaved edit');
        expect(report).toHaveBeenLastCalledWith(expect.stringContaining('could not be saved'));
        stop();
    });

    it('rejects duplicate IDs, invalid ordering, unsafe colors and out-of-range marks', () => {
        const duplicate = exampleProject();
        duplicate.alignments[0].id = duplicate.sequences[0].id!;
        expect(() => parseProject(duplicate)).toThrow('Duplicate');
        const order = exampleProject();
        order.entryOrder.pop();
        expect(() => parseProject(order)).toThrow('order');
        const marks = exampleProject();
        marks.sequences[1].marks[0].color = 'url(https://example.invalid)';
        expect(() => parseProject(marks)).toThrow('color');
        marks.sequences[1].marks[0].color = '#abcdef';
        marks.sequences[1].marks[0].end = 100000;
        expect(() => parseProject(marks)).toThrow('coordinates');
    });

    it('creates frontend share URLs without legacy routes or stale query parameters', () => {
        expect(shareUrl('http://localhost:5173/?project=old#fragment', 'new')).toBe('http://localhost:5173/?project=new');
    });
});
