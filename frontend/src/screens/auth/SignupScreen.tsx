import React, { useState } from 'react';
import { Chip, ChipRow, ErrorBanner, Field, InfoBanner, Muted, Button, Screen } from '../../components/ui';
import { useAction } from '../../hooks';
import { useAuth } from '../../state/AuthContext';
import type { UserType } from '../../types';

// Mirrors the backend's rule (SignupRequest): 8+ chars with an upper-case letter, a lower-case letter and a digit.
export const PASSWORD_RULE = /^(?=.*[A-Z])(?=.*[a-z])(?=.*\d).{8,}$/;

export function SignupScreen() {
  const { signup } = useAuth();
  const [userType, setUserType] = useState<UserType>('CUSTOMER');
  const [email, setEmail] = useState('');
  const [mobile, setMobile] = useState('');
  const [password, setPassword] = useState('');
  const passwordOk = PASSWORD_RULE.test(password);
  const submit = useAction(async () => { await signup({ email: email.trim(), mobile: mobile.trim(), password, userType }); });

  return (
    <Screen>
      <ChipRow>
        <Chip label="I need work done" selected={userType === 'CUSTOMER'} onPress={() => setUserType('CUSTOMER')} />
        <Chip label="I am a worker" selected={userType === 'LABOUR'} onPress={() => setUserType('LABOUR')} />
        {__DEV__ && <Chip label="Admin (test only)" selected={userType === 'ADMIN'} onPress={() => setUserType('ADMIN')} />}
      </ChipRow>
      {userType === 'ADMIN' && <InfoBanner message="Test-only: the backend currently lets anyone register as ADMIN. This is needed to approve KYC while testing, and must be closed before launch." />}
      <ErrorBanner message={submit.error} />
      <Field label="Email" value={email} onChangeText={setEmail} autoCapitalize="none" keyboardType="email-address" />
      <Field label="Mobile number" value={mobile} onChangeText={setMobile} keyboardType="phone-pad" />
      <Field label="Password" value={password} onChangeText={setPassword} secureTextEntry
        error={password && !passwordOk ? '8+ characters with an upper-case letter, a lower-case letter and a digit' : null} />
      <Muted>Accounts are created on the backend you set under Server settings.</Muted>
      <Button title="Sign up" onPress={() => submit.run()} loading={submit.loading} disabled={!email || !mobile || !passwordOk} />
    </Screen>
  );
}
