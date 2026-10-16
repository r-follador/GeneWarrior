<script lang="ts">
  import type {SequenceLogoDto} from '../api/types';
  import {logoLayout} from './layout';
  import {glyphs} from './glyphs';
  import license from '../../../public/licenses/dejavu.txt?raw';

  export let logo: SequenceLogoDto;
  export let columnsPerLine = 40;
  $: lines = logoLayout(logo, columnsPerLine);
  $: frequency = logo.mode === 'FREQUENCY';
</script>

<div class="logo-plot">
  {#each lines as line}
    <svg xmlns="http://www.w3.org/2000/svg" viewBox={`0 0 ${line.width} ${line.height}`}
      width={line.width} height={line.height} role="img" aria-label={`${logo.name}, positions ${line.columns[0].position}-${line.columns.at(-1)?.position}, ${frequency ? 'frequency' : 'information in bits'}`}>
      <title>{logo.name} ({frequency ? 'Frequency' : 'Information, bits'})</title>
      <desc>Uniform background; no pseudocounts, weighting or small-sample correction. Gaps and ambiguous symbols excluded; DNA U counted as T. Coverage bar: counted residues / sequences. {logo.sequenceCount} sequences. {logo.method}.</desc>
      <metadata>{license}</metadata>
      <rect width={line.width} height={line.height} fill="white" />
      <g font-family="sans-serif" font-size="10" fill="#526469">
        <text x="48" y="13">{frequency ? 'Frequency (%)' : 'Bits (uncorrected)'}</text>
        <text x={line.width - 12} y="13" text-anchor="end">{line.columns[0].position}-{line.columns.at(-1)?.position}</text>
        {#each line.ticks as tick}
          {@const y = 168 - tick / line.maximum * 140}
          <line x1="44" x2={line.width - 12} y1={y} y2={y} stroke="#e4e9e8" stroke-width="1" />
          <text x="39" y={y + 3} text-anchor="end">{frequency ? Math.round(tick * 100) : Number(tick.toFixed(2))}</text>
        {/each}
        <line x1="44" x2="44" y1="28" y2="168" stroke="#829599" />
        <text x="39" y="201" text-anchor="end" font-size="8">n</text>
        {#each line.columns as column}
          <g data-logo-position={column.position}>
            <title>Position {column.position}: {column.observations}/{logo.sequenceCount} counted ({Math.round(column.coverage * 100)}%); {column.gaps} gaps; {column.ignored} ambiguous/stops; {column.information.toFixed(3)} bits</title>
            <rect x={column.x} y="28" width="20" height="140" fill="transparent" />
            {#each column.glyphs as symbol}
              <path d={glyphs[symbol.residue]} fill={symbol.color}
                transform={`translate(${column.x},${symbol.y}) scale(0.2,${symbol.height / 100})`}>
                <title>Position {column.position}, {symbol.residue}: {symbol.count}/{column.observations} ({(symbol.frequency * 100).toFixed(1)}%); {symbol.bits.toFixed(3)} bits</title>
              </path>
            {/each}
            <text x={column.x + 10} y="182" text-anchor="middle" font-size="9">{column.position}</text>
            <rect x={column.x} y="191" width="20" height="4" fill="#e4e9e8" />
            <rect x={column.x} y="191" width={20 * column.coverage} height="4" fill="#627a80" />
            <text x={column.x + 10} y="207" text-anchor="middle" font-size={column.observations >= 10000 ? 7 : 8}>{column.observations}</text>
          </g>
        {/each}
      </g>
    </svg>
  {/each}
</div>

<style>
  .logo-plot { display: grid; gap: 12px; }
  svg { display: block; max-width: 100%; height: auto; break-inside: avoid; }
</style>
