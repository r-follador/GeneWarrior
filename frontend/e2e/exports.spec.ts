import {test, expect, type Download, type Page} from '@playwright/test';
import {strFromU8, unzipSync} from 'fflate';
import type {ProjectDto} from '../src/lib/api/types';

const project: ProjectDto = {
    name: '../Export test <project>',
    sequences: [
        {
            id: 'dna', name: 'DNA <sample>', kind: 'DNA', sequence: 'ACGT'.repeat(40), marks: [
                {start: 5, end: 25, type: 'HIGHLIGHT', color: '#f4c95d'},
                {
                    start: 45,
                    end: 135,
                    type: 'ANNOTATION',
                    direction: 'REVERSE',
                    color: '#9bc7b5',
                    annotation: '<b>Domain</b>'
                }
            ]
        },
        {id: 'protein', name: 'Protein sample', kind: 'PROTEIN', sequence: 'MAKX*', marks: []}
    ],
    alignments: ['aln1', 'aln2'].map(id => ({
        id, name: '../Same alignment', kind: 'DNA', score: 10, consensus: '* **'.repeat(40),
        sequences: [{name: 'Same name', value: 'A-CG'.repeat(40)}, {name: 'Same name', value: 'ATCG'.repeat(40)}],
        marks: [{
            start: 50,
            end: 130,
            type: 'ANNOTATION',
            direction: 'FORWARD',
            color: '#e6ab51',
            annotation: 'Aligned region'
        }]
    })),
    entryOrder: ['protein', 'aln2', 'dna', 'aln1']
};

async function load(page: Page, workspace = project) {
    await page.addInitScript(value => localStorage.setItem('genewarrior.workspace.v1', JSON.stringify(value)), workspace);
    await page.goto('/');
    await expect(page.locator('.entry-card')).toHaveCount(workspace.entryOrder.length);
    await page.getByRole('button', {name: 'Share & Export', exact: false}).click();
}

async function bytes(download: Download) {
    const stream = await download.createReadStream();
    const chunks: Buffer[] = [];
    for await (const chunk of stream!) chunks.push(Buffer.from(chunk));
    return Buffer.concat(chunks);
}

async function exportFile(page: Page, name: string) {
    const pending = page.waitForEvent('download');
    await page.getByRole('button', {name, exact: true}).click();
    return pending;
}

test('FASTA downloads preserve complete sequences and workspace order, with selected/all scope', async ({page}) => {
    await load(page);
    const all = await exportFile(page, 'Export FASTA');
    expect(all.suggestedFilename()).toBe('Export-test-project.fasta');
    const text = (await bytes(all)).toString('utf8');
    expect(text).toMatch(/^>Protein sample\nMAKX\*\n>DNA <sample>\n/);
    expect(text.split('>DNA <sample>\n')[1].replace(/\n/g, '')).toBe(project.sequences[0].sequence);
    expect(text).not.toContain('Same name');
    await page.locator('#entry-dna .entry-card-header').click();
    expect((await bytes(await exportFile(page, 'Export FASTA'))).toString('utf8')).not.toContain('>Protein');
    await expect(page.getByRole('button', {name: 'Export CLUSTAL', exact: true})).toBeDisabled();
    await page.getByLabel('Export scope').selectOption('all');
    expect((await bytes(await exportFile(page, 'Export FASTA'))).toString('utf8')).toBe(text);
});

test('CLUSTAL exports individual alignments or a ZIP containing distinct complete files', async ({page}) => {
    await load(page);
    const archive = await exportFile(page, 'Export CLUSTAL');
    expect(archive.suggestedFilename()).toBe('Export-test-project-alignments.zip');
    const files = unzipSync(await bytes(archive));
    expect(Object.keys(files)).toEqual(['Same-alignment.aln', 'Same-alignment_2.aln']);
    const content = strFromU8(files['Same-alignment.aln']);
    expect(content.startsWith('CLUSTAL ')).toBe(true);
    expect(content.split('\n').filter(line => line.startsWith('Same_name ')).map(line => line.trim().split(/\s+/)[1]).join(''))
        .toBe(project.alignments[0].sequences[0].value);
    expect(content.split('\n').filter(line => line.startsWith('Same_name_2 ')).map(line => line.trim().split(/\s+/)[1]).join(''))
        .toBe(project.alignments[0].sequences[1].value);
    await page.locator('#entry-aln1 .entry-card-header').click();
    await expect(page.getByRole('button', {name: 'Export FASTA', exact: true})).toBeDisabled();
    const single = await exportFile(page, 'Export CLUSTAL');
    expect(single.suggestedFilename()).toBe('Same-alignment.aln');
    expect((await bytes(single)).toString('utf8')).toBe(content);
});

test('Print button prepares selected collapsed cards without changing workspace state', async ({page}) => {
    await load(page);
    await page.locator('#entry-dna .entry-card-header').click();
    await page.locator('#entry-dna .entry-toggle').click();
    const before = await page.evaluate(() => localStorage.getItem('genewarrior.workspace.v1'));
    await page.evaluate(() => {
        window.print = () => {
            (window as any).printedText = document.querySelector('.print-workspace')!.textContent;
            (window as any).printedCount = document.querySelectorAll('.print-entry').length;
            window.dispatchEvent(new Event('afterprint'));
        };
    });
    await page.getByRole('button', {name: 'Print', exact: true}).click();
    const printed = await page.evaluate(() => ({
        text: (window as any).printedText,
        count: (window as any).printedCount
    }));
    expect(printed.count).toBe(1);
    expect(printed.text).toContain('DNA <sample>');
    expect(printed.text).toContain('<b>Domain</b>');
    expect(printed.text).toContain('ACGT');
    await expect(page.locator('#entry-dna')).toHaveClass(/selected/);
    await expect(page.locator('#entry-dna')).toHaveClass(/collapsed/);
    expect(await page.evaluate(() => localStorage.getItem('genewarrior.workspace.v1'))).toBe(before);
    await expect(page.locator('.print-entry')).toHaveCount(0);
});

test('native print renders wrapped annotations and complete alignments, without tools or clipping', async ({page}) => {
    await load(page);
    await page.emulateMedia({media: 'print'});
    await page.evaluate(() => window.dispatchEvent(new Event('beforeprint')));
    await expect(page.locator('.print-entry')).toHaveCount(4);
    await expect(page.locator('.tools-panel')).toBeHidden();
    await expect(page.locator('.navigator-panel')).toBeHidden();
    await expect(page.locator('.print-entry').first()).toContainText('Protein sample');
    await expect(page.locator('.print-arrow.reverse.continues-before.continues-after')).toHaveCount(1);
    await expect(page.locator('.print-marks b')).toHaveCount(0);
    await expect(page.locator('.print-consensus').first()).toHaveCSS('background-color', 'rgb(255, 242, 148)');
    await expect(page.locator('.print-entry').nth(2).locator('code span').nth(5)).toHaveCSS('background-color', 'rgb(244, 201, 93)');
    const residues = await page.locator('.print-entry').nth(2).locator('code').allTextContents();
    expect(residues.join('')).toBe(project.sequences[0].sequence);
    await page.setViewportSize({width: 794, height: 1123});
    const geometry = await page.locator('.print-row code').evaluateAll(rows => rows.map(row => {
        const rect = row.getBoundingClientRect();
        return {width: rect.width, right: rect.right};
    }));
    expect(geometry.every(row => row.width > 0 && row.right <= 680)).toBe(true);
    await page.screenshot({path: 'test-results/exports-print.png', fullPage: true});
    await page.pdf({path: 'test-results/exports-print.pdf', format: 'A4', printBackground: true});
    await page.emulateMedia({media: 'screen'});
    await page.evaluate(() => window.dispatchEvent(new Event('afterprint')));
    await expect(page.locator('.tools-panel')).toBeVisible();
    await page.setViewportSize({width: 390, height: 844});
    await page.getByRole('button', {name: 'Export FASTA', exact: true}).scrollIntoViewIfNeeded();
    await page.screenshot({path: 'test-results/exports-mobile.png', fullPage: true});
});

test('long sequences print completely across pages and empty workspaces disable exports', async ({page}) => {
    const sequence = {...project.sequences[0], sequence: 'ACGT'.repeat(900)};
    await load(page, {...project, sequences: [sequence], alignments: [], entryOrder: ['dna']});
    await page.emulateMedia({media: 'print'});
    await page.evaluate(() => window.dispatchEvent(new Event('beforeprint')));
    expect((await page.locator('.print-row code').allTextContents()).join('')).toBe(sequence.sequence);
    await expect(page.locator('.print-block')).toHaveCount(60);
    await page.pdf({path: 'test-results/exports-long.pdf', format: 'A4', printBackground: true});
    await page.emulateMedia({media: 'screen'});
    await page.evaluate(() => window.dispatchEvent(new Event('afterprint')));
    await page.locator('#entry-dna .entry-card-header').click();
    await page.getByRole('button', {name: 'Delete selected entries'}).click();
    await page.getByRole('dialog', {name: 'Delete selected entries'}).getByRole('button', {
        name: 'Delete entry',
        exact: true
    }).click();
    await expect(page.locator('.entry-card')).toHaveCount(0);
    for (const name of ['Export FASTA', 'Export CLUSTAL', 'Print']) {
        await expect(page.getByRole('button', {name, exact: true})).toBeDisabled();
    }
});
