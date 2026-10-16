import { useFocusEffect, useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import React, { useCallback, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { Button, Card, Chip, ChipRow, ErrorBanner, H2, Loading, Muted, Pill, Screen } from '../../components/ui';
import { messageOf } from '../../hooks';
import type { CustomerStackParams } from '../../navigation/types';
import { api } from '../../state/runtime';
import { colors, radius } from '../../theme';
import type { BulkJobPost, Category } from '../../types';
import { prettyCode } from '../../utils/catalog';

export function CustomerHomeScreen() {
  const nav = useNavigation<NativeStackNavigationProp<CustomerStackParams>>();
  const [mode, setMode] = useState<'single' | 'bulk'>('single');
  const [categories, setCategories] = useState<Category[] | null>(null);
  const [bulk, setBulk] = useState<BulkJobPost[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useFocusEffect(useCallback(() => {
    setError(null);
    api.catalog.categories().then(setCategories).catch((e) => setError(messageOf(e)));
    api.bulk.mine().then(setBulk).catch((e) => setError(messageOf(e)));
  }, []));

  return (
    <Screen>
      <ChipRow>
        <Chip label="Single worker" selected={mode === 'single'} onPress={() => setMode('single')} />
        <Chip label="Bulk hire (contractor)" selected={mode === 'bulk'} onPress={() => setMode('bulk')} />
      </ChipRow>
      <ErrorBanner message={error} />

      {mode === 'single' ? (
        <>
          <H2>What service do you need?</H2>
          {!categories ? <Loading /> : (
            <View style={styles.grid}>
              {categories.map((c) => (
                <Pressable key={c.code} style={styles.tile} onPress={() => nav.navigate('Category', { code: c.code, name: c.name })}>
                  <Text style={styles.tileText}>{c.name}</Text>
                </Pressable>
              ))}
            </View>
          )}
        </>
      ) : (
        <>
          <Muted>Need several workers for a site or event? Post once and every matching worker nearby is notified.</Muted>
          <Button title="Post a bulk requirement" onPress={() => nav.navigate('BulkPost')} />
          <H2>Your bulk requirements</H2>
          {!bulk ? <Loading /> : bulk.length === 0 ? <Muted>None yet.</Muted> : bulk.map((b) => (
            <Pressable key={b.id} onPress={() => nav.navigate('BulkDetail', { bulkJobId: b.id })}>
              <Card>
                <Text style={{ fontWeight: '700', color: colors.text }}>{b.workersNeeded} × {prettyCode(b.requiredLabourType)}</Text>
                <Muted>{b.siteAddressText ?? 'No address'}</Muted>
                <Pill text={b.status} tone={b.status === 'OPEN' ? 'ok' : 'neutral'} />
              </Card>
            </Pressable>
          ))}
        </>
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  tile: { width: '48%', minHeight: 84, backgroundColor: colors.card, borderRadius: radius, borderWidth: 1, borderColor: colors.line, padding: 12, justifyContent: 'center' },
  tileText: { fontSize: 15, fontWeight: '700', color: colors.navy },
});
