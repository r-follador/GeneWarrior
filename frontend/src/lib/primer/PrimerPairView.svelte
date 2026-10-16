<script lang="ts">
  import type {PrimerDesignDto, PrimerPair} from '../api/types';
  export let design: PrimerDesignDto;
  export let pair: PrimerPair;
  $: length = design.source.sequence.length;
  $: rows = [{name: 'Forward', oligo: pair.left, color: '#287d85'}, {name: 'Reverse', oligo: pair.right, color: '#b45166'},
    ...(pair.probe ? [{name: 'Probe', oligo: pair.probe, color: '#8463ac'}] : [])];
</script>

<div class="primer-map" role="img" aria-label={`Pair ${pair.index + 1}: ${pair.productSize} bp product, positions ${pair.left.start + 1}-${pair.right.end}`}>
  <div class="map-axis"><span>1</span><span>{length} bp</span></div>
  <div class="template-line">
    <span class="product" style:left={`${pair.left.start / length * 100}%`} style:width={`${pair.productSize / length * 100}%`}></span>
    {#if design.settings.target}<span class="target" title={`Target ${design.settings.target.start + 1}-${design.settings.target.end}`} style:left={`${design.settings.target.start / length * 100}%`} style:width={`${(design.settings.target.end - design.settings.target.start) / length * 100}%`}></span>{/if}
  </div>
  {#each rows as row}
    <div class="arrow-track"><span class="oligo-arrow" class:reverse={row.oligo.strand === 'REVERSE'} title={`${row.name}: ${row.oligo.start + 1}-${row.oligo.end}`} style:left={`${row.oligo.start / length * 100}%`} style:width={`${(row.oligo.end - row.oligo.start) / length * 100}%`} style:background={row.color}></span></div>
  {/each}
</div>
<div class="pair-meta">{pair.productSize} bp product &middot; Pair penalty {pair.penalty.toFixed(3)}</div>
<div class="oligo-table">
  {#each rows as row}
    <div class="oligo-row">
      <strong style:color={row.color}>{row.name}</strong>
      <code>{row.oligo.sequence}</code>
      <span class="oligo-stats">{row.oligo.strand === 'REVERSE' ? `${row.oligo.end}-${row.oligo.start + 1}` : `${row.oligo.start + 1}-${row.oligo.end}`} &middot; {row.oligo.sequence.length} nt &middot; Tm {row.oligo.tm.toFixed(1)} &deg;C &middot; GC {row.oligo.gc.toFixed(1)}% &middot; Penalty {row.oligo.penalty.toFixed(3)}</span>
    </div>
  {/each}
</div>

<style>
  .primer-map { margin: 10px 0; width: 100%; }
  .map-axis { display: flex; justify-content: space-between; color: #65767c; font-size: 10px; margin-bottom: 4px; }
  .template-line { position: relative; height: 6px; background: #e4eaeb; margin-bottom: 7px; }
  .product, .target { position: absolute; top: 0; height: 6px; background: #9ec5c9; }
  .target { background: #edca64; }
  .arrow-track { position: relative; height: 13px; }
  .oligo-arrow { position: absolute; height: 9px; min-width: 3px; clip-path: polygon(0 0, 75% 0, 100% 50%, 75% 100%, 0 100%); }
  .oligo-arrow.reverse { clip-path: polygon(25% 0, 100% 0, 100% 100%, 25% 100%, 0 50%); }
  .pair-meta { font-size: 12px; color: #52666c; margin-bottom: 10px; }
  .oligo-row { display: grid; grid-template-columns: 68px minmax(0, 1fr); gap: 5px 10px; padding: 9px 0; border-top: 1px solid #e0e8e8; font-size: 12px; }
  code { font-size: 13px; overflow-wrap: anywhere; user-select: text; }
  .oligo-stats { grid-column: 2; color: #5a6f75; font-size: 11px; }
  @media(max-width: 480px) { .oligo-row { grid-template-columns: 1fr; } .oligo-stats { grid-column: 1; } }
  @media print { .oligo-row { break-inside: avoid; } }
</style>
