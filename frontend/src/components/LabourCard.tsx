import React from 'react';
import { Text, View } from 'react-native';
import { colors } from '../theme';
import type { PublicLabour } from '../types';
import { prettyCode } from '../utils/catalog';
import { Card, Muted, Pill } from './ui';

// Customer-facing view of a labour: name, trust signals, skills, rate. No contact or location.
export function LabourCard({ labour, labourId, message, children }: {
  labour: PublicLabour | null | undefined; labourId: number; message?: string | null; children?: React.ReactNode;
}) {
  return (
    <Card>
      <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
        <Text style={{ fontSize: 17, fontWeight: '800', color: colors.text }}>{labour?.name || `Worker #${labourId}`}</Text>
        {labour && <Pill text={labour.kycVerified ? 'KYC verified' : 'Not verified'} tone={labour.kycVerified ? 'ok' : 'warn'} />}
      </View>
      {labour ? (
        <>
          <Muted>
            {labour.ratingCount > 0 ? `★ ${labour.rating.toFixed(1)} (${labour.ratingCount})` : 'No ratings yet'}
            {labour.rate != null ? `  ·  ₹${labour.rate}` : ''}
            {labour.city ? `  ·  ${labour.city}` : ''}
          </Muted>
          {labour.serviceCodes.length > 0 && <Muted>Offers: {labour.serviceCodes.slice(0, 4).map(prettyCode).join(', ')}{labour.serviceCodes.length > 4 ? ` +${labour.serviceCodes.length - 4} more` : ''}</Muted>}
          {labour.languages.length > 0 && <Muted>Speaks: {labour.languages.join(', ')}</Muted>}
          {labour.about ? <Muted>{labour.about}</Muted> : null}
        </>
      ) : <Muted>Loading profile…</Muted>}
      {message ? <Text style={{ marginTop: 8, color: colors.text, fontStyle: 'italic' }}>“{message}”</Text> : null}
      {children}
    </Card>
  );
}
