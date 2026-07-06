import { useFocusEffect, useRouter } from 'expo-router';
import { useCallback, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import { Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { characterColor, getCast } from '@/lib/cast';
import { useProfiles } from '@/lib/profiles';
import { getStory } from '@/lib/stories';
import type { CastMember } from '@/types/story';

export default function CastScreen() {
  const colors = useTheme();
  const router = useRouter();
  const { activeProfile } = useProfiles();

  const [cast, setCast] = useState<CastMember[]>([]);
  useFocusEffect(
    useCallback(() => {
      setCast(getCast(activeProfile?.id ?? null));
    }, [activeProfile?.id])
  );

  if (!activeProfile) {
    return (
      <View style={[styles.container, styles.centered, { backgroundColor: colors.background }]}>
        <Text style={[styles.introText, { color: colors.text }]}>
          Add a voice profile first — then every character you perform builds your voice cast.
        </Text>
      </View>
    );
  }

  const performed = cast.filter((m) => m.status !== 'none');
  const unperformed = cast.filter((m) => m.status === 'none');

  return (
    <ScrollView
      style={[styles.container, { backgroundColor: colors.background }]}
      contentContainerStyle={styles.content}>
      <Text style={[styles.introText, { color: colors.textSecondary }]}>
        Every character line {activeProfile.name} performs is saved with its exact words — building
        a labeled voice sample for that character. Once a character has enough lines, its voice can
        be modeled and cast in future stories. 🎭
      </Text>

      <Text style={[styles.sectionTitle, { color: colors.textSecondary }]}>
        {activeProfile.emoji} {activeProfile.name.toUpperCase()}&apos;S CAST
      </Text>

      {performed.length === 0 && (
        <Text style={[styles.emptyText, { color: colors.textSecondary }]}>
          No characters performed yet. Open a story, tap record, and give the Wolf your best growl —
          it will show up here.
        </Text>
      )}

      {performed.map((member) => (
        <CastCard key={`${member.storyId}-${member.character.id}`} member={member} />
      ))}

      {unperformed.length > 0 && (
        <>
          <Text style={[styles.sectionTitle, { color: colors.textSecondary, marginTop: Spacing.four }]}>
            ROLES WAITING TO BE PERFORMED
          </Text>
          {unperformed.map((member) => (
            <Pressable
              key={`${member.storyId}-${member.character.id}`}
              onPress={() => router.push(`/record/${member.storyId}`)}
              style={[styles.waitingRow, { backgroundColor: colors.backgroundElement }]}>
              <Text style={styles.waitingEmoji}>{member.character.emoji}</Text>
              <View style={styles.waitingBody}>
                <Text style={[styles.waitingName, { color: colors.text }]}>
                  {member.character.name}
                </Text>
                <Text style={[styles.waitingMeta, { color: colors.textSecondary }]}>
                  {member.storyTitle} · {member.totalLines}{' '}
                  {member.totalLines === 1 ? 'line' : 'lines'}
                </Text>
              </View>
              <Text style={[styles.waitingCta, { color: colors.accent }]}>Record ›</Text>
            </Pressable>
          ))}
        </>
      )}

      <Text style={[styles.privacyNote, { color: colors.textSecondary }]}>
        🔒 Voice samples stay on this device. If character voice modeling is added later, it will
        only ever happen with the voice owner&apos;s permission, and any voice model can be deleted
        at any time.
      </Text>
    </ScrollView>
  );
}

function CastCard({ member }: { member: CastMember }) {
  const colors = useTheme();
  const router = useRouter();
  const story = getStory(member.storyId);
  const color = story ? characterColor(story, member.character.id) : colors.accent;
  const complete = member.status === 'complete';

  return (
    <Pressable
      onPress={() => router.push(`/record/${member.storyId}`)}
      style={[styles.card, { backgroundColor: colors.backgroundElement }]}>
      <View style={[styles.cardEmojiWrap, { backgroundColor: color + '33' }]}>
        <Text style={styles.cardEmoji}>{member.character.emoji}</Text>
      </View>
      <View style={styles.cardBody}>
        <Text style={[styles.cardName, { color }]}>{member.character.name}</Text>
        <Text style={[styles.cardMeta, { color: colors.textSecondary }]}>
          {member.storyTitle} · {member.character.voiceHint}
        </Text>
        <Text style={[styles.cardStatus, { color: complete ? colors.recorded : colors.accent }]}>
          {complete
            ? `✨ Voice sample complete — ${member.recordedWords} words captured`
            : `${member.recordedLines} of ${member.totalLines} lines · record the rest to finish the sample`}
        </Text>
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  centered: {
    justifyContent: 'center',
  },
  content: {
    padding: Spacing.three,
    gap: Spacing.two,
  },
  introText: {
    fontSize: 14,
    lineHeight: 21,
    marginBottom: Spacing.two,
  },
  sectionTitle: {
    fontSize: 12,
    fontWeight: '700',
    letterSpacing: 0.8,
    marginBottom: Spacing.one,
  },
  emptyText: {
    fontSize: 14,
    lineHeight: 21,
  },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.three,
    padding: Spacing.three,
    borderRadius: 20,
  },
  cardEmojiWrap: {
    width: 56,
    height: 56,
    borderRadius: 28,
    alignItems: 'center',
    justifyContent: 'center',
  },
  cardEmoji: {
    fontSize: 26,
  },
  cardBody: {
    flex: 1,
    gap: 2,
  },
  cardName: {
    fontSize: 17,
    fontWeight: '700',
  },
  cardMeta: {
    fontSize: 13,
  },
  cardStatus: {
    fontSize: 13,
    fontWeight: '600',
    marginTop: 2,
  },
  waitingRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.three,
    paddingVertical: Spacing.two + 2,
    paddingHorizontal: Spacing.three,
    borderRadius: 16,
  },
  waitingEmoji: {
    fontSize: 22,
  },
  waitingBody: {
    flex: 1,
  },
  waitingName: {
    fontSize: 15,
    fontWeight: '600',
  },
  waitingMeta: {
    fontSize: 12,
  },
  waitingCta: {
    fontSize: 14,
    fontWeight: '700',
  },
  privacyNote: {
    fontSize: 13,
    lineHeight: 20,
    marginTop: Spacing.four,
  },
});
