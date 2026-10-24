'use client';

import { useCallback, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import {
  Alert, AppBar, Box, Button, Card, CardActionArea, CardContent, Container, Grid, Paper,
  Table, TableBody, TableCell, TableHead, TablePagination, TableRow, TextField, Toolbar, Typography,
} from '@mui/material';
import * as api from '@/lib/api';
import { Account, PageResponse, Transfer } from '@/types';
import TransferForm from '@/components/TransferForm';

const money = (n: number) => n.toLocaleString(undefined, { style: 'currency', currency: 'USD' });

export default function DashboardPage() {
  const router = useRouter();
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [history, setHistory] = useState<PageResponse<Transfer> | null>(null);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(5);
  const [deposit, setDeposit] = useState('100');
  const [error, setError] = useState<string | null>(null);

  const fail = useCallback((err: unknown) => {
    if (err instanceof api.ApiError && err.status === 401) {
      api.session.clear();
      router.replace('/login');
      return;
    }
    setError(err instanceof Error ? err.message : 'Something went wrong');
  }, [router]);

  const loadAccounts = useCallback(async () => {
    try {
      const list = await api.listAccounts();
      setAccounts(list);
      setSelectedId((prev) => prev ?? list[0]?.id ?? null);
    } catch (err) {
      fail(err);
    }
  }, [fail]);

  const loadHistory = useCallback(async () => {
    if (selectedId == null) return;
    try {
      setHistory(await api.getHistory(selectedId, page, rowsPerPage));
    } catch (err) {
      fail(err);
    }
  }, [selectedId, page, rowsPerPage, fail]);

  useEffect(() => {
    if (!api.session.isLoggedIn()) {
      router.replace('/login');
      return;
    }
    loadAccounts();
  }, [router, loadAccounts]);

  useEffect(() => {
    loadHistory();
  }, [loadHistory]);

  async function openAccount() {
    setError(null);
    try {
      const created = await api.createAccount(Number(deposit) || 0);
      setSelectedId(created.id);
      await loadAccounts();
    } catch (err) {
      fail(err);
    }
  }

  async function handleTransfer(toAccountNumber: string, amount: number, key: string) {
    if (selectedId == null) return;
    await api.transfer(selectedId, toAccountNumber, amount, key);
    await loadAccounts();
    setPage(0);
    await loadHistory();
  }

  function logout() {
    api.session.clear();
    router.replace('/login');
  }

  const selected = accounts.find((a) => a.id === selectedId) ?? null;

  return (
    <>
      <AppBar position="static">
        <Toolbar>
          <Typography variant="h6" sx={{ flexGrow: 1 }}>Digital Banking</Typography>
          <Typography sx={{ mr: 2 }}>{api.session.username()}</Typography>
          <Button color="inherit" onClick={logout}>Log out</Button>
        </Toolbar>
      </AppBar>

      <Container sx={{ mt: 4, mb: 6 }}>
        {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>{error}</Alert>}

        <Typography variant="h6" gutterBottom>Your accounts</Typography>
        <Grid container spacing={2} sx={{ mb: 2 }}>
          {accounts.map((a) => (
            <Grid item xs={12} md={4} key={a.id}>
              <Card variant="outlined" sx={{ borderColor: a.id === selectedId ? 'primary.main' : undefined, borderWidth: a.id === selectedId ? 2 : 1 }}>
                <CardActionArea onClick={() => { setSelectedId(a.id); setPage(0); }}>
                  <CardContent>
                    <Typography color="text.secondary">Account {a.accountNumber}</Typography>
                    <Typography variant="h5">{money(a.balance)}</Typography>
                  </CardContent>
                </CardActionArea>
              </Card>
            </Grid>
          ))}
          {accounts.length === 0 && (
            <Grid item xs={12}><Typography color="text.secondary">No accounts yet. Open one below.</Typography></Grid>
          )}
        </Grid>

        <Paper sx={{ p: 2, mb: 4, display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap' }}>
          <TextField size="small" label="Initial deposit (demo)" value={deposit} onChange={(e) => setDeposit(e.target.value)} />
          <Button variant="outlined" onClick={openAccount}>Open new account</Button>
        </Paper>

        {selected && (
          <Grid container spacing={4}>
            <Grid item xs={12} md={4}>
              <Paper sx={{ p: 3 }}>
                <TransferForm key={selected.id} fromAccountId={selected.id} onTransfer={handleTransfer} />
              </Paper>
            </Grid>
            <Grid item xs={12} md={8}>
              <Paper sx={{ p: 2 }}>
                <Typography variant="h6" gutterBottom>History for {selected.accountNumber}</Typography>
                <Box sx={{ overflowX: 'auto' }}>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Date</TableCell>
                        <TableCell>Type</TableCell>
                        <TableCell align="right">Amount</TableCell>
                        <TableCell>Status</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {history?.content.map((t) => {
                        const sent = t.fromAccountId === selected.id;
                        return (
                          <TableRow key={t.id}>
                            <TableCell>{new Date(t.createdAt).toLocaleString()}</TableCell>
                            <TableCell>{sent ? 'Sent' : 'Received'}</TableCell>
                            <TableCell align="right">{sent ? '-' : '+'}{money(t.amount)}</TableCell>
                            <TableCell>{t.status}</TableCell>
                          </TableRow>
                        );
                      })}
                      {history && history.content.length === 0 && (
                        <TableRow><TableCell colSpan={4}>No transfers yet</TableCell></TableRow>
                      )}
                    </TableBody>
                  </Table>
                </Box>
                <TablePagination
                  component="div"
                  count={history?.totalElements ?? 0}
                  page={page}
                  onPageChange={(_, p) => setPage(p)}
                  rowsPerPage={rowsPerPage}
                  rowsPerPageOptions={[5, 10, 25]}
                  onRowsPerPageChange={(e) => { setRowsPerPage(Number(e.target.value)); setPage(0); }}
                />
              </Paper>
            </Grid>
          </Grid>
        )}
      </Container>
    </>
  );
}
