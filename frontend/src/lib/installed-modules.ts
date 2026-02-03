/**
 * This api reports the installed modules in this server that can be shown to
 * the user.
 */
import fs from 'fs';

export type MenuEntry = {
    id: string;
    name: string;
    page: string;
    icon: string;
};

export type Module = {
    id: string;
    name: string;
    description: string;
    url: string;
    default_page: string;
    menu: MenuEntry[];
};

export type ModulesList = {
    modules: Module[];
};

function scanInstalledModulesInDir(path: string): Module[] {
    if (!fs.existsSync(path)) {
        return [];
    }
    const modules = fs
        .readdirSync(path)
        .filter(file => {
            return file !== 'api';
        })
        .filter(file => {
            return fs.existsSync(`${path}/${file}/module.json`);
        })
        .map(file => {
            try {
                // TODO Improve the resilience of the code for malformed files
                return JSON.parse(
                    fs.readFileSync(`${path}/${file}/module.json`).toString()
                );
            } catch (e) {
                const errmsg = `Error parsing module.properties file for module ${file}`;
                throw Error(errmsg);
            }
        });
    return modules;
}

export function scanInstalledModules(): ModulesList {
    let modules = scanInstalledModulesInDir('src/pages');
    modules.push(...scanInstalledModulesInDir('src/app'));
    modules.push(...scanInstalledModulesInDir('app'));
    return {
        modules: modules,
    };
}
