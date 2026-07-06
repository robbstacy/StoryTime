import { getSegmentNarration } from '@/lib/narration';
import { STORIES } from '@/lib/stories';
import type { CastMember, Story, StoryCharacter } from '@/types/story';

/**
 * The voice cast: every character a voice profile has performed.
 *
 * Each recorded character segment is a labeled training sample — audio plus
 * its exact text plus the character identity. Once a character has enough
 * recorded lines, its samples can train a dedicated voice model (v2), letting
 * future stories cast e.g. "Papa's Wolf" for any gruff villain role. Until
 * then, this library tracks how rich each character's sample set is.
 */

const CHARACTER_COLORS = ['#C4536A', '#7C6FB0', '#3E8E7E', '#B8860B', '#5B7DB1', '#A0623F'];

/** Stable display color for a character within its story. */
export function characterColor(story: Story, speakerId: string): string {
  const index = story.characters.findIndex((c) => c.id === speakerId);
  return CHARACTER_COLORS[Math.max(index, 0) % CHARACTER_COLORS.length];
}

export function getCharacter(story: Story, speakerId: string): StoryCharacter | undefined {
  return story.characters.find((c) => c.id === speakerId);
}

function countWords(text: string): number {
  return text.split(/\s+/).filter(Boolean).length;
}

/** All characters the profile could perform, with recording coverage. */
export function getCast(profileId: string | null): CastMember[] {
  if (!profileId) {
    return [];
  }
  const cast: CastMember[] = [];
  for (const story of STORIES) {
    for (const character of story.characters) {
      let totalLines = 0;
      let recordedLines = 0;
      let recordedWords = 0;
      story.pages.forEach((page, pageIndex) => {
        page.segments.forEach((segment, segIndex) => {
          if (segment.speaker !== character.id) {
            return;
          }
          totalLines += 1;
          const narration = getSegmentNarration(profileId, story.id, pageIndex, segIndex);
          if (narration.kind === 'recorded') {
            recordedLines += 1;
            recordedWords += countWords(segment.text);
          }
        });
      });
      if (totalLines === 0) {
        continue;
      }
      cast.push({
        storyId: story.id,
        storyTitle: story.title,
        character,
        totalLines,
        recordedLines,
        recordedWords,
        status:
          recordedLines === 0 ? 'none' : recordedLines === totalLines ? 'complete' : 'partial',
      });
    }
  }
  return cast;
}
