import { expect, test } from '@playwright/test';

/**
 * Dashboard ledger navigation. Seeds a transaction via the modal, navigates
 * to the dashboard, then clicks through to the detail page.
 *
 * Works in both modes:
 *  • NEXT_PUBLIC_API_MOCKING=enabled — POST returns a fixture and the ledger
 *    returns fixture rows that include it.
 *  • real stack — DB volume is wiped before each run by the e2e compose
 *    wrapper. We pin the locator to the just-created transaction's UUID
 *    (captured from the URL after POST) so the assertion stays deterministic
 *    across retries.
 */
test('lists transactions and navigates to a detail row', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('button', { name: /new transaction/i }).click();
  await expect(page.getByRole('dialog', { name: /new transaction/i })).toBeVisible();
  await page.getByLabel(/description/i).fill('Office supplies');
  await page.getByLabel(/transaction date/i).fill('2024-01-15');
  await page.getByLabel(/amount/i).fill('49.99');
  await page.getByRole('button', { name: /create transaction/i }).click();
  await expect(page).toHaveURL(/\/transactions\/[0-9a-f-]{36}$/);

  const id = new URL(page.url()).pathname.split('/').pop() ?? '';
  expect(id).toMatch(/^[0-9a-f-]{36}$/);

  await page.goto('/');
  const row = page.locator(`a[href="/transactions/${id}"]`);
  await expect(row).toBeVisible();
  await expect(row).toHaveText('Office supplies');
  await row.click();
  await expect(page).toHaveURL(`/transactions/${id}`);
});
