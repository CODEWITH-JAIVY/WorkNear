import React from 'react';
import { Switch, Text, View } from 'react-native';
import { useLocation } from '../state/LocationContext';
import { colors } from '../theme';
import { Field, Muted } from './ui';

// Lat/lon used for matching. Typed values by default (reliable on emulators); GPS is opt-in.
export function LocationFields() {
  const { lat, lon, setLat, setLon, useGps, setUseGps } = useLocation();
  return (
    <View>
      <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
        <Text style={{ color: colors.text, fontWeight: '600' }}>Use my phone's GPS</Text>
        <Switch value={useGps} onValueChange={setUseGps} />
      </View>
      {useGps
        ? <Muted>Your location is read when you submit. Current: {lat}, {lon}</Muted>
        : <View style={{ flexDirection: 'row', gap: 10 }}>
            <View style={{ flex: 1 }}><Field label="Latitude" value={lat} onChangeText={setLat} keyboardType="numbers-and-punctuation" /></View>
            <View style={{ flex: 1 }}><Field label="Longitude" value={lon} onChangeText={setLon} keyboardType="numbers-and-punctuation" /></View>
          </View>}
    </View>
  );
}
