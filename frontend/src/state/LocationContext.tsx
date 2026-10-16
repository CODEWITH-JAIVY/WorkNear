import * as Location from 'expo-location';
import React, { createContext, useContext, useMemo, useState } from 'react';

// Defaults to Noida so customer and labour test accounts are within the 5 km matching radius of
// each other without needing real GPS (emulators report a US location by default).
const NOIDA = { lat: '28.5355', lon: '77.3910' };

interface LocationValue {
  lat: string; lon: string;
  setLat: (v: string) => void; setLon: (v: string) => void;
  useGps: boolean; setUseGps: (v: boolean) => void;
  // Resolves the coordinates to use right now (GPS when enabled and permitted, else the typed values)
  resolve: () => Promise<{ lat: number; lon: number }>;
}

const Ctx = createContext<LocationValue | null>(null);

export function LocationProvider({ children }: { children: React.ReactNode }) {
  const [lat, setLat] = useState(NOIDA.lat);
  const [lon, setLon] = useState(NOIDA.lon);
  const [useGps, setUseGps] = useState(false);

  const value = useMemo<LocationValue>(() => ({
    lat, lon, setLat, setLon, useGps, setUseGps,
    resolve: async () => {
      if (useGps) {
        const perm = await Location.requestForegroundPermissionsAsync();
        if (perm.status !== 'granted') throw new Error('Location permission was denied. Turn off "Use GPS" to use the typed coordinates.');
        const pos = await Location.getCurrentPositionAsync({});
        const next = { lat: pos.coords.latitude, lon: pos.coords.longitude };
        setLat(String(next.lat)); setLon(String(next.lon));
        return next;
      }
      const la = Number(lat), lo = Number(lon);
      if (!Number.isFinite(la) || !Number.isFinite(lo) || Math.abs(la) > 90 || Math.abs(lo) > 180)
        throw new Error('Latitude/longitude are not valid numbers.');
      return { lat: la, lon: lo };
    },
  }), [lat, lon, useGps]);

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useLocation(): LocationValue {
  const v = useContext(Ctx);
  if (!v) throw new Error('useLocation outside LocationProvider');
  return v;
}
