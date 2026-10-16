import { useFocusEffect } from '@react-navigation/native';
import React, { useCallback, useState } from 'react';
import { Linking } from 'react-native';
import { Button, Card, ErrorBanner, H2, InfoBanner, Loading, Muted, Screen } from '../../components/ui';
import { messageOf, useAction } from '../../hooks';
import { useLocation } from '../../state/LocationContext';
import { api } from '../../state/runtime';
import type { BulkJobFeedItem } from '../../types';
import { prettyCode, splitCsv } from '../../utils/catalog';

const MAX_CODES = 10; // one feed request per service the worker offers

export function BulkFeedScreen() {
  const { resolve } = useLocation();
  const [items, setItems] = useState<BulkJobFeedItem[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [applied, setApplied] = useState<Set<number>>(new Set());

  const load = useCallback(async () => {
    try {
      const [profile, { lat, lon }] = await Promise.all([api.labour.me(), resolve()]);
      const codes = splitCsv(profile.serviceCodesCsv).slice(0, MAX_CODES);
      if (codes.length === 0) { setItems([]); setError('Choose your services in Profile first.'); return; }
      // the feed endpoint matches one service code at a time, so ask once per service and merge
      const feeds = await Promise.all(codes.map((c) => api.bulk.feed(c, lat, lon, 10)));
      const byId = new Map<number, BulkJobFeedItem>();
      feeds.forEach((f) => f.jobs.forEach((j) => byId.set(j.id, j)));
      setItems([...byId.values()].sort((a, b) => b.createdAt.localeCompare(a.createdAt)));
      setError(null);
    } catch (e) { setError(messageOf(e)); setItems((p) => p ?? []); }
  }, [resolve]);
  useFocusEffect(useCallback(() => { load(); }, [load]));

  const interested = useAction(async (id: number) => { await api.bulk.expressInterest(id); setApplied((p) => new Set(p).add(id)); });

  return (
    <Screen>
      <ErrorBanner message={error ?? interested.error} />
      <Muted>Group jobs from contractors near the location set on the Home tab.</Muted>
      {!items ? <Loading /> : items.length === 0 ? <Muted>No bulk requirements nearby right now.</Muted> : items.map((j) => (
        <Card key={j.id}>
          <H2>{j.workersNeeded} × {prettyCode(j.requiredLabourType)}</H2>
          {j.description ? <Muted>{j.description}</Muted> : null}
          {j.siteAddressText ? <Muted>📍 {j.siteAddressText}</Muted> : null}
          {j.externalFormLink
            ? <Button title="Apply on contractor's form" onPress={() => Linking.openURL(j.externalFormLink!).catch((e) => setError(messageOf(e)))} />
            : applied.has(j.id)
              ? <InfoBanner tone="ok" message="Interest sent to the contractor." />
              : <Button title="I'm interested" loading={interested.loading} onPress={() => interested.run(j.id)} />}
        </Card>
      ))}
      <Button title="Refresh" variant="ghost" onPress={load} />
    </Screen>
  );
}
