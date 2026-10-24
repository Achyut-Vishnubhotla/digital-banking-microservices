export interface Account {
  id: number;
  accountNumber: string;
  ownerUsername: string;
  balance: number;
  createdAt: string;
}

export interface Transfer {
  id: number;
  fromAccountId: number;
  toAccountId: number;
  amount: number;
  status: string;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  username: string;
  role: string;
}
