import { formatIsoDate } from '@/lib/format';
import type { ConvertedTransaction } from '@/lib/queries/transactions';
import styles from './TransactionDetail.module.scss';

interface ConversionResultProps {
  data: ConvertedTransaction;
}

export function ConversionResult({ data }: ConversionResultProps) {
  return (
    <dl className={styles.conversionResult}>
      <div className={styles.conversionRow}>
        <dt>Exchange rate</dt>
        <dd>
          {data.exchangeRate} {data.currency}/USD
        </dd>
      </div>
      <div className={styles.conversionRow}>
        <dt>Rate date</dt>
        <dd>{formatIsoDate(data.rateDate)}</dd>
      </div>
      <div className={styles.conversionRow}>
        <dt>Converted amount</dt>
        <dd className={styles.convertedAmount}>
          {data.convertedAmount} {data.currency}
        </dd>
      </div>
    </dl>
  );
}
