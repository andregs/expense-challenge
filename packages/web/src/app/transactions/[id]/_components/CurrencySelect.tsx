import { CURRENCIES } from '@/lib/currencies';
import styles from './TransactionDetail.module.scss';

interface CurrencySelectProps {
  value: string;
  onChange: (code: string) => void;
  disabled?: boolean;
}

export function CurrencySelect({ value, onChange, disabled }: CurrencySelectProps) {
  return (
    <div className={styles.currencyRow}>
      <label htmlFor="currency-select" className={styles.currencyLabel}>
        Convert to
      </label>
      <select
        id="currency-select"
        className={styles.currencySelect}
        value={value}
        disabled={disabled}
        onChange={(e) => {
          onChange(e.target.value);
        }}
      >
        <option value="">Select currency…</option>
        {CURRENCIES.map((c) => (
          <option key={c.code} value={c.code}>
            {c.code} – {c.label}
          </option>
        ))}
      </select>
    </div>
  );
}
