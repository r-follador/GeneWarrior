import {test, expect} from '@playwright/test';
import type {ProjectDto} from '../src/lib/api/types';

const workspace: ProjectDto = {
    name: 'Consensus regression',
    sequences: [{id: 'standalone', name: 'Standalone', kind: 'DNA', sequence: 'ACGT', marks: []}],
    alignments: [
        {
            id: 'dna', name: 'DNA alignment', kind: 'DNA', score: 0, marks: [], consensus: '  **'.repeat(25),
            sequences: [{name: 'First', value: 'A-GT'.repeat(25)}, {name: 'Second', value: 'C-GT'.repeat(25)}]
        },
        {
            id: 'protein',
            name: 'Protein alignment',
            kind: 'PROTEIN',
            score: 0,
            marks: [],
            consensus: '* * '.repeat(25),
            sequences: [{name: 'First', value: 'WMX-'.repeat(25)}, {name: 'Second', value: 'WAX-'.repeat(25)}]
        }
    ],
    entryOrder: ['dna', 'standalone', 'protein']
};

test('consensus processes every selected alignment, inserts below sources and survives reload/share/export', async ({
                                                                                                                        page,
                                                                                                                        browser
                                                                                                                    }) => {
    await page.addInitScript(value => {
        if (!localStorage.getItem('genewarrior.workspace.v1')) localStorage.setItem('genewarrior.workspace.v1', JSON.stringify(value));
    }, workspace);
    await page.goto('/');
    await page.getByRole('button', {name: 'Analyze Alignment', exact: false}).click();
    const button = page.getByRole('button', {name: 'Consensus sequence', exact: true});
    await expect(button).toBeDisabled();
    await page.locator('#entry-standalone .entry-card-header').click();
    await expect(button).toBeDisabled();
    // Click in the opposite order to the navigator to exercise workspace ordering.
    await page.locator('#entry-protein .entry-card-header').click();
    await page.locator('#entry-dna .entry-card-header').click();
    await page.locator('#entry-standalone .entry-card-header').click();
    await page.locator('#entry-standalone .entry-card-header').click();
    await button.click();
    await expect(page.locator('.entry-card')).toHaveCount(5);
    const state: ProjectDto = await page.evaluate(() => JSON.parse(localStorage.getItem('genewarrior.workspace.v1')!));
    const dna = state.sequences.find(sequence => sequence.name === 'DNA alignment [Consensus]')!;
    const protein = state.sequences.find(sequence => sequence.name === 'Protein alignment [Consensus]')!;
    expect(dna).toMatchObject({kind: 'DNA', sequence: 'MGT'.repeat(25), marks: []});
    expect(protein).toMatchObject({kind: 'PROTEIN', sequence: 'WXX'.repeat(25), marks: []});
    expect(state.entryOrder).toEqual(['dna', dna.id, 'standalone', 'protein', protein.id]);
    await page.reload();
    await expect(page.locator(`#entry-${dna.id} h2`)).toHaveText(dna.name);
    await expect(page.locator(`#entry-${protein.id} h2`)).toHaveText(protein.name);
    await page.screenshot({path: 'test-results/consensus-desktop.png', fullPage: true});
    await page.locator('.app-header').getByRole('button', {name: 'Share project'}).click();
    const link = page.getByRole('textbox', {name: /Shared project/});
    await expect(link).toBeVisible();
    const recipient = await browser.newContext();
    try {
        const other = await recipient.newPage();
        await other.goto(await link.inputValue());
        await expect(other.locator(`#entry-${dna.id} h2`)).toHaveText(dna.name);
        await expect(other.locator(`#entry-${protein.id} h2`)).toHaveText(protein.name);
    } finally {
        await recipient.close();
    }
    await page.getByRole('dialog', {name: 'Share project'}).getByRole('button', {name: 'Close', exact: true}).click();
    await page.getByRole('button', {name: 'Share & Export', exact: false}).click();
    const pending = page.waitForEvent('download');
    await page.getByRole('button', {name: 'Export FASTA', exact: true}).click();
    const chunks: Buffer[] = [];
    for await (const chunk of (await (await pending).createReadStream())!) chunks.push(Buffer.from(chunk));
    const fasta = Buffer.concat(chunks).toString('utf8');
    expect(fasta).toContain('>DNA alignment [Consensus]\n');
    expect(fasta).toContain('>Protein alignment [Consensus]\n');
    expect(fasta.split('>DNA alignment [Consensus]\n')[1].split('>')[0].replace(/\n/g, '')).toBe(dna.sequence);
    expect(fasta.split('>Protein alignment [Consensus]\n')[1].replace(/\n/g, '')).toBe(protein.sequence);
});

test('an all-gap alignment reports an error without creating an empty sequence', async ({page}) => {
    const empty: ProjectDto = {
        ...workspace, sequences: [], entryOrder: ['gaps'], alignments: [
            {
                id: 'gaps', name: 'Gap alignment', kind: 'DNA', score: 0, marks: [], consensus: '   ',
                sequences: [{name: 'First', value: '---'}, {name: 'Second', value: '---'}]
            }
        ]
    };
    await page.addInitScript(value => localStorage.setItem('genewarrior.workspace.v1', JSON.stringify(value)), empty);
    await page.goto('/');
    await page.locator('#entry-gaps .entry-card-header').click();
    await page.getByRole('button', {name: 'Analyze Alignment', exact: false}).click();
    await page.getByRole('button', {name: 'Consensus sequence', exact: true}).click();
    await expect(page.locator('.workspace .error')).toContainText('No consensus residues remain');
    await expect(page.locator('.entry-card')).toHaveCount(1);
    await expect(page.getByRole('button', {name: 'Consensus sequence', exact: true})).toBeEnabled();
});
