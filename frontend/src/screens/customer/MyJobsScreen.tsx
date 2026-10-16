import { useFocusEffect, useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import React, { useCallback, useState } from 'react';
import { Pressable } from 'react-native';
import { Button, Card, ErrorBanner, H2, Loading, Muted, Pill, Screen } from '../../components/ui';
import { messageOf } from '../../hooks';
import type { CustomerStackParams } from '../../navigation/types';
import { api } from '../../state/runtime';
import type { JobPost } from '../../types';
import { prettyCode } from '../../utils/catalog';

export function MyJobsScreen() {
  const nav = useNavigation<NativeStackNavigationProp<CustomerStackParams>>();
  const [jobs, setJobs] = useState<JobPost[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const load = useCallback(() => { api.jobs.mine().then((j) => { setJobs(j); setError(null); }).catch((e) => setError(messageOf(e))); }, []);
  useFocusEffect(load);

  return (
    <Screen>
      <ErrorBanner message={error} />
      {!jobs ? <Loading /> : jobs.length === 0 ? <Muted>You have not posted any jobs yet.</Muted> : jobs.map((j) => (
        <Pressable key={j.id} onPress={() => nav.navigate('JobDetail', { jobId: j.id })}>
          <Card>
            <H2>{j.title}</H2>
            <Muted>{prettyCode(j.requiredLabourType)}</Muted>
            <Pill text={j.status} tone={j.status === 'OPEN' ? 'warn' : j.status === 'COMPLETED' ? 'neutral' : 'ok'} />
          </Card>
        </Pressable>
      ))}
      <Button title="Refresh" variant="ghost" onPress={load} />
    </Screen>
  );
}
