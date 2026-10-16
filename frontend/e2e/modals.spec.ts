import {test, expect} from '@playwright/test';

test('deletion requires confirmation and cancellation preserves entries and selection', async ({page}) => {
    await page.goto('/');
    const trigger = page.getByRole('button', {name: 'Delete selected entries', exact: true});
    await expect(trigger).toHaveCount(0);
    await page.locator('#entry-example-ecoli .entry-card-header').click();
    await page.locator('#entry-example-alignment .entry-card-header').click();
    const before = await page.evaluate(() => localStorage.getItem('genewarrior.workspace.v1'));
    const dialog = page.getByRole('dialog', {name: 'Delete selected entries'});
    await trigger.click();
    await expect(dialog).toContainText('Delete 2 selected entries');
    await expect(dialog.getByRole('button', {name: 'Cancel'})).toBeFocused();
    await expect(page.locator('.entry-card')).toHaveCount(4);
    await dialog.getByRole('button', {name: 'Cancel'}).click();
    await expect(dialog).toHaveCount(0);
    await expect(trigger).toBeFocused();
    expect(await page.evaluate(() => localStorage.getItem('genewarrior.workspace.v1'))).toBe(before);
    await expect(page.locator('.entry-card.selected')).toHaveCount(2);

    await trigger.click();
    await page.keyboard.press('Escape');
    await expect(dialog).toHaveCount(0);
    await expect(trigger).toBeFocused();
    await trigger.click();
    await page.mouse.click(5, 5);
    await expect(dialog).toHaveCount(0);
    await expect(page.locator('.entry-card')).toHaveCount(4);

    await trigger.click();
    await page.screenshot({path: 'test-results/delete-dialog-desktop.png'});
    await page.setViewportSize({width: 390, height: 844});
    await page.screenshot({path: 'test-results/delete-dialog-mobile.png'});
    expect(await dialog.evaluate(node => node.scrollWidth <= node.clientWidth)).toBe(true);
    await dialog.getByRole('button', {name: 'Delete entries', exact: true}).click();
    await expect(dialog).toHaveCount(0);
    await expect(page.locator('.entry-card')).toHaveCount(2);
    await expect(page.locator('#entry-example-ecoli')).toHaveCount(0);
    await expect(page.locator('#entry-example-alignment')).toHaveCount(0);
    await expect(trigger).toHaveCount(0);
    await page.reload();
    await expect(page.locator('.entry-card')).toHaveCount(2);
});

test('sharing shows a closable modal with a copyable link and keyboard focus containment', async ({page}) => {
    await page.addInitScript(() => {
        Object.defineProperty(navigator, 'clipboard', {
            configurable: true, value: {
                writeText: async (value: string) => {
                    sessionStorage.setItem('copied-link', value);
                }
            }
        });
    });
    await page.goto('/');
    const trigger = page.locator('.app-header').getByRole('button', {name: 'Share project'});
    const topBefore = await page.locator('.entry-card').first().evaluate(node => node.getBoundingClientRect().top);
    await trigger.click();
    const dialog = page.getByRole('dialog', {name: 'Share project'});
    const link = dialog.getByRole('textbox', {name: 'Shared project link'});
    await expect(dialog).toBeVisible();
    await expect(link).toBeFocused();
    const url = await link.inputValue();
    expect(new URL(url).searchParams.get('project')).toBeTruthy();
    await expect(link).toHaveAttribute('readonly', '');
    await page.keyboard.press('Shift+Tab');
    await expect(dialog.getByRole('button', {name: 'Close', exact: true})).toBeFocused();
    await dialog.getByRole('button', {name: 'Copy link'}).click();
    await expect(dialog.getByRole('status')).toHaveText('Link copied.');
    expect(await page.evaluate(() => sessionStorage.getItem('copied-link'))).toBe(url);
    await page.screenshot({path: 'test-results/share-dialog-desktop.png'});
    await dialog.getByRole('button', {name: 'Close', exact: true}).click();
    await expect(dialog).toHaveCount(0);
    await expect(page.locator('.workspace .share-result')).toHaveCount(0);
    await expect(trigger).toBeFocused();
    expect(await page.locator('.entry-card').first().evaluate(node => node.getBoundingClientRect().top)).toBe(topBefore);

    await page.getByRole('button', {name: 'Share & Export', exact: false}).click();
    const toolTrigger = page.locator('.tools-panel').getByRole('button', {name: 'Share project', exact: true});
    await toolTrigger.click();
    await expect(dialog).toBeVisible();
    await expect(dialog.getByRole('status')).toBeEmpty();
    await page.setViewportSize({width: 320, height: 700});
    await page.screenshot({path: 'test-results/share-dialog-mobile.png'});
    expect(await dialog.evaluate(node => node.scrollWidth <= node.clientWidth)).toBe(true);
    await page.evaluate(() => Object.defineProperty(navigator, 'clipboard', {
        value: {
            writeText: async () => {
                throw new Error('Denied');
            }
        }
    }));
    await dialog.getByRole('button', {name: 'Copy link'}).click();
    await expect(dialog.getByRole('status')).toHaveText('Clipboard unavailable.');
    await expect(link).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(dialog).toHaveCount(0);
    await expect(toolTrigger).toBeFocused();
});

test('failed sharing reports an error without opening a stale link', async ({page}) => {
    await page.goto('/');
    const trigger = page.locator('.app-header').getByRole('button', {name: 'Share project'});
    await trigger.click();
    await page.getByRole('dialog').getByRole('button', {name: 'Close', exact: true}).click();
    await page.route('**/api/projects', route => route.fulfill({
        status: 413, contentType: 'application/json',
        body: JSON.stringify({message: 'Project is too large'})
    }));
    await trigger.click();
    await expect(page.locator('.workspace .error')).toContainText('Project is too large');
    await expect(page.getByRole('dialog')).toHaveCount(0);
    await expect(trigger).toBeEnabled();
});
