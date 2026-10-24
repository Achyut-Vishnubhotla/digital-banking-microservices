import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import TransferForm from './TransferForm';

async function fill(to: string, amount: string) {
  await userEvent.type(screen.getByLabelText(/destination account/i), to);
  await userEvent.type(screen.getByLabelText(/amount/i), amount);
}

describe('TransferForm', () => {
  it('rejects an account number that is not 12 digits', async () => {
    const onTransfer = jest.fn();
    render(<TransferForm fromAccountId={1} onTransfer={onTransfer} />);

    await fill('123', '10');
    await userEvent.click(screen.getByRole('button', { name: /send/i }));

    expect(await screen.findByText(/12 digits/i)).toBeInTheDocument();
    expect(onTransfer).not.toHaveBeenCalled();
  });

  it('rejects a non-positive amount', async () => {
    const onTransfer = jest.fn();
    render(<TransferForm fromAccountId={1} onTransfer={onTransfer} />);

    await fill('123456789012', '0');
    await userEvent.click(screen.getByRole('button', { name: /send/i }));

    expect(await screen.findByText(/greater than 0/i)).toBeInTheDocument();
    expect(onTransfer).not.toHaveBeenCalled();
  });

  it('submits valid data together with an idempotency key', async () => {
    const onTransfer = jest.fn().mockResolvedValue(undefined);
    render(<TransferForm fromAccountId={1} onTransfer={onTransfer} />);

    await fill('123456789012', '25.5');
    await userEvent.click(screen.getByRole('button', { name: /send/i }));

    expect(await screen.findByText(/transfer completed/i)).toBeInTheDocument();
    expect(onTransfer).toHaveBeenCalledWith('123456789012', 25.5, expect.any(String));
  });

  it('reuses the same idempotency key when retrying after a failure', async () => {
    const onTransfer = jest.fn()
      .mockRejectedValueOnce(new Error('network down'))
      .mockResolvedValueOnce(undefined);
    render(<TransferForm fromAccountId={1} onTransfer={onTransfer} />);

    await fill('123456789012', '10');
    await userEvent.click(screen.getByRole('button', { name: /send/i }));
    expect(await screen.findByText(/network down/i)).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /send/i }));
    expect(await screen.findByText(/transfer completed/i)).toBeInTheDocument();

    expect(onTransfer).toHaveBeenCalledTimes(2);
    expect(onTransfer.mock.calls[0][2]).toBe(onTransfer.mock.calls[1][2]);
  });
});
