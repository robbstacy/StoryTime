/**
 * Below are the colors that are used in the app. The colors are defined in the light and dark mode.
 * There are many other ways to style your app. For example, [Nativewind](https://www.nativewind.dev/), [Tamagui](https://tamagui.dev/), [unistyles](https://reactnativeunistyles.vercel.app), etc.
 */

import { Platform } from 'react-native';

export const Colors = {
  light: {
    text: '#2D2A32',
    background: '#FFF8F0',
    backgroundElement: '#F6EDE2',
    backgroundSelected: '#EFE2D2',
    textSecondary: '#6E6659',
    accent: '#E07856',
    accentSoft: '#F9DFD4',
    recorded: '#C4536A',
    cloned: '#7C6FB0',
    record: '#D64545',
  },
  dark: {
    text: '#F2EDE6',
    background: '#1A1B2E',
    backgroundElement: '#25263C',
    backgroundSelected: '#31324C',
    textSecondary: '#A8A5B8',
    accent: '#F2955F',
    accentSoft: '#3A2E33',
    recorded: '#E58BA0',
    cloned: '#A99BE0',
    record: '#E05A5A',
  },
} as const;

export type ThemeColor = keyof typeof Colors.light & keyof typeof Colors.dark;

export const Fonts = Platform.select({
  ios: {
    /** iOS `UIFontDescriptorSystemDesignDefault` */
    sans: 'system-ui',
    /** iOS `UIFontDescriptorSystemDesignSerif` */
    serif: 'ui-serif',
    /** iOS `UIFontDescriptorSystemDesignRounded` */
    rounded: 'ui-rounded',
    /** iOS `UIFontDescriptorSystemDesignMonospaced` */
    mono: 'ui-monospace',
  },
  default: {
    sans: 'normal',
    serif: 'serif',
    rounded: 'normal',
    mono: 'monospace',
  },
  web: {
    sans: 'var(--font-display)',
    serif: 'var(--font-serif)',
    rounded: 'var(--font-rounded)',
    mono: 'var(--font-mono)',
  },
});

export const Spacing = {
  half: 2,
  one: 4,
  two: 8,
  three: 16,
  four: 24,
  five: 32,
  six: 64,
} as const;

export const BottomTabInset = Platform.select({ ios: 50, android: 80 }) ?? 0;
export const MaxContentWidth = 800;
