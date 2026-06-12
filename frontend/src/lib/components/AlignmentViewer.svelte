<script lang="ts">
  import { onMount } from 'svelte';
  import { isSelected } from '../selection/selection';
  import { selectionStore } from '../stores/selectionStore';

  export let entryId: string;
  export let sequences: { name: string; value: string }[];
  export let consensus: string;
  export let onselect: () => void = () => {};

  let container: HTMLDivElement;
  let columns = 60;
  let dragging = false;
  let didDrag = false;
  let anchorPosition = 0;
  let pointerStartX = 0;
  let pointerStartY = 0;

  $: length = sequences[0]?.value.length ?? 0;
  $: blocks = Array.from({ length: Math.ceil(length / columns) }, (_, index) => ({
    start: index * columns,
    end: Math.min((index + 1) * columns, length)
  }));

  onMount(() => {
    const updateColumns = () => {
      columns = Math.max(12, Math.floor((container.clientWidth - 132) / 7.25));
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
</script>

<svelte:window on:pointerup={stopSelection} on:pointermove={nearestPosition} />

<div bind:this={container} class="wrapped-alignment" role="presentation" on:mouseleave={clearHover}>
  {#each blocks as block}
    <div class="alignment-block">
      <div class="alignment-ruler"><span>{block.start + 1}</span><span>{block.end}</span></div>
      {#each sequences as sequence}
        <div class="alignment-row">
          <strong>{sequence.name}</strong>
          <code>
            {#each [...sequence.value.slice(block.start, block.end)] as character, offset}
              {@const position = block.start + offset}
              <span
                role="presentation"
                data-position={position}
                class:range-selected={isSelected($selectionStore, entryId, position)}
                on:pointerdown={(event) => startSelection(position, event)}
                on:pointerenter={() => extendSelection(position)}
              >{character}</span>
            {/each}
          </code>
        </div>
      {/each}
      <div class="alignment-row consensus">
        <strong>Consensus</strong>
        <code>{consensus.slice(block.start, block.end)}</code>
      </div>
    </div>
  {/each}
</div>
