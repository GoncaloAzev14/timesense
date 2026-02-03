import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import resourcesToBackend from 'i18next-resources-to-backend';
import LanguageDetector from 'i18next-browser-languagedetector';
import Backend from 'i18next-http-backend';
import format from '@datacentric/datacentric-ui/lib/formatter';

i18n.use(Backend)
    .use(LanguageDetector)
    .use(initReactI18next)
    .init({
        interpolation: {
            escapeValue: false,
        },
    });

const initI18next = async (lang: string, namespace: string) => {
    const i18nInstance = i18n.createInstance();
    await i18nInstance
        .use(initReactI18next)
        .use(
            resourcesToBackend(
                () => import(`../public/locales/${lang}/${namespace}.json`)
            )
        )
        .init({
            lng: lang,
            ns: namespace,
            defaultNS: namespace,
            interpolation: {
                escapeValue: false,
            },
        });
    return i18nInstance;
};

export async function useTranslation(
    lang: string,
    namespace: string,
    options: { keyPrefix?: any } = {}
) {
    const i18nextInstance = await initI18next(lang, namespace);
    return {
        t: i18nextInstance.getFixedT(
            lang,
            Array.isArray(namespace) ? namespace[0] : namespace,
            options.keyPrefix
        ),
        i18n: i18nextInstance,
        f: (v: any, fmt: string, loc?: string): string =>
            format(v, fmt, loc || i18nextInstance.resolvedLanguage),
    };
}
