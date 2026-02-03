'use client';

class LocalStorageManager {
    private prefix: string;

    constructor(prefix = 'datacentric') {
        this.prefix = prefix;
    }

    private getKey(key: string): string {
        return `${this.prefix}:${key}`;
    }

    public set<T>(key: string, value: T): void {
        try {
            const data = JSON.stringify(value).replace(/\\"/g, '"');
            localStorage.setItem(this.getKey(key), data);
        } catch (error) {
            console.error(`Error saving to localStorage: ${key}`, error);
        }
    }

    public get<T>(key: string): T | null {
        try {
            const data = localStorage.getItem(this.getKey(key));
            return data ? (JSON.parse(data) as T) : null;
        } catch (error) {
            console.error(`Error reading from localStorage: ${key}`, error);
            return null;
        }
    }

    public remove(key: string): void {
        try {
            localStorage.removeItem(this.getKey(key));
        } catch (error) {
            console.error(`Error removing from localStorage: ${key}`, error);
        }
    }

    public clear(): void {
        try {
            const keysToRemove: string[] = [];
            for (let i = 0; i < localStorage.length; i++) {
                const key = localStorage.key(i);
                if (key && key.startsWith(`${this.prefix}:`)) {
                    keysToRemove.push(key);
                }
            }

            for (const key of keysToRemove) {
                localStorage.removeItem(key);
            }
        } catch (error) {
            console.error('Error clearing localStorage', error);
        }
    }

    public exists(key: string): boolean {
        return localStorage.getItem(this.getKey(key)) !== null;
    }
}

// Export a singleton instance (or you can export the class if multiple instances are needed)
const localStorageManager = new LocalStorageManager();
export default localStorageManager;
