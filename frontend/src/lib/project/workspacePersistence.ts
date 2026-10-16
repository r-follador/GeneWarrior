import type {ProjectDto} from '../api/types';
import {decodeProject, MAX_PROJECT_BYTES, parseProject} from './projectValidation';
import type {createProjectStore} from '../stores/projectStore';

export const WORKSPACE_KEY = 'genewarrior.workspace.v1';

export function workspaceKey(shareKey: string | null) {
    return shareKey ? WORKSPACE_KEY + '.shared.' + shareKey : WORKSPACE_KEY;
}

export function shareUrl(location: string, shareKey: string) {
    const url = new URL(location);
    url.search = '';
    url.hash = '';
    url.searchParams.set('project', shareKey);
    return url.toString();
}

// Restore before subscribing: the initial store emission must never overwrite a draft.
export async function restoreWorkspace(
    store: ReturnType<typeof createProjectStore>,
    storage: Pick<Storage, 'getItem' | 'setItem'> | undefined,
    shareKey: string | null,
    loadProject: (key: string) => Promise<ProjectDto>,
    report: (message?: string) => void
) {
    const key = workspaceKey(shareKey);
    let restored = false;
    let readFailed = false;
    if (storage) {
        try {
            const saved = storage.getItem(key);
            if (saved !== null) {
                store.set(decodeProject(saved));
                restored = true;
            }
        } catch {
            readFailed = true;
            report('The saved workspace could not be restored. Its browser copy has been left untouched.');
        }
    } else {
        report('Browser storage is unavailable. Changes will not survive a refresh.');
    }
    if (shareKey && !restored) {
        try {
            store.set(parseProject(await loadProject(shareKey)));
            restored = true;
        } catch (error) {
            report('Shared project could not be opened: ' + (error instanceof Error ? error.message : 'Request failed'));
            // Do not replace a failed shared-project load with the example workspace.
            throw error;
        }
    }
    if (!storage || readFailed) return () => {
    };
    let first = true;
    return store.subscribe((project) => {
        if (first && !restored) {
            first = false;
            return;
        }
        first = false;
        try {
            const payload = JSON.stringify(project);
            if (new TextEncoder().encode(payload).length > MAX_PROJECT_BYTES) throw new Error('Project exceeds 5 MB');
            storage.setItem(key, payload);
            report(undefined);
        } catch {
            report('Changes could not be saved in this browser. The previous saved copy is intact.');
        }
    });
}
