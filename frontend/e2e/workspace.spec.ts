import {test, expect} from '@playwright/test';

test('complete workspace survives refresh and sharing with an independent shared draft', async ({page, browser}) => {
    await page.goto('/');
    await expect(page.locator('.entry-card')).toHaveCount(4);
    await page.locator('.project-title button').dblclick();
    await page.getByRole('textbox', {name: 'Project name'}).fill('Persistence browser test');
    await page.getByRole('textbox', {name: 'Project name'}).press('Enter');

    await page.locator('#entry-example-ecoli .entry-card-header').click();
    await page.locator('#entry-example-lactis .entry-card-header').click();
    await page.getByRole('button', {name: 'Create Alignment', exact: false}).click();
    await page.getByRole('button', {name: 'Full sequence', exact: true}).click();
    await expect(page.locator('.alignment-card')).toHaveCount(2);
    const card = page.locator('.alignment-card').filter({hasText: 'Pairwise Full Sequence Alignment'});
    const start = await card.locator('[data-row="0"][data-position="2"]').boundingBox();
    const end = await card.locator('[data-row="0"][data-position="9"]').boundingBox();
    expect(start).not.toBeNull();
    expect(end).not.toBeNull();
    await page.mouse.move(start!.x + 2, start!.y + 5);
    await page.mouse.down();
    await page.mouse.move(end!.x + 3, end!.y + 5, {steps: 6});
    await page.mouse.up();
    await page.getByPlaceholder('Annotation label').fill('Shared domain');
    await page.getByRole('button', {name: 'Add annotation', exact: true}).click();
    await expect(card.locator('.annotation-list')).toContainText('Shared domain');
    await expect(card.locator('.alignment-annotation').first()).toBeVisible();

    const navigator = page.locator('.navigator-panel nav');
    await navigator.getByRole('button', {name: /LacZ protein alignment/})
        .dragTo(navigator.getByRole('button', {name: /lacZ beta-galactosidase, E. coli/}), {
            targetPosition: {
                x: 20,
                y: 2
            }
        });
    const snapshot = await page.evaluate(() => JSON.parse(localStorage.getItem('genewarrior.workspace.v1')!));
    expect(snapshot.entryOrder[0]).toBe('example-alignment');
    expect(snapshot.alignments).toHaveLength(2);
    await page.reload();
    await expect(page.locator('.project-title')).toContainText(snapshot.name);
    await expect(page.locator('.entry-card')).toHaveCount(5);
    await expect(page.locator('.entry-card').first()).toHaveAttribute('id', 'entry-example-alignment');
    await expect(card.locator('.annotation-list')).toContainText('Shared domain');
    await page.screenshot({path: 'test-results/workspace-desktop.png', fullPage: true});

    await page.locator('.app-header').getByRole('button', {name: 'Share project', exact: true}).click();
    const link = page.getByRole('textbox', {name: /Shared project/});
    await expect(link).toBeVisible();
    const url = await link.inputValue();
    expect(new URL(url).searchParams.get('project')).toBeTruthy();

    const recipient = await browser.newContext({viewport: {width: 1440, height: 1000}});
    const recipientPage = await recipient.newPage();
    await recipientPage.goto(url);
    await expect(recipientPage.locator('.entry-card')).toHaveCount(5);
    await expect(recipientPage.locator('.entry-card').first()).toHaveAttribute('id', 'entry-example-alignment');
    await expect(recipientPage.locator('.annotation-list').filter({hasText: 'Shared domain'})).toBeVisible();
    await expect(recipientPage.locator('.annotation-list').filter({hasText: 'beta-galactosidase CDS'})).toBeVisible();
    await recipientPage.locator('.project-title button').dblclick();
    await recipientPage.getByRole('textbox', {name: 'Project name'}).fill('Recipient draft');
    await recipientPage.getByRole('textbox', {name: 'Project name'}).press('Enter');
    await recipientPage.reload();
    await expect(recipientPage.locator('.project-title')).toContainText('Recipient draft');
    await recipientPage.setViewportSize({width: 390, height: 844});
    await expect(recipientPage.locator('.entry-card')).toHaveCount(5);
    await recipientPage.screenshot({path: 'test-results/workspace-mobile.png', fullPage: true});
    await recipient.close();

    await page.reload();
    await expect(page.locator('.project-title')).toContainText('Persistence browser test');
    const response = await page.request.get('/api/projects/' + new URL(url).searchParams.get('project'));
    expect(response.ok()).toBe(true);
    const saved = await response.json();
    expect(saved.sequences).toEqual(snapshot.sequences.map((sequence: any) => ({
        ...sequence,
        marks: sequence.marks.map((mark: any) => ({annotation: null, direction: null, type: null, ...mark}))
    })));
    expect(saved.entryOrder).toEqual(snapshot.entryOrder);
    expect(saved.alignments).toEqual(snapshot.alignments);
    expect(saved.name).toBe('Persistence browser test');
});

test('a missing shared project does not replace the local draft', async ({page}) => {
    await page.goto('/');
    await page.locator('.project-title button').dblclick();
    await page.getByRole('textbox', {name: 'Project name'}).fill('Keep my workspace');
    await page.getByRole('textbox', {name: 'Project name'}).press('Enter');
    await page.goto('/?project=missing-project');
    await expect(page.getByText('Shared project could not be opened.', {exact: false})).toBeVisible();
    await expect(page.locator('.entry-card')).toHaveCount(0);
    await page.getByRole('link', {name: 'Open local workspace'}).click();
    await expect(page.locator('.project-title')).toContainText('Keep my workspace');
});
