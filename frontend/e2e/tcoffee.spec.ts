import {test, expect} from '@playwright/test';
import type {ProjectDto} from '../src/lib/api/types';

const workspace: ProjectDto = {
    name: 'Multiple alignment', alignments: [], entryOrder: ['one', 'two', 'three'], sequences: [
        {id: 'one', name: 'Reference', kind: 'DNA', sequence: 'ACGTACGTACGTACGT', marks: []},
        {id: 'two', name: 'Variant', kind: 'DNA', sequence: 'ACGTACGTACGT', marks: []},
        {id: 'three', name: 'Variant', kind: 'DNA', sequence: 'ACGTACGTACGTACGTACGT', marks: []}
    ]
};

test('real T-Coffee survives refresh, preserves source order, shares and exports', async ({page, browser, request}) => {
    const capabilities = await (await request.get('/api/tools/tcoffee')).json();
    test.skip(!capabilities.available, 'Real tool requires the Docker deployment');
    await page.addInitScript(value => {
        if (!localStorage.getItem('genewarrior.workspace.v1')) localStorage.setItem('genewarrior.workspace.v1', JSON.stringify(value));
    }, workspace);
    await page.goto('/');
    await page.getByRole('button', {name: 'Create Alignment', exact: false}).click();
    const button = page.getByRole('button', {name: 'T-Coffee', exact: true});
    await expect(button).toBeDisabled();
    for (const id of ['three', 'two', 'one']) await page.locator(`#entry-${id} .entry-card-header`).click();
    await expect(button).toBeEnabled();
    const accepted = page.waitForResponse(response => response.url().endsWith('/api/alignments') && response.request().method() === 'POST');
    await button.click();
    expect((await accepted).status()).toBe(202);
    await page.reload();
    await page.getByRole('button', {name: 'Create Alignment', exact: false}).click();
    await expect(page.locator('.entry-card')).toHaveCount(4, {timeout: 30000});
    await expect(page.locator('.alignment-jobs')).toContainText('Alignment added');
    const state: ProjectDto = await page.evaluate(() => JSON.parse(localStorage.getItem('genewarrior.workspace.v1')!));
    const alignment = state.alignments[0];
    expect(alignment.sequences.map(row => row.value.replaceAll('-', ''))).toEqual(workspace.sequences.map(sequence => sequence.sequence));
    expect(alignment.sequences.map(row => row.name)).toEqual(['Reference', 'Variant', 'Variant']);
    expect(state.entryOrder).toEqual(['one', alignment.id, 'two', 'three']);
    expect(alignment.score).toBeNull();
    expect(alignment.provenance?.tool).toBe('T-Coffee');
    await page.screenshot({path: 'test-results/tcoffee-desktop.png', fullPage: true});
    await page.setViewportSize({width: 390, height: 844});
    await page.screenshot({path: 'test-results/tcoffee-mobile.png', fullPage: true});
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
    await page.setViewportSize({width: 1440, height: 1000});
    await page.reload();
    await expect(page.locator('.entry-card')).toHaveCount(4);
    await page.locator('.app-header').getByRole('button', {name: 'Share project'}).click();
    const link = page.getByRole('textbox', {name: /Shared project/});
    await expect(link).toBeVisible();
    const recipient = await browser.newContext();
    try {
        const other = await recipient.newPage();
        await other.goto(await link.inputValue());
        await expect(other.locator(`#entry-${alignment.id} h2`)).toHaveText('T-Coffee alignment');
        const shared: ProjectDto = await other.evaluate(() => JSON.parse(localStorage.getItem(Object.keys(localStorage).find(key => key.includes('.shared.') && !key.endsWith('.jobs.v1'))!)!));
        expect(shared.alignments[0].provenance).toEqual(alignment.provenance);
        await expect(other.locator('.alignment-job')).toHaveCount(0);
    } finally {
        await recipient.close();
    }
    await page.getByRole('dialog', {name: 'Share project'}).getByRole('button', {name: 'Close', exact: true}).click();
    await page.getByRole('button', {name: 'Share & Export', exact: false}).click();
    const download = page.waitForEvent('download');
    await page.getByRole('button', {name: 'Export CLUSTAL', exact: true}).click();
    const chunks: Buffer[] = [];
    for await (const chunk of (await (await download).createReadStream())!) chunks.push(Buffer.from(chunk));
    const clustal = Buffer.concat(chunks).toString('utf8');
    expect(clustal).toContain('CLUSTAL');
    expect(clustal).toContain(alignment.sequences[0].value);
});

test('pending job exposes cancel and retry without changing the workspace', async ({page}) => {
    await page.addInitScript(value => localStorage.setItem('genewarrior.workspace.v1', JSON.stringify(value)), workspace);
    let id = '';
    let status = 'QUEUED';
    let release!: () => void;
    const gate = new Promise<void>(resolve => release = resolve);
    const job = () => ({id, status, tool: 'TCOFFEE', createdAt: new Date().toISOString()});
    await page.route('**/api/tools/tcoffee', route => route.fulfill({
        json: {
            available: true,
            maxSequences: 20,
            maxLength: 2000,
            maxResidues: 10000
        }
    }));
    await page.route('**/api/alignments', async route => {
        id = route.request().postDataJSON().requestId;
        status = 'QUEUED';
        await gate;
        return route.fulfill({status: 202, json: job()});
    });
    await page.route('**/api/jobs/*', route => {
        if (route.request().method() === 'DELETE') status = 'CANCELLED';
        return route.fulfill({json: job()});
    });
    await page.goto('/');
    for (const source of ['one', 'two']) await page.locator(`#entry-${source} .entry-card-header`).click();
    await page.getByRole('button', {name: 'Create Alignment', exact: false}).click();
    await page.getByRole('button', {name: 'T-Coffee', exact: true}).click();
    await expect(page.locator('.job-card')).toBeVisible();
    await expect(page.locator('.job-card')).toContainText('Submitting...');
    await expect(page.locator('.job-spinner')).toBeVisible();
    await expect(page.locator('.entry-stack > :nth-child(2)')).toHaveClass(/job-card/);
    expect(await page.evaluate(() => JSON.parse(localStorage.getItem('genewarrior.workspace.v1')!).alignments)).toEqual([]);
    await page.screenshot({path: 'test-results/job-placeholder-desktop.png', fullPage: true});
    release();
    await expect(page.locator('.alignment-jobs')).toContainText('queued');
    const original = id;
    await page.reload();
    await expect(page.locator('.job-card')).toHaveCount(1);
    await page.getByRole('button', {name: 'Create Alignment', exact: false}).click();
    await page.locator('.job-card').getByRole('button', {name: 'Cancel', exact: true}).click();
    await expect(page.locator('.alignment-jobs')).toContainText('cancelled');
    await expect(page.locator('.entry-card')).toHaveCount(4);
    await page.locator('.job-card').getByRole('button', {name: 'Retry', exact: true}).click();
    await expect(page.locator('.alignment-jobs')).toContainText('queued');
    expect(id).not.toBe(original);
});
