/** @type {import('next').NextConfig} */
const API_URL = process.env.API_URL || 'http://localhost:8080';

module.exports = {
  output: 'standalone',
  // The browser only ever talks to the Next.js server (same origin, so no CORS needed);
  // Next forwards /api/* to the API gateway.
  async rewrites() {
    return [
      { source: '/api/:path*', destination: `${API_URL}/api/:path*` },
      // GitHub OAuth2: start of the flow and GitHub's redirect back to us (handled by auth-service)
      { source: '/oauth2/authorization/:path*', destination: `${API_URL}/oauth2/authorization/:path*` },
      { source: '/login/oauth2/code/:path*', destination: `${API_URL}/login/oauth2/code/:path*` },
    ];
  },
};
