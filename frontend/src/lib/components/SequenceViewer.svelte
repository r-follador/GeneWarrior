<script lang="ts">
  import type { SequenceDto, SequenceMarkDto } from '../api/types';
  import { isSelected } from '../selection/selection';
  import { selectionStore } from '../stores/selectionStore';

  export let sequence: SequenceDto;
  export let onselect: () => void = () => {};

  let container: HTMLDivElement;
  let dragging = false;
  let didDrag = false;
  let anchorPosition = 0;
  let pointerStartX = 0;
  let pointerStartY = 0;
  let annotationFragments: {
    annotation: SequenceMarkDto;
    left: number;
    top: number;
    width: number;
    label: string;
    continuesBefore: boolean;
    continuesAfter: boolean;
  }[] = [];

  function markAt(position: number): SequenceMarkDto | undefined {
    return sequence.marks.find((mark) => isFindHit(mark) && position >= mark.start && position < mark.end)
      ?? sequence.marks.find((mark) => mark.type !== 'ANNOTATION' && position >= mark.start && position < mark.end);
  }

  function isFindHit(mark: SequenceMarkDto | undefined) {
    return (mark as { type?: string } | undefined)?.type === 'FIND_HIT';
  }

  function startsFindHit(position: number, mark: SequenceMarkDto | undefined) {
    return isFindHit(mark) && mark?.start === position;
  }

  function endsFindHit(position: number, mark: SequenceMarkDto | undefined) {
    return isFindHit(mark) && mark !== undefined && mark.end - 1 === position;
  }

  function safeColor(color: string) {
    return /^#[0-9a-f]{6}$/i.test(color) ? color : '#d9e2e2';
  }

  function annotationMarks() {
    return sequence.marks.filter((mark) => mark.type === 'ANNOTATION');
  }

  function layoutAnnotations() {
    if (!container) return;
    const containerRect = container.getBoundingClientRect();
    const characters = [...container.querySelectorAll<HTMLElement>('[data-position]')];
    annotationFragments = annotationMarks().flatMap((annotation) => {
      const rowFragments: { left: number; top: number; width: number }[] = [];
      let current: { left: number; top: number; right: number } | undefined;
      characters
        .filter((character) => {
          const position = Number(character.dataset.position);
          return position >= annotation.start && position < annotation.end;
        })
        .forEach((character) => {
          const rect = character.getBoundingClientRect();
          const left = rect.left - containerRect.left;
          const top = rect.top - containerRect.top;
          const right = rect.right - containerRect.left;
          if (!current || Math.abs(current.top - top) > 1) {
            if (current) rowFragments.push({ left: current.left, top: current.top, width: current.right - current.left });
            current = { left, top, right };
          } else {
            current.right = right;
          }
        });
      if (current) rowFragments.push({ left: current.left, top: current.top, width: current.right - current.left });
      return rowFragments.map((fragment, index) => ({
        ...fragment,
        annotation,
        label: annotation.annotation ?? '',
        continuesBefore: index > 0,
        continuesAfter: index < rowFragments.length - 1
      }));
    });
  }

  function startSelection(position: number, event: PointerEvent) {
    if (event.button !== 0) return;
    dragging = true;
    didDrag = false;
    anchorPosition = position;
    pointerStartX = event.clientX;
    pointerStartY = event.clientY;
    event.preventDefault();
    event.stopPropagation();
  }

  function extendSelection(position: number) {
    selectionStore.update((selection) => ({
      ...selection,
      end: dragging && selection.entryId === sequence.id ? position : selection.end,
      hoverEntryId: sequence.id,
      hover: position
    }));
  }

  function clearHover() {
    selectionStore.update((selection) => selection.hoverEntryId === sequence.id ? { ...selection, hoverEntryId: undefined, hover: undefined } : selection);
  }

  function stopSelection() {
    if (dragging && !didDrag) {
      selectionStore.set({ hoverEntryId: sequence.id, hover: anchorPosition });
    }
    dragging = false;
    selectionStore.update((selection) => ({ ...selection, dragging: false }));
  }

  function nearestPosition(event: PointerEvent) {
    if (!dragging) return;
    const pointerDistance = Math.hypot(event.clientX - pointerStartX, event.clientY - pointerStartY);
    if (!didDrag && pointerDistance < 4) return;
    const characters = container.querySelectorAll<HTMLElement>('[data-position]');
    let nearest = 0;
    let nearestDistance = Number.POSITIVE_INFINITY;
    characters.forEach((character) => {
      const rect = character.getBoundingClientRect();
      const dx = event.clientX < rect.left ? rect.left - event.clientX : event.clientX > rect.right ? event.clientX - rect.right : 0;
      const dy = event.clientY < rect.top ? rect.top - event.clientY : event.clientY > rect.bottom ? event.clientY - rect.bottom : 0;
      const distance = dx * dx + dy * dy;
      if (distance < nearestDistance) {
        nearestDistance = distance;
        nearest = Number(character.dataset.position);
      }
    });
    if (!didDrag) {
      didDrag = true;
      onselect();
      selectionStore.set({ entryId: sequence.id, start: anchorPosition, end: nearest, hoverEntryId: sequence.id, hover: nearest, dragging: true });
      return;
    }
    extendSelection(nearest);
  }

  function observeLayout(node: HTMLElement) {
    const observer = new ResizeObserver(() => requestAnimationFrame(layoutAnnotations));
    observer.observe(node);
    requestAnimationFrame(layoutAnnotations);
    return {
      update: () => requestAnimationFrame(layoutAnnotations),
      destroy: () => observer.disconnect()
    };
  }
</script>

<svelte:window on:pointerup={stopSelection} on:pointermove={nearestPosition} />

<div bind:this={container} class:has-annotations={annotationMarks().length > 0} class="character-sequence" role="presentation" on:mouseleave={clearHover} use:observeLayout>
  {#each annotationFragments as fragment}
    <span
      class:reverse={fragment.annotation.direction === 'REVERSE'}
      class:continues-before={fragment.continuesBefore}
      class:continues-after={fragment.continuesAfter}
      class="sequence-annotation"
      style={`--annotation-color:${safeColor(fragment.annotation.color)};left:${fragment.left - (fragment.continuesBefore ? 8 : 0)}px;top:${fragment.top}px;width:${fragment.width + (fragment.continuesBefore ? 8 : 0) + (fragment.continuesAfter ? 8 : 0)}px`}
      title={fragment.label}
    >{fragment.label}</span>
  {/each}
  {#each [...sequence.sequence] as character, position}
    {@const mark = markAt(position)}
    <span
      role="presentation"
      data-position={position}
      class:marked={mark}
      class:find-hit={isFindHit(mark)}
      class:find-hit-start={startsFindHit(position, mark)}
      class:find-hit-end={endsFindHit(position, mark)}
      class:range-selected={isSelected($selectionStore, sequence.id ?? '', position)}
      style={mark ? `--mark-color: ${safeColor(mark.color)}` : ''}
      on:pointerdown={(event) => startSelection(position, event)}
      on:pointerenter={() => extendSelection(position)}
    >{character}</span>
  {/each}
</div>
