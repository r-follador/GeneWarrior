<script lang="ts">
  import { onMount } from 'svelte';
  import type { SequenceMarkDto } from '../api/types';
  import { isSelected } from '../selection/selection';
  import type { SearchHit } from '../sequence/searchUtils';
  import { selectionStore } from '../stores/selectionStore';

  export let entryId: string;
  export let sequences: { name: string; value: string }[];
  export let consensus: string;
  export let marks: SequenceMarkDto[] = [];
  export let onselect: () => void = () => {};
  export let findHits: SearchHit[] = [];

  let container: HTMLDivElement;
  let columns = 60;
  let dragging = false;
  let didDrag = false;
  let anchorPosition = 0;
  let pointerStartX = 0;
  let pointerStartY = 0;
  let hoveredRow: number | undefined;
  let annotationFragments: {
    annotation: SequenceMarkDto;
    left: number;
    top: number;
    width: number;
    label: string;
    continuesBefore: boolean;
    continuesAfter: boolean;
  }[] = [];

  $: length = sequences[0]?.value.length ?? 0;
  $: blocks = Array.from({ length: Math.ceil(length / columns) }, (_, index) => ({
    start: index * columns,
    end: Math.min((index + 1) * columns, length)
  }));

  onMount(() => {
    const updateColumns = () => {
      columns = Math.max(12, Math.floor((container.clientWidth - 54) / 7.25));
    };
    const observer = new ResizeObserver(updateColumns);
    observer.observe(container);
    updateColumns();
    return () => observer.disconnect();
  });

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
      end: dragging && selection.entryId === entryId ? position : selection.end,
      hoverEntryId: entryId,
      hover: position
    }));
  }

  function clearHover() {
    hoveredRow = undefined;
    selectionStore.update((selection) => selection.hoverEntryId === entryId ? { ...selection, hoverEntryId: undefined, hover: undefined } : selection);
  }

  function stopSelection() {
    if (dragging && !didDrag) {
      selectionStore.set({ hoverEntryId: entryId, hover: anchorPosition });
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
      selectionStore.set({ entryId, start: anchorPosition, end: nearest, hoverEntryId: entryId, hover: nearest, dragging: true });
      return;
    }
    extendSelection(nearest);
  }

  function findHitAt(row: number, position: number) {
    return findHits.find((hit) => hit.row === row && position >= hit.start && position <= hit.end);
  }

  function startsFindHit(row: number, position: number) {
    return findHits.some((hit) => hit.row === row && hit.start === position);
  }

  function endsFindHit(row: number, position: number) {
    return findHits.some((hit) => hit.row === row && hit.end === position);
  }

  function hasConsensus(position: number) {
    return Boolean(consensus[position]?.trim());
  }

  function safeColor(color: string) {
    return /^#[0-9a-f]{6}$/i.test(color) ? color : '#4e9d86';
  }

  function annotationMarks() {
    return marks.filter((mark) => mark.type === 'ANNOTATION');
  }

  function layoutAnnotations() {
    if (!container) return;
    const containerRect = container.getBoundingClientRect();
    const characters = [...container.querySelectorAll<HTMLElement>('[data-row="0"][data-position]')];
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

  function observeLayout(node: HTMLElement, _marks: SequenceMarkDto[]) {
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

<div bind:this={container} class:has-annotations={annotationMarks().length > 0} class="wrapped-alignment" role="presentation" on:mouseleave={clearHover} use:observeLayout={marks}>
  {#each annotationFragments as fragment}
    <span
      class:reverse={fragment.annotation.direction === 'REVERSE'}
      class:continues-before={fragment.continuesBefore}
      class:continues-after={fragment.continuesAfter}
      class="alignment-annotation"
      style={`--annotation-color:${safeColor(fragment.annotation.color)};left:${fragment.left - (fragment.continuesBefore ? 8 : 0)}px;top:${fragment.top}px;width:${fragment.width + (fragment.continuesBefore ? 8 : 0) + (fragment.continuesAfter ? 8 : 0)}px`}
      title={fragment.label}
    >{fragment.label}</span>
  {/each}
  <div class="alignment-legend" aria-label="Alignment row legend">
    {#each sequences as sequence, row}
      <span class:active={hoveredRow === row}><strong>{row + 1}</strong> {sequence.name}</span>
    {/each}
  </div>
  {#each blocks as block}
    <div class="alignment-block">
      {#each sequences as sequence, row}
        <div class="alignment-row">
          <strong title={sequence.name}>{row + 1}</strong>
          <code>
            {#each [...sequence.value.slice(block.start, block.end)] as character, offset}
              {@const position = block.start + offset}
              <span
                role="presentation"
                data-row={row}
                data-position={position}
                class:alignment-consensus={hasConsensus(position)}
                class:find-hit={findHitAt(row, position)}
                class:find-hit-start={startsFindHit(row, position)}
                class:find-hit-end={endsFindHit(row, position)}
                class:range-selected={isSelected($selectionStore, entryId, position)}
                on:pointerdown={(event) => startSelection(position, event)}
                on:pointerenter={() => {
                  hoveredRow = row;
                  extendSelection(position);
                }}
              >{character}</span>
            {/each}
          </code>
        </div>
      {/each}
    </div>
  {/each}
</div>
