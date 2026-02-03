'use client';
import { AvailableThemes } from '@/theme';
import localStorageManager from './services/localstorage-manager';

interface Settings {
    user_id?: string;
    theme: AvailableThemes;
}

interface IUserSettings {
    setTheme: (theme: AvailableThemes) => void;
    setUserId: (user_id: string) => void;
    get: () => Settings;
}

class UserSettings {
    private settings: Settings;

    constructor() {
        this.settings = { user_id: undefined, theme: 'default' };
    }

    public get(): Settings {
        return this.settings;
    }

    public setTheme(theme: AvailableThemes) {
        this.settings = { ...this.settings, theme: theme };
        this.saveLocalStorage();
    }

    public setUserId(user_id: string) {
        this.settings = { ...this.settings, user_id: user_id };
        if (this.settings.user_id !== undefined) {
            this.settings = this.loadSettings() || this.defaultSettings();
        } else {
            this.saveLocalStorage();
        }
    }

    /**
     * Case the user is not logged in, we set the user_id to 0 and the theme to default.
     */
    private defaultSettings(): Settings {
        return { user_id: '0', theme: 'default' };
    }

    private getLocalStorageKey() {
        return `user_settings_${this.settings.user_id}`;
    }

    private saveLocalStorage() {
        if (this.settings.user_id === undefined) {
            return;
        }
        localStorageManager.set<Settings>(
            this.getLocalStorageKey(),
            this.settings
        );
    }

    private loadLocalStorage(): Settings | null {
        return (
            localStorageManager.get<Settings>(this.getLocalStorageKey()) || null
        );
    }

    loadSettings(): Settings | null {
        return this.loadLocalStorage();
    }
}

export type { IUserSettings };

export default UserSettings;
