import type {SelectionState} from '../stores/selectionStore';

export function normalizedRange(selection: SelectionState) {
    if (selection.start === undefined || selection.end === undefined) return undefined;
    return {
        start: Math.min(selection.start, selection.end),
        end: Math.max(selection.start, selection.end)
    };
}

export function isSelected(selection: SelectionState, entryId: string, position: number) {
    const range = normalizedRange(selection);
    return selection.entryId === entryId && range !== undefined && position >= range.start && position <= range.end;
}
