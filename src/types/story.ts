/**
 * A run of text spoken by one voice. `speaker` is 'narrator' or the id of a
 * StoryCharacter. Segments are the unit of recording and playback — and every
 * recorded character segment doubles as a labeled (text, audio, character)
 * sample for future voice modeling.
 */
export interface StorySegment {
  speaker: string;
  text: string;
}

export interface StoryCharacter {
  id: string;
  name: string;
  emoji: string;
  /** How the parent should perform it, e.g. "big, gruff, growly voice". */
  voiceHint: string;
}

export interface StoryPage {
  segments: StorySegment[];
}

export interface Story {
  id: string;
  title: string;
  author: string;
  year: number;
  source: string;
  coverEmoji: string;
  coverColor: string;
  minutes: number;
  characters: StoryCharacter[];
  pages: StoryPage[];
}

export interface VoiceProfile {
  id: string;
  name: string;
  emoji: string;
  createdAt: number;
}

/**
 * Where a segment's narration audio comes from.
 * - 'recorded': a real recording made by the voice profile's owner (always preferred)
 * - 'cloned': synthesized by a voice model — the profile's narrator voice or one
 *   of their performed character voices (future)
 * - 'none': no narration available yet — the segment shows as recordable
 */
export type NarrationKind = 'recorded' | 'cloned' | 'none';

export interface SegmentNarration {
  kind: NarrationKind;
  uri: string | null;
}

export interface StoryNarrationSummary {
  recorded: number;
  total: number;
  status: 'complete' | 'partial' | 'none';
}

/**
 * One character as performed by one voice profile in one story — the unit of
 * the "voice cast". Recorded lines are labeled voice-model training samples.
 */
export interface CastMember {
  storyId: string;
  storyTitle: string;
  character: StoryCharacter;
  totalLines: number;
  recordedLines: number;
  /** Word count of recorded lines — a rough proxy for sample richness. */
  recordedWords: number;
  status: 'complete' | 'partial' | 'none';
}
