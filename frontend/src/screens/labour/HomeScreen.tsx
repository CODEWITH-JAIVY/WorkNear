import { useFocusEffect, useNavigation } from '@react-navigation/native';
import type { BottomTabNavigationProp } from '@react-navigation/bottom-tabs';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Switch, Text, View } from 'react-native';
import { LocationFields } from '../../components/LocationFields';
import { Button, Card, ErrorBanner, Field, H2, InfoBanner, Muted, Pill, Screen } from '../../components/ui';
import { messageOf } from '../../hooks';
import type { LabourTabParams } from '../../navigation/types';
import { useLocation } from '../../state/LocationContext';
import { useNotifications } from '../../state/NotificationsContext';
import { api } from '../../state/runtime';
import { colors } from '../../theme';
import type { LabourProfile } from '../../types';
import { splitCsv } from '../../utils/catalog';
import { JobOpportunity } from './JobOpportunity';

const PING_MS = 20_000;

export function LabourHomeScreen() {
  const nav = useNavigation<BottomTabNavigationProp<LabourTabParams>>();
  const { resolve } = useLocation();
  const { events, connected } = useNotifications();
  const [profile, setProfile] = useState<LabourProfile | null>(null);
  const [online, setOnline] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [manualId, setManualId] = useState('');
  const [openIds, setOpenIds] = useState<number[]>([]);
  const timer = useRef<ReturnType<typeof setInterval> | null>(null);

  useFocusEffect(useCallback(() => {
    api.labour.me().then((p) => { setProfile(p); setOnline(p.availableToday); }).catch((e) => setError(messageOf(e)));
  }, []));

  const stopPings = () => { if (timer.current) { clearInterval(timer.current); timer.current = null; } };
  useEffect(() => stopPings, []);

  const ping = useCallback(async () => {
    const { lat, lon } = await resolve();
    await api.labour.updateLocation(lat, lon);
  }, [resolve]);

  const toggle = async (next: boolean) => {
    setBusy(true); setError(null);
    try {
      if (next) {
        await ping();
        await api.labour.setAvailability(true);
        stopPings();
        // Foreground-only: pings stop if the app is closed or backgrounded.
        timer.current = setInterval(() => { ping().catch(() => {}); }, PING_MS);
      } else {
        stopPings();
        await api.labour.setAvailability(false);
      }
      setOnline(next);
    } catch (e) { setError(messageOf(e)); } finally { setBusy(false); }
  };

  const jobEvents = events.filter((e) => e.type === 'NEW_JOB_AVAILABLE' && typeof e.jobId === 'number');
  const shownIds = Array.from(new Set([...openIds, ...jobEvents.map((e) => e.jobId as number)]));
  const assigned = events.filter((e) => e.type === 'JOB_ASSIGNED_TO_YOU');
  const filled = events.filter((e) => e.type === 'JOB_FILLED_BY_ANOTHER');
  const bulkCount = events.filter((e) => e.type === 'BULK_JOB_AVAILABLE').length;

  const noServices = profile != null && splitCsv(profile.serviceCodesCsv).length === 0;

  return (
    <Screen>
      <ErrorBanner message={error} />
      {profile && !profile.kycVerified && <InfoBanner message="You will not be matched to jobs until your KYC is verified. Open Profile → KYC documents." />}
      {noServices && <InfoBanner message="You have not chosen any services yet. Open the Profile tab and tick the work you do." />}

      <Card>
        <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
          <View style={{ flex: 1 }}>
            <Text style={{ fontSize: 18, fontWeight: '800', color: online ? colors.green : colors.text }}>{online ? 'You are online' : 'You are offline'}</Text>
            <Muted>{online ? 'Nearby jobs for your services reach you.' : 'Go online to receive jobs.'}</Muted>
          </View>
          <Switch value={online} onValueChange={toggle} disabled={busy} />
        </View>
        <View style={{ marginTop: 12 }}><LocationFields /></View>
        <Pill text={connected ? 'Live notifications connected' : 'Live notifications not connected'} tone={connected ? 'ok' : 'warn'} />
      </Card>

      {assigned.map((e) => <InfoBanner key={e.receivedAt} tone="ok" message={`🎉 You got job #${e.jobId}! The customer chose you.`} />)}
      {filled.map((e) => <InfoBanner key={e.receivedAt} message={`Job #${e.jobId} was given to another worker.`} />)}
      {bulkCount > 0 && <Card><Muted>{bulkCount} new bulk requirement(s) near you.</Muted><Button title="Open Bulk tab" variant="secondary" onPress={() => nav.navigate('Bulk')} /></Card>}

      <H2>Jobs near you</H2>
      {shownIds.length === 0 && <Muted>Nothing yet. New jobs appear here the moment a customer posts one that matches you.</Muted>}
      {shownIds.map((id) => <JobOpportunity key={id} jobId={id} />)}

      <H2>Open a job by number</H2>
      <Muted>Handy for testing if live notifications are not connected.</Muted>
      <Field label="Job ID" value={manualId} onChangeText={setManualId} keyboardType="number-pad" />
      <Button title="Open job" variant="secondary" disabled={!/^\d+$/.test(manualId)}
        onPress={() => { setOpenIds((p) => [Number(manualId), ...p.filter((x) => x !== Number(manualId))]); setManualId(''); }} />
    </Screen>
  );
}
