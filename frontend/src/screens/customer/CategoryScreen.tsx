import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import React, { useEffect, useState } from 'react';
import { Pressable, Text } from 'react-native';
import { ErrorBanner, H2, Loading, Screen } from '../../components/ui';
import { messageOf } from '../../hooks';
import type { CustomerStackParams } from '../../navigation/types';
import { api } from '../../state/runtime';
import { colors } from '../../theme';
import type { CategoryWithItems } from '../../types';

export function CategoryScreen({ route, navigation }: NativeStackScreenProps<CustomerStackParams, 'Category'>) {
  const { code } = route.params;
  const [data, setData] = useState<CategoryWithItems | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => { api.catalog.categoryItems(code).then(setData).catch((e) => setError(messageOf(e))); }, [code]);

  return (
    <Screen>
      <ErrorBanner message={error} />
      {!data ? <Loading /> : Object.entries(data.groupedItems).map(([group, items]) => (
        <React.Fragment key={group}>
          <H2>{group}</H2>
          {items.map((it) => (
            <Pressable key={it.code} onPress={() => navigation.navigate('PostJob', { itemCode: it.code, itemName: it.name, categoryCode: code })}
              style={{ paddingVertical: 14, borderBottomWidth: 1, borderColor: colors.line }}>
              <Text style={{ fontSize: 16, color: colors.text }}>{it.name}</Text>
            </Pressable>
          ))}
        </React.Fragment>
      ))}
    </Screen>
  );
}
