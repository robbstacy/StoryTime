import {
  RecordingPresets,
  requestRecordingPermissionsAsync,
  setAudioModeAsync,
  useAudioPlayer,
  useAudioRecorder,
} from 'expo-audio';
import { Stack, useLocalSearchParams } from 'expo-router';
import { useEffect, useState } from 'react';
import { Alert, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import { Fonts, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { deletePageRecording, getStoryNarration, savePageRecording } from '@/lib/narration';
import { useProfiles } from '@/lib/profiles';
import { getStory } from '@/lib/stories';

export default function RecordScreen() {
  const colors = useTheme();
  const { id, page } = useLocalSearchParams<{ id: string; page?: string }>();
  const { activeProfile } = useProfiles();
  const story = getStory(id);

  const [pageIndex, setPageIndex] = useState(() => {
    const initial = Number(page);
    return Number.isInteger(initial) && initial >= 0 ? initial : 0;
  });
  const [permissionGranted, setPermissionGranted] = useState<boolean | null>(null);
  const [recordedUris, setRecordedUris] = useState<(string | null)[]>([]);
  const [isRecording, setIsRecording] = useState(false);
  const [busy, setBusy] = useState(false);

  const recorder = useAudioRecorder(RecordingPresets.HIGH_QUALITY);
  const previewPlayer = useAudioPlayer();

  useEffect(() => {
    requestRecordingPermissionsAsync().then((response) => {
      setPermissionGranted(response.granted);
    });
  }, []);

  useEffect(() => {
    if (story && activeProfile) {
      setRecordedUris(getStoryNarration(activeProfile.id, story).map((n) => n.uri));
    }
  }, [story, activeProfile]);

  if (!story) {
    return (
      <View style={[styles.container, { backgroundColor: colors.background }]}>
        <Text style={{ color: colors.text }}>Story not found.</Text>
      </View>
    );
  }

  if (!activeProfile) {
    return (
      <View style={[styles.container, styles.centered, { backgroundColor: colors.background }]}>
        <Text style={[styles.helperText, { color: colors.text }]}>
          Add a voice profile first, so StoryTime knows whose voice this is. Go back and tap the
          chip at the top of the library.
        </Text>
      </View>
    );
  }

  const totalPages = story.pages.length;
  const recordedCount = recordedUris.filter(Boolean).length;
  const currentUri = recordedUris[pageIndex] ?? null;

  const startRecording = async () => {
    if (busy) return;
    setBusy(true);
    try {
      previewPlayer.pause();
      await setAudioModeAsync({ allowsRecording: true, playsInSilentMode: true });
      await recorder.prepareToRecordAsync();
      recorder.record();
      setIsRecording(true);
    } catch {
      Alert.alert('Recording failed', 'Could not start recording. Please try again.');
    } finally {
      setBusy(false);
    }
  };

  const stopRecording = async () => {
    if (busy) return;
    setBusy(true);
    try {
      await recorder.stop();
      setIsRecording(false);
      await setAudioModeAsync({ allowsRecording: false, playsInSilentMode: true });
      if (recorder.uri) {
        const savedUri = await savePageRecording(activeProfile.id, story.id, pageIndex, recorder.uri);
        setRecordedUris((uris) => {
          const next = [...uris];
          next[pageIndex] = savedUri;
          return next;
        });
      }
    } catch {
      Alert.alert('Saving failed', 'Could not save the recording. Please try again.');
    } finally {
      setBusy(false);
    }
  };

  const playPreview = () => {
    if (!currentUri) return;
    // Cache-bust: re-recordings reuse the same file path.
    previewPlayer.replace({ uri: currentUri });
    previewPlayer.seekTo(0);
    previewPlayer.play();
  };

  const discardRecording = () => {
    Alert.alert('Re-record this page?', 'The current take will be deleted.', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete take',
        style: 'destructive',
        onPress: () => {
          previewPlayer.pause();
          deletePageRecording(activeProfile.id, story.id, pageIndex);
          setRecordedUris((uris) => {
            const next = [...uris];
            next[pageIndex] = null;
            return next;
          });
        },
      },
    ]);
  };

  const goToPage = (index: number) => {
    if (isRecording) return;
    previewPlayer.pause();
    setPageIndex(index);
  };

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <Stack.Screen options={{ title: `Record · ${story.title}` }} />

      <Text style={[styles.progressText, { color: colors.textSecondary }]}>
        {activeProfile.emoji} {activeProfile.name} · {recordedCount} of {totalPages} pages recorded
      </Text>

      <View style={styles.dots}>
        {story.pages.map((_, index) => (
          <Pressable key={index} onPress={() => goToPage(index)} hitSlop={6}>
            <View
              style={[
                styles.dot,
                {
                  backgroundColor: recordedUris[index]
                    ? colors.recorded
                    : colors.backgroundSelected,
                  borderWidth: index === pageIndex ? 2 : 0,
                  borderColor: colors.accent,
                },
              ]}
            />
          </Pressable>
        ))}
      </View>

      <ScrollView
        style={[styles.teleprompter, { backgroundColor: colors.backgroundElement }]}
        contentContainerStyle={styles.teleprompterContent}>
        <Text style={[styles.pageLabel, { color: colors.textSecondary }]}>
          Page {pageIndex + 1} of {totalPages} — read this aloud:
        </Text>
        <Text style={[styles.pageText, { color: colors.text }]}>
          {story.pages[pageIndex].text}
        </Text>
      </ScrollView>

      {permissionGranted === false && (
        <Text style={[styles.helperText, { color: colors.record }]}>
          Microphone access is off. Enable it in Settings to record.
        </Text>
      )}

      <View style={styles.controls}>
        {currentUri && !isRecording ? (
          <Pressable onPress={playPreview} style={styles.sideButton}>
            <Text style={[styles.sideButtonText, { color: colors.accent }]}>▶ Listen</Text>
          </Pressable>
        ) : (
          <View style={styles.sideButton} />
        )}

        <Pressable
          disabled={permissionGranted === false || busy}
          onPress={isRecording ? stopRecording : startRecording}
          style={[
            styles.recordButton,
            {
              backgroundColor: isRecording ? colors.record : colors.background,
              borderColor: colors.record,
            },
          ]}>
          {isRecording ? (
            <View style={styles.stopSquare} />
          ) : (
            <View style={[styles.recordCircle, { backgroundColor: colors.record }]} />
          )}
        </Pressable>

        {currentUri && !isRecording ? (
          <Pressable onPress={discardRecording} style={styles.sideButton}>
            <Text style={[styles.sideButtonText, { color: colors.record }]}>↺ Redo</Text>
          </Pressable>
        ) : (
          <View style={styles.sideButton} />
        )}
      </View>

      <Text style={[styles.recordHint, { color: colors.textSecondary }]}>
        {isRecording
          ? 'Recording… tap to stop'
          : currentUri
            ? 'This page is saved ❤️'
            : 'Tap to start recording'}
      </Text>

      <View style={styles.pageNav}>
        <Pressable
          disabled={pageIndex === 0 || isRecording}
          onPress={() => goToPage(pageIndex - 1)}
          style={{ opacity: pageIndex === 0 || isRecording ? 0.3 : 1 }}>
          <Text style={[styles.pageNavText, { color: colors.text }]}>‹ Previous</Text>
        </Pressable>
        <Pressable
          disabled={pageIndex >= totalPages - 1 || isRecording}
          onPress={() => goToPage(pageIndex + 1)}
          style={{ opacity: pageIndex >= totalPages - 1 || isRecording ? 0.3 : 1 }}>
          <Text style={[styles.pageNavText, { color: colors.text }]}>Next ›</Text>
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: Spacing.three,
  },
  centered: {
    justifyContent: 'center',
  },
  progressText: {
    textAlign: 'center',
    fontSize: 14,
    fontWeight: '600',
  },
  dots: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: Spacing.two,
    marginVertical: Spacing.three,
  },
  dot: {
    width: 12,
    height: 12,
    borderRadius: 6,
  },
  teleprompter: {
    flex: 1,
    borderRadius: 24,
  },
  teleprompterContent: {
    padding: Spacing.four,
    gap: Spacing.three,
  },
  pageLabel: {
    fontSize: 13,
    fontWeight: '600',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  pageText: {
    fontSize: 24,
    lineHeight: 38,
    fontFamily: Fonts?.serif,
  },
  helperText: {
    textAlign: 'center',
    fontSize: 14,
    marginTop: Spacing.two,
    paddingHorizontal: Spacing.three,
  },
  controls: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.four,
    marginTop: Spacing.four,
  },
  sideButton: {
    width: 88,
    alignItems: 'center',
  },
  sideButtonText: {
    fontSize: 15,
    fontWeight: '700',
  },
  recordButton: {
    width: 84,
    height: 84,
    borderRadius: 42,
    borderWidth: 4,
    alignItems: 'center',
    justifyContent: 'center',
  },
  recordCircle: {
    width: 60,
    height: 60,
    borderRadius: 30,
  },
  stopSquare: {
    width: 30,
    height: 30,
    borderRadius: 6,
    backgroundColor: '#FFFFFF',
  },
  recordHint: {
    textAlign: 'center',
    fontSize: 13,
    marginTop: Spacing.two,
  },
  pageNav: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.three,
    marginTop: Spacing.three,
  },
  pageNavText: {
    fontSize: 16,
    fontWeight: '600',
  },
});
