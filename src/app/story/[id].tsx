import {
  setAudioModeAsync,
  useAudioPlayer,
  useAudioPlayerStatus,
} from 'expo-audio';
import { Stack, useFocusEffect, useLocalSearchParams, useRouter } from 'expo-router';
import { useCallback, useEffect, useRef, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import { Fonts, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { getStoryNarration } from '@/lib/narration';
import { useProfiles } from '@/lib/profiles';
import { getStory } from '@/lib/stories';
import type { PageNarration } from '@/types/story';

export default function StoryScreen() {
  const colors = useTheme();
  const router = useRouter();
  const { id } = useLocalSearchParams<{ id: string }>();
  const { activeProfile } = useProfiles();
  const story = getStory(id);

  const [pageIndex, setPageIndex] = useState(0);
  const [narrations, setNarrations] = useState<PageNarration[]>([]);
  const [autoPlay, setAutoPlay] = useState(false);

  const player = useAudioPlayer();
  const status = useAudioPlayerStatus(player);
  const finishHandled = useRef(false);

  useFocusEffect(
    useCallback(() => {
      if (story) {
        setNarrations(getStoryNarration(activeProfile?.id ?? null, story));
      }
    }, [story, activeProfile?.id])
  );

  const narration = narrations[pageIndex] ?? { kind: 'none' as const, uri: null };
  const isLastPage = story ? pageIndex >= story.pages.length - 1 : true;

  const playPage = useCallback(
    async (index: number) => {
      const target = narrations[index];
      if (!target?.uri) {
        setAutoPlay(false);
        return;
      }
      await setAudioModeAsync({ allowsRecording: false, playsInSilentMode: true });
      finishHandled.current = false;
      player.replace({ uri: target.uri });
      player.play();
      setAutoPlay(true);
    },
    [narrations, player]
  );

  // When a page's audio finishes, turn the page and keep reading.
  useEffect(() => {
    if (!status.didJustFinish || finishHandled.current) {
      return;
    }
    finishHandled.current = true;
    if (autoPlay && !isLastPage) {
      const next = pageIndex + 1;
      setPageIndex(next);
      playPage(next);
    } else {
      setAutoPlay(false);
    }
  }, [status.didJustFinish, autoPlay, isLastPage, pageIndex, playPage]);

  const goToPage = (index: number) => {
    player.pause();
    setAutoPlay(false);
    setPageIndex(index);
  };

  if (!story) {
    return (
      <View style={[styles.container, { backgroundColor: colors.background }]}>
        <Text style={{ color: colors.text }}>Story not found.</Text>
      </View>
    );
  }

  const playing = status.playing;

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <Stack.Screen options={{ title: story.title }} />

      <View style={styles.badgeRow}>
        {narration.kind === 'recorded' && activeProfile ? (
          <Text style={[styles.badge, { color: colors.recorded }]}>
            ❤️ Read by {activeProfile.name}
          </Text>
        ) : narration.kind === 'cloned' && activeProfile ? (
          <Text style={[styles.badge, { color: colors.cloned }]}>
            ✨ {activeProfile.name}&apos;s voice
          </Text>
        ) : (
          <Text style={[styles.badge, { color: colors.textSecondary }]}>
            This page isn&apos;t recorded yet
          </Text>
        )}
      </View>

      <ScrollView
        style={[styles.pageCard, { backgroundColor: story.coverColor + '22' }]}
        contentContainerStyle={styles.pageContent}>
        <Text style={styles.pageEmoji}>{story.coverEmoji}</Text>
        <Text style={[styles.pageText, { color: colors.text }]}>
          {story.pages[pageIndex].text}
        </Text>
      </ScrollView>

      <View style={styles.dots}>
        {story.pages.map((_, index) => (
          <Pressable key={index} onPress={() => goToPage(index)} hitSlop={6}>
            <View
              style={[
                styles.dot,
                {
                  backgroundColor:
                    index === pageIndex
                      ? colors.accent
                      : narrations[index]?.kind === 'recorded'
                        ? colors.recorded
                        : colors.backgroundSelected,
                },
              ]}
            />
          </Pressable>
        ))}
      </View>

      <View style={styles.controls}>
        <Pressable
          disabled={pageIndex === 0}
          onPress={() => goToPage(pageIndex - 1)}
          style={[styles.navButton, { opacity: pageIndex === 0 ? 0.3 : 1 }]}>
          <Text style={[styles.navButtonText, { color: colors.text }]}>‹</Text>
        </Pressable>

        {narration.uri ? (
          <Pressable
            onPress={() => {
              if (playing) {
                player.pause();
                setAutoPlay(false);
              } else {
                playPage(pageIndex);
              }
            }}
            style={[styles.playButton, { backgroundColor: colors.accent }]}>
            <Text style={styles.playButtonText}>{playing ? '⏸' : '▶'}</Text>
          </Pressable>
        ) : (
          <Pressable
            onPress={() => router.push(`/record/${story.id}?page=${pageIndex}`)}
            style={[styles.recordCta, { backgroundColor: colors.accentSoft }]}>
            <Text style={[styles.recordCtaText, { color: colors.accent }]}>
              🎙 Record this page
            </Text>
          </Pressable>
        )}

        <Pressable
          disabled={isLastPage}
          onPress={() => goToPage(pageIndex + 1)}
          style={[styles.navButton, { opacity: isLastPage ? 0.3 : 1 }]}>
          <Text style={[styles.navButtonText, { color: colors.text }]}>›</Text>
        </Pressable>
      </View>

      <Text style={[styles.pageCount, { color: colors.textSecondary }]}>
        Page {pageIndex + 1} of {story.pages.length}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: Spacing.three,
  },
  badgeRow: {
    alignItems: 'center',
    marginBottom: Spacing.two,
  },
  badge: {
    fontSize: 14,
    fontWeight: '600',
  },
  pageCard: {
    flex: 1,
    borderRadius: 24,
  },
  pageContent: {
    padding: Spacing.four,
    alignItems: 'center',
    gap: Spacing.three,
  },
  pageEmoji: {
    fontSize: 44,
  },
  pageText: {
    fontSize: 22,
    lineHeight: 34,
    fontFamily: Fonts?.serif,
    textAlign: 'center',
  },
  dots: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: Spacing.two,
    marginTop: Spacing.three,
  },
  dot: {
    width: 10,
    height: 10,
    borderRadius: 5,
  },
  controls: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.four,
    marginTop: Spacing.three,
  },
  navButton: {
    width: 52,
    height: 52,
    alignItems: 'center',
    justifyContent: 'center',
  },
  navButtonText: {
    fontSize: 36,
    lineHeight: 40,
  },
  playButton: {
    width: 76,
    height: 76,
    borderRadius: 38,
    alignItems: 'center',
    justifyContent: 'center',
  },
  playButtonText: {
    fontSize: 30,
    color: '#FFFFFF',
  },
  recordCta: {
    paddingVertical: Spacing.three,
    paddingHorizontal: Spacing.four,
    borderRadius: 999,
  },
  recordCtaText: {
    fontSize: 16,
    fontWeight: '700',
  },
  pageCount: {
    textAlign: 'center',
    marginTop: Spacing.two,
    fontSize: 13,
  },
});
