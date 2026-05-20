import { expect, test } from '@playwright/test';

/**
 * Dashboard ledger navigation tests. Both work in MSW and real-stack modes:
 *  • The "lists transactions" test seeds data via the modal, navigates to the
 *    dashboard, then clicks through to the detail page.
 *  • The "new transaction modal" test asserts the button opens the form
 *    with no data dependency.
 */
test.describe('Dashboard ledger', () => {
  test('lists transactions and navigates to a detail row', async ({ page }) => {
    // Seed a transaction so the ledger is non-empty in real-stack mode.
    // In MSW mode the POST returns a fixture and the ledger returns fixture rows.
    await page.goto('/');
    await page.getByRole('button', { name: /new transaction/i }).click();
    await expect(page.getByRole('dialog', { name: /new transaction/i })).toBeVisible();
    await page.getByLabel(/description/i).fill('Office supplies');
    await page.getByLabel(/transaction date/i).fill('2024-01-15');
    await page.getByLabel(/amount/i).fill('49.99');
    await page.getByRole('button', { name: /create transaction/i }).click();
    await expect(page).toHaveURL(/\/transactions\/[0-9a-f-]{36}$/);

    await page.goto('/');
    await expect(page.getByText('Office supplies')).toBeVisible();

    await page.getByRole('link', { name: 'Office supplies' }).click();
    await expect(page).toHaveURL(/\/transactions\/[0-9a-f-]{36}$/);
  });

  test('clicking New transaction button opens the form modal', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: /new transaction/i }).click();
    await expect(page.getByRole('dialog', { name: /new transaction/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /create transaction/i })).toBeVisible();
  });
});
