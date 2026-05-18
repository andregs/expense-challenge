import { expect, test } from '@playwright/test';

test.describe('Ledger', () => {
  test('lists fixture transactions and navigates to a detail row', async ({ page }) => {
    await page.goto('/ledger');

    // samplePage fixture has three rows
    await expect(page.getByText('Office supplies')).toBeVisible();
    await expect(page.getByText('Conference ticket')).toBeVisible();
    await expect(page.getByText('Team lunch')).toBeVisible();

    // Clicking the description link navigates to /transactions/<id>
    await page.getByRole('link', { name: 'Office supplies' }).click();
    await expect(page).toHaveURL(/\/transactions\/11111111-1111-4111-8111-111111111111$/);
  });

  test('exposes the New transaction shortcut from the dashboard', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('link', { name: /new transaction/i }).first().click();
    await expect(page).toHaveURL(/\/transactions\/new$/);
    await expect(page.getByRole('button', { name: /create transaction/i })).toBeVisible();
  });
});
