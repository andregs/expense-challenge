import { useState } from 'react';
import { CURRENCIES } from '@/lib/currencies';
import styles from './TransactionDetail.module.scss';

const VALID_CODES = new Set(CURRENCIES.map((c) => c.code));

interface CurrencySelectProps {
  value: string;
  onChange: (code: string) => void;
  disabled?: boolean;
}

export function CurrencySelect({ value, onChange, disabled }: CurrencySelectProps) {
  // Local draft lets the user type freely; only valid codes are propagated upward.
  const [draft, setDraft] = useState(value);

  function handleChange(raw: string) {
    const upper = raw.toUpperCase();
    setDraft(upper);
    // Propagate empty (clear) or a recognised ISO 4217 code only.
    if (upper === '' || VALID_CODES.has(upper)) {
      onChange(upper);
    }
  }

  return (
    <div className={styles.currencyRow}>
      <label htmlFor="currency-select" className={styles.currencyLabel}>
        Convert to
      </label>
      <input
        id="currency-select"
        list="currency-list"
        className={styles.currencySelect}
        value={draft}
        disabled={disabled}
        placeholder="e.g. EUR, JPY, BRL"
        autoComplete="off"
        onChange={(e) => {
          handleChange(e.target.value);
        }}
      />
      <datalist id="currency-list">
        {CURRENCIES.map((c) => (
          <option key={c.code} value={c.code}>
            {c.code} – {c.label}
          </option>
        ))}
      </datalist>
    </div>
  );
}
