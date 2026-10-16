import { useFocusEffect } from '@react-navigation/native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Alert } from 'react-native';
import { LabourCard } from '../../components/LabourCard';
import { Button, Card, ErrorBanner, H2, InfoBanner, Loading, Muted, Pill, Screen } from '../../components/ui';
import { messageOf, useAction } from '../../hooks';
import type { CustomerStackParams } from '../../navigation/types';
import { useNotifications } from '../../state/NotificationsContext';
import { api } from '../../state/runtime';
import type { JobInterest, JobPost, PublicLabour } from '../../types';
import { prettyCode } from '../../utils/catalog';

export function JobDetailScreen({ route }: NativeStackScreenProps<CustomerStackParams, 'JobDetail'>) {
  const { jobId } = route.params;
  const { events } = useNotifications();
  const [job, setJob] = useState<JobPost | null>(null);
  const [interests, setInterests] = useState<JobInterest[]>([]);
  const [profiles, setProfiles] = useState<Record<number, PublicLabour>>({});
  const [error, setError] = useState<string | null>(null);
  const seen = useRef(new Set<number>());

  const load = useCallback(async () => {
    try {
      const [j, i] = await Promise.all([api.jobs.get(jobId), api.jobs.interested(jobId)]);
      setJob(j); setInterests(i); setError(null);
      const ids = new Set(i.map((x) => x.labourId));
      if (j.acceptedLabourId) ids.add(j.acceptedLabourId);
      for (const id of ids) {
        if (seen.current.has(id)) continue;
        seen.current.add(id);
        api.labour.publicProfile(id).then((p) => setProfiles((prev) => ({ ...prev, [id]: p }))).catch(() => seen.current.delete(id));
      }
    } catch (e) { setError(messageOf(e)); }
  }, [jobId]);

  // Refresh when the screen is focused, every 5 s while the job is still open, and straight away
  // when a "someone is interested" push arrives.
  useFocusEffect(useCallback(() => {
    load();
    const t = setInterval(() => { load(); }, 5000);
    return () => clearInterval(t);
  }, [load]));
  useEffect(() => { if (events[0]?.type === 'NEW_INTEREST' && events[0].jobId === jobId) load(); }, [events, jobId, load]);

  const select = useAction(async (labourId: number) => { await api.jobs.select(jobId, labourId); await load(); });
  const complete = useAction(async () => { await api.jobs.complete(jobId); await load(); });

  const confirmSelect = (labourId: number, name: string) =>
    Alert.alert('Hire this worker?', `${name} will get the job and everyone else will be told it is filled.`, [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Hire', onPress: () => select.run(labourId) },
    ]);

  if (!job) return <Screen><ErrorBanner message={error} />{!error && <Loading />}</Screen>;

  const tone = job.status === 'OPEN' ? 'warn' : job.status === 'COMPLETED' ? 'neutral' : 'ok';
  return (
    <Screen>
      <ErrorBanner message={error ?? select.error ?? complete.error} />
      <Card>
        <H2>{job.title}</H2>
        <Pill text={job.status} tone={tone} />
        <Muted style={{ marginTop: 8 }}>{prettyCode(job.requiredLabourType)}</Muted>
        {job.description ? <Muted>{job.description}</Muted> : null}
        {job.addressText ? <Muted>📍 {job.addressText}</Muted> : null}
        {job.budget != null ? <Muted>Budget ₹{job.budget}</Muted> : null}
        {job.scheduledAt ? <Muted>When: {job.scheduledAt.replace('T', ' ').slice(0, 16)}</Muted> : <Muted>When: as soon as possible</Muted>}
        {job.genderPreference ? <Muted>Preferred worker: {job.genderPreference.toLowerCase()}</Muted> : null}
      </Card>

      {job.status === 'OPEN' && (
        <>
          <H2>Interested workers ({interests.length})</H2>
          {interests.length === 0
            ? <InfoBanner message="Waiting for nearby workers. This list updates by itself — you will also get a notification when someone is interested." />
            : interests.map((i) => (
                <LabourCard key={i.id} labour={profiles[i.labourId]} labourId={i.labourId} message={i.message}>
                  <Button title="Hire this worker" loading={select.loading} onPress={() => confirmSelect(i.labourId, profiles[i.labourId]?.name || `Worker #${i.labourId}`)} />
                </LabourCard>))}
        </>
      )}

      {job.status === 'ACCEPTED' && job.acceptedLabourId != null && (
        <>
          <H2>Hired</H2>
          <LabourCard labour={profiles[job.acceptedLabourId]} labourId={job.acceptedLabourId} />
          <Button title="Mark work as completed" loading={complete.loading} onPress={() => complete.run()} />
        </>
      )}

      {job.status === 'COMPLETED' && <InfoBanner tone="ok" message="Work completed. Any photos or video you attached have been removed." />}
    </Screen>
  );
}
