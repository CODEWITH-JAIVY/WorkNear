export function normalizeBaseUrl(raw: string): string {
  return raw.trim().replace(/\/+$/, '');
}

// http(s) gateway URL -> ws(s) URL for the same host
export function toWsUrl(httpBase: string): string {
  return normalizeBaseUrl(httpBase).replace(/^http/i, 'ws');
}

export function isValidBaseUrl(raw: string): boolean {
  return /^https?:\/\/[^\s/:]+(:\d{1,5})?(\/.*)?$/i.test(normalizeBaseUrl(raw));
}
