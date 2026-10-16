import React, { useState } from 'react';
import { Button, Card, ErrorBanner, Field, InfoBanner, Muted, Screen } from '../../components/ui';
import { useAction } from '../../hooks';
import { api, getBaseUrl, saveBaseUrl } from '../../state/runtime';
import { isValidBaseUrl } from '../../utils/urls';

export function ServerScreen() {
  const [url, setUrl] = useState(getBaseUrl());
  const [result, setResult] = useState<string | null>(null);
  const valid = isValidBaseUrl(url);

  const test = useAction(async () => {
    setResult(null);
    await saveBaseUrl(url);
    // catalog browsing is public, so this checks gateway + catalog-service without needing a login
    const cats = await api.catalog.categories();
    setResult(`Connected. Gateway and catalog-service answered (${cats.length} categories).`);
  });

  return (
    <Screen>
      <Card>
        <Muted>Address of the API gateway (port 8080) as seen from this device.</Muted>
        <Muted>• Android emulator: http://10.0.2.2:8080</Muted>
        <Muted>• iOS simulator: http://localhost:8080</Muted>
        <Muted>• Physical phone: http://YOUR-PC-LAN-IP:8080 (same Wi-Fi)</Muted>
      </Card>
      <Field label="Server URL" value={url} onChangeText={setUrl} autoCapitalize="none" keyboardType="url"
        error={url && !valid ? 'Must start with http:// or https://' : null} />
      <ErrorBanner message={test.error} />
      <InfoBanner message={result} tone="ok" />
      <Button title="Save and test connection" onPress={() => test.run()} loading={test.loading} disabled={!valid} />
    </Screen>
  );
}
