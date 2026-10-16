<script lang="ts">
  import {toolJobStore, type WorkspaceJob} from '../stores/toolJobStore';
  import {jobBusy, jobEntryId, jobTitle, jobIcon, jobResultLabel} from '../jobs/workspaceJobs';
  import {projectStore} from '../stores/projectStore';
  export let item: WorkspaceJob;
  $: busy = jobBusy(item);
  $: status = item.abandoned ? 'Request failed' : item.job?.status === 'RUNNING' ? 'Working...'
    : item.job?.status === 'QUEUED' ? 'Queued' : item.job?.status === 'SUCCEEDED' ? 'Result ready'
    : item.job?.status === 'FAILED' ? 'Failed' : item.job?.status === 'CANCELLED' ? 'Cancelled' : 'Submitting...';
</script>

<article id={`entry-${jobEntryId(item)}`} class="entry-card job-card" class:type-alignment={item.tool !== 'PRIMER3'} class:type-other={item.tool === 'PRIMER3'} aria-busy={busy}>
  <header class="entry-card-header">
    <div>
      <div class="entry-heading"><span class="entry-icon">{jobIcon(item)}</span><h2>{jobTitle(item)}</h2></div>
      <div class="entry-meta"><p>{item.tree?.name ?? item.sequences[0]?.name}{item.tool !== 'PRIMER3' ? ` · ${item.tree?.sequences.length ?? item.sequences.length} sequences` : ''}</p></div>
    </div>
  </header>
  <div class="job-card-content">
    <div class="job-state" role="status">{#if busy}<span class="job-spinner" aria-hidden="true"></span>{/if}<strong>{status}</strong></div>
    {#if item.problem || item.job?.error}<p class="error">{item.problem || item.job?.error}</p>{/if}
    {#if item.job?.status === 'SUCCEEDED' && !$projectStore.entryOrder.includes(item.sourceId)}<p>Source entry was deleted.</p>{/if}
    <div class="job-buttons">
      {#if busy}
        <button on:click={() => toolJobStore.cancel(item.id)}>Cancel</button>
      {:else}
        {#if item.job?.status === 'SUCCEEDED'}
          <button on:click={() => toolJobStore.insert(item.id)}>Add {jobResultLabel(item)}</button>
        {:else}<button on:click={() => toolJobStore.retry(item.id)}>Retry</button>{/if}
        <button on:click={() => toolJobStore.dismiss(item.id)}>Dismiss</button>
      {/if}
    </div>
  </div>
</article>

<style>
  .job-card { animation: job-appear 180ms ease-out; }
  .job-card .entry-card-header { cursor: default; }
  .job-card-content { padding: 16px; }
  .job-state, .job-buttons { display: flex; align-items: center; gap: 10px; }
  .job-state { font-size: 13px; }
  .job-buttons { margin-top: 12px; }
  .job-buttons button { border: 1px solid #c4d4d5; border-radius: 3px; padding: 6px 9px; color: #284d54; background: white; font-size: 12px; }
  .job-buttons button:hover { border-color: #7da6a6; }
  .job-spinner { display: inline-block; width: 18px; height: 18px; border: 2px solid #d5e3e5; border-top-color: #247d87; border-radius: 50%; animation: job-spin 900ms linear infinite; flex-shrink: 0; }
  p { font-size: 12px; overflow-wrap: anywhere; }
  @keyframes job-spin { to { transform: rotate(360deg); } }
  @keyframes job-appear { from { opacity: 0; } to { opacity: 1; } }
  @media (prefers-reduced-motion: reduce) { .job-card, .job-spinner { animation: none; } }
</style>
