import * as ImagePicker from 'expo-image-picker';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import React, { useState } from 'react';
import { Image, Pressable, Text, View } from 'react-native';
import { LocationFields } from '../../components/LocationFields';
import { Button, Card, Chip, ChipRow, ErrorBanner, Field, H2, InfoBanner, Muted, Screen } from '../../components/ui';
import { messageOf, useAction } from '../../hooks';
import type { CustomerStackParams } from '../../navigation/types';
import { useLocation } from '../../state/LocationContext';
import { api } from '../../state/runtime';
import { colors } from '../../theme';
import type { JobPostRequest } from '../../types';
import { isGenderRelevant } from '../../utils/catalog';
import { parseManualSchedule, SCHEDULE_OPTIONS } from '../../utils/schedule';

const MAX_ATTACHMENTS = 5; // same cap job-service enforces
interface Attachment { key: string; uri: string }

export function PostJobScreen({ route, navigation }: NativeStackScreenProps<CustomerStackParams, 'PostJob'>) {
  const { itemCode, itemName, categoryCode } = route.params;
  const { resolve } = useLocation();

  const [title, setTitle] = useState(itemName);
  const [description, setDescription] = useState('');
  const [address, setAddress] = useState('');
  const [budget, setBudget] = useState('');
  const [radius, setRadius] = useState(5);
  const [gender, setGender] = useState<'MALE' | 'FEMALE' | null>(null);
  const [scheduleKey, setScheduleKey] = useState('asap');
  const [manual, setManual] = useState('');
  const [attachments, setAttachments] = useState<Attachment[]>([]);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);

  const manualValue = scheduleKey === 'manual' ? parseManualSchedule(manual) : null;
  const scheduleInvalid = scheduleKey === 'manual' && manualValue === null;
  const budgetInvalid = budget !== '' && !(Number(budget) >= 0);

  const attach = async () => {
    setUploadError(null);
    try {
      const perm = await ImagePicker.requestMediaLibraryPermissionsAsync();
      if (!perm.granted) { setUploadError('Photo library permission was denied.'); return; }
      const res = await ImagePicker.launchImageLibraryAsync({ mediaTypes: ['images', 'videos'], allowsMultipleSelection: false, quality: 0.7 });
      if (res.canceled || !res.assets[0]) return;
      const a = res.assets[0];
      setUploading(true);
      const out = await api.media.uploadJobAttachment({
        uri: a.uri,
        name: a.fileName ?? `attachment-${Date.now()}`,
        type: a.mimeType ?? (a.type === 'video' ? 'video/mp4' : 'image/jpeg'),
      });
      setAttachments((p) => [...p, { key: out.key, uri: a.uri }]);
    } catch (e) {
      setUploadError(`${messageOf(e)} — you can still post without attachments.`);
    } finally { setUploading(false); }
  };

  const submit = useAction(async () => {
    const { lat, lon } = await resolve();
    const schedule = scheduleKey === 'manual' ? manualValue : (SCHEDULE_OPTIONS.find((o) => o.key === scheduleKey)?.value(new Date()) ?? null);
    const req: JobPostRequest = {
      title: title.trim(),
      description: description.trim() || undefined,
      requiredLabourType: itemCode,
      genderPreference: gender ?? undefined,
      latitude: lat, longitude: lon,
      addressText: address.trim() || undefined,
      budget: budget ? Number(budget) : undefined,
      radiusKm: radius,
      scheduledAt: schedule,
      mediaKeys: attachments.map((a) => a.key),
    };
    const job = await api.jobs.post(req);
    navigation.replace('JobDetail', { jobId: job.id });
  });

  return (
    <Screen>
      <Card><Text style={{ fontWeight: '800', color: colors.navy, fontSize: 16 }}>{itemName}</Text><Muted>{itemCode}</Muted></Card>
      <ErrorBanner message={submit.error} />
      <Field label="Title" value={title} onChangeText={setTitle} />
      <Field label="Describe the work" value={description} onChangeText={setDescription} multiline placeholder="What needs to be done? Any details the worker should know." />

      <H2>Photos / video (optional)</H2>
      <Muted>Removed automatically after the job is completed, or within about 2–3 days.</Muted>
      <ErrorBanner message={uploadError} />
      <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginVertical: 8 }}>
        {attachments.map((a) => (
          <Pressable key={a.key} onPress={() => setAttachments((p) => p.filter((x) => x.key !== a.key))}>
            <Image source={{ uri: a.uri }} style={{ width: 72, height: 72, borderRadius: 8, backgroundColor: colors.line }} />
            <Text style={{ position: 'absolute', right: 4, top: 2, color: '#fff', fontWeight: '800' }}>✕</Text>
          </Pressable>
        ))}
      </View>
      <Button title={uploading ? 'Uploading…' : 'Add photo or video'} variant="secondary" onPress={attach} disabled={uploading || attachments.length >= MAX_ATTACHMENTS} />

      <H2>Where</H2>
      <Field label="Address / landmark" value={address} onChangeText={setAddress} />
      <LocationFields />
      <Muted>Search radius</Muted>
      <ChipRow>{[3, 5, 10].map((r) => <Chip key={r} label={`${r} km`} selected={radius === r} onPress={() => setRadius(r)} />)}</ChipRow>

      <H2>When</H2>
      <ChipRow>
        {SCHEDULE_OPTIONS.map((o) => <Chip key={o.key} label={o.label} selected={scheduleKey === o.key} onPress={() => setScheduleKey(o.key)} />)}
        <Chip label="Pick exact time" selected={scheduleKey === 'manual'} onPress={() => setScheduleKey('manual')} />
      </ChipRow>
      {scheduleKey === 'manual' && (
        <Field label="Date and time (YYYY-MM-DD HH:mm)" value={manual} onChangeText={setManual} placeholder="2026-10-12 10:00"
          error={manual && scheduleInvalid ? 'Use the format 2026-10-12 10:00' : null} />
      )}

      <H2>Budget</H2>
      <Field label="Estimated budget in ₹ (optional)" value={budget} onChangeText={setBudget} keyboardType="numeric"
        error={budgetInvalid ? 'Enter a number' : null} />
      <InfoBanner message={budget ? 'Workers whose quoted rate is above this budget (or who have not set a rate) will not be matched.' : null} />

      {isGenderRelevant(categoryCode) && (
        <>
          <H2>Preferred worker</H2>
          <ChipRow>
            <Chip label="No preference" selected={gender === null} onPress={() => setGender(null)} />
            <Chip label="Female" selected={gender === 'FEMALE'} onPress={() => setGender('FEMALE')} />
            <Chip label="Male" selected={gender === 'MALE'} onPress={() => setGender('MALE')} />
          </ChipRow>
        </>
      )}

      <Button title="Post job" onPress={() => submit.run()} loading={submit.loading} disabled={!title.trim() || scheduleInvalid || budgetInvalid || uploading} />
    </Screen>
  );
}
