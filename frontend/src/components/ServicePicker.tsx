import React, { useEffect, useRef, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { messageOf } from '../hooks';
import { api } from '../state/runtime';
import { colors, space } from '../theme';
import type { Category, CategoryWithItems, ItemDto } from '../types';
import { Chip, ChipRow, ErrorBanner, Field, Loading, Muted } from './ui';

export interface Picked { code: string; name: string }

// Browse the 25 categories / 336 services, or search across all of them. multi=true lets a
// labour tick many services; multi=false (customer posting a job) keeps exactly one.
export function ServicePicker({ selected, onChange, multi }: { selected: Picked[]; onChange: (next: Picked[]) => void; multi: boolean }) {
  const [categories, setCategories] = useState<Category[] | null>(null);
  const [open, setOpen] = useState<string | null>(null);
  const [loaded, setLoaded] = useState<Record<string, CategoryWithItems>>({});
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<ItemDto[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => { api.catalog.categories().then(setCategories).catch((e) => setError(messageOf(e))); }, []);

  useEffect(() => {
    if (timer.current) clearTimeout(timer.current);
    if (query.trim().length < 2) { setResults(null); return; }
    timer.current = setTimeout(() => {
      api.catalog.search(query.trim()).then(setResults).catch((e) => setError(messageOf(e)));
    }, 300);
    return () => { if (timer.current) clearTimeout(timer.current); };
  }, [query]);

  const toggleCategory = async (code: string) => {
    if (open === code) { setOpen(null); return; }
    setOpen(code);
    if (!loaded[code]) {
      try { const data = await api.catalog.categoryItems(code); setLoaded((p) => ({ ...p, [code]: data })); }
      catch (e) { setError(messageOf(e)); }
    }
  };

  const isSelected = (code: string) => selected.some((s) => s.code === code);
  const pick = (item: ItemDto) => {
    if (!multi) { onChange([{ code: item.code, name: item.name }]); return; }
    onChange(isSelected(item.code) ? selected.filter((s) => s.code !== item.code) : [...selected, { code: item.code, name: item.name }]);
  };

  const row = (item: ItemDto) => (
    <Pressable key={item.code} onPress={() => pick(item)} style={styles.item}>
      <Text style={[styles.itemText, isSelected(item.code) && { color: colors.blue, fontWeight: '700' }]}>{item.name}</Text>
      <Text style={{ color: colors.blue, fontSize: 18 }}>{isSelected(item.code) ? '✓' : ''}</Text>
    </Pressable>
  );

  return (
    <View>
      <ErrorBanner message={error} />
      {selected.length > 0 && (
        <ChipRow>{selected.map((s) => <Chip key={s.code} label={`${s.name}  ✕`} selected onPress={() => onChange(selected.filter((x) => x.code !== s.code))} />)}</ChipRow>
      )}
      <Field label="Search all services" value={query} onChangeText={setQuery} placeholder="e.g. plumber, tap, kitchen" autoCapitalize="none" />
      {results ? (
        results.length === 0 ? <Muted>No services match “{query}”.</Muted> : results.map(row)
      ) : !categories ? <Loading label="Loading services…" /> : (
        categories.map((c) => (
          <View key={c.code}>
            <Pressable onPress={() => toggleCategory(c.code)} style={styles.cat}>
              <Text style={styles.catText}>{c.name}</Text>
              <Text style={{ color: colors.muted }}>{open === c.code ? '▲' : '▼'}</Text>
            </Pressable>
            {open === c.code && (loaded[c.code]
              ? Object.entries(loaded[c.code]!.groupedItems).map(([group, items]) => (
                  <View key={group} style={{ marginLeft: space.md }}>
                    <Text style={styles.group}>{group}</Text>
                    {items.map(row)}
                  </View>))
              : <Loading />)}
          </View>
        ))
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  cat: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: 14, borderBottomWidth: 1, borderColor: colors.line },
  catText: { fontSize: 16, fontWeight: '600', color: colors.text },
  group: { fontSize: 12, fontWeight: '800', color: colors.orange, marginTop: space.md, marginBottom: 2, textTransform: 'uppercase' },
  item: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: 11, paddingHorizontal: 6 },
  itemText: { fontSize: 15, color: colors.text },
});
