import type { Story } from '@/types/story';

import peterRabbit from '@/content/stories/the-tale-of-peter-rabbit.json';
import threeLittlePigs from '@/content/stories/the-three-little-pigs.json';
import boyWhoCriedWolf from '@/content/stories/the-boy-who-cried-wolf.json';

export const STORIES: Story[] = [peterRabbit, threeLittlePigs, boyWhoCriedWolf];

export function getStory(id: string): Story | undefined {
  return STORIES.find((story) => story.id === id);
}
