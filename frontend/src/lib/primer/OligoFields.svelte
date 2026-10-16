<script lang="ts">
  import type {OligoConstraints} from '../api/types';
  export let value: OligoConstraints;
  export let label: string;
</script>

<fieldset>
  <legend>{label}</legend>
  <div class="constraint-grid">
    <span></span><span>Minimum</span><span>Optimum</span><span>Maximum</span>
    <span>Length (nt)</span>
    <input type="number" aria-label={`${label} minimum length`} min="15" max="35" required bind:value={value.minSize} />
    <input type="number" aria-label={`${label} optimum length`} min="15" max="35" required bind:value={value.optSize} />
    <input type="number" aria-label={`${label} maximum length`} min="15" max="35" required bind:value={value.maxSize} />
    <span>Tm (&deg;C)</span>
    <input type="number" aria-label={`${label} minimum Tm`} min="30" max="90" step="0.1" required bind:value={value.minTm} />
    <input type="number" aria-label={`${label} optimum Tm`} min="30" max="90" step="0.1" required bind:value={value.optTm} />
    <input type="number" aria-label={`${label} maximum Tm`} min="30" max="90" step="0.1" required bind:value={value.maxTm} />
    <span>GC (%)</span>
    <input type="number" aria-label={`${label} minimum GC`} min="0" max="100" step="0.1" required bind:value={value.minGc} />
    <span></span>
    <input type="number" aria-label={`${label} maximum GC`} min="0" max="100" step="0.1" required bind:value={value.maxGc} />
  </div>
</fieldset>

<style>
  fieldset { border: 0; border-top: 1px solid #d9e2e2; margin: 4px 0; padding: 12px 0 0; min-width: 0; }
  legend { font-size: 13px; font-weight: 600; padding: 0 8px 0 0; }
  .constraint-grid { display: grid; grid-template-columns: 82px repeat(3, minmax(0, 1fr)); gap: 8px; align-items: center; font-size: 11px; }
  input { width: 100%; min-width: 0; padding: 6px; border: 1px solid #c4d3d5; border-radius: 3px; font-size: 13px; }
  @media(max-width: 420px) { .constraint-grid { grid-template-columns: 68px repeat(3, minmax(0, 1fr)); gap: 5px; } }
</style>
