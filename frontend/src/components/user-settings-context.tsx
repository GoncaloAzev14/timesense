'use client';

import { createContext, PropsWithChildren, useContext } from 'react';
import UserSettings, { IUserSettings } from '../lib/user-settings';

interface UserSettingsContext {
    userSettings: IUserSettings;
}

// Create and export the context
const UserSettingsContext = createContext<IUserSettings>(new UserSettings());

const UserSettingsProvider = ({ children }: PropsWithChildren) => {
    const userSettings = new UserSettings();

    return (
        <UserSettingsContext.Provider value={userSettings}>
            {children}
        </UserSettingsContext.Provider>
    );
};

export const useUserSettings = () => {
    return useContext(UserSettingsContext);
};

export default UserSettingsProvider;
