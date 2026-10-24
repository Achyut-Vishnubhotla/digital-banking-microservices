import { Account, AuthResponse, PageResponse, Transfer } from '@/types';

const ACCESS_KEY = 'accessToken';
const REFRESH_KEY = 'refreshToken';
const USER_KEY = 'username';

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

// NOTE: localStorage is simple but readable by any XSS. A production app would keep tokens in
// httpOnly, Secure, SameSite cookies set by the server instead.
export const session = {
  isLoggedIn: () => typeof window !== 'undefined' && !!localStorage.getItem(ACCESS_KEY),
  username: () => (typeof window !== 'undefined' ? localStorage.getItem(USER_KEY) : null),
  save(auth: AuthResponse) {
    localStorage.setItem(ACCESS_KEY, auth.accessToken);
    localStorage.setItem(REFRESH_KEY, auth.refreshToken);
    localStorage.setItem(USER_KEY, auth.username);
  },
  clear() {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(USER_KEY);
  },
};

async function request<T>(path: string, init: RequestInit = {}, allowRefresh = true): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set('Content-Type', 'application/json');
  const token = localStorage.getItem(ACCESS_KEY);
  if (token) headers.set('Authorization', `Bearer ${token}`);

  const res = await fetch(`/api${path}`, { ...init, headers });

  // Access token expired: try the refresh token once, then replay the request.
  if (res.status === 401 && allowRefresh && (await tryRefresh())) {
    return request<T>(path, init, false);
  }
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new ApiError(res.status, body.message ?? `Request failed (${res.status})`);
  }
  return res.json() as Promise<T>;
}

async function tryRefresh(): Promise<boolean> {
  const refreshToken = localStorage.getItem(REFRESH_KEY);
  if (!refreshToken) return false;
  try {
    const res = await fetch('/api/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });
    if (!res.ok) return false;
    session.save((await res.json()) as AuthResponse);
    return true;
  } catch {
    return false;
  }
}

export async function login(username: string, password: string) {
  const auth = await request<AuthResponse>(
    '/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) }, false);
  session.save(auth);
}

export async function register(username: string, email: string, password: string) {
  const auth = await request<AuthResponse>(
    '/auth/register', { method: 'POST', body: JSON.stringify({ username, email, password }) }, false);
  session.save(auth);
}

export const listAccounts = () => request<Account[]>('/accounts');

export const createAccount = (initialDeposit: number) =>
  request<Account>('/accounts', { method: 'POST', body: JSON.stringify({ initialDeposit }) });

export const getHistory = (accountId: number, page: number, size: number) =>
  request<PageResponse<Transfer>>(`/accounts/${accountId}/transfers?page=${page}&size=${size}`);

export const transfer = (fromAccountId: number, toAccountNumber: string, amount: number, idempotencyKey: string) =>
  request<Transfer>('/transfers', {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
    body: JSON.stringify({ fromAccountId, toAccountNumber, amount }),
  });
