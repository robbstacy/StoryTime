import AsyncStorage from '@react-native-async-storage/async-storage';
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react';

import type { VoiceProfile } from '@/types/story';

const STORAGE_KEY = 'bedtimecast.profiles.v1';

interface PersistedState {
  profiles: VoiceProfile[];
  activeProfileId: string | null;
}

interface ProfilesContextValue {
  loading: boolean;
  profiles: VoiceProfile[];
  activeProfile: VoiceProfile | null;
  addProfile: (name: string, emoji: string) => Promise<VoiceProfile>;
  removeProfile: (id: string) => Promise<void>;
  setActiveProfileId: (id: string) => Promise<void>;
}

const ProfilesContext = createContext<ProfilesContextValue | null>(null);

export function ProfilesProvider({ children }: { children: ReactNode }) {
  const [loading, setLoading] = useState(true);
  const [profiles, setProfiles] = useState<VoiceProfile[]>([]);
  const [activeProfileId, setActiveId] = useState<string | null>(null);

  useEffect(() => {
    AsyncStorage.getItem(STORAGE_KEY)
      .then((raw) => {
        if (raw) {
          const state: PersistedState = JSON.parse(raw);
          setProfiles(state.profiles);
          setActiveId(state.activeProfileId);
        }
      })
      .finally(() => setLoading(false));
  }, []);

  const persist = useCallback(async (next: PersistedState) => {
    setProfiles(next.profiles);
    setActiveId(next.activeProfileId);
    await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(next));
  }, []);

  const addProfile = useCallback(
    async (name: string, emoji: string) => {
      const profile: VoiceProfile = {
        id: `p-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`,
        name: name.trim(),
        emoji,
        createdAt: Date.now(),
      };
      await persist({
        profiles: [...profiles, profile],
        activeProfileId: activeProfileId ?? profile.id,
      });
      return profile;
    },
    [profiles, activeProfileId, persist]
  );

  const removeProfile = useCallback(
    async (id: string) => {
      const remaining = profiles.filter((p) => p.id !== id);
      await persist({
        profiles: remaining,
        activeProfileId: activeProfileId === id ? (remaining[0]?.id ?? null) : activeProfileId,
      });
    },
    [profiles, activeProfileId, persist]
  );

  const setActiveProfileId = useCallback(
    async (id: string) => {
      await persist({ profiles, activeProfileId: id });
    },
    [profiles, persist]
  );

  const activeProfile = profiles.find((p) => p.id === activeProfileId) ?? null;

  return (
    <ProfilesContext.Provider
      value={{ loading, profiles, activeProfile, addProfile, removeProfile, setActiveProfileId }}>
      {children}
    </ProfilesContext.Provider>
  );
}

export function useProfiles(): ProfilesContextValue {
  const value = useContext(ProfilesContext);
  if (!value) {
    throw new Error('useProfiles must be used within a ProfilesProvider');
  }
  return value;
}
