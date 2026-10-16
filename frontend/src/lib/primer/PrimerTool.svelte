<script lang="ts">
  import {onMount} from 'svelte';
  import type {SequenceDto, PrimerSettings} from '../api/types';
  import {apiClient} from '../api/client';
  import {toolJobStore} from '../stores/toolJobStore';
  import {selectionStore} from '../stores/selectionStore';
  import {normalizedRange} from '../selection/selection';
  import Modal from '../components/Modal.svelte';
  import OligoFields from './OligoFields.svelte';
  import {defaultPrimerSettings} from './model';
  import {parsePrimerSettings} from './validation';

  export let source: SequenceDto | undefined;
  let available = false;
  let open = false;
  let snapshot: SequenceDto;
  let settings: PrimerSettings;
  let targetEnabled = false;
  let targetStart = 1;
  let targetEnd = 1;
  let error = '';
  let trigger: HTMLElement;
  onMount(() => { apiClient.primerCapabilities().then(c => available = c.available).catch(() => error = 'Cannot check Primer3 availability.'); });
  $: validSource = source?.kind === 'DNA' && source.sequence.length >= 40 && source.sequence.length <= 20000 && /^[ACGTN]+$/i.test(source.sequence);
  function show() {
    if (!source || !available || !validSource) return;
    snapshot = structuredClone(source);
    const range = $selectionStore.entryId === source.id ? normalizedRange($selectionStore) : undefined;
    const target = range ? {start: range.start, end: range.end + 1} : null;
    settings = defaultPrimerSettings(source.sequence.length, target);
    targetEnabled = Boolean(target);
    targetStart = (target?.start ?? 0) + 1; targetEnd = target?.end ?? source.sequence.length;
    error = ''; open = true;
  }
  async function submit() {
    error = '';
    try {
      settings.target = targetEnabled ? {start: targetStart - 1, end: targetEnd} : null;
      const checked = parsePrimerSettings(settings, snapshot.sequence.length);
      await toolJobStore.submit([snapshot], checked);
      open = false;
    } catch (reason) { error = reason instanceof Error ? reason.message : 'Could not submit primer design'; }
  }
</script>

<button bind:this={trigger} on:click={show} disabled={!available || !validSource} title="Design primers for one DNA template (40-20,000 ungapped A/C/G/T/N bases)">Primer design</button>
{#if !available}<small>Primer3 unavailable</small>{/if}
{#if error && !open}<p class="error">{error}</p>{/if}

{#if open}
  <Modal title="Primer design" id="primer-design" onClose={() => open = false} returnFocus={trigger}>
    <form id="primer-form" on:submit|preventDefault={submit}>
      <p class="source-name"><strong>{snapshot.name}</strong><span>{snapshot.sequence.length} bp</span></p>
      <label class="toggle"><input type="checkbox" bind:checked={targetEnabled} /> Flank target region</label>
      {#if targetEnabled}
        <div class="form-row">
          <label>Target start<input type="number" aria-label="Target start" min="1" max={snapshot.sequence.length} required bind:value={targetStart} /></label>
          <label>Target end<input type="number" aria-label="Target end" min="1" max={snapshot.sequence.length} required bind:value={targetEnd} /></label>
        </div>
      {/if}
      <div class="form-row">
        <label>Product min (bp)<input data-initial-focus type="number" min="40" max="20000" required bind:value={settings.productMin} /></label>
        <label>Product max (bp)<input type="number" min="40" max="20000" required bind:value={settings.productMax} /></label>
        <label>Pairs<input type="number" min="1" max="10" required bind:value={settings.numReturn} /></label>
      </div>
      <OligoFields label="Primers" bind:value={settings.primers} />
      <label class="toggle"><input type="checkbox" bind:checked={settings.includeProbe} /> Include internal probe</label>
      {#if settings.includeProbe}<OligoFields label="Probe" bind:value={settings.probe} />{/if}
      <details>
        <summary>Reaction conditions</summary>
        <div class="form-row">
          <label>Monovalent salt (mM)<input type="number" min="0.1" max="500" step="0.1" required bind:value={settings.saltMonovalent} /></label>
          <label>Mg2+ (mM)<input type="number" min="0" max="100" step="0.1" required bind:value={settings.saltDivalent} /></label>
        </div>
        <div class="form-row">
          <label>dNTP (mM)<input type="number" min="0" max="100" step="0.1" required bind:value={settings.dntp} /></label>
          <label>Oligo concentration (nM)<input type="number" min="0.1" max="1000" step="0.1" required bind:value={settings.dnaConcentration} /></label>
        </div>
      </details>
      <p class="specificity">Genome-wide specificity is not checked.</p>
      {#if error}<p class="error" role="alert">{error}</p>{/if}
    </form>
    <svelte:fragment slot="footer">
      <button on:click={() => open = false}>Cancel</button>
      <button class="primary-button" type="submit" form="primer-form">Design primers</button>
    </svelte:fragment>
  </Modal>
{/if}

<style>
  form { display: grid; gap: 14px; min-width: 0; }
  .source-name { margin: 0; font-size: 13px; overflow-wrap: anywhere; }
  .source-name span { display: block; color: #687a80; font-size: 12px; margin-top: 3px; }
  .form-row { display: flex; gap: 10px; }
  .form-row label { flex: 1; min-width: 0; display: grid; gap: 5px; font-size: 12px; }
  input[type=number] { width: 100%; min-width: 0; padding: 7px; border: 1px solid #c4d3d5; border-radius: 3px; }
  .toggle { display: flex; gap: 8px; align-items: center; font-size: 13px; font-weight: 600; }
  .toggle input[type=checkbox] { width: 16px; height: 16px; flex: 0 0 16px; margin: 0; }
  summary { cursor: pointer; font-size: 13px; margin-bottom: 10px; }
  details .form-row { margin-top: 8px; }
  .specificity, small { color: #687a80; font-size: 12px; margin: 0; }
</style>
