import type {SequenceMarkDto} from '../api/types';

export function printColor(color: string): string {
    return /^#[0-9a-f]{6}$/i.test(color) ? color : '#d9e2e2';
}

export function annotationFragments(marks: SequenceMarkDto[], start: number, end: number) {
    return marks.filter(mark => mark.type === 'ANNOTATION' && mark.start < end && mark.end > start)
        .map(mark => ({
            mark, start: Math.max(start, mark.start) - start, end: Math.min(end, mark.end) - start,
            continuesBefore: mark.start < start, continuesAfter: mark.end > end
        }));
}

export function highlightAt(marks: SequenceMarkDto[], position: number): string | undefined {
    const mark = marks.find(mark => mark.type !== 'ANNOTATION' && mark.start <= position && position < mark.end);
    return mark ? printColor(mark.color) : undefined;
}
