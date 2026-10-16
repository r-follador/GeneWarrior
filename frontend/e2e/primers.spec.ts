import {test, expect} from '@playwright/test';
import type {ProjectDto} from '../src/lib/api/types';
import {exampleProject} from '../src/lib/stores/projectStore';
import {reverseComplement} from '../src/lib/primer/model';

const source = exampleProject().sequences[0];
const workspace: ProjectDto = {
    name: 'Primer design regression',
    sequences: [source],
    alignments: [],
    entryOrder: [source.id!]
};

test('real Primer3 designs internal probes, replaces placeholder, persists, shares and exports', async ({
                                                                                                            page,
                                                                                                            request,
                                                                                                            browser
                                                                                                        }) => {
    const capabilities = await (await request.get('/api/tools/primer3')).json();
    test.skip(!capabilities.available, 'Real Primer3 requires the Docker deployment');
    await page.addInitScript(value => {
        if (!localStorage.getItem('genewarrior.workspace.v1')) localStorage.setItem('genewarrior.workspace.v1', JSON.stringify(value));
    }, workspace);
    await page.goto('/');
    await page.locator(`#entry-${source.id} .entry-card-header`).click();
    await page.getByRole('button', {name: 'PCR', exact: false}).click();
    await page.getByRole('button', {name: 'Primer design', exact: true}).click();
    const dialog = page.getByRole('dialog', {name: 'Primer design'});
    await dialog.getByLabel('Include internal probe').check();
    await expect(dialog.getByRole('spinbutton', {name: 'Probe optimum Tm'})).toBeVisible();
    await dialog.getByLabel('Flank target region').check();
    await dialog.getByRole('spinbutton', {name: 'Target start', exact: true}).fill('100');
    await dialog.getByRole('spinbutton', {name: 'Target end', exact: true}).fill('120');
    await dialog.getByRole('spinbutton', {name: 'Product max (bp)', exact: true}).fill('350');
    await page.screenshot({path: 'test-results/primer-dialog-desktop.png', fullPage: true});
    await page.setViewportSize({width: 390, height: 844});
    const submitBounds = await dialog.getByRole('button', {name: 'Design primers', exact: true}).boundingBox();
    expect(submitBounds!.y + submitBounds!.height).toBeLessThanOrEqual(844);
    expect(await dialog.getByLabel('Include internal probe').evaluate(input => input.getBoundingClientRect().width)).toBeLessThanOrEqual(20);
    await page.screenshot({path: 'test-results/primer-dialog-mobile.png', fullPage: true});
    await page.setViewportSize({width: 1440, height: 1000});
    await dialog.getByRole('button', {name: 'Design primers', exact: true}).click();
    await expect(dialog).not.toBeVisible();
    await expect(page.locator('.primer-card')).toBeVisible({timeout: 30000});
    await expect(page.locator('.job-card')).toHaveCount(0);
    await expect(page.locator('.primer-card')).toContainText('Probe');
    const state: ProjectDto = await page.evaluate(() => JSON.parse(localStorage.getItem('genewarrior.workspace.v1')!));
    const design = state.primerDesigns![0];
    expect(design.pairs.length).toBeGreaterThan(0);
    expect(design.settings.target).toEqual({start: 99, end: 120});
    expect(state.entryOrder).toEqual([source.id, design.id]);
    for (const pair of design.pairs) {
        expect(pair.right.sequence).toBe(reverseComplement(source.sequence.slice(pair.right.start, pair.right.end)));
        expect(pair.probe?.sequence).toBe(source.sequence.slice(pair.probe!.start, pair.probe!.end));
    }
    await page.reload();
    await expect(page.locator('.primer-card')).toBeVisible();
    await page.screenshot({path: 'test-results/primer-result-desktop.png', fullPage: true});
    await page.setViewportSize({width: 390, height: 844});
    await page.screenshot({path: 'test-results/primer-result-mobile.png', fullPage: true});
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
    await page.setViewportSize({width: 1440, height: 1000});
    const card = page.locator('.primer-card');
    const download = page.waitForEvent('download');
    await card.getByRole('button', {name: 'Export pair FASTA'}).click();
    const chunks: Buffer[] = [];
    for await (const chunk of (await (await download).createReadStream())!) chunks.push(Buffer.from(chunk));
    const text = Buffer.concat(chunks).toString('utf8');
    expect(text).toContain(design.pairs[0].right.sequence);
    expect(text).toContain(design.pairs[0].probe!.sequence);
    await card.getByRole('button', {name: 'Add oligos as sequences'}).click();
    await expect(page.locator('.entry-card')).toHaveCount(5);
    await page.locator('.app-header').getByRole('button', {name: 'Share project'}).click();
    const link = page.getByRole('textbox', {name: /Shared project/});
    await expect(link).toBeVisible();
    const recipient = await browser.newContext();
    try {
        const other = await recipient.newPage();
        await other.goto(await link.inputValue());
        await expect(other.locator('.primer-card')).toBeVisible();
        await expect(other.locator('.primer-card')).toContainText(design.pairs[0].probe!.sequence);
        await expect(other.locator('.job-card')).toHaveCount(0);
    } finally {
        await recipient.close();
    }
    await page.getByRole('dialog', {name: 'Share project'}).getByRole('button', {name: 'Close', exact: true}).click();
    await page.emulateMedia({media: 'print'});
    await page.evaluate(() => window.dispatchEvent(new Event('beforeprint')));
    await expect(page.locator('.print-workspace')).toContainText(design.pairs[0].probe!.sequence);
    await page.screenshot({path: 'test-results/primer-print.png', fullPage: true});
});

test('no acceptable primers is a completed design rather than a failed job', async ({page, request}) => {
    const capabilities = await (await request.get('/api/tools/primer3')).json();
    test.skip(!capabilities.available, 'Real Primer3 requires Docker');
    const emptySource = {...source, sequence: 'A'.repeat(200)};
    await page.addInitScript(value => localStorage.setItem('genewarrior.workspace.v1', JSON.stringify(value)), {
        ...workspace,
        sequences: [emptySource]
    });
    await page.goto('/');
    await page.locator(`#entry-${source.id} .entry-card-header`).click();
    await page.getByRole('button', {name: 'PCR', exact: false}).click();
    await page.getByRole('button', {name: 'Primer design', exact: true}).click();
    await page.getByRole('button', {name: 'Design primers', exact: true}).click();
    await expect(page.locator('.primer-card')).toContainText('No acceptable primer pairs found.', {timeout: 30000});
    await expect(page.locator('.job-card')).toHaveCount(0);
    await expect(page.locator('.primer-card details')).toHaveAttribute('open', '');
});
