import {test, expect} from '@playwright/test';
import type {GuideTreeDto, ProjectDto} from '../src/lib/api/types';
import {exampleProject} from '../src/lib/stores/projectStore';

const source = exampleProject().alignments[0];
const workspace: ProjectDto = {name: 'Guide trees', sequences: [], alignments: [source], entryOrder: [source.id]};

test('real T-Coffee guide tree renders, survives refresh/share, and exports SVG/Newick/print', async ({
                                                                                                          page,
                                                                                                          request,
                                                                                                          browser
                                                                                                      }) => {
    test.skip(!(await (await request.get('/api/tools/tcoffee')).json()).available, 'Real T-Coffee requires Docker');
    await page.addInitScript(value => {
        if (!localStorage.getItem('genewarrior.workspace.v1')) localStorage.setItem('genewarrior.workspace.v1', JSON.stringify(value));
    }, workspace);
    await page.goto('/');
    await page.getByRole('button', {name: 'Analyze Alignment', exact: false}).click();
    const trigger = page.getByRole('button', {name: 'Guide tree (T-Coffee)', exact: true});
    await expect(trigger).toBeDisabled();
    await page.locator(`#entry-${source.id} .entry-card-header`).click();
    const response = page.waitForResponse(response => response.url().endsWith('/api/trees') && response.request().method() === 'POST');
    await trigger.click();
    await expect(page.locator('.job-card')).toContainText('T-Coffee guide tree');
    expect((await response).status()).toBe(202);
    await page.reload();
    const card = page.locator('.tree-card');
    await expect(card).toBeVisible({timeout: 30000});
    await expect(page.locator('.job-card')).toHaveCount(0);
    const project: ProjectDto = await page.evaluate(() => JSON.parse(localStorage.getItem('genewarrior.workspace.v1')!));
    const tree = project.trees![0];
    expect(project.entryOrder).toEqual([source.id, tree.id]);
    expect(tree.sources.map(row => row.name)).toEqual(source.sequences.map(row => row.name));
    await expect(card.locator('svg.guide-tree')).toBeVisible();
    expect(await card.locator('svg path').count()).toBeGreaterThan(2);
    await page.screenshot({path: 'test-results/tree-desktop.png', fullPage: true});
    await page.setViewportSize({width: 390, height: 844});
    await page.screenshot({path: 'test-results/tree-mobile.png', fullPage: true});
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await page.setViewportSize({width: 1440, height: 1000});
    for (const format of ['SVG', 'Newick']) {
        const downloading = page.waitForEvent('download');
        await card.getByRole('button', {name: `Download ${format}`, exact: true}).click();
        const chunks: Buffer[] = [];
        for await (const chunk of (await (await downloading).createReadStream())!) chunks.push(Buffer.from(chunk));
        const text = Buffer.concat(chunks).toString('utf8');
        expect(text).toContain(source.sequences[0].name);
        if (format === 'SVG') {
            expect(text).toContain('<svg');
            expect(text).not.toContain('<script');
        } else expect(text.trim()).toMatch(/;$/);
    }
    await card.getByLabel('Layout').selectOption({label: 'Topology'});
    await expect(card.locator('svg')).not.toContainText('guide distance');
    await card.getByRole('button', {name: 'Minimize card', exact: true}).click();
    await expect(card.locator('svg')).toHaveCount(0);
    await card.locator('.entry-card-header').click();
    await expect(card.locator('svg')).toBeVisible();
    await page.locator('.app-header').getByRole('button', {name: 'Share project'}).click();
    const link = page.getByRole('textbox', {name: /Shared project/});
    await expect(link).toBeVisible();
    const recipient = await browser.newContext();
    try {
        const other = await recipient.newPage();
        await other.goto(await link.inputValue());
        await expect(other.locator('.tree-card svg')).toBeVisible();
        await expect(other.locator('.tree-card')).toContainText(source.sequences[0].name);
    } finally {
        await recipient.close();
    }
    await page.getByRole('dialog', {name: 'Share project'}).getByRole('button', {name: 'Close', exact: true}).click();
    await page.emulateMedia({media: 'print'});
    await page.evaluate(() => window.dispatchEvent(new Event('beforeprint')));
    await expect(page.locator('.print-workspace svg.guide-tree')).toBeVisible();
    await page.screenshot({path: 'test-results/tree-print.png', fullPage: true});
});

test('zero-length trees keep visible topology and safely wrap long untrusted names on mobile', async ({page}) => {
    const names = ["O'Brien <script>alert(1)</script> " + 'Long '.repeat(30), 'Duplicate', 'Duplicate'];
    const tree: GuideTreeDto = {
        id: 'tree-zero',
        name: 'Zero guide tree',
        sourceAlignmentId: 'deleted',
        kind: 'DNA',
        version: '13.41',
        method: 'tcoffee-nj-guide-v1',
        sources: names.map(name => ({name, sha256: 'a'.repeat(64)})),
        nodes: [{id: 0, parentId: null, name: '', sourceIndex: null, length: null},
            ...names.map((name, index) => ({id: index + 1, parentId: 0, name, sourceIndex: index, length: 0}))]
    };
    await page.addInitScript(value => localStorage.setItem('genewarrior.workspace.v1', JSON.stringify(value)),
        {name: 'Edge cases', sequences: [], alignments: [], trees: [tree], entryOrder: [tree.id]});
    await page.setViewportSize({width: 390, height: 844});
    await page.goto('/');
    await expect(page.locator('.tree-card')).toContainText('No positive branch lengths; topology shown.');
    await expect(page.locator('.tree-card script')).toHaveCount(0);
    const geometry = await page.locator('.tree-card svg text').evaluateAll(labels => labels.map(label => {
        const b = label.getBoundingClientRect();
        return {top: b.top, bottom: b.bottom, right: b.right};
    }));
    expect(geometry[1].top).toBeGreaterThan(geometry[0].bottom);
    expect(geometry.every(bounds => bounds.right <= 390)).toBe(true);
    await page.screenshot({path: 'test-results/tree-edge-mobile.png', fullPage: true});
});

test('pairwise alignments disable guide-tree submission with a minimum-row constraint', async ({page}) => {
    await page.route('**/api/tools/tcoffee', route => route.fulfill({json: {available: true}}));
    await page.addInitScript(value => localStorage.setItem('genewarrior.workspace.v1', JSON.stringify(value)),
        {...workspace, alignments: [{...source, sequences: source.sequences.slice(0, 2)}]});
    await page.goto('/');
    await page.locator(`#entry-${source.id} .entry-card-header`).click();
    await page.getByRole('button', {name: 'Analyze Alignment', exact: false}).click();
    await expect(page.getByRole('button', {name: 'Guide tree (T-Coffee)', exact: true})).toBeDisabled();
    await expect(page.getByText('Guide trees require at least 3 alignment rows.', {exact: true})).toBeVisible();
});
