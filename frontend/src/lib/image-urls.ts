const basePathThemes = '/images/themes';

const themePaths = {
    light: `${basePathThemes}/light`,
    dark: `${basePathThemes}/dark`,
};

const imagesUrls = {
    default: {
        logo: `${themePaths.light}/logo500x375.png`,
        background: `${themePaths.light}/background.jpg`,
    },
    light: {
        logo: `${themePaths.light}/logo500x375.png`,
        background: `${themePaths.light}/background.jpg`,
    },
    dark: {
        logo: `${themePaths.dark}/logo500x375.png`,
        background: `${themePaths.dark}/background2.png`,
    },
};

export default imagesUrls;
