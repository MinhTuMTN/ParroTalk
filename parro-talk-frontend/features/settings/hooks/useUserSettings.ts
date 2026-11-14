"use client";

import { useCallback, useEffect, useSyncExternalStore } from "react";
import { AUTH_CHANGED_EVENT } from "@/features/auth/hooks/useAuth";
import {
  normalizeUserSettings,
  settingsService,
} from "../services/settingsService";
import {
  DEFAULT_USER_SETTINGS,
  ThemePreference,
  UserSettings,
} from "../types";

export const USER_SETTINGS_STORAGE_KEY = "parrotalk_user_settings";
export const VIDEO_LOOP_STORAGE_KEY = "parrotalk_video_loop";
export const SETTINGS_UPDATED_EVENT = "parrotalk:settings-updated";

export interface SettingsStoreSnapshot {
  settings: UserSettings;
  isLoading: boolean;
  isSaving: boolean;
  isLoaded: boolean;
  syncError: boolean;
}

const SERVER_SNAPSHOT: SettingsStoreSnapshot = {
  settings: DEFAULT_USER_SETTINGS,
  isLoading: false,
  isSaving: false,
  isLoaded: false,
  syncError: false,
};

const listeners = new Set<() => void>();
let clientSnapshot: SettingsStoreSnapshot | null = null;
let listenersAttached = false;
let dispatchingInternally = false;

let inFlightFetch: Promise<UserSettings> | null = null;
let inFlightFetchToken: string | null = null;
let lastSyncedToken: string | null = null;
let localMutationVersion = 0;

let debounceTimer: ReturnType<typeof setTimeout> | null = null;
let pendingPatch: Partial<UserSettings> = {};
let pendingResolvers: Array<(value: UserSettings) => void> = [];

function emitChange() {
  for (const listener of listeners) {
    listener();
  }
}

export function resolveEffectiveDarkMode(theme: ThemePreference): boolean {
  if (theme === "dark") {
    return true;
  }
  if (theme === "light") {
    return false;
  }
  if (typeof window !== "undefined" && typeof window.matchMedia === "function") {
    return window.matchMedia("(prefers-color-scheme: dark)").matches;
  }
  return false;
}

export function applyThemePreference(theme: ThemePreference): void {
  if (typeof document === "undefined") {
    return;
  }
  const isDark = resolveEffectiveDarkMode(theme);
  const root = document.documentElement;
  root.classList.toggle("dark", isDark);
  root.style.colorScheme = isDark ? "dark" : "light";
}

function readLegacyVideoLoop(): boolean | undefined {
  if (typeof window === "undefined") {
    return undefined;
  }
  try {
    const raw = window.localStorage.getItem(VIDEO_LOOP_STORAGE_KEY);
    if (raw === null) {
      return undefined;
    }
    const parsed: unknown = JSON.parse(raw);
    return typeof parsed === "boolean" ? parsed : undefined;
  } catch {
    return undefined;
  }
}

function getAuthToken(): string | null {
  if (typeof window === "undefined") {
    return null;
  }
  return window.localStorage.getItem("token");
}

function clearLocalSettingsStorage(): void {
  if (typeof window === "undefined") {
    return;
  }
  try {
    window.localStorage.removeItem(USER_SETTINGS_STORAGE_KEY);
    window.localStorage.removeItem(VIDEO_LOOP_STORAGE_KEY);
  } catch {
    // Ignore storage access errors
  }
}

export function getStoredUserSettings(): UserSettings {
  if (typeof window === "undefined") {
    return { ...DEFAULT_USER_SETTINGS };
  }

  try {
    const raw = window.localStorage.getItem(USER_SETTINGS_STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as Partial<UserSettings>;
      return normalizeUserSettings(parsed);
    }
  } catch {
    // Fallback to defaults below
  }

  const legacyLoop = readLegacyVideoLoop();
  if (typeof legacyLoop === "boolean") {
    return normalizeUserSettings({ dictationAutoReplay: legacyLoop });
  }

  return { ...DEFAULT_USER_SETTINGS };
}

function updateSnapshot(partial: Partial<SettingsStoreSnapshot>) {
  const token = getAuthToken();
  const base = clientSnapshot ?? {
    settings: getStoredUserSettings(),
    isLoading: Boolean(token && lastSyncedToken !== token),
    isSaving: false,
    isLoaded: true,
    syncError: false,
  };
  clientSnapshot = { ...base, ...partial };
  emitChange();
}

export function saveStoredUserSettings(next: UserSettings): UserSettings {
  const normalized = normalizeUserSettings(next);

  if (typeof window !== "undefined") {
    try {
      window.localStorage.setItem(
        USER_SETTINGS_STORAGE_KEY,
        JSON.stringify(normalized)
      );
      window.localStorage.setItem(
        VIDEO_LOOP_STORAGE_KEY,
        JSON.stringify(normalized.dictationAutoReplay)
      );
    } catch {
      // Ignore storage quota / private-browsing errors
    }

    applyThemePreference(normalized.theme);
    updateSnapshot({ settings: normalized, isLoaded: true });

    dispatchingInternally = true;
    try {
      window.dispatchEvent(
        new CustomEvent<UserSettings>(SETTINGS_UPDATED_EVENT, {
          detail: normalized,
        })
      );
    } finally {
      dispatchingInternally = false;
    }
  }

  return normalized;
}

async function fetchSettingsFromServer(force = false): Promise<UserSettings> {
  const token = getAuthToken();
  if (!token) {
    const stored = getStoredUserSettings();
    updateSnapshot({ settings: stored, isLoading: false, isLoaded: true });
    return stored;
  }

  if (!force && lastSyncedToken === token) {
    return ensureClientSnapshot().settings;
  }

  if (!force && inFlightFetch && inFlightFetchToken === token) {
    return inFlightFetch;
  }

  const versionAtStart = localMutationVersion;
  updateSnapshot({ isLoading: true });

  inFlightFetchToken = token;
  const currentPromise = settingsService
    .getMySettings()
    .then((remoteSettings) => {
      if (getAuthToken() !== token) {
        return ensureClientSnapshot().settings;
      }
      lastSyncedToken = token;
      if (localMutationVersion === versionAtStart) {
        saveStoredUserSettings(remoteSettings);
        updateSnapshot({ isLoading: false, syncError: false });
        return remoteSettings;
      }
      updateSnapshot({ isLoading: false });
      return ensureClientSnapshot().settings;
    })
    .catch(() => {
      if (getAuthToken() === token) {
        lastSyncedToken = token;
        updateSnapshot({ isLoading: false });
      }
      return ensureClientSnapshot().settings;
    })
    .finally(() => {
      if (inFlightFetch === currentPromise) {
        inFlightFetch = null;
        inFlightFetchToken = null;
      }
    });

  inFlightFetch = currentPromise;
  return currentPromise;
}

function resetAuthScopedState(clearStorageOnEmptyToken = true) {
  if (debounceTimer) {
    clearTimeout(debounceTimer);
    debounceTimer = null;
  }
  pendingPatch = {};
  const staleResolvers = pendingResolvers;
  pendingResolvers = [];

  inFlightFetch = null;
  inFlightFetchToken = null;
  lastSyncedToken = null;
  localMutationVersion += 1;

  const token = getAuthToken();
  if (!token) {
    if (clearStorageOnEmptyToken) {
      clearLocalSettingsStorage();
    }
    const defaults = { ...DEFAULT_USER_SETTINGS };
    clientSnapshot = {
      settings: defaults,
      isLoading: false,
      isSaving: false,
      isLoaded: true,
      syncError: false,
    };
    applyThemePreference(defaults.theme);
    emitChange();
    staleResolvers.forEach((resolve) => resolve(defaults));
    return;
  }

  const stored = getStoredUserSettings();
  clientSnapshot = {
    settings: stored,
    isLoading: true,
    isSaving: false,
    isLoaded: true,
    syncError: false,
  };
  applyThemePreference(stored.theme);
  emitChange();
  staleResolvers.forEach((resolve) => resolve(stored));
  void fetchSettingsFromServer(true);
}

function attachWindowListeners() {
  if (listenersAttached || typeof window === "undefined") {
    return;
  }
  listenersAttached = true;

  window.addEventListener(AUTH_CHANGED_EVENT, () => {
    resetAuthScopedState(true);
  });

  window.addEventListener(SETTINGS_UPDATED_EVENT, (event: Event) => {
    if (dispatchingInternally) {
      return;
    }
    const custom = event as CustomEvent<UserSettings>;
    const next = custom.detail
      ? normalizeUserSettings(custom.detail)
      : getStoredUserSettings();
    applyThemePreference(next.theme);
    updateSnapshot({ settings: next, isLoaded: true });
  });

  window.addEventListener("storage", (event: StorageEvent) => {
    if (event.key === "token") {
      if (event.newValue !== event.oldValue) {
        resetAuthScopedState(!event.newValue);
      }
      return;
    }

    if (
      event.key === USER_SETTINGS_STORAGE_KEY ||
      event.key === VIDEO_LOOP_STORAGE_KEY ||
      event.key === null
    ) {
      const next =
        event.key === VIDEO_LOOP_STORAGE_KEY
          ? normalizeUserSettings({
              ...(clientSnapshot?.settings ?? getStoredUserSettings()),
              dictationAutoReplay: readLegacyVideoLoop() ?? false,
            })
          : getStoredUserSettings();
      applyThemePreference(next.theme);
      updateSnapshot({ settings: next, isLoaded: true });
    }
  });
}

function ensureClientSnapshot(): SettingsStoreSnapshot {
  if (typeof window === "undefined") {
    return SERVER_SNAPSHOT;
  }
  if (!clientSnapshot) {
    const token = getAuthToken();
    const stored = getStoredUserSettings();
    clientSnapshot = {
      settings: stored,
      isLoading: Boolean(token && lastSyncedToken !== token),
      isSaving: false,
      isLoaded: true,
      syncError: false,
    };
  }
  return clientSnapshot;
}

function subscribe(listener: () => void): () => void {
  attachWindowListeners();
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

function getSnapshot(): SettingsStoreSnapshot {
  return ensureClientSnapshot();
}

function getServerSnapshot(): SettingsStoreSnapshot {
  return SERVER_SNAPSHOT;
}

function flushPendingPatch() {
  debounceTimer = null;
  const patchToSend = { ...pendingPatch };
  const resolversToNotify = pendingResolvers;
  pendingPatch = {};
  pendingResolvers = [];

  const token = getAuthToken();
  if (!token) {
    const current = ensureClientSnapshot().settings;
    updateSnapshot({ isSaving: false, syncError: false });
    resolversToNotify.forEach((resolve) => resolve(current));
    return;
  }

  const versionAtSend = localMutationVersion;

  settingsService
    .updateMySettings(patchToSend)
    .then((serverSettings) => {
      if (getAuthToken() !== token) {
        const current = ensureClientSnapshot().settings;
        resolversToNotify.forEach((resolve) => resolve(current));
        return;
      }
      if (localMutationVersion === versionAtSend) {
        saveStoredUserSettings(serverSettings);
      }
      updateSnapshot({ isSaving: false, syncError: false });
      const resolved =
        localMutationVersion === versionAtSend
          ? serverSettings
          : ensureClientSnapshot().settings;
      resolversToNotify.forEach((resolve) => resolve(resolved));
    })
    .catch(() => {
      const current = ensureClientSnapshot().settings;
      if (getAuthToken() === token) {
        updateSnapshot({ isSaving: false, syncError: true });
      }
      resolversToNotify.forEach((resolve) => resolve(current));
    });
}

export function useUserSettings() {
  const snapshot = useSyncExternalStore(
    subscribe,
    getSnapshot,
    getServerSnapshot
  );

  useEffect(() => {
    const token = getAuthToken();
    if (!token) {
      if (lastSyncedToken !== null) {
        resetAuthScopedState(true);
      }
      return;
    }
    if (lastSyncedToken !== token) {
      void fetchSettingsFromServer(false);
    }
  }, []);

  const refreshSettings = useCallback(async () => {
    return fetchSettingsFromServer(true);
  }, []);

  const updateSettings = useCallback(
    (partial: Partial<UserSettings>): Promise<UserSettings> => {
      const current = ensureClientSnapshot().settings;
      const next = normalizeUserSettings({ ...current, ...partial });

      localMutationVersion += 1;
      saveStoredUserSettings(next);

      const token = getAuthToken();
      if (!token) {
        updateSnapshot({ isSaving: false, syncError: false });
        return Promise.resolve(next);
      }

      pendingPatch = { ...pendingPatch, ...partial };
      updateSnapshot({ isSaving: true, syncError: false });

      if (debounceTimer) {
        clearTimeout(debounceTimer);
      }

      return new Promise<UserSettings>((resolve) => {
        pendingResolvers.push(resolve);
        debounceTimer = setTimeout(flushPendingPatch, 250);
      });
    },
    []
  );

  const resetSettings = useCallback((): Promise<UserSettings> => {
    return updateSettings({ ...DEFAULT_USER_SETTINGS });
  }, [updateSettings]);

  return {
    settings: snapshot.settings,
    isLoading: snapshot.isLoading,
    isSaving: snapshot.isSaving,
    isLoaded: snapshot.isLoaded,
    syncError: snapshot.syncError,
    updateSettings,
    resetSettings,
    refreshSettings,
  };
}
