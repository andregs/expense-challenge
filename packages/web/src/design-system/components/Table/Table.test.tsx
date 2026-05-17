import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Table } from './Table';

describe('Table', () => {
  it('renders a visually-hidden caption when provided', () => {
    render(
      <Table caption="Transactions">
        <tbody>
          <tr>
            <td>x</td>
          </tr>
        </tbody>
      </Table>,
    );
    expect(screen.getByText('Transactions')).toBeInTheDocument();
  });

  it('omits the caption when not provided', () => {
    render(
      <Table>
        <tbody>
          <tr>
            <td>x</td>
          </tr>
        </tbody>
      </Table>,
    );
    expect(screen.queryByText('Transactions')).not.toBeInTheDocument();
  });
});
