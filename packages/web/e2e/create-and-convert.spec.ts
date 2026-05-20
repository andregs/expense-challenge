import { expect, test } from '@playwright/test';

/**
 * Golden-path flow: open the new-transaction modal from the dashboard,
 * submit it, land on the detail page, then convert the amount to BRL.
 *
 * Works in both modes:
 *  • NEXT_PUBLIC_API_MOCKING=enabled — all API calls handled by MSW; data
 *    comes from fixtures, conversion amount is the fixture value.
 *  • real stack — calls hit the live backend; the transaction date (2024-01-15)
 *    falls within a published Treasury quarter, so a real BRL rate is returned.
 *
 * Assertions use patterns rather than hardcoded fixture values so the same
 * test validates both modes without branching.
 */
test('creates a transaction and converts it to BRL', async ({ page }) => {
  await page.goto('/');

  await page.getByRole('button', { name: /new transaction/i }).click();
  await expect(page.getByRole('dialog', { name: /new transaction/i })).toBeVisible();

  await page.getByLabel(/description/i).fill('Hotel stay');
  await page.getByLabel(/transaction date/i).fill('2024-01-15');
  await page.getByLabel(/amount/i).fill('49.99');

  await page.getByRole('button', { name: /create transaction/i }).click();

  // UUID differs between MSW fixture and real backend — match any UUID
  await expect(page).toHaveURL(/\/transactions\/[0-9a-f-]{36}$/);

  // Type a currency code into the searchable input to trigger the conversion request
  await page.getByLabel(/convert to/i).fill('BRL');

  // Amount is fixture-specific in MSW mode and real-rate-based in real mode;
  // assert that some valid converted amount is displayed rather than a fixed value
  await expect(page.getByText(/\d+\.\d{2} BRL/)).toBeVisible();
});
