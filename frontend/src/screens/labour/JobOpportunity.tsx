import React, { useEffect, useState } from 'react';
import { Button, Card, ErrorBanner, Field, H2, InfoBanner, Loading, Muted, Pill } from '../../components/ui';
import { messageOf, useAction } from '../../hooks';
import { api } from '../../state/runtime';
import type { JobPost } from '../../types';
import { prettyCode } from '../../utils/catalog';

// One nearby job as the worker sees it, with the "I'm interested" action.
export function JobOpportunity({ jobId }: { jobId: number }) {
  const [job, setJob] = useState<JobPost | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [message, setMessage] = useState('');
  const [done, setDone] = useState(false);

  useEffect(() => { api.jobs.get(jobId).then(setJob).catch((e) => setLoadError(messageOf(e))); }, [jobId]);

  const interest = useAction(async () => { await api.jobs.expressInterest(jobId, message.trim() || undefined); setDone(true); });

  if (loadError) return <Card><ErrorBanner message={`Job #${jobId}: ${loadError}`} /></Card>;
  if (!job) return <Card><Loading /></Card>;

  return (
    <Card>
      <H2>{job.title}</H2>
      <Pill text={job.status} tone={job.status === 'OPEN' ? 'warn' : 'neutral'} />
      <Muted style={{ marginTop: 8 }}>{prettyCode(job.requiredLabourType)}</Muted>
      {job.description ? <Muted>{job.description}</Muted> : null}
      {job.addressText ? <Muted>📍 {job.addressText}</Muted> : null}
      {job.budget != null ? <Muted>Budget ₹{job.budget}</Muted> : null}
      <Muted>{job.scheduledAt ? `When: ${job.scheduledAt.replace('T', ' ').slice(0, 16)}` : 'When: as soon as possible'}</Muted>
      {job.status !== 'OPEN' ? <InfoBanner message="This job is no longer open." />
        : done ? <InfoBanner tone="ok" message="Interest sent. The customer can now see your profile." />
        : <>
            <ErrorBanner message={interest.error} />
            <Field label="Message to the customer (optional)" value={message} onChangeText={setMessage} placeholder="e.g. Can come by 4pm" />
            <Button title="I'm interested" loading={interest.loading} onPress={() => interest.run()} />
          </>}
    </Card>
  );
}
