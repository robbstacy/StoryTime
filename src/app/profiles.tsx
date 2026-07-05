import { useState } from 'react';
import {
  Alert,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';

import { Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { useProfiles } from '@/lib/profiles';

const EMOJI_CHOICES = ['👩', '👨', '👵', '👴', '🧑', '🦸'];

export default function ProfilesScreen() {
  const colors = useTheme();
  const { profiles, activeProfile, addProfile, removeProfile, setActiveProfileId } = useProfiles();

  const [name, setName] = useState('');
  const [emoji, setEmoji] = useState(EMOJI_CHOICES[0]);

  const submit = async () => {
    if (!name.trim()) return;
    await addProfile(name, emoji);
    setName('');
    setEmoji(EMOJI_CHOICES[0]);
  };

  const confirmRemove = (id: string, profileName: string) => {
    Alert.alert(
      `Remove ${profileName}?`,
      'Their recordings will stay on this device, but no stories will play in this voice until the profile is added again.',
      [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Remove', style: 'destructive', onPress: () => removeProfile(id) },
      ]
    );
  };

  return (
    <KeyboardAvoidingView
      style={[styles.container, { backgroundColor: colors.background }]}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={[styles.sectionTitle, { color: colors.textSecondary }]}>
          WHO READS THE STORIES?
        </Text>

        {profiles.length === 0 && (
          <Text style={[styles.emptyText, { color: colors.textSecondary }]}>
            Add a voice for each person who will read — Mom, Dad, Grandma, anyone your little one
            loves to hear.
          </Text>
        )}

        {profiles.map((profile) => {
          const isActive = profile.id === activeProfile?.id;
          return (
            <Pressable
              key={profile.id}
              onPress={() => setActiveProfileId(profile.id)}
              onLongPress={() => confirmRemove(profile.id, profile.name)}
              style={[
                styles.profileRow,
                {
                  backgroundColor: isActive ? colors.backgroundSelected : colors.backgroundElement,
                  borderColor: isActive ? colors.accent : 'transparent',
                },
              ]}>
              <Text style={styles.profileEmoji}>{profile.emoji}</Text>
              <Text style={[styles.profileName, { color: colors.text }]}>{profile.name}</Text>
              {isActive && (
                <Text style={[styles.activeBadge, { color: colors.accent }]}>Reading now</Text>
              )}
            </Pressable>
          );
        })}

        {profiles.length > 0 && (
          <Text style={[styles.hint, { color: colors.textSecondary }]}>
            Tap a voice to make it the reader. Press and hold to remove it.
          </Text>
        )}

        <Text style={[styles.sectionTitle, { color: colors.textSecondary, marginTop: Spacing.four }]}>
          ADD A VOICE
        </Text>

        <View style={styles.emojiRow}>
          {EMOJI_CHOICES.map((choice) => (
            <Pressable
              key={choice}
              onPress={() => setEmoji(choice)}
              style={[
                styles.emojiChoice,
                {
                  backgroundColor:
                    choice === emoji ? colors.backgroundSelected : colors.backgroundElement,
                  borderColor: choice === emoji ? colors.accent : 'transparent',
                },
              ]}>
              <Text style={styles.emojiChoiceText}>{choice}</Text>
            </Pressable>
          ))}
        </View>

        <View style={styles.addRow}>
          <TextInput
            value={name}
            onChangeText={setName}
            placeholder="Name (e.g. Mom)"
            placeholderTextColor={colors.textSecondary}
            style={[
              styles.input,
              { backgroundColor: colors.backgroundElement, color: colors.text },
            ]}
            onSubmitEditing={submit}
            returnKeyType="done"
          />
          <Pressable
            onPress={submit}
            disabled={!name.trim()}
            style={[
              styles.addButton,
              { backgroundColor: colors.accent, opacity: name.trim() ? 1 : 0.4 },
            ]}>
            <Text style={styles.addButtonText}>Add</Text>
          </Pressable>
        </View>

        <Text style={[styles.privacyNote, { color: colors.textSecondary }]}>
          🔒 Recordings are stored only on this device. If voice cloning is added later, it will
          always ask the voice&apos;s owner for permission first, and their voice model can be
          deleted at any time.
        </Text>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  content: {
    padding: Spacing.three,
    gap: Spacing.two,
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
  profileRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.three,
    padding: Spacing.three,
    borderRadius: 16,
    borderWidth: 2,
  },
  profileEmoji: {
    fontSize: 28,
  },
  profileName: {
    flex: 1,
    fontSize: 17,
    fontWeight: '600',
  },
  activeBadge: {
    fontSize: 13,
    fontWeight: '700',
  },
  hint: {
    fontSize: 12,
    textAlign: 'center',
    marginTop: Spacing.one,
  },
  emojiRow: {
    flexDirection: 'row',
    gap: Spacing.two,
  },
  emojiChoice: {
    width: 48,
    height: 48,
    borderRadius: 24,
    borderWidth: 2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  emojiChoiceText: {
    fontSize: 24,
  },
  addRow: {
    flexDirection: 'row',
    gap: Spacing.two,
    marginTop: Spacing.one,
  },
  input: {
    flex: 1,
    borderRadius: 14,
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.two + 4,
    fontSize: 16,
  },
  addButton: {
    borderRadius: 14,
    paddingHorizontal: Spacing.four,
    justifyContent: 'center',
  },
  addButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '700',
  },
  privacyNote: {
    fontSize: 13,
    lineHeight: 20,
    marginTop: Spacing.four,
  },
});
