import { useFocusEffect } from '@react-navigation/native';
import React, { useCallback, useState } from 'react';
import { Button, Card, ErrorBanner, H2, Loading, Muted, Screen } from '../../components/ui';
import { messageOf, useAction } from '../../hooks';
import { useAuth } from '../../state/AuthContext';
import { api } from '../../state/runtime';
import type { KycDocument } from '../../types';

export function KycReviewScreen() {
  const { logout } = useAuth();
  const [docs, setDocs] = useState<KycDocument[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const load = useCallback(() => { api.kyc.pending().then((d) => { setDocs(d); setError(null); }).catch((e) => setError(messageOf(e))); }, []);
  useFocusEffect(load);

  const review = useAction(async (id: number, approved: boolean) => {
    await api.kyc.review(id, approved, approved ? undefined : 'Document not clear — please upload again');
    load();
  });

  return (
    <Screen>
      <ErrorBanner message={error ?? review.error} />
      <Muted>A worker becomes matchable once their Aadhaar, PAN and selfie are all approved.</Muted>
      {!docs ? <Loading /> : docs.length === 0 ? <Muted>No documents waiting for review.</Muted> : docs.map((d) => (
        <Card key={d.id}>
          <H2>{d.docType} — worker #{d.labourId}</H2>
          <Muted numberOfLines={1}>{d.documentUrl}</Muted>
          <Button title="Approve" loading={review.loading} onPress={() => review.run(d.id, true)} />
          <Button title="Reject" variant="danger" onPress={() => review.run(d.id, false)} />
        </Card>
      ))}
      <Button title="Refresh" variant="ghost" onPress={load} />
      <Button title="Log out" variant="secondary" onPress={() => logout()} />
    </Screen>
  );
}
