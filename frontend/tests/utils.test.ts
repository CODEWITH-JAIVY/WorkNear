import { test } from 'node:test';
import assert from 'node:assert/strict';
import { toWsUrl, normalizeBaseUrl, isValidBaseUrl } from '../src/utils/urls';
import { SCHEDULE_OPTIONS, parseManualSchedule, toLocalDateTimeString } from '../src/utils/schedule';
import { categoryOfCode, prettyCode, isGenderRelevant, splitCsv } from '../src/utils/catalog';
import { nextBackoffMs } from '../src/utils/backoff';

test('toWsUrl maps http->ws and https->wss and trims trailing slashes', () => {
  assert.equal(toWsUrl('http://10.0.2.2:8080/'), 'ws://10.0.2.2:8080');
  assert.equal(toWsUrl('https://api.worknear.in'), 'wss://api.worknear.in');
  assert.equal(normalizeBaseUrl('  http://a:1///'), 'http://a:1');
});

test('isValidBaseUrl accepts hosts/IPs with ports and rejects junk', () => {
  for (const ok of ['http://192.168.1.10:8080', 'https://api.worknear.in', 'http://localhost:8080/'])
    assert.ok(isValidBaseUrl(ok), ok);
  for (const bad of ['192.168.1.10:8080', 'ftp://x', 'http://', 'http://a b', ''])
    assert.ok(!isValidBaseUrl(bad), bad);
});

test('schedule strings are LocalDateTime-shaped: no zone suffix', () => {
  const s = toLocalDateTimeString(new Date(2026, 9, 12, 9, 5));
  assert.equal(s, '2026-10-12T09:05:00');
  assert.ok(!/[Zz+]/.test(s));
});

test('schedule options: ASAP is null, +2h and tomorrow-10:00 are computed from now', () => {
  const now = new Date(2026, 9, 12, 21, 30);
  const by = Object.fromEntries(SCHEDULE_OPTIONS.map((o) => [o.key, o.value(now)]));
  assert.equal(by.asap, null);
  assert.equal(by['2h'], '2026-10-12T23:30:00');
  assert.equal(by.tom10, '2026-10-13T10:00:00');
});

test('tomorrow rolls over month ends correctly', () => {
  const tom = SCHEDULE_OPTIONS.find((o) => o.key === 'tom10')!;
  assert.equal(tom.value(new Date(2026, 0, 31, 8, 0)), '2026-02-01T10:00:00');
});

test('manual schedule parsing accepts valid input and rejects impossible dates', () => {
  assert.equal(parseManualSchedule('2026-10-12 10:00'), '2026-10-12T10:00:00');
  assert.equal(parseManualSchedule('2026-10-12T10:00'), '2026-10-12T10:00:00');
  assert.equal(parseManualSchedule('2026-02-30 10:00'), null);
  assert.equal(parseManualSchedule('2026-10-12 25:00'), null);
  assert.equal(parseManualSchedule('tomorrow'), null);
});

test('catalog code helpers', () => {
  assert.equal(categoryOfCode('HOME_REPAIR__ELECTRICIAN'), 'HOME_REPAIR');
  assert.equal(categoryOfCode('NOPREFIX'), 'NOPREFIX');
  assert.equal(prettyCode('HOME_REPAIR__TAP_REPAIR'), 'Tap Repair');
  assert.equal(prettyCode('CLEANING__MOVE_IN_CLEANING'), 'Move In Cleaning');
  assert.deepEqual(splitCsv(' A, B ,,C '), ['A', 'B', 'C']);
  assert.deepEqual(splitCsv(null), []);
});

test('gender option is offered only for in-home / personal-care categories', () => {
  for (const c of ['CLEANING', 'DOMESTIC', 'COOKING_KITCHEN', 'CARE_WELLNESS', 'BEAUTY_PERSONAL']) assert.ok(isGenderRelevant(c), c);
  for (const c of ['HOME_REPAIR', 'CONSTRUCTION', 'LOADING_TRANSPORT', 'SKILLED_TRADES']) assert.ok(!isGenderRelevant(c), c);
});

test('reconnect backoff doubles and is capped at 15s', () => {
  assert.deepEqual([0, 1, 2, 3, 4, 5, 10].map(nextBackoffMs), [1000, 2000, 4000, 8000, 15000, 15000, 15000]);
});
