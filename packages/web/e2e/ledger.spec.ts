import { expect, test } from '@playwright/test';

/**
 * Ledger navigation tests. Both work in MSW and real-stack modes:
 *  • The "lists transactions" test creates its own data first so it is
 *    self-contained in real mode (no prior seeding required). In MSW mode the
 *    same description ("Office supplies") already appears in the fixture page,
 *    so the assertion holds regardless of which backend responds.
 *  • The "new transaction shortcut" test is pure UI navigation with no data
 *    dependency.
 */
test.describe('Ledger', () => {
  test('lists transactions and navigates to a detail row', async ({ page }) => {
    // Seed a transaction so the ledger is non-empty in real-stack mode.
    // In MSW mode the POST returns a fixture and the ledger returns fixture rows.
    await page.goto('/transactions/new');
    await page.getByLabel(/description/i).fill('Office supplies');
    await page.getByLabel(/transaction date/i).fill('2024-01-15');
    await page.getByLabel(/amount/i).fill('49.99');
    await page.getByRole('button', { name: /create transaction/i }).click();
    await expect(page).toHaveURL(/\/transactions\/[0-9a-f-]{36}$/);

    await page.goto('/ledger');
    await expect(page.getByText('Office supplies')).toBeVisible();

    await page.getByRole('link', { name: 'Office supplies' }).click();
    await expect(page).toHaveURL(/\/transactions\/[0-9a-f-]{36}$/);
  });

  test('exposes the New transaction shortcut from the dashboard', async ({ page }) => {
    await page.goto('/');
    await page
      .getByRole('link', { name: /new transaction/i })
      .first()
      .click();
    await expect(page).toHaveURL(/\/transactions\/new$/);
    await expect(page.getByRole('button', { name: /create transaction/i })).toBeVisible();
  });
});
