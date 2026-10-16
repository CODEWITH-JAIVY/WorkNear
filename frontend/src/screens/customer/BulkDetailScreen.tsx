import { useFocusEffect } from '@react-navigation/native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import React, { useCallback, useState } from 'react';
import { LabourCard } from '../../components/LabourCard';
import { Button, Card, ErrorBanner, H2, InfoBanner, Loading, Muted, Pill, Screen } from '../../components/ui';
import { messageOf, useAction } from '../../hooks';
import type { CustomerStackParams } from '../../navigation/types';
import { api } from '../../state/runtime';
import type { BulkJobInterest, BulkJobPost, PublicLabour } from '../../types';
import { prettyCode } from '../../utils/catalog';

export function BulkDetailScreen({ route }: NativeStackScreenProps<CustomerStackParams, 'BulkDetail'>) {
  const { bulkJobId } = route.params;
  const [job, setJob] = useState<BulkJobPost | null>(null);
  const [interests, setInterests] = useState<BulkJobInterest[]>([]);
  const [profiles, setProfiles] = useState<Record<number, PublicLabour>>({});
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      // there is no GET-by-id for bulk jobs, so find it in the contractor's own list
      const mine = await api.bulk.mine();
      const found = mine.find((b) => b.id === bulkJobId) ?? null;
      setJob(found);
      if (found && !found.externalFormLink) {
        const list = await api.bulk.interested(bulkJobId);
        setInterests(list);
        for (const i of list) {
          api.labour.publicProfile(i.labourId).then((p) => setProfiles((prev) => ({ ...prev, [i.labourId]: p }))).catch(() => {});
        }
      }
      setError(null);
    } catch (e) { setError(messageOf(e)); }
  }, [bulkJobId]);
  useFocusEffect(useCallback(() => { load(); }, [load]));

  const close = useAction(async () => { await api.bulk.close(bulkJobId); await load(); });

  if (!job) return <Screen><ErrorBanner message={error} />{!error && <Loading />}</Screen>;
  return (
    <Screen>
      <ErrorBanner message={error ?? close.error} />
      <Card>
        <H2>{job.workersNeeded} × {prettyCode(job.requiredLabourType)}</H2>
        <Pill text={job.status} tone={job.status === 'OPEN' ? 'ok' : 'neutral'} />
        {job.description ? <Muted style={{ marginTop: 8 }}>{job.description}</Muted> : null}
        {job.siteAddressText ? <Muted>📍 {job.siteAddressText}</Muted> : null}
        {job.externalFormLink ? <Muted>Form: {job.externalFormLink}</Muted> : null}
      </Card>
      {job.externalFormLink
        ? <InfoBanner message="Workers apply on your form, so applicants are not listed here." />
        : <>
            <H2>Interested workers ({interests.length})</H2>
            {interests.map((i) => <LabourCard key={i.id} labour={profiles[i.labourId]} labourId={i.labourId} />)}
          </>}
      {job.status === 'OPEN' && <Button title="Close this requirement" variant="danger" loading={close.loading} onPress={() => close.run()} />}
    </Screen>
  );
}
