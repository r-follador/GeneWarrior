import {writable} from 'svelte/store';

export interface UiState {
    busy: boolean;
    error?: string;
}

export const uiStore = writable<UiState>({busy: false});
