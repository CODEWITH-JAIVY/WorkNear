import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import React, { useState } from 'react';
import { Text, View } from 'react-native';
import { Button, ErrorBanner, Field, H1, Muted, Screen } from '../../components/ui';
import { useAction } from '../../hooks';
import type { AuthStackParams } from '../../navigation/types';
import { useAuth } from '../../state/AuthContext';
import { colors } from '../../theme';

export function LoginScreen({ navigation }: NativeStackScreenProps<AuthStackParams, 'Login'>) {
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const submit = useAction(async () => { await login(email.trim(), password); });

  return (
    <Screen>
      <View style={{ marginTop: 40 }}>
        <Text style={{ fontSize: 34, fontWeight: '900', color: colors.navy }}>Work<Text style={{ color: colors.orange }}>Near</Text></Text>
        <Muted>Find work. Build better lives.</Muted>
      </View>
      <H1> </H1>
      <ErrorBanner message={submit.error} />
      <Field label="Email" value={email} onChangeText={setEmail} autoCapitalize="none" keyboardType="email-address" />
      <Field label="Password" value={password} onChangeText={setPassword} secureTextEntry />
      <Button title="Log in" onPress={() => submit.run()} loading={submit.loading} disabled={!email || !password} />
      <Button title="Create an account" variant="secondary" onPress={() => navigation.navigate('Signup')} />
      <Button title="Server settings" variant="ghost" onPress={() => navigation.navigate('Server')} />
    </Screen>
  );
}
