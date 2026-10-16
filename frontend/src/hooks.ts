import { useCallback, useState } from 'react';

export const messageOf = (e: unknown): string => (e instanceof Error ? e.message : String(e));

// Wraps an async action with loading + error state so every button behaves the same way.
export function useAction<A extends unknown[]>(fn: (...args: A) => Promise<void>) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const run = useCallback(async (...args: A) => {
    setLoading(true);
    setError(null);
    try { await fn(...args); } catch (e) { setError(messageOf(e)); } finally { setLoading(false); }
  }, [fn]);
  return { run, loading, error, clearError: () => setError(null) };
}
