<script lang="ts">
  import type {GuideTreeDto} from '../api/types';
  import {treeLayout} from './layout';
  export let tree: GuideTreeDto;
  export let proportional = true;
  export let width = 760;
  $: model = treeLayout(tree, proportional, width);
</script>

<svg class="guide-tree" xmlns="http://www.w3.org/2000/svg" viewBox={`0 0 ${model.width} ${model.height}`} width={model.width} height={model.height} role="img" aria-label={tree.name}>
  <title>{tree.name}</title>
  <desc>T-Coffee neighbor-joining guide tree. Not a phylogenetic inference. Root position is for display only.</desc>
  <rect width={model.width} height={model.height} fill="white" />
  <g fill="none" stroke="#28747a" stroke-width="1.5">
    {#each model.links as link}<path d={link.path}><title>Branch length: {link.length}</title></path>{/each}
  </g>
  {#each model.nodes as node}
    {#if node.sourceIndex !== null}
      <line x1={node.x + 5} x2={model.labelX - 8} y1={node.y} y2={node.y} stroke="#d3dddd" stroke-dasharray="2 3" />
      <circle cx={node.x} cy={node.y} r="3" fill="#28747a"><title>{node.name}</title></circle>
      <text x={model.labelX} y={node.y - (node.lines.length - 1) * 8 + 4} fill="#263e43" font-size="12" font-family="Arial, sans-serif">
        <title>{node.name}</title>
        {#each node.lines as line, index}<tspan x={model.labelX} dy={index === 0 ? 0 : 16}>{line}</tspan>{/each}
      </text>
    {:else}<circle cx={node.x} cy={node.y} r="2.5" fill="white" stroke="#28747a" stroke-width="1.5" />{/if}
  {/each}
  {#if model.scale !== null}
    <path d={`M18,${model.height - 30}v5h${model.scaleWidth}v-5`} fill="none" stroke="#536d73" />
    <text x="18" y={model.height - 7} font-size="11" font-family="Arial, sans-serif" fill="#536d73">{Number(model.scale.toPrecision(3))} guide distance</text>
  {/if}
</svg>

<style>
  svg { display: block; width: 100%; height: auto; overflow: visible; }
</style>
