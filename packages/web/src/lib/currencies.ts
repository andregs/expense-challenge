export interface Currency {
  code: string;
  label: string;
}

/**
 * Static subset of Treasury-supported currencies. Mirrored by
 * `currencies.json` on the backend which maps these codes to the
 * `country_currency_desc` field required by the FiscalData API.
 */
export const CURRENCIES: readonly Currency[] = [
  { code: 'AUD', label: 'Australian Dollar' },
  { code: 'BRL', label: 'Brazilian Real' },
  { code: 'CAD', label: 'Canadian Dollar' },
  { code: 'CNY', label: 'Chinese Renminbi' },
  { code: 'EUR', label: 'Euro' },
  { code: 'GBP', label: 'British Pound' },
  { code: 'INR', label: 'Indian Rupee' },
  { code: 'JPY', label: 'Japanese Yen' },
  { code: 'MXN', label: 'Mexican Peso' },
  { code: 'CHF', label: 'Swiss Franc' },
];
