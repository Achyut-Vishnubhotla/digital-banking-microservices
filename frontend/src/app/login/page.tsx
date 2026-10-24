'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Alert, Box, Button, Container, Divider, Paper, Tab, Tabs, TextField, Typography } from '@mui/material';
import * as api from '@/lib/api';

export default function LoginPage() {
  const router = useRouter();
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (api.session.isLoggedIn()) router.replace('/dashboard');
    if (new URLSearchParams(window.location.search).has('error')) setError('Sign-in failed. Please try again.');
  }, [router]);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      if (mode === 'login') await api.login(username, password);
      else await api.register(username, email, password);
      router.push('/dashboard');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Something went wrong');
    } finally {
      setBusy(false);
    }
  }

  return (
    <Container maxWidth="xs" sx={{ mt: 10 }}>
      <Paper sx={{ p: 4 }}>
        <Typography variant="h5" gutterBottom>Digital Banking</Typography>
        <Tabs value={mode} onChange={(_, v) => setMode(v)} sx={{ mb: 2 }}>
          <Tab label="Log in" value="login" />
          <Tab label="Register" value="register" />
        </Tabs>
        <Box component="form" onSubmit={submit} sx={{ display: 'grid', gap: 2 }}>
          <TextField label="Username" value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" />
          {mode === 'register' && (
            <TextField label="Email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" />
          )}
          <TextField
            label="Password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            helperText={mode === 'register' ? 'At least 8 characters' : undefined}
            autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
          />
          {error && <Alert severity="error">{error}</Alert>}
          <Button type="submit" variant="contained" disabled={busy}>
            {mode === 'login' ? 'Log in' : 'Create account'}
          </Button>
        </Box>
        <Divider sx={{ my: 2 }}>or</Divider>
        <Button fullWidth variant="outlined" onClick={() => { window.location.href = '/oauth2/authorization/github'; }}>
          Continue with GitHub
        </Button>
      </Paper>
    </Container>
  );
}
