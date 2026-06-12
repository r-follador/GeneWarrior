import {writable} from 'svelte/store';

export interface SelectionState {
    entryId?: string;
    start?: number;
    end?: number;
    hoverEntryId?: string;
    hover?: number;
    dragging?: boolean;
}

export const selectionStore = writable<SelectionState>({});
