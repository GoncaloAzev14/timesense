export class Sidebar {
    /** default application */
    default_app_id: string;
    /** sidebar applications */
    applications: SidebarApplication[];

    constructor(applications: SidebarApplication[]) {
        if (applications.length !== 0) {
            this.default_app_id = applications[0].id;
        } else {
            this.default_app_id = '';
        }
        this.applications = applications;
    }
}

// SidebarApplication class definition
export class SidebarApplication {
    /** unique identifier for the sidebar application */
    id: string;
    /** name of the sidebar application */
    name: string;
    /** description of the sidebar item */
    description: string;
    /** priority of the sidebar item */
    priority: number;
    /** URL of the sidebar item */
    url: string;
    /** default page of the sidebar item */
    default_page: string;
    /** whether the sidebar item is clickable */
    is_clickable: boolean = true;
    /** submenu of the sidebar item */
    submenu: SidebarItem[];

    constructor(
        id: string,
        name: string,
        description: string,
        priority: number,
        url: string,
        default_page: string,
        submenu: SidebarItem[]
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.priority = priority;
        this.url = url;
        this.default_page = default_page;
        this.submenu = submenu;
    }

    validateActiveItem(url: string): SidebarItem | null {
        let matches: SidebarItem[] = [];

        for (const item of this.submenu) {
            this.collectMatches(item, url, matches);
        }

        // Find the match with the longest URL
        return matches.length > 0
            ? matches.reduce((longest, current) =>
                current.url.length > longest.url.length ? current : longest
            )
            : null;
    }

    private collectMatches(
        item: SidebarItem,
        url: string,
        matches: SidebarItem[]
    ): void {
        if (url.startsWith(item.url)) {
            matches.push(item);
        }

        for (const child of item.submenu) {
            this.collectMatches(child, url, matches);
        }
    }
}

export class SidebarItem {
    /** unique identifier for the sidebar item */
    id: string;
    /** name of the sidebar item */
    name: string;
    /** icon of the sidebar item */
    icon: string;
    /** URL of the sidebar item */
    url: string;
    /** submenu of the sidebar item */
    submenu: SidebarItem[];
    /** permissions required to access the sidebar item */
    permissions: string[];
    /** whether the sidebar item is clickable */
    is_clickable: boolean = true;

    constructor(
        id: string,
        name: string,
        icon: string,
        url: string,
        is_clickable: boolean,
        submenu: SidebarItem[],
        permissions: string[]
    ) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.url = url;
        this.is_clickable = is_clickable;
        this.submenu = submenu;
        this.permissions = permissions;
    }

    convertPermissionsToLevel(role: string): number {
        switch (role) {
            case 'Admin':
                return 3;
            case 'Manager':
                return 2;
            case 'User':
                return 1;
        }
        return 0; // Without permission
    }

    hasPermission = (levels: number[]): boolean => {
        if (this.permissions.length === 0) {
            return true;
        }
        for (const permission of this.permissions) {
            const level = this.convertPermissionsToLevel(permission);
            if (levels.includes(level)) {
                return true;
            }
        }
        return false;
    };
}

export function getSidebarJson(structure: any): any {
    return JSON.parse(JSON.stringify(getSidebar(structure)));
}

export function getSidebar(structure: any): Sidebar {
    /** Returns the sidebar structure for the application.
     * @returns {Sidebar[]}
     * The sidebar structure is an array of Sidebar objects, each containing a unique identifier, name, default application ID, and an array of SidebarApplication objects.
     * Each SidebarApplication object contains a unique identifier, name, URL, default page, and an array of SidebarItem objects.
     * Each SidebarItem object contains a unique identifier, name, icon, URL, an array of child SidebarItem objects, and an array of permissions required to access the item.
     * The sidebar structure is used to generate the sidebar navigation for the application.
     */
    const readStructure = (root_page: string, structure: any): SidebarItem => {
        const readSubmenu = (base_page: string, items: any): SidebarItem[] => {
            if (!Array.isArray(items)) {
                return [];
            }
            return items.map(item => {
                const page = `${base_page}/${item.page}`;
                return new SidebarItem(
                    item.id,
                    item.name,
                    item.icon,
                    page,
                    item.is_clickable !== undefined ? item.is_clickable : true,
                    readSubmenu(page, item.submenu),
                    item.permissions || []
                );
            });
        };

        const page = `${root_page}/${structure.page}`;
        return new SidebarItem(
            structure.id,
            structure.name,
            structure.icon,
            page,
            structure.is_clickable !== undefined
                ? structure.is_clickable
                : true,
            readSubmenu(page, structure.submenu || []),
            structure.permissions || []
        );
    };

    // Create the sidebar applications
    const applications: SidebarApplication[] = [];
    for (const app of structure) {
        const submenuList: SidebarItem[] = [];

        if (Array.isArray(app.submenu)) {
            for (const item of app.submenu) {
                submenuList.push(readStructure(app.url, item));
            }
        }

        applications.push(
            new SidebarApplication(
                app.id,
                app.name,
                app.description,
                app.priority,
                app.url,
                app.default_page,
                submenuList
            )
        );
    }

    const sidebar = new Sidebar(
        applications.sort((app1, app2) =>
            app1.priority < app2.priority ? 1 : -1
        )
    );

    return sidebar;
}

export function parseSidebar(jsonData: any): Sidebar {
    function parseSidebarItems(items: any[]): SidebarItem[] {
        return items.map(
            item =>
                new SidebarItem(
                    item.id.toString(),
                    item.name,
                    item.icon,
                    item.url,
                    item.is_clickable,
                    parseSidebarItems(item.submenu || []),
                    item.permissions || []
                )
        );
    }

    const applications = jsonData.applications.map(
        (app: any) =>
            new SidebarApplication(
                app.id,
                app.name,
                app.description,
                app.priority,
                app.url,
                app.default_page,
                parseSidebarItems(app.submenu)
            )
    );
    return new Sidebar(applications);
}
