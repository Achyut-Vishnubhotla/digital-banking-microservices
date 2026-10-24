'use client';

import { useEffect, useRef } from 'react';
import { useRouter } from 'next/navigation';
import { CircularProgress, Container, Typography } from '@mui/material';
import * as api from '@/lib/api';

/** Landing page after GitHub login: auth-service redirects here with our JWTs in the URL fragment. */
export default function OAuthCallbackPage() {
  const router = useRouter();
  const handled = useRef(false); // React strict mode runs effects twice in dev

  useEffect(() => {
    if (handled.current) return;
    handled.current = true;

    const params = new URLSearchParams(window.location.hash.replace(/^#/, ''));
    const accessToken = params.get('accessToken');
    const refreshToken = params.get('refreshToken');
    const username = params.get('username');

    // Remove the tokens from the address bar and browser history straight away.
    window.history.replaceState(null, '', '/auth/callback');

    if (accessToken && refreshToken && username) {
      api.session.save({ accessToken, refreshToken, username, role: 'USER' });
      router.replace('/dashboard');
    } else {
      router.replace('/login?error=1');
    }
  }, [router]);

  return (
    <Container maxWidth="xs" sx={{ mt: 10, textAlign: 'center' }}>
      <CircularProgress />
      <Typography sx={{ mt: 2 }}>Signing you in...</Typography>
    </Container>
  );
}
