import React from 'react';
import {
  ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, TextInput, View,
  type TextInputProps, type TextProps, type ViewStyle,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors, radius, space } from '../theme';

export function Screen({ children, scroll = true, style }: { children: React.ReactNode; scroll?: boolean; style?: ViewStyle }) {
  const body = scroll
    ? <ScrollView contentContainerStyle={[styles.screenPad, style]} keyboardShouldPersistTaps="handled">{children}</ScrollView>
    : <View style={[styles.screenPad, { flex: 1 }, style]}>{children}</View>;
  return <SafeAreaView style={styles.screen} edges={['left', 'right', 'bottom']}>{body}</SafeAreaView>;
}

export function H1({ children }: { children: React.ReactNode }) { return <Text style={styles.h1}>{children}</Text>; }
export function H2({ children }: { children: React.ReactNode }) { return <Text style={styles.h2}>{children}</Text>; }
export function Muted({ children, style, ...rest }: TextProps & { children: React.ReactNode }) { return <Text {...rest} style={[styles.muted, style]}>{children}</Text>; }

export function Button({ title, onPress, variant = 'primary', disabled, loading, style }: {
  title: string; onPress: () => void; variant?: 'primary' | 'secondary' | 'danger' | 'ghost';
  disabled?: boolean; loading?: boolean; style?: ViewStyle;
}) {
  const off = disabled || loading;
  const bg = variant === 'primary' ? colors.blue : variant === 'danger' ? colors.red : variant === 'secondary' ? colors.card : 'transparent';
  const fg = variant === 'secondary' ? colors.blue : variant === 'ghost' ? colors.blue : '#fff';
  return (
    <Pressable
      accessibilityRole="button"
      onPress={off ? undefined : onPress}
      style={({ pressed }) => [styles.btn, { backgroundColor: bg, opacity: off ? 0.5 : pressed ? 0.85 : 1 },
        variant === 'secondary' && { borderWidth: 1.5, borderColor: colors.blue }, style]}
    >
      {loading ? <ActivityIndicator color={fg} /> : <Text style={[styles.btnText, { color: fg }]}>{title}</Text>}
    </Pressable>
  );
}

export function Field({ label, error, ...rest }: TextInputProps & { label: string; error?: string | null }) {
  return (
    <View style={{ marginBottom: space.md }}>
      <Text style={styles.label}>{label}</Text>
      <TextInput placeholderTextColor="#9AA3B5" {...rest} style={[styles.input, error ? { borderColor: colors.red } : null, rest.multiline && { minHeight: 90, textAlignVertical: 'top' }]} />
      {error ? <Text style={styles.errorSmall}>{error}</Text> : null}
    </View>
  );
}

export function Chip({ label, selected, onPress }: { label: string; selected?: boolean; onPress?: () => void }) {
  return (
    <Pressable onPress={onPress} style={[styles.chip, selected && styles.chipOn]}>
      <Text style={[styles.chipText, selected && { color: '#fff' }]}>{label}</Text>
    </Pressable>
  );
}

export function ChipRow({ children }: { children: React.ReactNode }) { return <View style={styles.chipRow}>{children}</View>; }

export function Card({ children, style }: { children: React.ReactNode; style?: ViewStyle }) { return <View style={[styles.card, style]}>{children}</View>; }

export function ErrorBanner({ message }: { message?: string | null }) {
  if (!message) return null;
  return <View style={styles.errBox}><Text style={styles.errText}>{message}</Text></View>;
}

export function InfoBanner({ message, tone = 'info' }: { message?: string | null; tone?: 'info' | 'ok' }) {
  if (!message) return null;
  return (
    <View style={[styles.errBox, { backgroundColor: tone === 'ok' ? colors.greenBg : colors.amberBg, borderColor: tone === 'ok' ? colors.green : colors.orange }]}>
      <Text style={{ color: colors.text }}>{message}</Text>
    </View>
  );
}

export function Loading({ label }: { label?: string }) {
  return <View style={{ padding: space.xl, alignItems: 'center' }}><ActivityIndicator color={colors.blue} />{label ? <Muted style={{ marginTop: 8 }}>{label}</Muted> : null}</View>;
}

export function Pill({ text, tone }: { text: string; tone: 'ok' | 'warn' | 'bad' | 'neutral' }) {
  const map = { ok: [colors.greenBg, colors.green], warn: [colors.amberBg, '#B25E00'], bad: [colors.redBg, colors.red], neutral: ['#EEF0F6', colors.muted] } as const;
  const [bg, fg] = map[tone];
  return <View style={{ backgroundColor: bg, paddingHorizontal: 10, paddingVertical: 3, borderRadius: 20, alignSelf: 'flex-start' }}><Text style={{ color: fg, fontSize: 12, fontWeight: '700' }}>{text}</Text></View>;
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.bg },
  screenPad: { padding: space.lg, paddingBottom: 40 },
  h1: { fontSize: 24, fontWeight: '800', color: colors.navy, marginBottom: space.sm },
  h2: { fontSize: 17, fontWeight: '700', color: colors.text, marginTop: space.lg, marginBottom: space.sm },
  muted: { color: colors.muted, fontSize: 14 },
  btn: { minHeight: 52, borderRadius: radius, alignItems: 'center', justifyContent: 'center', paddingHorizontal: space.lg, marginVertical: 6 },
  btnText: { fontSize: 16, fontWeight: '700' },
  label: { fontSize: 13, fontWeight: '600', color: colors.muted, marginBottom: 6 },
  input: { backgroundColor: colors.card, borderWidth: 1, borderColor: colors.line, borderRadius: 10, paddingHorizontal: 12, paddingVertical: 12, fontSize: 16, color: colors.text },
  errorSmall: { color: colors.red, fontSize: 12, marginTop: 4 },
  chipRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginBottom: space.md },
  chip: { paddingHorizontal: 14, paddingVertical: 9, borderRadius: 22, borderWidth: 1, borderColor: colors.line, backgroundColor: colors.card },
  chipOn: { backgroundColor: colors.blue, borderColor: colors.blue },
  chipText: { color: colors.text, fontSize: 14, fontWeight: '600' },
  card: { backgroundColor: colors.card, borderRadius: radius, padding: space.lg, marginBottom: space.md, borderWidth: 1, borderColor: colors.line },
  errBox: { backgroundColor: colors.redBg, borderColor: colors.red, borderWidth: 1, borderRadius: 10, padding: space.md, marginBottom: space.md },
  errText: { color: colors.red },
});
