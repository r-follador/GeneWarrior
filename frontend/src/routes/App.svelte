<script lang="ts">
  import { flushSync } from 'svelte';
  import { slide } from 'svelte/transition';
  import { apiClient, ApiError } from '../lib/api/client';
  import type { SequenceDto, SequenceMarkDto, TranslationFramesDto } from '../lib/api/types';
  import AlignmentViewer from '../lib/components/AlignmentViewer.svelte';
  import SequenceViewer from '../lib/components/SequenceViewer.svelte';
  import { normalizedRange } from '../lib/selection/selection';
  import { findDnaHits, findProteinHits, sanitizeDnaQuery, sanitizeProteinQuery, type SearchHit } from '../lib/sequence/searchUtils';
  import { projectStore } from '../lib/stores/projectStore';
  import { selectionStore } from '../lib/stores/selectionStore';
  import { uiStore, persistenceError } from '../lib/stores/uiStore';
  import { shareUrl } from '../lib/project/workspacePersistence';
  import { exportEntries, type ExportEntry, type ExportScope } from '../lib/export/formats';
  import { downloadClustal, downloadFasta } from '../lib/export/download';
  import PrintWorkspace from '../lib/export/PrintWorkspace.svelte';
  import LogoContent from '../lib/logo/LogoContent.svelte';
  import Modal from '../lib/components/Modal.svelte';
  import TcoffeeTool from '../lib/components/TcoffeeTool.svelte';
  import PrimerTool from '../lib/primer/PrimerTool.svelte';
  import PrimerContent from '../lib/primer/PrimerContent.svelte';
  import JobCard from '../lib/components/JobCard.svelte';
  import {toolJobStore} from '../lib/stores/toolJobStore';
  import {workspaceOrder, jobEntryId, jobTitle, jobIcon} from '../lib/jobs/workspaceJobs';
  import TreeTool from '../lib/tree/TreeTool.svelte';
  import TreeContent from '../lib/tree/TreeContent.svelte';
  import {allPrimerSequences} from '../lib/primer/model';

  const translationTables = [
    { id: 1, label: 'Standard Code' },
    { id: 2, label: 'Vertebrate Mitochondrial Code' },
    { id: 3, label: 'Yeast Mitochondrial Code' },
    { id: 4, label: 'Mold, Protozoan, Coelenterate Mitochondrial Code & Mycoplasma/Spiroplasma Code' },
    { id: 5, label: 'Invertebrate Mitochondrial Code' },
    { id: 6, label: 'Ciliate, Dasycladacean and Hexamita Nuclear Code' },
    { id: 9, label: 'Echinoderm and Flatworm Mitochondrial Code' },
    { id: 10, label: 'Euplotid Nuclear Code' },
    { id: 11, label: 'Bacterial and Plant Plastid Code' },
    { id: 12, label: 'Alternative Yeast Nuclear Code' },
    { id: 13, label: 'Ascidian Mitochondrial Code' },
    { id: 14, label: 'Alternative Flatworm Mitochondrial Code' },
    { id: 15, label: 'Blepharisma Nuclear Code' },
    { id: 16, label: 'Chlorophycean Mitochondrial Code' },
    { id: 21, label: 'Trematode Mitochondrial Code' },
    { id: 22, label: 'Scenedesmus obliquus mitochondrial Code' },
    { id: 23, label: 'Thraustochytrium Mitochondrial Code' },
    { id: 24, label: 'Pterobranchia mitochondrial Code' }
  ];

  let importText = '';
  let sharedUrl = '';
  let shareCopied = false;
  let shareModalOpen = false;
  let shareCopyError = '';
  let shareCopying = false;
  let shareTrigger: HTMLElement | null = null;
  let pendingDeleteIds: string[] = [];
  let selectedIds: string[] = [];
  let exportScope: ExportScope = 'auto';
  let printEntries: ExportEntry[] = [];
  let activeEntryId = '';
  let draggedEntryId = '';
  let dropIndicator: { entryId: string; position: 'before' | 'after' } | undefined;
  let removingIds: string[] = [];
  let enteringIds: string[] = [];
  let collapsedEntryIds: string[] = [];
  let translationTable = 1;
  let openTool = 'transform';
  let highlightColor = '#f4c95d';
  let annotationColor = '#9bc7b5';
  let annotationLabel = '';
  let renamingEntryId = '';
  let renamingName = '';
  let renamingProject = false;
  let renamingProjectName = '';
  let translationModalOpen = false;
  let translationPreview: TranslationFramesDto | undefined;
  let translationPreviewSource: SequenceDto | undefined;
  let translationDraft: { frame: number; start: number } | undefined;
  let translationHoverRange: { frame: number; start: number; end: number } | undefined;
  let translationRange: { frame: number; start: number; end: number } | undefined;
  let findType: 'DNA' | 'PROTEIN' = 'DNA';
  let findQuery = '';
  let findBothStrands = true;
  let findHits: SearchHit[] = [];
  let currentFindHit = -1;
  let findPerformed = false;
  let findInput: HTMLInputElement;

  $: project = $projectStore;
  $: alignments = project.alignments;
  $: logos = project.logos ?? [];
  $: primerDesigns = project.primerDesigns ?? [];
  $: trees = project.trees ?? [];
  $: selectedAlignments = project.entryOrder.flatMap(id => selectedIds.includes(id) ? project.alignments.filter(alignment => alignment.id === id) : []);
  $: entryOrder = project.entryOrder;
  $: displayOrder = workspaceOrder(entryOrder, $toolJobStore);
  $: entriesToExport = exportEntries(project, selectedIds, exportScope);
  $: sequencesToExport = entriesToExport.flatMap(entry => entry.type === 'sequence' ? [entry.value] : entry.type === 'primers' ? allPrimerSequences(entry.value) : []);
  $: alignmentsToExport = entriesToExport.flatMap(entry => entry.type === 'alignment' ? [entry.value] : []);
  $: selected = project.sequences.find((sequence) => sequence.id === activeEntryId);
  $: selectedDnaSequences = selectedIds.map((id) => project.sequences.find((sequence) => sequence.id === id)).filter((sequence): sequence is SequenceDto => sequence?.kind === 'DNA');
  $: selectedProteinSequences = selectedIds.map((id) => project.sequences.find((sequence) => sequence.id === id)).filter((sequence): sequence is SequenceDto => sequence?.kind === 'PROTEIN');
  $: selectedPairwiseSequences = selectedIds.map((id) => project.sequences.find((sequence) => sequence.id === id)).filter((sequence): sequence is SequenceDto => sequence !== undefined);
  $: canCreatePairwiseAlignment = selectedPairwiseSequences.length === 2 && selectedPairwiseSequences[0].kind === selectedPairwiseSequences[1].kind;
  $: selectedAlignment = entryAlignment(activeEntryId, alignments);
  $: selectionLabel = selectedIds.length === 0 ? 'No entries selected' : `${selectedIds.length} ${selectedIds.length === 1 ? 'entry' : 'entries'} selected`;
  $: markTarget = activeSequenceRange($selectionStore, project.sequences);
  $: alignmentMarkTarget = activeAlignmentRange($selectionStore, alignments);
  $: canAnnotateRange = Boolean(markTarget || alignmentMarkTarget);
  $: visibleTranslationRange = translationRange ?? translationHoverRange;

  function isEntrySelected(id: string) {
    return selectedIds.includes(id);
  }

  function isEntryCollapsed(id: string) {
    return collapsedEntryIds.includes(id);
  }

  function toggleEntryCollapsed(id: string) {
    collapsedEntryIds = collapsedEntryIds.includes(id) ? collapsedEntryIds.filter((entryId) => entryId !== id) : [...collapsedEntryIds, id];
  }

  function activateEntryHeader(id: string) {
    if (collapsedEntryIds.includes(id)) {
      collapsedEntryIds = collapsedEntryIds.filter((entryId) => entryId !== id);
      return;
    }
    toggleEntry(id);
  }

  function handleEntryHeaderKeydown(id: string, event: KeyboardEvent) {
    if (event.key !== 'Enter' && event.key !== ' ') return;
    event.preventDefault();
    activateEntryHeader(id);
  }

  function toggleEntry(id: string, scroll = false) {
    selectionStore.set({});
    if (isEntrySelected(id)) {
      selectedIds = selectedIds.filter((entryId) => entryId !== id);
      if (activeEntryId === id) activeEntryId = selectedIds.at(-1) ?? '';
    } else {
      selectedIds = [...selectedIds, id];
      activeEntryId = id;
    }
    if (scroll) document.getElementById(`entry-${id}`)?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }

  function ensureEntrySelected(id: string) {
    if (!isEntrySelected(id)) selectedIds = [...selectedIds, id];
    activeEntryId = id;
  }

  function focusAndSelect(node: HTMLInputElement) {
    requestAnimationFrame(() => {
      node.focus();
      node.select();
    });
  }

  function startRename(sequence: SequenceDto) {
    renamingEntryId = sequence.id ?? '';
    renamingName = sequence.name;
  }

  function commitRename(sequence: SequenceDto) {
    if (renamingEntryId !== sequence.id) return;
    const name = renamingName.trim();
    if (name && name !== sequence.name) {
      projectStore.replaceSequence({ ...sequence, name });
    }
    renamingEntryId = '';
    renamingName = '';
  }

  function cancelRename() {
    renamingEntryId = '';
    renamingName = '';
  }

  function startProjectRename() {
    renamingProject = true;
    renamingProjectName = project.name;
  }

  function commitProjectRename() {
    if (!renamingProject) return;
    const name = renamingProjectName.trim();
    if (name && name !== project.name) {
      projectStore.renameProject(name);
    }
    renamingProject = false;
    renamingProjectName = '';
  }

  function cancelProjectRename() {
    renamingProject = false;
    renamingProjectName = '';
  }

  function entrySequence(id: string, sequences = project.sequences) {
    return sequences.find((sequence) => sequence.id === id);
  }

  function entryAlignment(id: string, entries = alignments) {
    return entries.find((entry) => entry.id === id);
  }

  function startReorder(id: string, event: DragEvent) {
    draggedEntryId = id;
    event.dataTransfer?.setData('text/plain', id);
    if (event.dataTransfer) event.dataTransfer.effectAllowed = 'move';
  }

  function updateDropIndicator(targetId: string, event: DragEvent) {
    event.preventDefault();
    if (!draggedEntryId || draggedEntryId === targetId) {
      dropIndicator = undefined;
      return;
    }
    const target = event.currentTarget as HTMLElement;
    dropIndicator = {
      entryId: targetId,
      position: event.clientY > target.getBoundingClientRect().top + target.offsetHeight / 2 ? 'after' : 'before'
    };
  }

  function clearDragState() {
    draggedEntryId = '';
    dropIndicator = undefined;
  }

  function confirmDeleteSelectedEntries() {
    if (removingIds.length || $uiStore.busy) return;
    pendingDeleteIds = [...selectedIds];
  }

  async function deleteSelectedEntries() {
    if (!pendingDeleteIds.length || removingIds.length) return;
    const idsToDelete = pendingDeleteIds;
    pendingDeleteIds = [];
    removingIds = idsToDelete;
    await new Promise((resolve) => setTimeout(resolve, 220));
    projectStore.removeEntries(idsToDelete);
    removingIds = [];
    selectedIds = [];
    collapsedEntryIds = collapsedEntryIds.filter((id) => !idsToDelete.includes(id));
    activeEntryId = '';
    selectionStore.set({});
  }

  function animateEntriesIn(ids: string[]) {
    if (ids.length === 0) return;
    enteringIds = [...new Set([...enteringIds, ...ids])];
    requestAnimationFrame(() => requestAnimationFrame(() => {
      enteringIds = enteringIds.filter((id) => !ids.includes(id));
    }));
  }

  function insertEntryIdsAfter(sourceId: string, ids: string[]) {
    projectStore.insertEntriesAfter(sourceId, ids.filter(Boolean));
  }

  function reorderAt(targetId: string, event: DragEvent) {
    event.preventDefault();
    const sourceId = draggedEntryId || event.dataTransfer?.getData('text/plain');
    if (!sourceId || sourceId === targetId) return;
    const insertAfter = dropIndicator?.entryId === targetId
      ? dropIndicator.position === 'after'
      : event.clientY > (event.currentTarget as HTMLElement).getBoundingClientRect().top + (event.currentTarget as HTMLElement).offsetHeight / 2;
    projectStore.moveEntry(sourceId, targetId, insertAfter);
    clearDragState();
  }

  function safeColor(color: string) {
    return /^#[0-9a-f]{6}$/i.test(color) ? color : '#d9e2e2';
  }

  function selectionDirection(selection = $selectionStore): 'FORWARD' | 'REVERSE' {
    return selection.start !== undefined && selection.end !== undefined && selection.start > selection.end ? 'REVERSE' : 'FORWARD';
  }

  function rangeSummary(entryId: string, unit: string, kind?: string, selection = $selectionStore) {
    const range = selection.entryId === entryId ? normalizedRange(selection) : undefined;
    const hover = selection.hoverEntryId === entryId && selection.hover !== undefined ? `Position ${selection.hover + 1}` : '';
    const selectedRange = range
      ? `Selected ${(selection.start ?? range.start) + 1}-${(selection.end ?? range.end) + 1} · ${range.end - range.start + 1} ${unit}${kind === 'DNA' ? ` · ${selectionDirection(selection) === 'REVERSE' ? "3'->5'" : "5'->3'"}` : ''}`
      : '';
    return selectedRange || hover;
  }

  function activeSequenceRange(selection = $selectionStore, sequences = project.sequences) {
    const range = normalizedRange(selection);
    const sequence = sequences.find((entry) => entry.id === selection.entryId);
    return sequence && sequence.id === selection.entryId && range ? { sequence, range, direction: selectionDirection(selection) } : undefined;
  }

  function activeAlignmentRange(selection = $selectionStore, entries = alignments) {
    const range = normalizedRange(selection);
    const alignment = entryAlignment(selection.entryId ?? '', entries);
    return alignment && range ? { alignment, range, direction: selectionDirection(selection) } : undefined;
  }

  function sequenceWithFindMarks(sequence: SequenceDto, hits = findHits): SequenceDto {
    const marks = hits
      .filter((hit) => hit.entryId === sequence.id)
      .map((hit) => ({
        start: hit.start,
        end: hit.end + 1,
        color: '#b9dfff',
        type: 'FIND_HIT' as 'HIGHLIGHT'
      }));
    return marks.length ? { ...sequence, marks: [...sequence.marks, ...marks] } : sequence;
  }

  function mergeHighlights(marks: SequenceMarkDto[]) {
    const annotations = marks.filter((mark) => mark.type === 'ANNOTATION');
    const highlights = marks
      .filter((mark) => mark.type !== 'ANNOTATION')
      .sort((a, b) => a.start - b.start || a.end - b.end);
    const merged = highlights.reduce<SequenceMarkDto[]>((result, mark) => {
      const previous = result.at(-1);
      if (previous && previous.color === mark.color && mark.start <= previous.end) {
        previous.end = Math.max(previous.end, mark.end);
      } else {
        result.push({ ...mark, type: 'HIGHLIGHT' });
      }
      return result;
    }, []);
    return [...merged, ...annotations];
  }

  function subtractRange(mark: SequenceMarkDto, start: number, end: number) {
    if (mark.end <= start || mark.start >= end) return [mark];
    if (mark.type === 'ANNOTATION') return [];
    return [
      mark.start < start ? { ...mark, end: start } : undefined,
      mark.end > end ? { ...mark, start: end } : undefined
    ].filter((fragment): fragment is SequenceMarkDto => fragment !== undefined);
  }

  function addMark(type: 'HIGHLIGHT' | 'ANNOTATION') {
    const target = markTarget;
    if (!target) return;
    const annotation = type === 'ANNOTATION' ? annotationLabel.trim() : undefined;
    if (type === 'ANNOTATION' && !annotation) return;
    const start = target.range.start;
    const end = target.range.end + 1;
    const newMark: SequenceMarkDto = {
      start: target.range.start,
      end: target.range.end + 1,
      color: type === 'HIGHLIGHT' ? highlightColor : annotationColor,
      annotation,
      type,
      direction: type === 'ANNOTATION' ? target.sequence.kind === 'PROTEIN' ? 'FORWARD' : target.direction : undefined
    };
    const marks = [
      ...target.sequence.marks.flatMap((mark) => {
        if (type === 'ANNOTATION' && mark.type === 'ANNOTATION') return subtractRange(mark, start, end);
        if (type === 'HIGHLIGHT' && mark.type !== 'ANNOTATION') return subtractRange(mark, start, end);
        return [mark];
      }),
      newMark
    ];
    projectStore.replaceSequence({
      ...target.sequence,
      marks: type === 'HIGHLIGHT' ? mergeHighlights(marks) : marks
    });
    if (type === 'ANNOTATION') annotationLabel = '';
  }

  function addAnnotation() {
    const target = markTarget;
    const alignmentTarget = alignmentMarkTarget;
    const annotation = annotationLabel.trim();
    if (!annotation) return;
    if (target) {
      addMark('ANNOTATION');
      return;
    }
    if (!alignmentTarget) return;
    const start = alignmentTarget.range.start;
    const end = alignmentTarget.range.end + 1;
    const newMark: SequenceMarkDto = {
      start,
      end,
      color: annotationColor,
      annotation,
      type: 'ANNOTATION',
      direction: alignmentTarget.direction
    };
    projectStore.replaceAlignment({
      ...alignmentTarget.alignment,
      marks: [
        ...alignmentTarget.alignment.marks.flatMap((mark) => subtractRange(mark, start, end)),
        newMark
      ]
    });
    annotationLabel = '';
  }

  function applyHighlight(color: string) {
    highlightColor = color;
    addMark('HIGHLIGHT');
  }

  function markRevision(sequence: { marks: unknown[] }) {
    return JSON.stringify(sequence.marks);
  }

  function removeFormatting() {
    const target = markTarget;
    const alignmentTarget = alignmentMarkTarget;
    if (!target && !alignmentTarget) return;
    if (alignmentTarget && !target) {
      projectStore.replaceAlignment({
        ...alignmentTarget.alignment,
        marks: alignmentTarget.alignment.marks.flatMap((mark) => subtractRange(mark, alignmentTarget.range.start, alignmentTarget.range.end + 1))
      });
      return;
    }
    if (!target) return;
    projectStore.replaceSequence({
      ...target.sequence,
      marks: target.sequence.marks.flatMap((mark) => subtractRange(mark, target.range.start, target.range.end + 1))
    });
  }

  function extractSelectedSequence() {
    const target = markTarget;
    if (!target) return;
    const start = target.range.start;
    const end = target.range.end + 1;
    const id = `sequence-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
    const extracted: SequenceDto = {
      id,
      name: `${target.sequence.name} ${start + 1}-${end}`,
      kind: target.sequence.kind,
      sequence: target.sequence.sequence.slice(start, end),
      marks: target.sequence.marks
        .filter((mark) => mark.end > start && mark.start < end)
        .map((mark) => ({
          ...mark,
          start: Math.max(mark.start, start) - start,
          end: Math.min(mark.end, end) - start
        }))
    };
    projectStore.addSequencesAfter(target.sequence.id ?? '', [extracted]);
    insertEntryIdsAfter(target.sequence.id ?? '', [id]);
    animateEntriesIn([id]);
    ensureEntrySelected(id);
  }

  function selectMark(sequenceId: string, start: number, end: number) {
    selectionStore.set({ entryId: sequenceId, start, end: end - 1 });
  }

  function copySelection(event: ClipboardEvent) {
    if (event.target instanceof HTMLElement && event.target.closest('input, textarea, select, [contenteditable="true"]')) return;
    const range = normalizedRange($selectionStore);
    if (!$selectionStore.entryId || !range || !event.clipboardData) return;
    const sequence = project.sequences.find((entry) => entry.id === $selectionStore.entryId);
    const alignmentEntry = entryAlignment($selectionStore.entryId);
    const text = sequence
      ? sequence.sequence.slice(range.start, range.end + 1)
      : alignmentEntry
        ? alignmentEntry.sequences.map((entry) => entry.value.slice(range.start, range.end + 1)).join('\n')
        : '';
    if (!text) return;
    event.preventDefault();
    event.clipboardData.setData('text/plain', text);
  }

  function runFind() {
    const query = findType === 'DNA' ? sanitizeDnaQuery(findQuery) : sanitizeProteinQuery(findQuery);
    findQuery = query;
    findPerformed = true;
    if (!query) {
      findHits = [];
      currentFindHit = -1;
      return;
    }
    const sequenceHits = project.sequences.flatMap((sequence) => {
      if (findType === 'DNA' && sequence.kind === 'DNA') return findDnaHits(sequence.id ?? '', sequence.sequence, query, findBothStrands);
      if (findType === 'PROTEIN' && sequence.kind === 'PROTEIN') return findProteinHits(sequence.id ?? '', sequence.sequence, query);
      return [];
    });
    const alignmentHits = alignments.flatMap((entry) => {
      if (findType === 'DNA' && entry.kind === 'DNA') return entry.sequences.flatMap((sequence, row) => findDnaHits(entry.id, sequence.value, query, findBothStrands).map((hit) => ({ ...hit, row })));
      if (findType === 'PROTEIN' && entry.kind === 'PROTEIN') return entry.sequences.flatMap((sequence, row) => findProteinHits(entry.id, sequence.value, query).map((hit) => ({ ...hit, row })));
      return [];
    });
    findHits = [...sequenceHits, ...alignmentHits].sort((a, b) => {
      const entryOrderDelta = entryOrder.indexOf(a.entryId) - entryOrder.indexOf(b.entryId);
      return entryOrderDelta || (a.row ?? -1) - (b.row ?? -1) || a.start - b.start || a.end - b.end || (a.strand === 'reverse' ? 1 : 0) - (b.strand === 'reverse' ? 1 : 0);
    });
    currentFindHit = -1;
    if (findHits.length > 0) goToFindHit(1);
  }

  function resetFind() {
    findQuery = '';
    findHits = [];
    currentFindHit = -1;
    findPerformed = false;
    selectionStore.set({});
  }

  function focusFindInput() {
    requestAnimationFrame(() => findInput?.focus());
  }

  function handleGlobalKeydown(event: KeyboardEvent) {
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'f') {
      event.preventDefault();
      openTool = 'find';
      focusFindInput();
    }
  }

  function goToFindHit(delta: 1 | -1) {
    if (findHits.length === 0) return;
    currentFindHit = (currentFindHit + delta + findHits.length) % findHits.length;
    const hit = findHits[currentFindHit];
    selectionStore.set({ entryId: hit.entryId, start: hit.start, end: hit.end });
    document.getElementById(`entry-${hit.entryId}`)?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }

  async function runImport() {
    if (!importText.trim()) return;
    await withBusy(async () => {
      const sequences = await apiClient.importSequences(importText);
      projectStore.addSequences(sequences);
      animateEntriesIn(sequences.flatMap((sequence) => sequence.id ? [sequence.id] : []));
      if (sequences[0]?.id) ensureEntrySelected(sequences[0].id);
      importText = '';
    });
  }

  async function transform(type: 'REVERSE' | 'COMPLEMENT' | 'REVERSE_COMPLEMENT') {
    const sources = selectedDnaSequences;
    if (sources.length === 0) return;
    await withBusy(async () => {
      const createdIds: string[] = [];
      for (const source of sources) {
        const sourceId = source.id ?? '';
        const sequence = await apiClient.transform(source, type);
        projectStore.addSequencesAfter(sourceId, [sequence]);
        if (sequence.id) {
          insertEntryIdsAfter(sourceId, [sequence.id]);
          createdIds.push(sequence.id);
        }
      }
      animateEntriesIn(createdIds);
      if (createdIds.length) ensureEntrySelected(createdIds.at(-1) ?? '');
    });
  }

  async function translate(frame: number) {
    const sources = selectedDnaSequences;
    if (sources.length === 0) return;
    await withBusy(async () => {
      const createdIds: string[] = [];
      for (const source of sources) {
        const sourceId = source.id ?? '';
        const sequence = await apiClient.translate(source, frame, translationTable);
        projectStore.addSequencesAfter(sourceId, [sequence]);
        if (sequence.id) {
          insertEntryIdsAfter(sourceId, [sequence.id]);
          createdIds.push(sequence.id);
        }
      }
      animateEntriesIn(createdIds);
      if (createdIds.length) ensureEntrySelected(createdIds.at(-1) ?? '');
    });
  }

  async function translateBestOrf() {
    const sources = selectedDnaSequences;
    if (sources.length === 0) return;
    await withBusy(async () => {
      const createdIds: string[] = [];
      for (const source of sources) {
        const sourceId = source.id ?? '';
        const sequence = await apiClient.translateBestOrf(source, translationTable);
        projectStore.addSequencesAfter(sourceId, [sequence]);
        if (sequence.id) {
          insertEntryIdsAfter(sourceId, [sequence.id]);
          createdIds.push(sequence.id);
        }
      }
      animateEntriesIn(createdIds);
      if (createdIds.length) ensureEntrySelected(createdIds.at(-1) ?? '');
    });
  }

  async function createPairwiseAlignment(mode: 'CFE' | 'LOCAL') {
    if (!canCreatePairwiseAlignment) return;
    const sources = selectedPairwiseSequences;
    await withBusy(async () => {
      const alignment = await apiClient.pairwiseAlignment(sources, mode);
      const sourceIds = sources.flatMap((source) => source.id ? [source.id] : []);
      const sourceId = sourceIds
        .slice()
        .sort((a, b) => entryOrder.indexOf(a) - entryOrder.indexOf(b))
        .at(-1) ?? '';
      projectStore.addAlignmentAfter(sourceId, alignment);
      animateEntriesIn([alignment.id]);
      ensureEntrySelected(alignment.id);
    });
  }

  async function createConsensus() {
    if ($uiStore.busy || !selectedAlignments.length) return;
    const sources = selectedAlignments;
    await withBusy(async () => {
      for (const source of sources) {
        const sequence = await apiClient.consensus(source);
        projectStore.addSequencesAfter(source.id, [sequence]);
        if (sequence.id) animateEntriesIn([sequence.id]);
      }
    });
  }

  async function createLogos() {
    if ($uiStore.busy || !selectedAlignments.length) return;
    const sources = selectedAlignments;
    await withBusy(async () => {
      for (const source of sources) {
        const logo = await apiClient.logo(source);
        projectStore.addLogoAfter(source.id, logo);
        animateEntriesIn([logo.id]);
      }
    });
  }

  async function showAllFrames() {
    if (!selected || selected.kind !== 'DNA') return;
    await withBusy(async () => {
      translationPreview = await apiClient.translationFrames(selected, translationTable);
      translationPreviewSource = selected;
      translationDraft = undefined;
      translationHoverRange = undefined;
      translationRange = undefined;
      translationModalOpen = true;
    });
  }

  function closeTranslationModal() {
    translationModalOpen = false;
    translationPreview = undefined;
    translationPreviewSource = undefined;
    translationDraft = undefined;
    translationHoverRange = undefined;
    translationRange = undefined;
  }

  function frameText(frame: number) {
    if (!translationPreview) return '';
    return frame === 0 ? translationPreview.frame1 : frame === 1 ? translationPreview.frame2 : translationPreview.frame3;
  }

  function frameMarkers(frame: number) {
    if (!translationPreview) return '';
    return frame === 0 ? translationPreview.markers1 : frame === 1 ? translationPreview.markers2 : translationPreview.markers3;
  }

  function frameCodons(frame: number) {
    return [...frameText(frame)].map((aminoAcid, index) => ({
      aminoAcid,
      marker: frameMarkers(frame)[index] ?? 'o',
      index,
      start: frame + index * 3,
      end: frame + index * 3 + 2,
      dna: translationPreview?.sequence.slice(frame + index * 3, frame + index * 3 + 3) ?? ''
    }));
  }

  function selectedTranslationLength(range = visibleTranslationRange) {
    return range ? Math.floor((range.end - range.start + 1) / 3) : 0;
  }

  function translationStatusText(range = visibleTranslationRange) {
    if (translationRange) {
      return `Selected ${translationRange.start + 1}-${translationRange.end + 1} nt · ${selectedTranslationLength(range)} aa`;
    }
    if (translationDraft) {
      return range
        ? `Selecting frame ${translationDraft.frame + 1}: ${range.start + 1}-${range.end + 1} nt · ${selectedTranslationLength(range)} aa`
        : `Start codon selected in frame ${translationDraft.frame + 1}`;
    }
    return 'Click codons to choose start and end. Shift-click finds the nearest upstream start and downstream stop, or uses the frame boundaries.';
  }

  function selectFrameCodon(frame: number, index: number, shiftKey: boolean) {
    const start = frame + index * 3;
    const end = start + 2;
    if (shiftKey) {
      const markers = frameMarkers(frame);
      let stop = -1;
      for (let cursor = index; cursor < markers.length; cursor += 1) {
        if (markers[cursor] === 'b') {
          stop = cursor;
          break;
        }
      }
      if (stop === -1) stop = markers.length - 1;
      let startCodon = -1;
      for (let cursor = index; cursor >= 0; cursor -= 1) {
        if (markers[cursor] === 'b') break;
        if (markers[cursor] === 'a') startCodon = cursor;
      }
      if (startCodon === -1) startCodon = 0;
      translationDraft = undefined;
      translationHoverRange = undefined;
      translationRange = { frame, start: frame + startCodon * 3, end: frame + stop * 3 + 2 };
      return;
    }
    if (!translationDraft || translationDraft.frame !== frame) {
      translationDraft = { frame, start };
      translationHoverRange = { frame, start, end };
      translationRange = undefined;
      return;
    }
    translationRange = {
      frame,
      start: Math.min(translationDraft.start, start),
      end: Math.max(translationDraft.start + 2, end)
    };
    translationDraft = undefined;
    translationHoverRange = undefined;
  }

  function previewFrameCodon(frame: number, index: number) {
    if (!translationDraft || translationDraft.frame !== frame) return;
    const hoverStart = frame + index * 3;
    translationHoverRange = {
      frame,
      start: Math.min(translationDraft.start, hoverStart),
      end: Math.max(translationDraft.start + 2, hoverStart + 2)
    };
  }

  function previewFrameCodonFromPointer(event: PointerEvent) {
    const button = (event.target as HTMLElement).closest<HTMLButtonElement>('[data-frame][data-codon-index]');
    if (!button) return;
    previewFrameCodon(Number(button.dataset.frame), Number(button.dataset.codonIndex));
  }

  function isTranslationCodonSelected(frame: number, index: number) {
    if (!translationRange || translationRange.frame !== frame) return false;
    const start = frame + index * 3;
    return start >= translationRange.start && start + 2 <= translationRange.end;
  }

  function isTranslationCodonPreviewed(frame: number, index: number, range = visibleTranslationRange) {
    if (translationRange) return false;
    if (!range || range.frame !== frame) return false;
    const start = frame + index * 3;
    return start >= range.start && start + 2 <= range.end;
  }

  function isTranslationDraftAnchor(frame: number, index: number) {
    return translationDraft?.frame === frame && translationDraft.start === frame + index * 3;
  }

  async function exportSelectedTranslation() {
    if (!translationPreviewSource || !translationRange) return;
    const source = translationPreviewSource;
    const range = translationRange;
    const sourceId = source.id ?? '';
    await withBusy(async () => {
      const sequence = await apiClient.translateRange(source, range.start, range.end, translationTable);
      projectStore.addSequencesAfter(sourceId, [sequence]);
      if (sequence.id) insertEntryIdsAfter(sourceId, [sequence.id]);
      if (sequence.id) animateEntriesIn([sequence.id]);
      if (sequence.id) ensureEntrySelected(sequence.id);
      closeTranslationModal();
    });
  }

  function asFasta(sequences: SequenceDto[]) {
    return sequences.map((sequence) => `>${sequence.name}\n${sequence.sequence}`).join('\n') + '\n';
  }

  function submitNcbiBlast(kind: 'DNA' | 'PROTEIN') {
    const sequences = kind === 'DNA' ? selectedDnaSequences : selectedProteinSequences;
    if (sequences.length === 0) return;
    const form = document.createElement('form');
    form.action = 'https://blast.ncbi.nlm.nih.gov/Blast.cgi';
    form.method = 'POST';
    form.target = '_blank';
    const fields = {
      PAGE: 'MegaBlast',
      PROGRAM: kind === 'DNA' ? 'blastn' : 'blastp',
      BLAST_PROGRAMS: kind === 'DNA' ? 'megaBlast' : 'blastp',
      PAGE_TYPE: 'BlastSearch',
      DBSEARCH: 'true',
      QUERY: asFasta(sequences)
    };
    Object.entries(fields).forEach(([name, value]) => {
      const input = document.createElement('input');
      input.type = 'hidden';
      input.name = name;
      input.value = value;
      form.appendChild(input);
    });
    document.body.appendChild(form);
    form.submit();
    form.remove();
  }

  async function share(event: MouseEvent) {
    if ($uiStore.busy) return;
    shareTrigger = event.currentTarget as HTMLElement;
    await withBusy(async () => {
      const result = await apiClient.saveProject(project);
      sharedUrl = shareUrl(location.href, result.shareKey);
      shareCopied = false;
      shareCopyError = '';
      shareModalOpen = true;
    });
  }

  async function copyShareLink() {
    if (shareCopying) return;
    shareCopying = true;
    shareCopied = false;
    shareCopyError = '';
    try {
      await navigator.clipboard.writeText(sharedUrl);
      shareCopied = true;
    } catch {
      shareCopyError = 'Clipboard unavailable.';
    } finally {
      shareCopying = false;
    }
  }

  function preparePrint() {
    // Native browser printing fires beforeprint synchronously; render before pagination.
    flushSync(() => { printEntries = exportEntries(project, selectedIds, exportScope); });
  }

  function printWorkspace() {
    preparePrint();
    try { window.print(); }
    finally { printEntries = []; }
  }

  async function withBusy(work: () => Promise<void>) {
    uiStore.set({ busy: true });
    try {
      await work();
      uiStore.set({ busy: false });
    } catch (error) {
      uiStore.set({ busy: false, error: error instanceof ApiError ? error.message : 'Request failed' });
    }
  }
</script>

<svelte:window on:copy={copySelection} on:keydown={handleGlobalKeydown}
  on:beforeprint={preparePrint} on:afterprint={() => { printEntries = []; }} />

<PrintWorkspace name={project.name} entries={printEntries} />

<header class="app-header">
  <div class="brand-lockup">
    <span class="brand-mark">GW</span>
    <div>
      <strong>GeneWarrior</strong>
      <small>Sequence workbench</small>
    </div>
  </div>
  <div class="project-title">
    <span>Project</span>
    {#if renamingProject}
      <input
        class="project-title-input"
        bind:value={renamingProjectName}
        maxlength="160"
        aria-label="Project name"
        use:focusAndSelect
        on:keydown={(event) => {
          if (event.key === 'Enter') commitProjectRename();
          if (event.key === 'Escape') cancelProjectRename();
        }}
        on:blur={commitProjectRename}
      />
    {:else}
      <strong><button type="button" title="Double-click to rename project" on:dblclick={startProjectRename}>{project.name}</button></strong>
    {/if}
  </div>
  <button class="quiet-button" on:click={share} disabled={$uiStore.busy}>Share project</button>
</header>

<main>
  <aside class="navigator-panel">
    <div class="panel-title">
      <span>Project Navigator</span>
      <small>{entryOrder.length} entries</small>
    </div>
    <nav>
      {#each displayOrder as entryId (entryId)}
        {@const sequence = entrySequence(entryId, project.sequences)}
        {@const alignment = alignments.find((entry) => entry.id === entryId)}
        {@const logo = logos.find((entry) => entry.id === entryId)}
        {@const primerDesign = primerDesigns.find(entry => entry.id === entryId)}
        {@const tree = trees.find(entry => entry.id === entryId)}
        {@const job = $toolJobStore.find(item => jobEntryId(item) === entryId)}
        {#if job}
          <button on:click={() => document.getElementById(`entry-${entryId}`)?.scrollIntoView({behavior: 'smooth', block: 'nearest'})}>
            <span class="entry-icon">{jobIcon(job)}</span>
            <span><strong>{jobTitle(job)}</strong><small>{job.job?.status.toLowerCase() ?? 'submitting'}</small></span>
          </button>
        {:else if tree}
          <button draggable="true" class:active={isEntrySelected(entryId)} class:dragging={draggedEntryId === entryId} class:removing={removingIds.includes(entryId)} class:entering={enteringIds.includes(entryId)} class:drop-before={dropIndicator?.entryId === entryId && dropIndicator.position === 'before'} class:drop-after={dropIndicator?.entryId === entryId && dropIndicator.position === 'after'} on:click={() => toggleEntry(entryId, true)} on:dragstart={(event) => startReorder(entryId, event)} on:dragend={clearDragState} on:dragover={(event) => updateDropIndicator(entryId, event)} on:drop={(event) => reorderAt(entryId, event)}>
            <span class="entry-icon">T</span><span><strong>{tree.name}</strong><small>{tree.sources.length} leaves</small></span>
          </button>
        {:else if primerDesign}
          <button draggable="true" class:active={isEntrySelected(entryId)} class:dragging={draggedEntryId === entryId} class:removing={removingIds.includes(entryId)} class:drop-before={dropIndicator?.entryId === entryId && dropIndicator.position === 'before'} class:drop-after={dropIndicator?.entryId === entryId && dropIndicator.position === 'after'} on:click={() => toggleEntry(entryId, true)} on:dragstart={(event) => startReorder(entryId, event)} on:dragend={clearDragState} on:dragover={(event) => updateDropIndicator(entryId, event)} on:drop={(event) => reorderAt(entryId, event)}>
            <span class="entry-icon">PCR</span><span><strong>{primerDesign.name}</strong><small>{primerDesign.pairs.length} primer pairs</small></span>
          </button>
        {:else if sequence}
          <button draggable="true" class:active={isEntrySelected(entryId)} class:dragging={draggedEntryId === entryId} class:removing={removingIds.includes(entryId)} class:entering={enteringIds.includes(entryId)} class:drop-before={dropIndicator?.entryId === entryId && dropIndicator.position === 'before'} class:drop-after={dropIndicator?.entryId === entryId && dropIndicator.position === 'after'} on:click={() => toggleEntry(entryId, true)} on:dragstart={(event) => startReorder(entryId, event)} on:dragend={clearDragState} on:dragover={(event) => updateDropIndicator(entryId, event)} on:drop={(event) => reorderAt(entryId, event)}>
            <span class:protein={sequence.kind === 'PROTEIN'} class="entry-icon">{sequence.kind === 'DNA' ? 'D' : 'P'}</span>
            <span>
              <strong>{sequence.name}</strong>
              <small>{sequence.kind === 'DNA' ? `${sequence.sequence.length} bp` : `${sequence.sequence.length} aa`}</small>
            </span>
          </button>
        {:else if alignment}
          <button draggable="true" class:active={isEntrySelected(entryId)} class:dragging={draggedEntryId === entryId} class:removing={removingIds.includes(entryId)} class:entering={enteringIds.includes(entryId)} class:drop-before={dropIndicator?.entryId === entryId && dropIndicator.position === 'before'} class:drop-after={dropIndicator?.entryId === entryId && dropIndicator.position === 'after'} on:click={() => toggleEntry(entryId, true)} on:dragstart={(event) => startReorder(entryId, event)} on:dragend={clearDragState} on:dragover={(event) => updateDropIndicator(entryId, event)} on:drop={(event) => reorderAt(entryId, event)}>
            <span class="entry-icon alignment-icon">A</span>
            <span>
              <strong>{alignment.name}</strong>
              <small>{alignment.sequences.length} sequences</small>
            </span>
          </button>
        {:else if logo}
          <button draggable="true" class:active={isEntrySelected(entryId)} class:dragging={draggedEntryId === entryId} class:removing={removingIds.includes(entryId)} class:entering={enteringIds.includes(entryId)} class:drop-before={dropIndicator?.entryId === entryId && dropIndicator.position === 'before'} class:drop-after={dropIndicator?.entryId === entryId && dropIndicator.position === 'after'} on:click={() => toggleEntry(entryId, true)} on:dragstart={(event) => startReorder(entryId, event)} on:dragend={clearDragState} on:dragover={(event) => updateDropIndicator(entryId, event)} on:drop={(event) => reorderAt(entryId, event)}>
            <span class="entry-icon logo-icon">L</span>
            <span><strong>{logo.name}</strong><small>{logo.columns.length} positions</small></span>
          </button>
        {/if}
      {/each}
    </nav>
    <div class="import-box">
      <label for="sequence-import">Add sequence</label>
      <textarea id="sequence-import" bind:value={importText} rows="5" placeholder="Paste DNA, protein, or FASTA"></textarea>
      <button class="primary-button" on:click={runImport} disabled={$uiStore.busy || !importText.trim()}>Import sequence</button>
    </div>
  </aside>

  <section class="workspace">
    <div class="workspace-heading">
      <div>
        <h1>Sequence workspace</h1>
        <p>{selectionLabel}. Select an entry to apply tools or inspect annotations.</p>
      </div>
      {#if $uiStore.busy}<span class="status">Working...</span>{/if}
    </div>

    {#if $uiStore.error}
      <div class="error">{$uiStore.error}</div>
    {/if}
    {#if $persistenceError}
      <div class="error" role="alert">{$persistenceError}</div>
    {/if}

    <div class="entry-stack">
      {#each displayOrder as entryId (entryId)}
        {@const sequence = entrySequence(entryId, project.sequences)}
        {@const alignment = alignments.find((entry) => entry.id === entryId)}
        {@const logo = logos.find((entry) => entry.id === entryId)}
        {@const primerDesign = primerDesigns.find(entry => entry.id === entryId)}
        {@const tree = trees.find(entry => entry.id === entryId)}
        {@const job = $toolJobStore.find(item => jobEntryId(item) === entryId)}
        {#if job}
          <JobCard item={job} />
        {:else if tree}
          {@const collapsed = collapsedEntryIds.includes(entryId)}
          <div id={`entry-${entryId}`} class="entry-card tree-card type-other" class:selected={isEntrySelected(entryId)} class:collapsed={collapsed} class:removing={removingIds.includes(entryId)} class:entering={enteringIds.includes(entryId)}>
            <header class="entry-card-header" role="button" tabindex="0" aria-pressed={isEntrySelected(entryId)} on:click={() => activateEntryHeader(entryId)} on:keydown={(event) => handleEntryHeaderKeydown(entryId, event)}>
              <div><div class="entry-heading"><span class="entry-icon">T</span><h2>{tree.name}</h2></div>
                <div class="entry-meta"><p>{tree.sources.length} leaves &middot; T-Coffee {tree.version} &middot; Guide tree</p></div></div>
              <button class="entry-toggle" type="button" aria-label={collapsed ? 'Maximize card' : 'Minimize card'} title={collapsed ? 'Maximize card' : 'Minimize card'} on:click|stopPropagation={() => toggleEntryCollapsed(entryId)}>{collapsed ? '+' : '-'}</button>
            </header>
            {#if !collapsed}<TreeContent {tree} />{/if}
          </div>
        {:else if primerDesign}
          {@const collapsed = collapsedEntryIds.includes(entryId)}
          <div id={`entry-${entryId}`} class="entry-card primer-card type-other" class:selected={isEntrySelected(entryId)} class:collapsed={collapsed} class:removing={removingIds.includes(entryId)}>
            <header class="entry-card-header" role="button" tabindex="0" aria-pressed={isEntrySelected(entryId)} on:click={() => activateEntryHeader(entryId)} on:keydown={(event) => handleEntryHeaderKeydown(entryId, event)}>
              <div><div class="entry-heading"><span class="entry-icon">PCR</span><h2>{primerDesign.name}</h2></div>
                <div class="entry-meta"><p>{primerDesign.pairs.length} primer pairs{primerDesign.settings.includeProbe ? ' with internal probes' : ''} &middot; Primer3 {primerDesign.version}</p></div></div>
              <button class="entry-toggle" type="button" aria-label={collapsed ? 'Maximize card' : 'Minimize card'} title={collapsed ? 'Maximize card' : 'Minimize card'} on:click|stopPropagation={() => toggleEntryCollapsed(entryId)}>{collapsed ? '+' : '-'}</button>
            </header>
            {#if !collapsed}<PrimerContent design={primerDesign} />{/if}
          </div>
        {:else if sequence}
        {@const renderedSequence = sequenceWithFindMarks(sequence, findHits)}
        {@const summary = rangeSummary(entryId, sequence.kind === 'DNA' ? 'bp' : 'aa', sequence.kind, $selectionStore)}
        {@const collapsed = collapsedEntryIds.includes(entryId)}
        <div id={`entry-${entryId}`} class:selected={isEntrySelected(entryId)} class:collapsed={collapsed} class:removing={removingIds.includes(entryId)} class:entering={enteringIds.includes(entryId)} class:type-dna={sequence.kind === 'DNA'} class:type-protein={sequence.kind === 'PROTEIN'} class:type-other={sequence.kind !== 'DNA' && sequence.kind !== 'PROTEIN'} class="entry-card">
          <header class="entry-card-header" role="button" tabindex="0" aria-pressed={isEntrySelected(entryId)} on:click={() => activateEntryHeader(entryId)} on:keydown={(event) => handleEntryHeaderKeydown(entryId, event)}>
            <div>
              <div class="entry-heading">
                <span class:protein={sequence.kind === 'PROTEIN'} class="entry-icon">{sequence.kind === 'DNA' ? 'D' : 'P'}</span>
                {#if renamingEntryId === sequence.id}
                  <input
                    class="entry-title-input"
                    bind:value={renamingName}
                    maxlength="160"
                    aria-label="Sequence name"
                    use:focusAndSelect
                    on:click|stopPropagation
                    on:dblclick|stopPropagation
                    on:keydown|stopPropagation={(event) => {
                      if (event.key === 'Enter') commitRename(sequence);
                      if (event.key === 'Escape') cancelRename();
                    }}
                    on:blur={() => commitRename(sequence)}
                  />
                {:else}
                  <h2 class="entry-title"><button type="button" title="Double-click to rename" on:click|stopPropagation on:dblclick|stopPropagation={() => startRename(sequence)}>{sequence.name}</button></h2>
                {/if}
              </div>
              <div class="entry-meta">
                <p>{sequence.kind === 'DNA' ? `Nucleotide sequence · ${sequence.sequence.length} bp` : `Protein sequence · ${sequence.sequence.length} amino acids`}</p>
                <p class:visible={summary} class="selection-readout">{summary || 'Position'}</p>
              </div>
            </div>
            <button class="entry-toggle" type="button" aria-label={collapsed ? 'Maximize card' : 'Minimize card'} title={collapsed ? 'Maximize card' : 'Minimize card'} on:click|stopPropagation={() => toggleEntryCollapsed(entryId)}>{collapsed ? '+' : '-'}</button>
          </header>
          {#if !collapsed && sequence.marks.length}
            <div class="annotation-list">
              {#each sequence.marks as mark}
                <button class:annotation-chip={mark.type === 'ANNOTATION'} class:highlight-chip={mark.type !== 'ANNOTATION'} class:reverse={mark.direction === 'REVERSE'} style={mark.type === 'ANNOTATION' ? `--annotation-color: ${safeColor(mark.color)}` : ''} title={mark.type === 'ANNOTATION' ? `${mark.annotation} (${mark.start + 1}-${mark.end})` : `Colored region ${mark.start + 1}-${mark.end}`} on:click|stopPropagation={() => selectMark(sequence.id ?? '', mark.start, mark.end)}>
                  <i style={`background-color: ${safeColor(mark.color)}`}></i>
                  {#if mark.type === 'ANNOTATION'}{mark.annotation}{/if}<small>{mark.start + 1}-{mark.end}</small>
                </button>
              {/each}
            </div>
          {/if}
          {#if !collapsed}
            <div class="sequence-view" aria-label={`${sequence.name} sequence`}>
              {#key markRevision(renderedSequence)}
                <SequenceViewer sequence={renderedSequence} onselect={() => (openTool = 'mark')} />
              {/key}
            </div>
          {/if}
        </div>
        {:else if alignment}
        {@const alignmentCollapsed = collapsedEntryIds.includes(entryId)}
      <div id={`entry-${entryId}`} class:selected={isEntrySelected(entryId)} class:collapsed={alignmentCollapsed} class:removing={removingIds.includes(entryId)} class:entering={enteringIds.includes(entryId)} class="entry-card alignment-card type-alignment">
        <header class="entry-card-header" role="button" tabindex="0" aria-pressed={isEntrySelected(entryId)} on:click={() => activateEntryHeader(entryId)} on:keydown={(event) => handleEntryHeaderKeydown(entryId, event)}>
          <div>
            <div class="entry-heading">
              <span class="entry-icon alignment-icon">A</span>
              <h2>{alignment.name}</h2>
            </div>
            <div class="entry-meta">
              <p>{alignment.kind === 'DNA' ? 'DNA' : 'Protein'} alignment · {alignment.sequences.length} sequences · {alignment.sequences[0].value.length} columns</p>
              <p class:visible={rangeSummary(entryId, 'columns', undefined, $selectionStore)} class="selection-readout">{rangeSummary(entryId, 'columns', undefined, $selectionStore) || 'Position'}</p>
            </div>
          </div>
          <button class="entry-toggle" type="button" aria-label={alignmentCollapsed ? 'Maximize card' : 'Minimize card'} title={alignmentCollapsed ? 'Maximize card' : 'Minimize card'} on:click|stopPropagation={() => toggleEntryCollapsed(entryId)}>{alignmentCollapsed ? '+' : '-'}</button>
        </header>
        {#if !alignmentCollapsed && alignment.marks.length}
          <div class="annotation-list">
            {#each alignment.marks as mark}
              <button class:annotation-chip={mark.type === 'ANNOTATION'} class:highlight-chip={mark.type !== 'ANNOTATION'} class:reverse={mark.direction === 'REVERSE'} style={mark.type === 'ANNOTATION' ? `--annotation-color: ${safeColor(mark.color)}` : ''} title={`${mark.annotation} (${mark.start + 1}-${mark.end})`} on:click|stopPropagation={() => selectMark(entryId, mark.start, mark.end)}>
                <i style={`background-color: ${safeColor(mark.color)}`}></i>
                {mark.annotation}<small>{mark.start + 1}-{mark.end}</small>
              </button>
            {/each}
          </div>
        {/if}
        {#if !alignmentCollapsed}
          <div class="alignment-view">
            <AlignmentViewer entryId={entryId} sequences={alignment.sequences} consensus={alignment.consensus} marks={alignment.marks} findHits={findHits.filter((hit) => hit.entryId === entryId)} onselect={() => (openTool = 'mark')} />
          </div>
        {/if}
      </div>
        {:else if logo}
          {@const logoCollapsed = collapsedEntryIds.includes(entryId)}
          <div id={`entry-${entryId}`} class:selected={isEntrySelected(entryId)} class:collapsed={logoCollapsed} class:removing={removingIds.includes(entryId)} class:entering={enteringIds.includes(entryId)} class="entry-card logo-card type-other">
            <header class="entry-card-header" role="button" tabindex="0" aria-pressed={isEntrySelected(entryId)} on:click={() => activateEntryHeader(entryId)} on:keydown={(event) => handleEntryHeaderKeydown(entryId, event)}>
              <div>
                <div class="entry-heading"><span class="entry-icon logo-icon">L</span><h2>{logo.name}</h2></div>
                <div class="entry-meta"><p>{logo.kind === 'DNA' ? 'DNA' : 'Protein'} logo &middot; {logo.sequenceCount} sequences &middot; {logo.columns.length} positions</p></div>
              </div>
              <button class="entry-toggle" type="button" aria-label={logoCollapsed ? 'Maximize card' : 'Minimize card'} title={logoCollapsed ? 'Maximize card' : 'Minimize card'} on:click|stopPropagation={() => toggleEntryCollapsed(entryId)}>{logoCollapsed ? '+' : '-'}</button>
            </header>
            {#if !logoCollapsed}<LogoContent {logo} />{/if}
          </div>
        {/if}
      {/each}
    </div>
  </section>

  <aside class="tools-panel">
    <div class="panel-title">
      <span>Tools</span>
      <small>{selectionLabel}</small>
    </div>

    {#if selectedIds.length > 0}
      <div class="tool-actions">
        <button on:click={confirmDeleteSelectedEntries} disabled={$uiStore.busy || removingIds.length > 0}>Delete selected entries</button>
      </div>
    {/if}

    <div class="tool-sections">
      <section class:open={openTool === 'transform'} class="tool-section">
        <button class="tool-heading" on:click={() => (openTool = openTool === 'transform' ? '' : 'transform')}>Reverse Complement <span>+</span></button>
        {#if openTool === 'transform'}
          <div class="tool-content" transition:slide={{ duration: 160 }}>
            <button on:click={() => transform('REVERSE_COMPLEMENT')} disabled={selectedDnaSequences.length === 0}>Reverse complement</button>
            <button on:click={() => transform('REVERSE')} disabled={selectedDnaSequences.length === 0}>Reverse only</button>
            <button on:click={() => transform('COMPLEMENT')} disabled={selectedDnaSequences.length === 0}>Complement only</button>
          </div>
        {/if}
      </section>

      <section class:open={openTool === 'translation'} class="tool-section">
        <button class="tool-heading" on:click={() => (openTool = openTool === 'translation' ? '' : 'translation')}>Translation <span>+</span></button>
        {#if openTool === 'translation'}
          <div class="tool-content" transition:slide={{ duration: 160 }}>
            <label for="translation-table">Genetic code</label>
            <select id="translation-table" bind:value={translationTable}>
              {#each translationTables as table}
                <option value={table.id}>{table.label}</option>
              {/each}
            </select>
            <button on:click={showAllFrames} disabled={!selected || selected.kind !== 'DNA'}>Show all frames</button>
            <div class="frame-grid">
              <button on:click={() => translate(1)} disabled={selectedDnaSequences.length === 0}>Forward 1</button>
              <button on:click={() => translate(4)} disabled={selectedDnaSequences.length === 0}>Reverse 1</button>
              <button on:click={() => translate(2)} disabled={selectedDnaSequences.length === 0}>Forward 2</button>
              <button on:click={() => translate(5)} disabled={selectedDnaSequences.length === 0}>Reverse 2</button>
              <button on:click={() => translate(3)} disabled={selectedDnaSequences.length === 0}>Forward 3</button>
              <button on:click={() => translate(6)} disabled={selectedDnaSequences.length === 0}>Reverse 3</button>
            </div>
            <button on:click={translateBestOrf} disabled={selectedDnaSequences.length === 0}>Best guess ORF</button>
          </div>
        {/if}
      </section>

      <section class:open={openTool === 'blast'} class="tool-section">
        <button class="tool-heading" on:click={() => (openTool = openTool === 'blast' ? '' : 'blast')}>BLAST <span>+</span></button>
        {#if openTool === 'blast'}
          <div class="tool-content" transition:slide={{ duration: 160 }}>
            <button on:click={() => submitNcbiBlast('DNA')} disabled={selectedDnaSequences.length === 0}>NCBI BLAST for DNA</button>
            <button on:click={() => submitNcbiBlast('PROTEIN')} disabled={selectedProteinSequences.length === 0}>NCBI BLAST for proteins</button>
          </div>
        {/if}
      </section>

      <section class:open={openTool === 'mark'} class="tool-section">
        <button class="tool-heading" on:click={() => (openTool = openTool === 'mark' ? '' : 'mark')}>Marked Sequence <span>+</span></button>
        {#if openTool === 'mark'}
          <div class="tool-content" transition:slide={{ duration: 160 }}>
            <div class="tool-subsection">
              <div class="tool-subsection-header">Background color</div>
              <div class="swatches">
                {#each ['#f4c95d', '#9bc7b5', '#9ab8d3', '#d99a92', '#b6a2c8'] as color}
                  <button title="Apply highlight color" style={`background:${color}`} on:click={() => applyHighlight(color)} disabled={!markTarget}></button>
                {/each}
              </div>
            </div>

            <div class="tool-subsection">
              <div class="tool-subsection-header">Annotation arrow</div>
              <div class="swatches">
                {#each ['#4e9d86', '#5a88b2', '#ba755f', '#8b70aa', '#c59635'] as color}
                  <button class:chosen={annotationColor === color} title="Annotation color" style={`background:${color}`} on:click={() => (annotationColor = color)} disabled={!canAnnotateRange}></button>
                {/each}
              </div>
              <input bind:value={annotationLabel} maxlength="100" placeholder="Annotation label" disabled={!canAnnotateRange} />
              <button on:click={addAnnotation} disabled={!canAnnotateRange || !annotationLabel.trim()}>Add annotation</button>
            </div>

            <div class="tool-subsection">
              <div class="tool-subsection-header">Selected range</div>
              <button on:click={removeFormatting} disabled={!canAnnotateRange}>Remove formatting</button>
              <button on:click={extractSelectedSequence} disabled={!markTarget}>Extract sequence</button>
            </div>
          </div>
        {/if}
      </section>

      <section class:open={openTool === 'find'} class="tool-section">
        <button class="tool-heading" on:click={() => (openTool = openTool === 'find' ? '' : 'find')}>Find <span>+</span></button>
        {#if openTool === 'find'}
          <div class="tool-content" transition:slide={{ duration: 160 }}>
            <div class="segmented-control">
              <button class:active={findType === 'DNA'} on:click={() => (findType = 'DNA')}>DNA</button>
              <button class:active={findType === 'PROTEIN'} on:click={() => (findType = 'PROTEIN')}>Protein</button>
            </div>
            <input bind:this={findInput} bind:value={findQuery} placeholder="Enter sequence" on:keydown={(event) => event.key === 'Enter' && runFind()} />
            {#if findType === 'DNA'}
              <label class="checkbox-label"><input type="checkbox" bind:checked={findBothStrands} /> Both strands</label>
            {/if}
            <div class:single={!findPerformed} class="button-row">
              <button on:click={runFind}>Search</button>
              {#if findPerformed}
                <button on:click={resetFind}>Reset</button>
              {/if}
            </div>
            {#if findPerformed}
              <div class="find-nav">
                {#if findHits.length > 0}
                  <button on:click={() => goToFindHit(-1)}>Prev</button>
                  <span>{currentFindHit + 1} / {findHits.length} matches</span>
                  <button on:click={() => goToFindHit(1)}>Next</button>
                {:else}
                  <span class="find-status">No matches</span>
                {/if}
              </div>
            {/if}
          </div>
        {/if}
      </section>

      <section class:open={openTool === 'create-alignment'} class="tool-section">
        <button class="tool-heading" on:click={() => (openTool = openTool === 'create-alignment' ? '' : 'create-alignment')}>Create Alignment <span>+</span></button>
        {#if openTool === 'create-alignment'}
          <div class="tool-content" transition:slide={{ duration: 160 }}>
            <div class="tool-label">Pairwise</div>
            <button on:click={() => createPairwiseAlignment('CFE')} disabled={!canCreatePairwiseAlignment || $uiStore.busy}>Full sequence</button>
            <button on:click={() => createPairwiseAlignment('LOCAL')} disabled={!canCreatePairwiseAlignment || $uiStore.busy}>Highest similarity region</button>
            <div class="tool-label spaced">Multiple sequence alignment</div>
            <TcoffeeTool sequences={project.entryOrder.flatMap(id => selectedPairwiseSequences.filter(sequence => sequence.id === id))} />
          </div>
        {/if}
      </section>

      <section class:open={openTool === 'analyze-alignment'} class="tool-section">
        <button class="tool-heading" on:click={() => (openTool = openTool === 'analyze-alignment' ? '' : 'analyze-alignment')}>Analyze Alignment <span>+</span></button>
        {#if openTool === 'analyze-alignment'}
          <div class="tool-content" transition:slide={{ duration: 160 }}>
            <button on:click={createConsensus} disabled={$uiStore.busy || !selectedAlignments.length}>Consensus sequence</button>
            <TreeTool alignments={selectedAlignments} />
            <button on:click={createLogos} disabled={$uiStore.busy || !selectedAlignments.length}>Sequence logo</button>
          </div>
        {/if}
      </section>

      <section class:open={openTool === 'pcr'} class="tool-section">
        <button class="tool-heading" on:click={() => (openTool = openTool === 'pcr' ? '' : 'pcr')}>PCR <span>+</span></button>
        {#if openTool === 'pcr'}
          <div class="tool-content" transition:slide={{ duration: 160 }}>
            <PrimerTool source={markTarget?.sequence.kind === 'DNA' ? markTarget.sequence : selected} />
          </div>
        {/if}
      </section>

      <section class:open={openTool === 'share-export'} class="tool-section">
        <button class="tool-heading" on:click={() => (openTool = openTool === 'share-export' ? '' : 'share-export')}>Share & Export <span>+</span></button>
        {#if openTool === 'share-export'}
          <div class="tool-content" transition:slide={{ duration: 160 }}>
            <button on:click={share} disabled={$uiStore.busy}>Share project</button>
            <label class="export-scope">Export scope
              <select bind:value={exportScope}>
                <option value="auto">{selectedIds.length ? `Selected entries (${selectedIds.length})` : 'Automatic (all entries)'}</option>
                <option value="all">All entries</option>
              </select>
            </label>
            <button disabled={!sequencesToExport.length} title="Export full DNA and protein sequences as FASTA"
              on:click={() => downloadFasta(project.name, sequencesToExport)}>Export FASTA</button>
            <button disabled={!alignmentsToExport.length} title="Export alignments as CLUSTAL; multiple alignments are bundled in a ZIP"
              on:click={() => downloadClustal(project.name, alignmentsToExport)}>Export CLUSTAL</button>
            <button disabled={!entriesToExport.length} on:click={printWorkspace}>Print</button>
          </div>
        {/if}
      </section>
    </div>
  </aside>
</main>

{#if pendingDeleteIds.length}
  <Modal id="delete-entries" title="Delete selected entries" onClose={() => { pendingDeleteIds = []; }}>
    <p>Delete {pendingDeleteIds.length} selected {pendingDeleteIds.length === 1 ? 'entry' : 'entries'} from &quot;{project.name}&quot;?</p>
    <p>This cannot be undone.</p>
    <svelte:fragment slot="footer">
      <button data-initial-focus on:click={() => { pendingDeleteIds = []; }}>Cancel</button>
      <button class="danger-button" on:click={deleteSelectedEntries}>Delete {pendingDeleteIds.length === 1 ? 'entry' : 'entries'}</button>
    </svelte:fragment>
  </Modal>
{/if}

{#if shareModalOpen}
  <Modal id="share-project" title="Share project" returnFocus={shareTrigger} onClose={() => { shareModalOpen = false; }}>
    <div class="share-result">
      <label for="shared-project-link">Shared project link</label>
      <input id="shared-project-link" data-initial-focus readonly value={sharedUrl} on:focus={(event) => event.currentTarget.select()} />
      <a href={sharedUrl} target="_blank" rel="noopener">Open shared project</a>
      <p class="share-copy-status" role="status">{shareCopied ? 'Link copied.' : shareCopyError}</p>
    </div>
    <svelte:fragment slot="footer">
      <button on:click={copyShareLink} disabled={shareCopying}>Copy link</button>
      <button class="primary-button" on:click={() => { shareModalOpen = false; }}>Close</button>
    </svelte:fragment>
  </Modal>
{/if}

{#if translationModalOpen && translationPreview}
  <div class="modal-backdrop" role="presentation" on:click={closeTranslationModal}>
    <div class="translation-modal" role="dialog" aria-modal="true" aria-labelledby="translation-modal-title" tabindex="-1" on:click|stopPropagation on:keydown|stopPropagation>
      <header>
        <div>
          <h2 id="translation-modal-title">Translation frames</h2>
          <p>{translationPreviewSource?.name}</p>
        </div>
        <button class="icon-button" on:click={closeTranslationModal} aria-label="Close">x</button>
      </header>
      <div class="translation-status">
        {translationStatusText(visibleTranslationRange)}
      </div>
      <div class="translation-frame-list">
        {#each [0, 1, 2] as frame}
          <div class="translation-frame-row">
            <strong>Frame {frame + 1}</strong>
            <div class="translation-codons" role="group" aria-label={`Frame ${frame + 1} codons`} on:pointermove={previewFrameCodonFromPointer} on:pointerover={previewFrameCodonFromPointer}>
              {#each frameCodons(frame) as codon}
                <button
                  data-frame={frame}
                  data-codon-index={codon.index}
                  class:start-codon={codon.marker === 'a'}
                  class:stop-codon={codon.marker === 'b'}
                  class:selected={isTranslationCodonSelected(frame, codon.index)}
                  class:preview={isTranslationCodonPreviewed(frame, codon.index, visibleTranslationRange)}
                  class:draft-anchor={isTranslationDraftAnchor(frame, codon.index)}
                  title={`${codon.start + 1}-${codon.end + 1} ${codon.dna} > ${codon.marker === 'b' ? 'STOP' : codon.aminoAcid}`}
                  on:focus={() => previewFrameCodon(frame, codon.index)}
                  on:mouseover={() => previewFrameCodon(frame, codon.index)}
                  on:mousemove={() => previewFrameCodon(frame, codon.index)}
                  on:pointerenter={() => previewFrameCodon(frame, codon.index)}
                  on:pointermove={() => previewFrameCodon(frame, codon.index)}
                  on:click={(event) => selectFrameCodon(frame, codon.index, event.shiftKey)}
                >
                  <span>{codon.aminoAcid}</span>
                  <small>{codon.start + 1}</small>
                </button>
              {/each}
            </div>
          </div>
        {/each}
      </div>
      <footer>
        <button on:click={closeTranslationModal}>Cancel</button>
        <button class="primary-button" on:click={exportSelectedTranslation} disabled={!translationRange}>Export selected</button>
      </footer>
    </div>
  </div>
{/if}
