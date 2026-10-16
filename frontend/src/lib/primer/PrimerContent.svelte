<script lang="ts">
  import type {PrimerDesignDto} from '../api/types';
  import {pairSequences} from './model';
  import {fasta} from '../export/formats';
  import {downloadFasta} from '../export/download';
  import {projectStore} from '../stores/projectStore';
  import PrimerPairView from './PrimerPairView.svelte';
  export let design: PrimerDesignDto;
  let selectedPair = 0;
  let message = '';
  $: pair = design.pairs[selectedPair] ?? design.pairs[0];
  async function copy() {
    try { await navigator.clipboard.writeText(fasta(pairSequences(design, pair))); message = 'Copied'; }
    catch { message = 'Clipboard unavailable'; }
  }
</script>

<div class="primer-content" on:copy|stopPropagation>
  {#if pair}
    <div class="pair-toolbar">
      <label>Pair <select aria-label="Primer pair" bind:value={selectedPair}>{#each design.pairs as candidate}<option value={candidate.index}>{candidate.index + 1} · {candidate.productSize} bp</option>{/each}</select></label>
      <span>Oligos 5' to 3'</span>
    </div>
    <PrimerPairView {design} {pair} />
    <div class="primer-actions">
      <button on:click={copy}>Copy oligos</button>
      <button on:click={() => downloadFasta(design.name, pairSequences(design, pair))}>Export pair FASTA</button>
      <button on:click={() => projectStore.addSequencesAfter(design.id, pairSequences(design, pair))}>Add oligos as sequences</button>
      {#if message}<span role="status">{message}</span>{/if}
    </div>
  {:else}<p class="no-primers">No acceptable primer {design.settings.includeProbe ? 'and probe ' : ''}pairs found.</p>{/if}
  <details open={!design.pairs.length}>
    <summary>Design details</summary>
    <p>Primer3 {design.version} &middot; {design.source.name} &middot; {design.source.sequence.length} bp</p>
    <p>Product {design.settings.productMin}-{design.settings.productMax} bp &middot; Primer length {design.settings.primers.minSize}/{design.settings.primers.optSize}/{design.settings.primers.maxSize} nt &middot; Tm {design.settings.primers.minTm}/{design.settings.primers.optTm}/{design.settings.primers.maxTm} &deg;C &middot; GC {design.settings.primers.minGc}-{design.settings.primers.maxGc}%</p>
    {#if design.settings.includeProbe}<p>Probe length {design.settings.probe.minSize}/{design.settings.probe.optSize}/{design.settings.probe.maxSize} nt &middot; Tm {design.settings.probe.minTm}/{design.settings.probe.optTm}/{design.settings.probe.maxTm} &deg;C &middot; GC {design.settings.probe.minGc}-{design.settings.probe.maxGc}%</p>{/if}
    <p>Monovalent salt {design.settings.saltMonovalent} mM &middot; Mg2+ {design.settings.saltDivalent} mM &middot; dNTP {design.settings.dntp} mM &middot; Oligos {design.settings.dnaConcentration} nM</p>
    {#if design.settings.target}<p>Target {design.settings.target.start + 1}-{design.settings.target.end}</p>{/if}
    {#each design.explanations as explanation}<p>{explanation}</p>{/each}
  </details>
  <small>Genome-wide specificity not checked.</small>
</div>

<style>
  .primer-content { padding: 15px; }
  .pair-toolbar, .primer-actions { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; font-size: 12px; }
  .pair-toolbar { justify-content: space-between; }
  .pair-toolbar label { display: flex; gap: 8px; align-items: center; }
  .primer-actions { margin: 12px 0; }
  .primer-actions button, select { border: 1px solid #c4d4d5; border-radius: 3px; padding: 6px 9px; color: #284d54; background: white; }
  summary { cursor: pointer; font-size: 12px; margin: 12px 0 8px; }
  details p { font-size: 12px; color: #536a71; overflow-wrap: anywhere; }
  small { color: #657a80; font-size: 11px; }
  .no-primers { font-size: 13px; font-weight: 600; }
</style>
