import {test, expect} from '@playwright/test';
import type {ProjectDto} from '../src/lib/api/types';

const workspace: ProjectDto = {
    name: 'Logo regression', sequences: [], entryOrder: ['dna', 'protein'],
    alignments: [
        {
            id: 'dna', name: 'DNA <alignment>', kind: 'DNA', score: 0, marks: [],
            sequences: ['AA-N', 'AC-N', 'AG-N', 'AT-N'].map((value, i) => ({
                name: `DNA ${i}`,
                value: value.repeat(20)
            })), consensus: '    '.repeat(20)
        },
        {
            id: 'protein',
            name: 'Protein alignment',
            kind: 'PROTEIN',
            score: 0,
            marks: [],
            sequences: [{name: 'First', value: 'WMX-'.repeat(20)}, {name: 'Second', value: 'WAX-'.repeat(20)}],
            consensus: '*   '.repeat(20)
        }
    ]
};

test('native logos render, switch scales, download, persist, share and print', async ({page, browser}) => {
    const errors: string[] = [];
    page.on('pageerror', error => errors.push(error.message));
    await page.addInitScript(value => {
        if (!localStorage.getItem('genewarrior.workspace.v1')) localStorage.setItem('genewarrior.workspace.v1', JSON.stringify(value));
    }, workspace);
    await page.goto('/');
    await page.locator('#entry-dna .entry-card-header').click();
    await page.locator('#entry-protein .entry-card-header').click();
    await page.getByRole('button', {name: 'Analyze Alignment', exact: false}).click();
    await page.getByRole('button', {name: 'Sequence logo', exact: true}).click();
    await expect(page.locator('.logo-card')).toHaveCount(2);
    const dna = page.locator('.logo-card').filter({hasText: 'DNA <alignment>'});
    const protein = page.locator('.logo-card').filter({hasText: 'Protein alignment'});
    await expect(dna.locator('[data-logo-position="1"] path')).toHaveCount(1);
    await expect(dna.locator('[data-logo-position="2"] path')).toHaveCount(4);
    await expect(dna.locator('[data-logo-position="3"] path')).toHaveCount(0);
    await expect(dna.locator('[data-logo-position="4"] path')).toHaveCount(0);
    await expect(protein.locator('[data-logo-position="1"] path')).toHaveCount(1);
    await expect(dna.locator('[data-logo-position="3"] > title')).toContainText('4 gaps');
    await dna.getByLabel('Logo scale').selectOption('INFORMATION');
    await expect(dna.locator('[data-logo-position="2"] path')).toHaveCount(0);
    const state = await page.evaluate(() => JSON.parse(localStorage.getItem('genewarrior.workspace.v1')!));
    expect(state.entryOrder).toEqual(['dna', state.logos[0].id, 'protein', state.logos[1].id]);
    expect(state.logos[0].mode).toBe('INFORMATION');
    await dna.screenshot({path: 'test-results/logo-desktop.png'});
    await protein.screenshot({path: 'test-results/logo-protein.png'});

    const pending = page.waitForEvent('download');
    await dna.getByRole('button', {name: 'Download SVG'}).click();
    const download = await pending;
    expect(download.suggestedFilename()).toBe('DNA-alignment-Logo.svg');
    const chunks: Buffer[] = [];
    for await (const chunk of (await download.createReadStream())!) chunks.push(Buffer.from(chunk));
    const svg = Buffer.concat(chunks).toString('utf8');
    expect(svg).toContain('http://www.w3.org/2000/svg');
    expect(svg).toContain('DejaVu');
    expect(svg).toContain('DNA &lt;alignment&gt;');
    const inspected = await page.evaluate(async svg => {
        const xml = new DOMParser().parseFromString(svg, 'image/svg+xml');
        const image = new Image();
        image.src = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg);
        await image.decode();
        const canvas = document.createElement('canvas');
        canvas.width = image.width;
        canvas.height = image.height;
        const context = canvas.getContext('2d')!;
        context.drawImage(image, 0, 0);
        const pixels = context.getImageData(0, 0, canvas.width, canvas.height).data;
        let green = 0;
        for (let i = 0; i < pixels.length; i += 4) if (pixels[i + 1] > pixels[i] + 30 && pixels[i + 1] > pixels[i + 2] + 30) green++;
        return {
            errors: xml.querySelectorAll('parsererror').length,
            positions: xml.querySelectorAll('[data-logo-position]').length,
            green
        };
    }, svg);
    expect(inspected.errors).toBe(0);
    expect(inspected.positions).toBe(80);
    expect(inspected.green).toBeGreaterThan(1000);

    await page.reload();
    await expect(dna.getByLabel('Logo scale')).toHaveValue('INFORMATION');
    await page.locator('.app-header').getByRole('button', {name: 'Share project'}).click();
    const link = page.getByRole('textbox', {name: /Shared project/});
    await expect(link).toBeVisible();
    const recipient = await browser.newContext();
    const other = await recipient.newPage();
    await other.goto(await link.inputValue());
    await expect(other.locator('.logo-card')).toHaveCount(2);
    await expect(other.locator('.logo-card').first().getByLabel('Logo scale')).toHaveValue('INFORMATION');
    await recipient.close();
    await page.getByRole('dialog', {name: 'Share project'}).getByRole('button', {name: 'Close', exact: true}).click();

    await page.setViewportSize({width: 390, height: 844});
    await dna.scrollIntoViewIfNeeded();
    await expect(dna.locator('svg')).not.toHaveCount(0);
    expect(await dna.evaluate(node => node.scrollWidth <= node.clientWidth)).toBe(true);
    await dna.screenshot({path: 'test-results/logo-mobile.png'});
    await page.setViewportSize({width: 320, height: 700});
    await dna.screenshot({path: 'test-results/logo-narrow.png'});
    expect(await dna.evaluate(node => node.scrollWidth <= node.clientWidth)).toBe(true);
    expect(await dna.locator('svg').first().evaluate(svg => {
        const texts = svg.querySelectorAll(':scope > g > text');
        return texts[0].getBoundingClientRect().right < texts[1].getBoundingClientRect().left;
    })).toBe(true);
    await page.emulateMedia({media: 'print'});
    await page.evaluate(() => window.dispatchEvent(new Event('beforeprint')));
    await expect(page.locator('.print-workspace .logo-plot')).toHaveCount(2);
    await page.pdf({path: 'test-results/logos.pdf', format: 'A4', printBackground: true});
    await page.emulateMedia({media: 'screen'});
    await page.evaluate(() => window.dispatchEvent(new Event('afterprint')));
    await page.setViewportSize({width: 1440, height: 1000});
    await page.locator('#entry-dna .entry-card-header').click();
    await page.getByRole('button', {name: 'Delete selected entries'}).click();
    await page.getByRole('dialog', {name: 'Delete selected entries'}).getByRole('button', {
        name: 'Delete entry',
        exact: true
    }).click();
    await expect(page.locator('#entry-dna')).toHaveCount(0);
    await page.reload();
    await expect(dna).toBeVisible();
    await dna.locator('.entry-toggle').click();
    await expect(dna.locator('svg')).toHaveCount(0);
    await dna.locator('.entry-card-header').click();
    await expect(dna.locator('[data-logo-position="1"] path')).toHaveCount(1);
    expect(errors).toEqual([]);
});

test('a failed logo request leaves the workspace intact and reports its error', async ({page}) => {
    await page.goto('/');
    await page.locator('#entry-example-alignment .entry-card-header').click();
    await page.getByRole('button', {name: 'Analyze Alignment', exact: false}).click();
    await page.route('**/api/logos', route => route.fulfill({
        status: 413,
        contentType: 'application/json',
        body: JSON.stringify({message: 'Logo limit exceeded'})
    }));
    await page.getByRole('button', {name: 'Sequence logo', exact: true}).click();
    await expect(page.locator('.workspace .error')).toContainText('Logo limit exceeded');
    await expect(page.locator('.entry-card')).toHaveCount(4);
    await expect(page.locator('.logo-card')).toHaveCount(0);
});
