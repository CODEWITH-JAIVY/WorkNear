import { NavigationContainer } from '@react-navigation/native';
import { StatusBar } from 'expo-status-bar';
import React from 'react';
import { ActivityIndicator, View } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { RootNavigator } from './src/navigation/RootNavigator';
import { AuthProvider, useAuth } from './src/state/AuthContext';
import { LocationProvider } from './src/state/LocationContext';
import { NotificationsProvider } from './src/state/NotificationsContext';
import { colors } from './src/theme';

function Gate() {
  const { session, ready } = useAuth();
  if (!ready) return <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}><ActivityIndicator color={colors.blue} /></View>;
  const tree = <NavigationContainer><RootNavigator /></NavigationContainer>;
  // Live push is only for customers and workers; admins use pull screens only.
  return session && session.userType !== 'ADMIN'
    ? <NotificationsProvider session={session}>{tree}</NotificationsProvider>
    : tree;
}

export default function App() {
  return (
    <SafeAreaProvider>
      <AuthProvider>
        <LocationProvider>
          <Gate />
          <StatusBar style="light" />
        </LocationProvider>
      </AuthProvider>
    </SafeAreaProvider>
  );
}
