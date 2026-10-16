import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import React, { useState } from 'react';
import { LocationFields } from '../../components/LocationFields';
import { type Picked, ServicePicker } from '../../components/ServicePicker';
import { Button, Chip, ChipRow, ErrorBanner, Field, H2, Muted, Screen } from '../../components/ui';
import { useAction } from '../../hooks';
import type { CustomerStackParams } from '../../navigation/types';
import { useLocation } from '../../state/LocationContext';
import { api } from '../../state/runtime';

export function BulkPostScreen({ navigation }: NativeStackScreenProps<CustomerStackParams, 'BulkPost'>) {
  const { resolve } = useLocation();
  const [service, setService] = useState<Picked[]>([]);
  const [count, setCount] = useState('5');
  const [description, setDescription] = useState('');
  const [address, setAddress] = useState('');
  const [radius, setRadius] = useState(5);
  const [formLink, setFormLink] = useState('');

  const countOk = /^\d+$/.test(count) && Number(count) >= 1;
  const linkOk = formLink === '' || /^https?:\/\/\S+$/i.test(formLink.trim());

  const submit = useAction(async () => {
    const { lat, lon } = await resolve();
    await api.bulk.post({
      requiredLabourType: service[0]!.code,
      workersNeeded: Number(count),
      description: description.trim() || undefined,
      siteAddressText: address.trim() || undefined,
      latitude: lat, longitude: lon, radiusKm: radius,
      externalFormLink: formLink.trim() || undefined,
    });
    navigation.goBack();
  });

  return (
    <Screen>
      <ErrorBanner message={submit.error} />
      <H2>Which worker type?</H2>
      <ServicePicker selected={service} onChange={setService} multi={false} />
      <Field label="How many workers?" value={count} onChangeText={setCount} keyboardType="number-pad" error={!countOk ? 'Enter 1 or more' : null} />
      <Field label="Details" value={description} onChangeText={setDescription} multiline placeholder="Site, dates, wages, shift timing…" />
      <Field label="Site address" value={address} onChangeText={setAddress} />
      <LocationFields />
      <ChipRow>{[3, 5, 10, 20].map((r) => <Chip key={r} label={`${r} km`} selected={radius === r} onPress={() => setRadius(r)} />)}</ChipRow>
      <Field label="Application form link (optional)" value={formLink} onChangeText={setFormLink} autoCapitalize="none" keyboardType="url"
        placeholder="https://forms.gle/…" error={!linkOk ? 'Must start with http:// or https://' : null} />
      <Muted>With a link, workers apply on your form. Without one, they can tap “I’m interested” and you see who did.</Muted>
      <Button title="Post requirement" onPress={() => submit.run()} loading={submit.loading} disabled={service.length === 0 || !countOk || !linkOk} />
    </Screen>
  );
}
