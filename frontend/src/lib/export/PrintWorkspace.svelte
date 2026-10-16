<script lang="ts">
  import type {ExportEntry} from './formats';
  import {blocks} from './formats';
  import {annotationFragments, highlightAt, printColor} from './printLayout';
  import LogoViewer from '../logo/LogoViewer.svelte';
  import PrimerPairView from '../primer/PrimerPairView.svelte';
  import TreeViewer from '../tree/TreeViewer.svelte';

  export let name: string;
  export let entries: ExportEntry[];
</script>

<div class="print-workspace">
  <header><strong>GeneWarrior</strong><h1>{name}</h1></header>
  {#each entries as entry}
    {#if entry.type === 'tree'}
      <article class="print-entry">
        <header><h2>{entry.value.name}</h2><p>T-Coffee {entry.value.version} &middot; Neighbor-joining guide tree &middot; {entry.value.sources.length} sequences</p></header>
        <TreeViewer tree={entry.value} width={680} />
        <p>Alignment guide tree, not a phylogenetic inference. Display root is arbitrary. Negative branches are drawn at zero.</p>
      </article>
    {:else if entry.type === 'primers'}
      <article class="print-entry">
        <header><h2>{entry.value.name}</h2><p>Primer3 {entry.value.version} &middot; {entry.value.source.name} &middot; Oligos 5' to 3'</p></header>
        {#each entry.value.pairs as pair}<section class="print-block"><h3>Pair {pair.index + 1}</h3><PrimerPairView design={entry.value} {pair} /></section>{/each}
        {#if !entry.value.pairs.length}<p>No acceptable primer pairs found.</p>{/if}
        <p>Product {entry.value.settings.productMin}-{entry.value.settings.productMax} bp &middot; Primer Tm {entry.value.settings.primers.minTm}/{entry.value.settings.primers.optTm}/{entry.value.settings.primers.maxTm} &deg;C</p>
        {#each entry.value.explanations as explanation}<p>{explanation}</p>{/each}
        <p>Genome-wide specificity not checked.</p>
      </article>
    {:else if entry.type === 'logo'}
      <article class="print-entry">
        <header><h2>{entry.value.name}</h2><p>{entry.value.kind} logo &middot; {entry.value.sequenceCount} sequences &middot; {entry.value.columns.length} positions</p></header>
        <LogoViewer logo={entry.value} columnsPerLine={24} />
      </article>
    {:else}
    {@const value = entry.value}
    {@const length = entry.type === 'sequence' ? entry.value.sequence.length : entry.value.sequences[0]?.value.length ?? 0}
    <article class="print-entry">
      <header>
        <h2>{value.name}</h2>
        <p>{entry.type === 'alignment' ? `${entry.value.sequences.length} sequences aligned` : value.kind === 'DNA' ? 'Nucleotide sequence' : 'Protein sequence'} &middot; {length} {entry.type === 'alignment' ? 'columns' : value.kind === 'DNA' ? 'bp' : 'amino acids'}</p>
      </header>
      {#if entry.type === 'alignment'}
        <ol class="print-legend">{#each entry.value.sequences as row}<li>{row.name}</li>{/each}</ol>
      {/if}
      {#if value.marks.length}
        <ul class="print-marks">
          {#each value.marks as mark}
            <li><i style:background={printColor(mark.color)}></i>{mark.annotation || ''} {mark.start + 1}-{mark.end}{mark.type === 'ANNOTATION' ? (mark.direction === 'REVERSE' ? ' (<-)' : ' (->)') : ''}</li>
          {/each}
        </ul>
      {/if}
      {#each blocks(length) as block}
        <section class="print-block">
          <div class="print-coordinates">{block.start + 1}-{block.end}</div>
          {#each annotationFragments(value.marks, block.start, block.end) as fragment}
            <div class="print-arrow-row">
              <span class="print-arrow" class:reverse={fragment.mark.direction === 'REVERSE'}
                class:continues-before={fragment.continuesBefore} class:continues-after={fragment.continuesAfter}
                style={`--arrow-color:${printColor(fragment.mark.color)};--start:${fragment.start};--length:${fragment.end - fragment.start}`}>
                {fragment.mark.annotation || ''}
              </span>
            </div>
          {/each}
          {#if entry.type === 'sequence'}
            <div class="print-row"><span class="print-row-number"></span><code>{#each [...entry.value.sequence.slice(block.start, block.end)] as character, offset}<span style:background={highlightAt(value.marks, block.start + offset)}>{character}</span>{/each}</code></div>
          {:else}
            {#each entry.value.sequences as row, index}
              <div class="print-row"><span class="print-row-number">{index + 1}</span><code>{#each [...row.value.slice(block.start, block.end)] as character, offset}<span class:print-consensus={Boolean(entry.value.consensus[block.start + offset]?.trim())}>{character}</span>{/each}</code></div>
            {/each}
          {/if}
        </section>
      {/each}
    </article>
    {/if}
  {/each}
</div>

<style>
  .print-workspace { display: none; }
  @media print {
    .print-workspace { display: block; color: #172124; font: 10pt Arial, sans-serif; print-color-adjust: exact; -webkit-print-color-adjust: exact; }
    h1 { font-size: 18pt; margin: 4mm 0 7mm; overflow-wrap: anywhere; }
    h2 { font-size: 12pt; margin: 0 0 2mm; overflow-wrap: anywhere; }
    p { margin: 0; color: #526469; }
    .print-entry { margin-bottom: 8mm; }
    .print-entry > header { border-top: 1px solid #829599; padding-top: 3mm; break-inside: avoid; break-after: avoid; }
    .print-legend, .print-marks { padding-left: 5mm; overflow-wrap: anywhere; break-inside: avoid; break-after: avoid; }
    .print-marks { list-style: none; padding-left: 0; display: flex; flex-wrap: wrap; gap: 2mm 4mm; }
    .print-marks i { display: inline-block; width: 3mm; height: 3mm; border: 1px solid #829599; margin-right: 1mm; }
    .print-block { margin-top: 3mm; break-inside: avoid; }
    .print-coordinates { color: #526469; font-size: 8pt; margin-bottom: 1mm; }
    .print-row, .print-arrow-row { font: 10pt/1.5 monospace; }
    .print-row { display: flex; }
    .print-row-number { display: inline-block; width: 4ch; flex-shrink: 0; font: inherit; color: #526469; }
    code { display: flex; font: inherit; white-space: pre; }
    code span { display: inline-block; width: 1ch; flex-shrink: 0; }
    .print-consensus { background: #fff294; }
    .print-arrow-row { margin-left: 4ch; height: 16pt; width: 60ch; position: relative; }
    .print-arrow { position: absolute; left: calc(var(--start) * 1ch); width: calc(var(--length) * 1ch); height: 13pt; padding: 0 4pt; overflow: hidden; white-space: nowrap; font-size: inherit; line-height: 13pt; background: var(--arrow-color); clip-path: polygon(0 0, calc(100% - 4pt) 0, 100% 50%, calc(100% - 4pt) 100%, 0 100%); }
    .print-arrow.reverse { clip-path: polygon(4pt 0, 100% 0, 100% 100%, 4pt 100%, 0 50%); }
    .print-arrow.continues-before { left: calc(var(--start) * 1ch - 4pt); padding-left: 8pt; }
    .print-arrow.continues-after, .print-arrow.continues-before { width: calc(var(--length) * 1ch + 4pt); clip-path: none; }
    .print-arrow.continues-before.continues-after { width: calc(var(--length) * 1ch + 8pt); }
    .print-arrow.continues-before:not(.reverse):not(.continues-after) { clip-path: polygon(0 0, calc(100% - 4pt) 0, 100% 50%, calc(100% - 4pt) 100%, 0 100%); }
    .print-arrow.reverse.continues-after:not(.continues-before) { clip-path: polygon(4pt 0, 100% 0, 100% 100%, 4pt 100%, 0 50%); }
  }
</style>
