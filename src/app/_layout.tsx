import { DarkTheme, DefaultTheme, Stack, ThemeProvider } from 'expo-router';
import { useColorScheme } from 'react-native';

import { Colors } from '@/constants/theme';
import { ProfilesProvider } from '@/lib/profiles';

export default function RootLayout() {
  const scheme = useColorScheme();
  const colors = Colors[scheme === 'dark' ? 'dark' : 'light'];

  return (
    <ProfilesProvider>
      <ThemeProvider value={scheme === 'dark' ? DarkTheme : DefaultTheme}>
        <Stack
          screenOptions={{
            headerStyle: { backgroundColor: colors.background },
            headerTintColor: colors.text,
            headerShadowVisible: false,
            contentStyle: { backgroundColor: colors.background },
          }}>
          <Stack.Screen name="index" options={{ title: 'StoryTime' }} />
          <Stack.Screen name="story/[id]" options={{ title: '' }} />
          <Stack.Screen name="record/[id]" options={{ title: 'Record' }} />
          <Stack.Screen name="profiles" options={{ title: 'Voices', presentation: 'modal' }} />
        </Stack>
      </ThemeProvider>
    </ProfilesProvider>
  );
}
