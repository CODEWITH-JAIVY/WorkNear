// Reconnect delay for the notification socket: 1s, 2s, 4s, ... capped at 15s.
export function nextBackoffMs(attempt: number): number {
  return Math.min(1000 * 2 ** Math.max(0, attempt), 15_000);
}
