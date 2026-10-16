<script lang="ts">
  import {onMount} from 'svelte';
  import type {SequenceDto, TcoffeeCapabilities} from '../api/types';
  import {apiClient} from '../api/client';
  import {toolJobStore} from '../stores/toolJobStore';
  import {projectStore} from '../stores/projectStore';

  export let sequences: SequenceDto[] = [];
  let capabilities: TcoffeeCapabilities | undefined;
  let error = '';
  const notice = toolJobStore.notice;
  onMount(() => { apiClient.tcoffeeCapabilities().then(value => capabilities = value).catch(() => error = 'Cannot check T-Coffee availability.'); });
  $: valid = capabilities?.available && sequences.length >= 2 && sequences.length <= capabilities.maxSequences
    && sequences.every(sequence => sequence.kind === sequences[0].kind && sequence.sequence.length <= capabilities!.maxLength
      && (sequence.kind === 'DNA' ? /^[ACGT]+$/i : /^[ACDEFGHIKLMNPQRSTVWY]+$/i).test(sequence.sequence))
    && sequences.reduce((sum, sequence) => sum + sequence.sequence.length, 0) <= capabilities.maxResidues;
  async function submit() {
    error = '';
    try { await toolJobStore.submit(sequences); }
    catch (reason) { error = reason instanceof Error ? reason.message : 'Could not submit alignment'; }
  }
</script>

<button on:click={submit} disabled={!valid} title="Align 2-20 ungapped sequences of the same kind. A/C/G/T DNA or standard 20-letter proteins; up to 2,000 residues each, 10,000 total.">T-Coffee</button>
{#if capabilities && !capabilities.available}<p class="job-note">T-Coffee unavailable</p>{/if}
{#if error || $notice}<p class="error" role="alert">{error || $notice}</p>{/if}
<div class="alignment-jobs" aria-live="polite">
  {#each $toolJobStore.filter(item => !item.tool || item.tool === 'TCOFFEE') as item (item.id)}
    <div class="alignment-job">
      <div class="job-title">{item.sequences[0]?.name} <span>{item.sequences.length} sequences</span></div>
      <div class="job-note">{item.applied ? 'Alignment added' : item.job?.status === 'SUCCEEDED' ? 'Alignment ready' : item.job?.status.toLowerCase() ?? 'Submitting'}</div>
      {#if item.problem || item.job?.error}<p class="error">{item.problem || item.job?.error}</p>{/if}
      <div class="job-actions">
        {#if item.job?.status === 'SUCCEEDED' && !item.applied}
          {#if !$projectStore.entryOrder.includes(item.sourceId)}<span class="job-note">Source entry was deleted.</span>{/if}
          <button on:click={() => toolJobStore.insert(item.id)}>Add alignment</button>
          <button on:click={() => toolJobStore.dismiss(item.id)}>Dismiss</button>
        {:else if item.applied || item.abandoned || item.job?.status === 'FAILED' || item.job?.status === 'CANCELLED'}
          {#if !item.applied}<button on:click={() => toolJobStore.retry(item.id)}>Retry</button>{/if}
          <button on:click={() => toolJobStore.dismiss(item.id)}>Dismiss</button>
        {:else}
          <button on:click={() => toolJobStore.cancel(item.id)}>Cancel</button>
        {/if}
      </div>
    </div>
  {/each}
</div>

<style>
  .alignment-job { border-top: 1px solid #d9e0e1; padding: 10px 0; margin-top: 8px; }
  .job-title { font-size: 12px; overflow-wrap: anywhere; }
  .job-title span, .job-note { color: #647176; font-size: 11px; }
  .job-title span { display: block; }
  .job-note { margin: 4px 0; }
  .job-actions { display: flex; flex-wrap: wrap; gap: 6px; }
  .error { font-size: 12px; overflow-wrap: anywhere; }
</style>
