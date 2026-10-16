// The backend's scheduledAt is a Java LocalDateTime: ISO date-time with NO zone suffix.
// (A trailing "Z" or offset makes Jackson reject it for a LocalDateTime field.)

const pad = (n: number) => String(n).padStart(2, '0');

export function toLocalDateTimeString(d: Date): string {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:00`;
}

export interface ScheduleOption { key: string; label: string; value: (now: Date) => string | null }

export const SCHEDULE_OPTIONS: ScheduleOption[] = [
  { key: 'asap', label: 'As soon as possible', value: () => null },
  { key: '2h', label: 'In 2 hours', value: (now) => toLocalDateTimeString(new Date(now.getTime() + 2 * 3600_000)) },
  {
    key: 'tom10',
    label: 'Tomorrow 10:00',
    value: (now) => {
      const d = new Date(now);
      d.setDate(d.getDate() + 1);
      d.setHours(10, 0, 0, 0);
      return toLocalDateTimeString(d);
    },
  },
];

// Accepts "YYYY-MM-DD HH:mm" (or with a T); returns the LocalDateTime string, or null if invalid.
export function parseManualSchedule(input: string): string | null {
  const m = /^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2})$/.exec(input.trim());
  if (!m) return null;
  const [y, mo, d, h, mi] = m.slice(1).map(Number) as [number, number, number, number, number];
  const date = new Date(y, mo - 1, d, h, mi);
  const valid =
    date.getFullYear() === y && date.getMonth() === mo - 1 && date.getDate() === d &&
    date.getHours() === h && date.getMinutes() === mi;
  return valid ? toLocalDateTimeString(date) : null;
}
