import { describe, expect, test, afterEach, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import { fs, vol } from 'memfs';
import { scanInstalledModules } from '@/lib/installed-modules';

vi.mock('node:fs', () => {
    const { fs } = require('memfs');
    return { default: fs };
});

vi.mock('node:fs/promises', () => {
    const { fs } = require('memfs');
    return { default: fs.promises };
});

const FIRST_MODULE = {
    id: 'app1',
    name: 'App 1',
    description: 'App 1 description',
    url: '/app1',
    default_page: 'app1',
    priority: 1,
    children: [
        {
            id: 'app1',
            name: 'App 1',
            page: 'app1',
            icon: 'icon',
            permissions: [
                "Admin"
            ]
        },
    ],
};

const SECOND_MODULE = {
    id: 'app2',
    name: 'App 2',
    description: 'App 2 description',
    url: '/app2',
    default_page: 'app2',
    priority: 1,
    children: [
        {
            id: 'app2',
            name: 'App 2',
            page: 'app2',
            icon: 'icon',
            permissions: [
                "Admin"
            ]
        },
    ],
};

describe('installed-modules loader', () => {
    afterEach(() => {
        cleanup();
        vol.reset();
    });

    test('will load a single file', () => {
        fs.mkdirSync('src/pages', { recursive: true });
        fs.mkdirSync('src/app/app1', { recursive: true });
        fs.writeFileSync(
            'src/app/app1/module.json',
            JSON.stringify(FIRST_MODULE)
        );
        const apps = scanInstalledModules();
        expect(apps.modules.length).toBe(1);
        expect(apps.modules[0]).toStrictEqual(FIRST_MODULE);
    });

    test('will scan multiple folders', () => {
        fs.mkdirSync('src/pages', { recursive: true });
        fs.mkdirSync('src/app/app1', { recursive: true });
        fs.mkdirSync('src/app/app2', { recursive: true });
        fs.writeFileSync(
            'src/app/app1/module.json',
            JSON.stringify(FIRST_MODULE)
        );
        fs.writeFileSync(
            'src/app/app2/module.json',
            JSON.stringify(SECOND_MODULE)
        );
        const apps = scanInstalledModules();
        expect(apps.modules.length).toBe(2);
        expect(apps.modules[0]).toStrictEqual(FIRST_MODULE);
        expect(apps.modules[1]).toStrictEqual(SECOND_MODULE);
    });

    test('supports app and pages router', () => {
        fs.mkdirSync('src/pages/app1', { recursive: true });
        fs.mkdirSync('src/app/app2', { recursive: true });
        fs.writeFileSync(
            'src/pages/app1/module.json',
            JSON.stringify(FIRST_MODULE)
        );
        fs.writeFileSync(
            'src/app/app2/module.json',
            JSON.stringify(SECOND_MODULE)
        );
        const apps = scanInstalledModules();
        expect(apps.modules.length).toBe(2);
        expect(apps.modules[0]).toStrictEqual(FIRST_MODULE);
        expect(apps.modules[1]).toStrictEqual(SECOND_MODULE);
    });
});
