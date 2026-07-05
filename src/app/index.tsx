import { Link, useFocusEffect, useRouter } from 'expo-router';
import { useCallback, useState } from 'react';
import { FlatList, Pressable, StyleSheet, Text, View } from 'react-native';

import { Fonts, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { summarizeStory } from '@/lib/narration';
import { useProfiles } from '@/lib/profiles';
import { STORIES } from '@/lib/stories';
import type { Story } from '@/types/story';

export default function LibraryScreen() {
  const colors = useTheme();
  const router = useRouter();
  const { loading, activeProfile } = useProfiles();

  // Recordings are written by the record screen; refresh summaries on focus.
  const [refreshKey, setRefreshKey] = useState(0);
  useFocusEffect(
    useCallback(() => {
      setRefreshKey((key) => key + 1);
    }, [])
  );

  if (loading) {
    return <View style={[styles.container, { backgroundColor: colors.background }]} />;
  }

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <Link href="/profiles" asChild>
        <Pressable style={[styles.profileChip, { backgroundColor: colors.backgroundElement }]}>
          {activeProfile ? (
            <Text style={[styles.profileChipText, { color: colors.text }]}>
              {activeProfile.emoji} Reading with {activeProfile.name}
            </Text>
          ) : (
            <Text style={[styles.profileChipText, { color: colors.accent }]}>
              👋 Tap to add a voice — Mom, Dad, Grandma…
            </Text>
          )}
        </Pressable>
      </Link>

      <FlatList
        data={STORIES}
        extraData={refreshKey}
        keyExtractor={(story) => story.id}
        contentContainerStyle={styles.list}
        renderItem={({ item }) => (
          <StoryCard
            story={item}
            profileId={activeProfile?.id ?? null}
            profileName={activeProfile?.name ?? null}
            onPress={() => router.push(`/story/${item.id}`)}
          />
        )}
      />
    </View>
  );
}

function StoryCard({
  story,
  profileId,
  profileName,
  onPress,
}: {
  story: Story;
  profileId: string | null;
  profileName: string | null;
  onPress: () => void;
}) {
  const colors = useTheme();
  const summary = summarizeStory(profileId, story);

  let statusText = 'Not recorded yet';
  let statusColor: string = colors.textSecondary;
  if (summary.status === 'complete' && profileName) {
    statusText = `❤️ Read by ${profileName}`;
    statusColor = colors.recorded;
  } else if (summary.status === 'partial') {
    statusText = `${summary.recordedPages} of ${summary.totalPages} pages recorded`;
    statusColor = colors.accent;
  }

  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => [
        styles.card,
        { backgroundColor: colors.backgroundElement, opacity: pressed ? 0.85 : 1 },
      ]}>
      <View style={[styles.cover, { backgroundColor: story.coverColor }]}>
        <Text style={styles.coverEmoji}>{story.coverEmoji}</Text>
      </View>
      <View style={styles.cardBody}>
        <Text style={[styles.title, { color: colors.text }]}>{story.title}</Text>
        <Text style={[styles.meta, { color: colors.textSecondary }]}>
          {story.author} · {story.minutes} min
        </Text>
        <Text style={[styles.status, { color: statusColor }]}>{statusText}</Text>
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  profileChip: {
    marginHorizontal: Spacing.three,
    marginTop: Spacing.two,
    borderRadius: 999,
    paddingVertical: Spacing.two + 2,
    paddingHorizontal: Spacing.three,
    alignItems: 'center',
  },
  profileChipText: {
    fontSize: 15,
    fontWeight: '600',
  },
  list: {
    padding: Spacing.three,
    gap: Spacing.three,
  },
  card: {
    flexDirection: 'row',
    borderRadius: 20,
    padding: Spacing.three,
    gap: Spacing.three,
    alignItems: 'center',
  },
  cover: {
    width: 72,
    height: 72,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
  },
  coverEmoji: {
    fontSize: 36,
  },
  cardBody: {
    flex: 1,
    gap: 2,
  },
  title: {
    fontSize: 18,
    fontWeight: '700',
    fontFamily: Fonts?.serif,
  },
  meta: {
    fontSize: 13,
  },
  status: {
    fontSize: 13,
    fontWeight: '600',
    marginTop: 2,
  },
});
