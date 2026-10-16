<script lang="ts">
  import {onMount} from 'svelte';
  import type {AlignmentDto} from '../api/types';
  import {apiClient} from '../api/client';
  import {toolJobStore} from '../stores/toolJobStore';
  import {treeEligible} from './model';
  export let alignments: AlignmentDto[];
  let available = false;
  let error = '';
  onMount(() => { apiClient.tcoffeeCapabilities().then(c => available = c.available).catch(() => {}); });
  $: eligible = alignments.length > 0 && alignments.every(treeEligible);
  async function create() {
    error = '';
    try { for (const alignment of alignments) await toolJobStore.submitTree(alignment); }
    catch (reason) { error = reason instanceof Error ? reason.message : 'Could not submit guide tree'; }
  }
</script>

<button disabled={!available || !eligible} on:click={create} title="T-Coffee guide tree for 3-20 aligned DNA/protein sequences; up to 2,000 ungapped residues each and 10,000 total">Guide tree (T-Coffee)</button>
{#if alignments.some(alignment => alignment.sequences.length < 3)}<small>Guide trees require at least 3 alignment rows.</small>{/if}
{#if error}<p class="error" role="alert">{error}</p>{/if}
