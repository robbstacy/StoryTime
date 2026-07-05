export interface StoryPage {
  text: string;
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
  pages: StoryPage[];
}

export interface VoiceProfile {
  id: string;
  name: string;
  emoji: string;
  createdAt: number;
}

/**
 * Where a page's narration audio comes from.
 * - 'recorded': a real recording made by the voice profile's owner (always preferred)
 * - 'cloned': synthesized in the owner's voice by a TTS voice model (future)
 * - 'none': no narration available yet — the page shows as recordable
 */
export type NarrationKind = 'recorded' | 'cloned' | 'none';

export interface PageNarration {
  kind: NarrationKind;
  uri: string | null;
}

export interface StoryNarrationSummary {
  recordedPages: number;
  totalPages: number;
  status: 'complete' | 'partial' | 'none';
}
