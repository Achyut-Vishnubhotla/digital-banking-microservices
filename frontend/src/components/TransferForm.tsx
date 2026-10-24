'use client';

import { useRef, useState } from 'react';
import { Alert, Box, Button, TextField, Typography } from '@mui/material';

export interface TransferFormProps {
  fromAccountId: number;
  onTransfer: (toAccountNumber: string, amount: number, idempotencyKey: string) => Promise<void>;
}

export default function TransferForm({ fromAccountId, onTransfer }: TransferFormProps) {
  const [to, setTo] = useState('');
  const [amount, setAmount] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [busy, setBusy] = useState(false);

  // One key per "intent to pay". It is kept after a failure, so retrying (e.g. after a network
  // timeout) cannot move the money twice, and replaced only after a success.
  const keyRef = useRef<string>(crypto.randomUUID());

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSuccess(false);

    const value = Number(amount);
    if (!/^\d{12}$/.test(to)) {
      setError('Account number must be 12 digits');
      return;
    }
    if (!(value > 0)) {
      setError('Amount must be greater than 0');
      return;
    }

    setBusy(true);
    try {
      await onTransfer(to, value, keyRef.current);
      keyRef.current = crypto.randomUUID();
      setTo('');
      setAmount('');
      setSuccess(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Transfer failed');
    } finally {
      setBusy(false);
    }
  }

  return (
    <Box component="form" onSubmit={submit} data-from-account={fromAccountId} sx={{ display: 'grid', gap: 2 }}>
      <Typography variant="h6">Send money</Typography>
      <TextField label="Destination account number" value={to} onChange={(e) => setTo(e.target.value)} />
      <TextField label="Amount" value={amount} onChange={(e) => setAmount(e.target.value)} inputProps={{ inputMode: 'decimal' }} />
      {error && <Alert severity="error">{error}</Alert>}
      {success && <Alert severity="success">Transfer completed</Alert>}
      <Button type="submit" variant="contained" disabled={busy}>Send</Button>
    </Box>
  );
}
