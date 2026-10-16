import * as ImagePicker from 'expo-image-picker';
import { useFocusEffect } from '@react-navigation/native';
import React, { useCallback, useState } from 'react';
import { Button, Card, ErrorBanner, H2, InfoBanner, Loading, Muted, Pill, Screen } from '../../components/ui';
import { messageOf, useAction } from '../../hooks';
import { api } from '../../state/runtime';
import type { DocType, KycDocument } from '../../types';

const DOCS: { type: DocType; label: string }[] = [
  { type: 'AADHAAR', label: 'Aadhaar card' }, { type: 'PAN', label: 'PAN card' }, { type: 'SELFIE', label: 'Selfie' },
];

export function KycScreen() {
  const [docs, setDocs] = useState<KycDocument[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const load = useCallback(() => { api.kyc.mine().then((d) => { setDocs(d); setError(null); }).catch((e) => setError(messageOf(e))); }, []);
  useFocusEffect(load);

  const upload = useAction(async (type: DocType) => {
    const perm = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!perm.granted) throw new Error('Photo library permission was denied.');
    const res = await ImagePicker.launchImageLibraryAsync({ mediaTypes: ['images'], quality: 0.7 });
    if (res.canceled || !res.assets[0]) return;
    const a = res.assets[0];
    const out = await api.media.uploadKycDoc({ uri: a.uri, name: a.fileName ?? `${type}.jpg`, type: a.mimeType ?? 'image/jpeg' });
    await api.kyc.submit(type, out.url);
    load();
  });

  // Test aid: lets you exercise the flow without S3 being configured. Not shown in release builds.
  const placeholders = useAction(async () => {
    for (const d of DOCS) if (!docs?.some((x) => x.docType === d.type)) await api.kyc.submit(d.type, `https://example.com/test-${d.type.toLowerCase()}.jpg`);
    load();
  });

  const statusOf = (t: DocType) => docs?.find((d) => d.docType === t);

  return (
    <Screen>
      <ErrorBanner message={error ?? upload.error ?? placeholders.error} />
      <Muted>All three documents must be approved before you can be matched to jobs.</Muted>
      {!docs ? <Loading /> : DOCS.map((d) => {
        const s = statusOf(d.type);
        return (
          <Card key={d.type}>
            <H2>{d.label}</H2>
            {s ? <Pill text={s.status} tone={s.status === 'VERIFIED' ? 'ok' : s.status === 'REJECTED' ? 'bad' : 'warn'} /> : <Pill text="Not submitted" tone="neutral" />}
            {s?.status === 'REJECTED' && s.reviewNotes ? <Muted style={{ marginTop: 6 }}>Reason: {s.reviewNotes}</Muted> : null}
            <Button title={s ? 'Upload again' : 'Upload photo'} variant="secondary" loading={upload.loading} onPress={() => upload.run(d.type)} />
          </Card>
        );
      })}
      {__DEV__ && (
        <>
          <InfoBanner message="Test only: submit placeholder documents (no real upload) so an admin account can approve them." />
          <Button title="Submit placeholder documents" variant="ghost" loading={placeholders.loading} onPress={() => placeholders.run()} />
        </>
      )}
    </Screen>
  );
}
