import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useNavigation } from '@react-navigation/native';
import React from 'react';
import { Button, Card, H2, Muted, Pill, Screen } from '../../components/ui';
import { useAuth } from '../../state/AuthContext';
import { api } from '../../state/runtime';
import { useNotifications } from '../../state/NotificationsContext';
import { getBaseUrl } from '../../state/runtime';

export function AccountScreen() {
  const { session, logout } = useAuth();
  const { connected, events, clear } = useNotifications();
  const nav = useNavigation<NativeStackNavigationProp<{ Server: undefined }>>();
  if (!session) return null;
  return (
    <Screen>
      <Card>
        <H2>Signed in</H2>
        <Muted>User ID: {session.userId}</Muted>
        <Muted>Role: {session.userType}</Muted>
        <Muted>Server: {getBaseUrl()}</Muted>
      </Card>
      <Card>
        <H2>Live notifications</H2>
        {session.userType === 'ADMIN'
          ? <Muted>Not used for admin accounts.</Muted>
          : <><Pill text={connected ? 'Connected' : 'Not connected'} tone={connected ? 'ok' : 'warn'} />
              <Muted style={{ marginTop: 8 }}>{events.length} received this session.</Muted>
              {events.length > 0 && <Button title="Clear received list" variant="ghost" onPress={clear} />}</>}
      </Card>
      <Button title="Server settings" variant="secondary" onPress={() => nav.navigate('Server')} />
      <Button title="Log out" variant="danger" onPress={async () => {
        // Location pings stop on logout, so mark a worker offline first — otherwise they would
        // keep showing as available (and keep being matched) with a stale position.
        if (session.userType === 'LABOUR') await api.labour.setAvailability(false).catch(() => {});
        await logout();
      }} />
    </Screen>
  );
}
