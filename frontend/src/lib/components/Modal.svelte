<script lang="ts">
  export let title: string;
  export let id: string;
  export let onClose: () => void;
  export let returnFocus: HTMLElement | null = null;

  function openModal(dialog: HTMLDialogElement) {
    const previousFocus = returnFocus ?? document.activeElement;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    dialog.showModal();
    dialog.querySelector<HTMLElement>('[data-initial-focus]')?.focus();
    return {
      destroy() {
        dialog.close();
        document.body.style.overflow = previousOverflow;
        if (previousFocus instanceof HTMLElement && previousFocus.isConnected) previousFocus.focus();
      }
    };
  }

  function clickBackdrop(event: MouseEvent) {
    const dialog = event.currentTarget as HTMLDialogElement;
    const bounds = dialog.getBoundingClientRect();
    if (event.target === dialog && (event.clientX < bounds.left || event.clientX > bounds.right
      || event.clientY < bounds.top || event.clientY > bounds.bottom)) onClose();
  }

  function keepFocusInModal(event: KeyboardEvent) {
    if (event.key !== 'Tab') return;
    const dialog = event.currentTarget as HTMLDialogElement;
    const controls = Array.from(dialog.querySelectorAll<HTMLElement>(
      'button:not(:disabled), a[href], input:not(:disabled), select:not(:disabled), textarea:not(:disabled), [tabindex]'
    )).filter(node => node.tabIndex >= 0 && node.getClientRects().length > 0);
    const first = controls[0];
    const last = controls.at(-1);
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last?.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first?.focus();
    }
  }
</script>

<dialog class="workspace-modal" aria-modal="true" aria-labelledby={`${id}-title`} use:openModal
  on:cancel|preventDefault={onClose} on:click={clickBackdrop} on:keydown|stopPropagation={keepFocusInModal} on:copy|stopPropagation>
  <header><h2 id={`${id}-title`}>{title}</h2></header>
  <div class="workspace-modal-body"><slot /></div>
  <footer><slot name="footer" /></footer>
</dialog>
