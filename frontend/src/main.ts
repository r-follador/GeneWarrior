import './styles.css';
import {mount} from 'svelte';
import App from './routes/App.svelte';
import {apiClient} from './lib/api/client';
import {projectStore} from './lib/stores/projectStore';
import {persistenceError} from './lib/stores/uiStore';
import {restoreWorkspace} from './lib/project/workspacePersistence';
import {toolJobStore} from './lib/stores/toolJobStore';

async function start() {
    const target = document.getElementById('app') as HTMLElement;
    target.textContent = 'Opening workspace...';
    let storage: Storage | undefined;
    try {
        storage = window.localStorage;
    } catch { /* Storage can be blocked by browser settings. */
    }
    const shareKey = new URL(location.href).searchParams.get('project');
    let stopSaving: () => void;
    try {
        stopSaving = await restoreWorkspace(projectStore, storage, shareKey, apiClient.loadProject, persistenceError.set);
    } catch {
        target.textContent = 'Shared project could not be opened. ';
        const retry = document.createElement('button');
        retry.textContent = 'Retry';
        retry.onclick = () => location.reload();
        const local = document.createElement('a');
        local.textContent = 'Open local workspace';
        const url = new URL(location.href);
        url.searchParams.delete('project');
        local.href = url.toString();
        target.append(retry, document.createTextNode(' '), local);
        return;
    }
    target.textContent = '';
    const stopJobs = toolJobStore.start(storage, shareKey);
    const app = mount(App, {target});
    if (import.meta.hot) import.meta.hot.dispose(() => {
        stopSaving();
        stopJobs();
    });
    return app;
}

export default start();
