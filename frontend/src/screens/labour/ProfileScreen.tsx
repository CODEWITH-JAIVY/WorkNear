import { useFocusEffect, useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import React, { useCallback, useState } from 'react';
import { type Picked, ServicePicker } from '../../components/ServicePicker';
import { Button, Chip, ChipRow, ErrorBanner, Field, H2, InfoBanner, Loading, Muted, Pill, Screen } from '../../components/ui';
import { messageOf, useAction } from '../../hooks';
import type { LabourStackParams } from '../../navigation/types';
import { api } from '../../state/runtime';
import type { LabourProfile } from '../../types';
import { categoryOfCode, prettyCode, splitCsv } from '../../utils/catalog';

const LANGUAGES = ['HINDI', 'ENGLISH', 'PUNJABI', 'BENGALI', 'BHOJPURI'];

export function LabourProfileScreen() {
  const nav = useNavigation<NativeStackNavigationProp<LabourStackParams>>();
  const [loaded, setLoaded] = useState<LabourProfile | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [name, setName] = useState('');
  const [city, setCity] = useState('');
  const [rate, setRate] = useState('');
  const [about, setAbout] = useState('');
  const [gender, setGender] = useState<'MALE' | 'FEMALE' | null>(null);
  const [languages, setLanguages] = useState<string[]>([]);
  const [services, setServices] = useState<Picked[]>([]);
  const [saved, setSaved] = useState(false);

  useFocusEffect(useCallback(() => {
    (async () => {
      try {
        const [p, all] = await Promise.all([api.labour.me(), api.catalog.all()]);
        const names = new Map(all.map((i) => [i.code, i.name]));
        setLoaded(p);
        setName(p.name ?? ''); setCity(p.city ?? ''); setRate(p.rate != null ? String(p.rate) : ''); setAbout(p.about ?? '');
        setGender(p.gender); setLanguages(splitCsv(p.languagesCsv));
        setServices(splitCsv(p.serviceCodesCsv).map((code) => ({ code, name: names.get(code) ?? prettyCode(code) })));
        setLoadError(null);
      } catch (e) { setLoadError(messageOf(e)); }
    })();
  }, []));

  const rateInvalid = rate !== '' && !(Number(rate) >= 0);
  const save = useAction(async () => {
    setSaved(false);
    const p = await api.labour.update({
      name: name.trim(),
      labourType: services[0] ? categoryOfCode(services[0].code) : undefined,
      serviceCodes: services.map((s) => s.code),
      gender: gender ?? undefined,
      languagesCsv: languages.join(',') || undefined,
      rate: rate ? Number(rate) : undefined,
      about: about.trim() || undefined,
      city: city.trim() || undefined,
    });
    setLoaded(p); setSaved(true);
  });

  if (!loaded) return <Screen><ErrorBanner message={loadError} />{!loadError && <Loading />}</Screen>;

  return (
    <Screen>
      <ErrorBanner message={save.error} />
      <InfoBanner tone="ok" message={saved ? 'Profile saved and synced for matching.' : null} />
      <Pill text={loaded.kycVerified ? 'KYC verified' : 'KYC not verified yet'} tone={loaded.kycVerified ? 'ok' : 'warn'} />
      <Button title="KYC documents" variant="secondary" onPress={() => nav.navigate('Kyc')} />

      <H2>About you</H2>
      <Field label="Name" value={name} onChangeText={setName} />
      <Field label="City" value={city} onChangeText={setCity} />
      <Field label="Your usual rate in ₹ (optional)" value={rate} onChangeText={setRate} keyboardType="numeric" error={rateInvalid ? 'Enter a number' : null} />
      <Field label="About your work" value={about} onChangeText={setAbout} multiline />

      <H2>Gender</H2>
      <Muted>Only used when a customer asks for a female or male worker. If you leave this empty, those requests will not reach you.</Muted>
      <ChipRow>
        <Chip label="Female" selected={gender === 'FEMALE'} onPress={() => setGender('FEMALE')} />
        <Chip label="Male" selected={gender === 'MALE'} onPress={() => setGender('MALE')} />
        <Chip label="Not stated" selected={gender === null} onPress={() => setGender(null)} />
      </ChipRow>

      <H2>Languages</H2>
      <ChipRow>
        {LANGUAGES.map((l) => (
          <Chip key={l} label={l[0] + l.slice(1).toLowerCase()} selected={languages.includes(l)}
            onPress={() => setLanguages((p) => (p.includes(l) ? p.filter((x) => x !== l) : [...p, l]))} />
        ))}
      </ChipRow>

      <H2>Services you offer ({services.length})</H2>
      <Muted>Tick everything you can do. You are matched to a job if it is any one of these.</Muted>
      <ServicePicker selected={services} onChange={setServices} multi />

      <Button title="Save profile" onPress={() => save.run()} loading={save.loading} disabled={!name.trim() || rateInvalid} />
    </Screen>
  );
}
