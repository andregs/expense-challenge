import { expect, test } from '@playwright/test';

/**
 * Golden-path flow: open the new-transaction form, submit it, land on
 * the detail page, then convert the amount to BRL. Every API call is
 * served by MSW so this exercises the full UI without a live backend.
 */
test('creates a transaction and converts it to BRL', async ({ page }) => {
  await page.goto('/transactions/new');

  await page.getByLabel(/description/i).fill('Office supplies');
  await page.getByLabel(/transaction date/i).fill('2026-05-10');
  await page.getByLabel(/amount/i).fill('49.99');

  await page.getByRole('button', { name: /create transaction/i }).click();

  // MSW redirects on success to /transactions/<singleTransaction.id>
  await expect(page).toHaveURL(/\/transactions\/11111111-1111-4111-8111-111111111111$/);
  await expect(page.getByRole('heading', { name: 'Office supplies' })).toBeVisible();

  // Pick a currency from the dropdown to trigger the conversion request
  await page.getByLabel(/convert to/i).selectOption('BRL');

  // sampleConverted fixture: 640.40 BRL, rate 5.123400 on 2026-03-31
  await expect(page.getByText('640.40 BRL')).toBeVisible();
  await expect(page.getByText('5.123400 BRL/USD')).toBeVisible();
});
