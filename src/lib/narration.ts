import { Directory, File, Paths } from 'expo-file-system';

import type { SegmentNarration, Story, StoryNarrationSummary } from '@/types/story';

/**
 * Narration resolution — the heart of the hybrid recorded/cloned model.
 *
 * The unit of audio is the SEGMENT: a run of text spoken by one voice
 * (the narrator or a character). Each segment slot is filled by, in order:
 *   1. a real recording made by the profile owner  → kind 'recorded'
 *   2. cloned TTS in the matching voice (future)   → kind 'cloned'
 *   3. nothing yet                                 → kind 'none'
 *
 * Because every character segment is recorded against known text and a known
 * character, recordings double as labeled training samples for future
 * character voice models (see lib/cast.ts).
 *
 * Recordings live on-device under:
 *   <documents>/recordings/<profileId>/<storyId>/p<page>-s<segment>.m4a
 */

function storyDir(profileId: string, storyId: string): Directory {
  return new Directory(Paths.document, 'recordings', profileId, storyId);
}

export function segmentRecordingFile(
  profileId: string,
  storyId: string,
  pageIndex: number,
  segIndex: number
): File {
  return new File(storyDir(profileId, storyId), `p${pageIndex}-s${segIndex}.m4a`);
}

export async function saveSegmentRecording(
  profileId: string,
  storyId: string,
  pageIndex: number,
  segIndex: number,
  sourceUri: string
): Promise<string> {
  const dir = storyDir(profileId, storyId);
  dir.create({ intermediates: true, idempotent: true });

  const dest = segmentRecordingFile(profileId, storyId, pageIndex, segIndex);
  if (dest.exists) {
    dest.delete();
  }
  await new File(sourceUri).copy(dest);
  return dest.uri;
}

export function deleteSegmentRecording(
  profileId: string,
  storyId: string,
  pageIndex: number,
  segIndex: number
): void {
  const file = segmentRecordingFile(profileId, storyId, pageIndex, segIndex);
  if (file.exists) {
    file.delete();
  }
}

export function getSegmentNarration(
  profileId: string | null,
  storyId: string,
  pageIndex: number,
  segIndex: number
): SegmentNarration {
  if (!profileId) {
    return { kind: 'none', uri: null };
  }
  const recording = segmentRecordingFile(profileId, storyId, pageIndex, segIndex);
  if (recording.exists) {
    return { kind: 'recorded', uri: recording.uri };
  }
  // Future: fall back to cloned TTS — the narrator voice model for narrator
  // segments, or the matching character voice model from the profile's cast —
  // returning { kind: 'cloned', uri } so the UI can label the source.
  return { kind: 'none', uri: null };
}

/** Per page, per segment. */
export function getStoryNarration(profileId: string | null, story: Story): SegmentNarration[][] {
  return story.pages.map((page, pageIndex) =>
    page.segments.map((_, segIndex) => getSegmentNarration(profileId, story.id, pageIndex, segIndex))
  );
}

export function summarizeStory(profileId: string | null, story: Story): StoryNarrationSummary {
  const narrations = getStoryNarration(profileId, story).flat();
  const recorded = narrations.filter((n) => n.kind === 'recorded').length;
  const total = narrations.length;
  return {
    recorded,
    total,
    status: recorded === 0 ? 'none' : recorded === total ? 'complete' : 'partial',
  };
}
