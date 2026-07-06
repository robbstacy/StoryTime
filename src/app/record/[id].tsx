import {
  RecordingPresets,
  requestRecordingPermissionsAsync,
  setAudioModeAsync,
  useAudioPlayer,
  useAudioRecorder,
} from 'expo-audio';
import { Stack, useLocalSearchParams } from 'expo-router';
import { useEffect, useMemo, useState } from 'react';
import { Alert, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import { Fonts, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { characterColor, getCharacter } from '@/lib/cast';
import {
  deleteSegmentRecording,
  getStoryNarration,
  saveSegmentRecording,
} from '@/lib/narration';
import { useProfiles } from '@/lib/profiles';
import { getStory } from '@/lib/stories';

interface ScriptLine {
  pageIndex: number;
  segIndex: number;
  speaker: string;
  text: string;
}

export default function RecordScreen() {
  const colors = useTheme();
  const { id, page, seg } = useLocalSearchParams<{ id: string; page?: string; seg?: string }>();
  const { activeProfile } = useProfiles();
  const story = getStory(id);

  // The story flattened into a script: one line per segment, in reading order.
  const script = useMemo<ScriptLine[]>(
    () =>
      story
        ? story.pages.flatMap((p, pageIndex) =>
            p.segments.map((s, segIndex) => ({ pageIndex, segIndex, ...s }))
          )
        : [],
    [story]
  );

  const [lineIndex, setLineIndex] = useState(() => {
    const targetPage = Number(page);
    const targetSeg = Number(seg);
    const found = script.findIndex(
      (l) => l.pageIndex === targetPage && l.segIndex === (Number.isInteger(targetSeg) ? targetSeg : 0)
    );
    return found >= 0 ? found : 0;
  });
  const [permissionGranted, setPermissionGranted] = useState<boolean | null>(null);
  const [recordedUris, setRecordedUris] = useState<(string | null)[][]>([]);
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
      setRecordedUris(
        getStoryNarration(activeProfile.id, story).map((pageRow) => pageRow.map((n) => n.uri))
      );
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
          Add a voice profile first, so BedtimeCast knows whose voice this is. Go back and tap the
          chip at the top of the library.
        </Text>
      </View>
    );
  }

  const line = script[lineIndex];
  const character = line.speaker === 'narrator' ? null : getCharacter(story, line.speaker);
  const speakerColor =
    line.speaker === 'narrator' ? colors.textSecondary : characterColor(story, line.speaker);
  const totalLines = script.length;
  const recordedCount = recordedUris.flat().filter(Boolean).length;
  const currentUri = recordedUris[line.pageIndex]?.[line.segIndex] ?? null;

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
        const savedUri = await saveSegmentRecording(
          activeProfile.id,
          story.id,
          line.pageIndex,
          line.segIndex,
          recorder.uri
        );
        setRecordedUris((rows) => {
          const next = rows.map((row) => [...row]);
          next[line.pageIndex][line.segIndex] = savedUri;
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
    previewPlayer.replace({ uri: currentUri });
    previewPlayer.seekTo(0);
    previewPlayer.play();
  };

  const discardRecording = () => {
    Alert.alert('Re-record this line?', 'The current take will be deleted.', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete take',
        style: 'destructive',
        onPress: () => {
          previewPlayer.pause();
          deleteSegmentRecording(activeProfile.id, story.id, line.pageIndex, line.segIndex);
          setRecordedUris((rows) => {
            const next = rows.map((row) => [...row]);
            next[line.pageIndex][line.segIndex] = null;
            return next;
          });
        },
      },
    ]);
  };

  const goToLine = (index: number) => {
    if (isRecording) return;
    previewPlayer.pause();
    setLineIndex(index);
  };

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <Stack.Screen options={{ title: `Record · ${story.title}` }} />

      <Text style={[styles.progressText, { color: colors.textSecondary }]}>
        {activeProfile.emoji} {activeProfile.name} · {recordedCount} of {totalLines} lines recorded
      </Text>

      <View style={[styles.progressTrack, { backgroundColor: colors.backgroundSelected }]}>
        <View
          style={[
            styles.progressFill,
            { backgroundColor: colors.recorded, width: `${(recordedCount / totalLines) * 100}%` },
          ]}
        />
      </View>

      <View style={[styles.speakerChip, { backgroundColor: colors.backgroundElement }]}>
        {character ? (
          <>
            <Text style={styles.speakerEmoji}>{character.emoji}</Text>
            <View style={styles.speakerLabels}>
              <Text style={[styles.speakerName, { color: speakerColor }]}>{character.name}</Text>
              <Text style={[styles.speakerHint, { color: colors.textSecondary }]}>
                🎭 Perform it in {character.voiceHint}!
              </Text>
            </View>
          </>
        ) : (
          <>
            <Text style={styles.speakerEmoji}>📖</Text>
            <View style={styles.speakerLabels}>
              <Text style={[styles.speakerName, { color: colors.text }]}>Narrator</Text>
              <Text style={[styles.speakerHint, { color: colors.textSecondary }]}>
                Your own storytelling voice
              </Text>
            </View>
          </>
        )}
      </View>

      <ScrollView
        style={[styles.teleprompter, { backgroundColor: colors.backgroundElement }]}
        contentContainerStyle={styles.teleprompterContent}>
        <Text style={[styles.pageLabel, { color: colors.textSecondary }]}>
          Page {line.pageIndex + 1} · Line {lineIndex + 1} of {totalLines} — read this aloud:
        </Text>
        <Text style={[styles.lineText, { color: character ? speakerColor : colors.text }]}>
          {line.text}
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
            ? 'This line is saved ❤️'
            : 'Tap to start recording'}
      </Text>

      <View style={styles.pageNav}>
        <Pressable
          disabled={lineIndex === 0 || isRecording}
          onPress={() => goToLine(lineIndex - 1)}
          style={{ opacity: lineIndex === 0 || isRecording ? 0.3 : 1 }}>
          <Text style={[styles.pageNavText, { color: colors.text }]}>‹ Previous</Text>
        </Pressable>
        <Pressable
          disabled={lineIndex >= totalLines - 1 || isRecording}
          onPress={() => goToLine(lineIndex + 1)}
          style={{ opacity: lineIndex >= totalLines - 1 || isRecording ? 0.3 : 1 }}>
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
  progressTrack: {
    height: 6,
    borderRadius: 3,
    marginTop: Spacing.two,
    marginBottom: Spacing.three,
    overflow: 'hidden',
  },
  progressFill: {
    height: '100%',
    borderRadius: 3,
  },
  speakerChip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two + 2,
    borderRadius: 16,
    paddingVertical: Spacing.two,
    paddingHorizontal: Spacing.three,
    marginBottom: Spacing.two,
  },
  speakerEmoji: {
    fontSize: 26,
  },
  speakerLabels: {
    flex: 1,
  },
  speakerName: {
    fontSize: 16,
    fontWeight: '700',
  },
  speakerHint: {
    fontSize: 13,
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
  lineText: {
    fontSize: 26,
    lineHeight: 40,
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
