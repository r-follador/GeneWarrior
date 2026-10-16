<script lang="ts">
  import type {SequenceLogoDto} from '../api/types';
  import {projectStore} from '../stores/projectStore';
  import {downloadBlob} from '../export/download';
  import {filenameStem} from '../export/formats';
  import LogoViewer from './LogoViewer.svelte';

  export let logo: SequenceLogoDto;
  let container: HTMLDivElement;
  let columnsPerLine = 40;

  function resize(node: HTMLElement) {
    const observer = new ResizeObserver(() => { columnsPerLine = Math.max(8, Math.min(40, Math.floor((node.clientWidth - 60) / 22))); });
    observer.observe(node);
    return {destroy: () => observer.disconnect()};
  }

  function exportSvg() {
    const rows = [...container.querySelectorAll('svg')];
    if (!rows.length) return;
    const ns = 'http://www.w3.org/2000/svg';
    const svg = document.createElementNS(ns, 'svg');
    const width = rows[0].viewBox.baseVal.width;
    const height = rows.reduce((sum, row) => sum + row.viewBox.baseVal.height + 12, 0) - 12;
    svg.setAttribute('viewBox', `0 0 ${width} ${height}`);
    svg.setAttribute('width', String(width));
    svg.setAttribute('height', String(height));
    const title = document.createElementNS(ns, 'title');
    title.textContent = logo.name;
    svg.append(title);
    let y = 0;
    for (const row of rows) {
      const group = document.createElementNS(ns, 'g');
      group.setAttribute('transform', `translate(0,${y})`);
      for (const child of row.children) group.append(child.cloneNode(true));
      svg.append(group);
      y += row.viewBox.baseVal.height + 12;
    }
    downloadBlob(new Blob([new XMLSerializer().serializeToString(svg)], {type: 'image/svg+xml'}), `${filenameStem(logo.name)}.svg`);
  }
</script>

<div class="logo-content" bind:this={container} use:resize>
  <div class="logo-controls">
    <label>Logo scale
      <select value={logo.mode} on:change={(event) => projectStore.replaceLogo({...logo, mode: event.currentTarget.value as SequenceLogoDto['mode']})}>
        <option value="FREQUENCY">Frequency (%)</option>
        <option value="INFORMATION">Information (bits)</option>
      </select>
    </label>
    <button on:click={exportSvg}>Download SVG</button>
  </div>
  <LogoViewer {logo} {columnsPerLine} />
</div>

<style>
  .logo-content { padding: 14px; min-width: 0; }
  .logo-controls { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; margin-bottom: 14px; }
  label { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; font-size: 12px; }
  select, button { padding: 6px 8px; border: 1px solid #bacaca; border-radius: 4px; background: white; color: #294a50; font-size: 12px; }
</style>
