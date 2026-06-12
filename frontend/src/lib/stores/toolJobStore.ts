import {writable} from 'svelte/store';
import type {ToolJobDto} from '../api/types';

function createToolJobStore() {
    const {subscribe, update} = writable<Record<string, ToolJobDto>>({});

    return {
        subscribe,
        upsert: (job: ToolJobDto) => update((jobs) => ({...jobs, [job.id]: job})),
        remove: (id: string) =>
            update((jobs) => {
                const next = {...jobs};
                delete next[id];
                return next;
            })
    };
}

export const toolJobStore = createToolJobStore();
