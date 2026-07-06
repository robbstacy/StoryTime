import type { Story } from '@/types/story';

import peterRabbit from '@/content/stories/the-tale-of-peter-rabbit.json';
import threeLittlePigs from '@/content/stories/the-three-little-pigs.json';
import boyWhoCriedWolf from '@/content/stories/the-boy-who-cried-wolf.json';
import bremenTownMusicians from '@/content/stories/the-bremen-town-musicians.json';
import elvesAndShoemaker from '@/content/stories/the-elves-and-the-shoemaker.json';
import frogPrince from '@/content/stories/the-frog-prince.json';
import uglyDuckling from '@/content/stories/the-ugly-duckling.json';
import princessAndPea from '@/content/stories/the-princess-and-the-pea.json';
import tomSawyerFence from '@/content/stories/tom-sawyer-whitewashes-the-fence.json';

export const STORIES: Story[] = [
  peterRabbit,
  threeLittlePigs,
  boyWhoCriedWolf,
  bremenTownMusicians,
  elvesAndShoemaker,
  frogPrince,
  uglyDuckling,
  princessAndPea,
  tomSawyerFence,
];

export function getStory(id: string): Story | undefined {
  return STORIES.find((story) => story.id === id);
}
