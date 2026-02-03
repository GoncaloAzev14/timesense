'use client';
import React, { ReactNode, useContext, useEffect, useState } from 'react';
import {
    Box,
    CssBaseline,
    Drawer,
    IconButton,
    Grid,
    List,
    Switch,
    ListItemButton,
    ListItemText,
    ListItemIcon,
    Icon,
    Collapse,
    ThemeProvider,
    Typography,
} from '@mui/material';
import {
    ChevronLeft,
    ChevronRight,
    ExpandLess,
    ExpandMore,
} from '@mui/icons-material';
import { useRouter, usePathname } from 'next/navigation';
import { signIn, useSession } from 'next-auth/react';
import Loading from './load';
import LoginButton from './login-btn';
import Logo from './logo';
import { Sidebar, SidebarItem, parseSidebar } from '@/lib/sidebar-structure';
import { darkTheme, lightTheme } from '@/theme';
import UsersService from '@/lib/users_service';
import ProtectedPage from './protected';
import { useUserSettings } from './user-settings-context';

const drawerWidth = 320;

interface ISidebarLayoutProps {
    children: ReactNode;
    sidebar: Sidebar;
    provider: string;
}

const SidebarItemComponent = ({
    item,
    menuItemSelected,
    setMenuItemSelected,
    level = 0,
    userPermissions,
}: {
    item: SidebarItem;
    menuItemSelected: string;
    setMenuItemSelected: (key: string) => void;
    level?: number;
    userPermissions: number[];
}) => {
    const router = useRouter();
    const pathname = usePathname(); // Get current path

    // Determine if the item should be open based on the path
    const isActive = pathname!.startsWith(item.url) || false;
    const [open, setOpen] = useState(isActive);

    useEffect(() => {
        setOpen(isActive); // Update open state when the path changes
    }, [pathname]);

    const handleMenuItem = (
        event: React.MouseEvent<HTMLDivElement>,
        item: SidebarItem,
        url: string
    ) => {
        event.preventDefault();
        if (item.submenu.length > 0) {
            setOpen(prev => !prev);
        }
        if (item.is_clickable) {
            router.push(url);
            setMenuItemSelected(url);
        }
    };

    if (level !== 0 && !item.hasPermission(userPermissions)) return null;

    return (
        <>
            <ListItemButton
                onClick={e => handleMenuItem(e, item, item.url)}
                selected={menuItemSelected === item.url}>
                {level !== 0 && (
                    <ListItemIcon sx={{ minWidth: 32 }}>
                        <Icon fontSize="small">{item.icon}</Icon>
                    </ListItemIcon>
                )}
                <ListItemText primary={item.name} sx={{ fontSize: '0.9rem' }} />
                {item.submenu.length > 0 &&
                    (open ? <ExpandLess /> : <ExpandMore />)}
            </ListItemButton>

            <Collapse in={open} timeout="auto" unmountOnExit>
                <List component="div" disablePadding sx={{ pl: 2 }}>
                    {item.submenu.map(child => {
                        if (!item.hasPermission(userPermissions)) return null;
                        return (
                            <SidebarItemComponent
                                key={child.id}
                                item={child}
                                menuItemSelected={menuItemSelected}
                                setMenuItemSelected={setMenuItemSelected}
                                level={level + 1}
                                userPermissions={userPermissions}
                            />
                        );
                    })}
                </List>
            </Collapse>
        </>
    );
};

const SidebarLayout = ({
    sidebar,
    children,
    provider,
}: ISidebarLayoutProps) => {
    const [menuItemSelected, setMenuItemSelected] = useState<string>('');
    const { status, data: session } = useSession();
    const router = useRouter();
    const url = usePathname();
    const [menuOpen, setMenuOpen] = useState(true);
    const [sidebarObject] = useState<Sidebar>(parseSidebar(sidebar));
    const [userRoles, setUserRoles] = useState<number[]>([]); // Initialize with an empty array
    const [theme, setTheme] = useState<'default' | 'light' | 'dark'>('default');
    const userSettings = useUserSettings();

    const handleThemeChange = (checked: boolean) => {
        if (checked) {
            setTheme('dark');
            userSettings.setTheme('dark');
        } else {
            setTheme('light');
            userSettings.setTheme('light');
        }
    };

    // Fetch user roles based on session
    useEffect(() => {
        if (session?.user) {
            userSettings.setUserId(session.user.id);
            setTheme(userSettings.get().theme);
        }
        const fetchInternalRoles = async () => {
            if (!session) return; // Ensure session is available
            const userService = new UsersService(session);
            const permissions = await userService.fetchInternalRoles();

            if (permissions) {
                const userLevels: number[] = []; // Explicitly type as number[]

                permissions.forEach(role => {
                    switch (role.name) {
                        case 'Admin':
                            userLevels.push(3);
                        case 'Manager':
                            userLevels.push(2);
                        case 'User':
                            userLevels.push(1);
                    }
                });

                setUserRoles(Array.from(new Set(userLevels))); // Remove duplicates
            }
        };

        fetchInternalRoles();
    }, [session]); // Run effect when session changes

    useEffect(() => {
        const app = sidebarObject.applications.find(app =>
            url?.startsWith(app.url)
        );
        if (!app || !url) {
            setMenuItemSelected('');
            return;
        }

        const itemResult = app.validateActiveItem(url);

        if (itemResult) {
            setMenuItemSelected(itemResult.url);
        } else {
            setMenuItemSelected('');
        }
    }, [url, sidebar]);

    useEffect(() => {
        if (
            session?.error === 'RefreshAccessTokenError' ||
            session?.error === 'EndpointAccessTokenError'
        ) {
            signIn(provider);
        }
    }, [session?.error, provider]);

    if (status === 'loading') {
        return <Loading />;
    }

    return (
        <ThemeProvider theme={theme === 'dark' ? darkTheme : lightTheme}>
            {status === 'unauthenticated' && url === '/' ? (
                <>{children}</>
            ) : (
                <Box sx={{ display: 'flex' }}>
                    <CssBaseline />
                    <Box
                        component="nav"
                        sx={{
                            width: { sm: menuOpen ? drawerWidth : 0 },
                            flexShrink: { sm: 0 },
                        }}>
                        {/* Sidebar Toggle Button */}
                        <IconButton
                            onClick={() => setMenuOpen(!menuOpen)}
                            size="small"
                            sx={{
                                position: 'fixed',
                                left: menuOpen ? drawerWidth - 18 : 0,
                                top: 12,
                                transition: 'all 0.3s ease',
                                border: `1px solid ${theme === 'dark'
                                        ? 'rgba(255, 255, 255, 0.2)'
                                        : 'rgba(68, 68, 68, 0.2)'
                                    }`,
                                borderRadius: '50%',
                                zIndex: 1201,
                            }}>
                            {menuOpen ? <ChevronLeft /> : <ChevronRight />}
                        </IconButton>

                        {/* Sidebar Drawer */}
                        <Drawer
                            variant="persistent"
                            open={menuOpen}
                            sx={{
                                '& .MuiDrawer-paper': {
                                    boxSizing: 'border-box',
                                    width: drawerWidth,
                                    backgroundColor:
                                        theme === 'dark'
                                            ? '#1E1E2F'
                                            : '#ffffff',
                                    color: theme === 'dark' ? 'white' : 'black',
                                    boxShadow:
                                        theme === 'dark'
                                            ? '2px 0px 10px rgba(0,0,0,0.2)'
                                            : '2px 0px 10px rgba(0,0,0,0.1)',
                                    borderRight:
                                        theme === 'dark'
                                            ? '1px solid rgba(255,255,255,0.1)'
                                            : '1px solid rgba(0,0,0,0.1)',
                                    borderRadius: '8px 0px 0px 8px',
                                },
                            }}>
                            <Grid
                                container
                                direction="column"
                                sx={{ height: '100%' }}>
                                {/* Logo Section */}
                                <Box
                                    p={3}
                                    display="flex"
                                    alignItems="center"
                                    justifyContent="center"
                                    onClick={() => router.push('/')}>
                                    <Logo />
                                </Box>

                                {/* Sidebar Content */}
                                <Box
                                    sx={{
                                        'overflowY': 'auto',
                                        'flex': 1,
                                        '&::-webkit-scrollbar': {
                                            width: '8px',
                                        },
                                        '&::-webkit-scrollbar-thumb': {
                                            backgroundColor:
                                                theme === 'dark'
                                                    ? '#4C4C62'
                                                    : '#BDBDBD',
                                            borderRadius: '10px',
                                        },
                                        '&::-webkit-scrollbar-thumb:hover': {
                                            backgroundColor:
                                                theme === 'dark'
                                                    ? '#666666'
                                                    : '#9E9E9E',
                                        },
                                        '&::-webkit-scrollbar-track': {
                                            backgroundColor:
                                                theme === 'dark'
                                                    ? '#2A2A40'
                                                    : '#F1F1F1',
                                            borderRadius: '10px',
                                        },
                                    }}>
                                    <List>
                                        {sidebarObject.applications.map(app => (
                                            <SidebarItemComponent
                                                key={app.id}
                                                item={
                                                    new SidebarItem(
                                                        app.id,
                                                        app.name,
                                                        '',
                                                        app.url,
                                                        true,
                                                        app.submenu,
                                                        []
                                                    )
                                                }
                                                menuItemSelected={
                                                    menuItemSelected
                                                }
                                                setMenuItemSelected={
                                                    setMenuItemSelected
                                                }
                                                userPermissions={userRoles}
                                            />
                                        ))}
                                    </List>
                                </Box>

                                {/* Bottom Section with Login Button */}
                                <Box
                                    sx={{
                                        position: 'sticky',
                                        bottom: 0,
                                        p: 2,
                                        maxWidth: '100%',
                                    }}>
                                    <LoginButton loginPopupUrl="/login" />
                                </Box>
                            </Grid>
                        </Drawer>
                    </Box>

                    {/* Main Content Area */}
                    <Box sx={{ p: 3, width: '100%' }}>
                        {/* Protected Page */}
                        <ProtectedPage provider={provider}>
                            {children}
                        </ProtectedPage>
                    </Box>

                    {/* Theme Switcher */}
                    <Box
                        sx={{
                            position: 'absolute',
                            top: 16,
                            right: 16,
                            display: 'flex',
                            alignItems: 'center',
                        }}>
                        <Typography variant="body1" sx={{ marginRight: 1 }}>
                            Dark mode
                        </Typography>
                        <Switch
                            checked={theme === 'dark'}
                            onChange={e => handleThemeChange(e.target.checked)}
                        />
                    </Box>
                </Box>
            )}
        </ThemeProvider>
    );
};

export default SidebarLayout;
