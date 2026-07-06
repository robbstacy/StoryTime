import { setAudioModeAsync, useAudioPlayer, useAudioPlayerStatus } from 'expo-audio';
import { Stack, useFocusEffect, useLocalSearchParams, useRouter } from 'expo-router';
import { useCallback, useEffect, useRef, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import { Fonts, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { characterColor, getCharacter } from '@/lib/cast';
import { getStoryNarration } from '@/lib/narration';
import { useProfiles } from '@/lib/profiles';
import { getStory } from '@/lib/stories';
import type { SegmentNarration } from '@/types/story';

export default function StoryScreen() {
  const colors = useTheme();
  const router = useRouter();
  const { id } = useLocalSearchParams<{ id: string }>();
  const { activeProfile } = useProfiles();
  const story = getStory(id);

  const [pageIndex, setPageIndex] = useState(0);
  const [segIndex, setSegIndex] = useState(0);
  const [narrations, setNarrations] = useState<SegmentNarration[][]>([]);
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

  const page = story?.pages[pageIndex];
  const pageNarrations = narrations[pageIndex] ?? [];
  const isLastPage = story ? pageIndex >= story.pages.length - 1 : true;
  const pageFullyRecorded = (row?: SegmentNarration[]) =>
    !!row && row.length > 0 && row.every((n) => n.uri);

  const playSegment = useCallback(
    async (pIndex: number, sIndex: number) => {
      const target = narrations[pIndex]?.[sIndex];
      if (!target?.uri) {
        setAutoPlay(false);
        return;
      }
      await setAudioModeAsync({ allowsRecording: false, playsInSilentMode: true });
      finishHandled.current = false;
      setSegIndex(sIndex);
      player.replace({ uri: target.uri });
      player.play();
      setAutoPlay(true);
    },
    [narrations, player]
  );

  // When a segment finishes, move to the next segment — or the next page.
  useEffect(() => {
    if (!status.didJustFinish || finishHandled.current) {
      return;
    }
    finishHandled.current = true;
    if (!autoPlay || !story) {
      return;
    }
    const nextSeg = segIndex + 1;
    if (nextSeg < (story.pages[pageIndex]?.segments.length ?? 0)) {
      playSegment(pageIndex, nextSeg);
    } else if (!isLastPage) {
      const nextPage = pageIndex + 1;
      setPageIndex(nextPage);
      setSegIndex(0);
      playSegment(nextPage, 0);
    } else {
      setAutoPlay(false);
    }
  }, [status.didJustFinish, autoPlay, story, segIndex, pageIndex, isLastPage, playSegment]);

  const goToPage = (index: number) => {
    player.pause();
    setAutoPlay(false);
    setPageIndex(index);
    setSegIndex(0);
  };

  if (!story || !page) {
    return (
      <View style={[styles.container, { backgroundColor: colors.background }]}>
        <Text style={{ color: colors.text }}>Story not found.</Text>
      </View>
    );
  }

  const playing = status.playing;
  const currentSegment = page.segments[segIndex];
  const currentCharacter =
    currentSegment && currentSegment.speaker !== 'narrator'
      ? getCharacter(story, currentSegment.speaker)
      : null;
  const firstMissing = pageNarrations.findIndex((n) => !n.uri);
  const pagePlayable = pageNarrations.some((n) => n.uri);

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <Stack.Screen options={{ title: story.title }} />

      <View style={styles.badgeRow}>
        {playing && currentCharacter ? (
          <Text style={[styles.badge, { color: characterColor(story, currentCharacter.id) }]}>
            {currentCharacter.emoji} {activeProfile?.name}&apos;s {currentCharacter.name}
          </Text>
        ) : pageFullyRecorded(pageNarrations) && activeProfile ? (
          <Text style={[styles.badge, { color: colors.recorded }]}>
            ❤️ Read by {activeProfile.name}
          </Text>
        ) : (
          <Text style={[styles.badge, { color: colors.textSecondary }]}>
            {pagePlayable ? 'Some lines aren’t recorded yet' : 'This page isn’t recorded yet'}
          </Text>
        )}
      </View>

      <ScrollView
        style={[styles.pageCard, { backgroundColor: story.coverColor + '22' }]}
        contentContainerStyle={styles.pageContent}>
        <Text style={styles.pageEmoji}>{story.coverEmoji}</Text>
        <Text style={[styles.pageText, { color: colors.text }]}>
          {page.segments.map((segment, index) => {
            const isCurrent = playing && index === segIndex;
            const isDialogue = segment.speaker !== 'narrator';
            return (
              <Text
                key={index}
                style={[
                  isDialogue && { color: characterColor(story, segment.speaker), fontWeight: '600' },
                  isCurrent && styles.currentSegment,
                ]}>
                {segment.text}
                {index < page.segments.length - 1 ? ' ' : ''}
              </Text>
            );
          })}
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
                      : pageFullyRecorded(narrations[index])
                        ? colors.recorded
                        : narrations[index]?.some((n) => n.uri)
                          ? colors.accentSoft
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

        {pagePlayable ? (
          <Pressable
            onPress={() => {
              if (playing) {
                player.pause();
                setAutoPlay(false);
              } else {
                playSegment(pageIndex, pageNarrations[0]?.uri ? 0 : Math.max(firstMissing, 0));
              }
            }}
            style={[styles.playButton, { backgroundColor: colors.accent }]}>
            <Text style={styles.playButtonText}>{playing ? '⏸' : '▶'}</Text>
          </Pressable>
        ) : (
          <Pressable
            onPress={() => router.push(`/record/${story.id}?page=${pageIndex}&seg=0`)}
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

      {pagePlayable && firstMissing >= 0 && (
        <Pressable
          onPress={() => router.push(`/record/${story.id}?page=${pageIndex}&seg=${firstMissing}`)}>
          <Text style={[styles.missingLink, { color: colors.accent }]}>
            🎙 Record the missing lines on this page
          </Text>
        </Pressable>
      )}

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
  currentSegment: {
    textDecorationLine: 'underline',
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
  missingLink: {
    textAlign: 'center',
    fontSize: 14,
    fontWeight: '600',
    marginTop: Spacing.two,
  },
  pageCount: {
    textAlign: 'center',
    marginTop: Spacing.two,
    fontSize: 13,
  },
});
