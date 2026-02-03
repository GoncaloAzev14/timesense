// utils/CookieManager.ts

export class CookieManager {
    /**
     * Get a cookie value by name
     * @param name - The name of the cookie
     * @returns The cookie value or null if not found
     */
    static get(name: string): string | null {
        if (typeof document === 'undefined') return null;
        const match = document.cookie.match(`(^|;)\\s*${name}=([^;]+)`);
        return match ? decodeURIComponent(match[2]) : null;
    }

    /**
     * Set a cookie
     * @param name - The name of the cookie
     * @param value - The value of the cookie
     * @param days - Number of days until expiration (default: 365)
     */
    static set(name: string, value: string, days: number = 365): void {
        if (typeof document === 'undefined') return;
        const expires = new Date();
        expires.setTime(expires.getTime() + days * 24 * 60 * 60 * 1000);
        document.cookie = `${name}=${encodeURIComponent(
            value
        )}; expires=${expires.toUTCString()}; path=/`;
    }

    /**
     * Delete a cookie
     * @param name - The name of the cookie
     */
    static delete(name: string): void {
        if (typeof document === 'undefined') return;
        document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;`;
    }
}
