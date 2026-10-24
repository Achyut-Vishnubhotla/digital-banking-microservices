import '@testing-library/jest-dom';

// Make sure crypto.randomUUID exists (and returns a different value each call) in the test environment.
if (!globalThis.crypto?.randomUUID) {
  let counter = 0;
  Object.defineProperty(globalThis, 'crypto', {
    value: { ...(globalThis.crypto ?? {}), randomUUID: () => `test-key-${++counter}` },
    configurable: true,
  });
}
