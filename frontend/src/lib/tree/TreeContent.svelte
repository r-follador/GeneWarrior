<script lang="ts">
  import type {GuideTreeDto} from '../api/types';
  import {treeNewick} from './model';
  import {downloadBlob} from '../export/download';
  import {filenameStem} from '../export/formats';
  import TreeViewer from './TreeViewer.svelte';
  export let tree: GuideTreeDto;
  let proportional = true;
  let width = 760;
  let container: HTMLDivElement;
  $: newick = treeNewick(tree);
  function resize(node: HTMLElement) {
    const observer = new ResizeObserver(entries => width = Math.max(300, entries[0].contentRect.width));
    observer.observe(node);
    return {destroy: () => observer.disconnect()};
  }
  function svgDownload() {
    const svg = container.querySelector('svg');
    if (svg) downloadBlob(new Blob([new XMLSerializer().serializeToString(svg)], {type: 'image/svg+xml'}), `${filenameStem(tree.name)}.svg`);
  }
</script>

<div class="tree-content" bind:this={container} use:resize on:copy|stopPropagation>
  <div class="tree-controls">
    <label>Layout <select bind:value={proportional}><option value={true}>Branch lengths</option><option value={false}>Topology</option></select></label>
    <button on:click={svgDownload}>Download SVG</button>
    <button on:click={() => downloadBlob(new Blob([newick], {type: 'text/plain;charset=utf-8'}), `${filenameStem(tree.name)}.nwk`)}>Download Newick</button>
  </div>
  <TreeViewer {tree} {proportional} {width} />
  <p class="tree-note">Alignment guide tree, not a phylogenetic inference. Display root is arbitrary.</p>
  {#if tree.nodes.some(node => (node.length ?? 0) < 0)}<p class="tree-note">Negative branch lengths are drawn at zero; original values are retained in Newick.</p>{/if}
  {#if proportional && tree.nodes.every(node => (node.length ?? 0) <= 0)}<p class="tree-note">No positive branch lengths; topology shown.</p>{/if}
  <details>
    <summary>Tree details</summary>
    <p>T-Coffee {tree.version} &middot; Neighbor joining &middot; {tree.sources.length} sequences</p>
    <p>Recomputed from ungapped alignment rows using {"slow_pair,lalign_id_pair"}. Branch lengths are guide distances, not evolutionary time or bootstrap support.</p>
    <pre>{newick}</pre>
  </details>
</div>

<style>
  .tree-content { padding: 14px; min-width: 0; }
  .tree-controls { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-bottom: 12px; font-size: 12px; }
  label { display: flex; gap: 6px; align-items: center; }
  select, button { padding: 6px 8px; border: 1px solid #bacaca; border-radius: 4px; background: white; color: #294a50; }
  .tree-note, details p { font-size: 11px; color: #657a80; margin-top: 8px; }
  summary { cursor: pointer; font-size: 12px; margin-top: 12px; }
  pre { white-space: pre-wrap; overflow-wrap: anywhere; font-size: 11px; }
</style>
