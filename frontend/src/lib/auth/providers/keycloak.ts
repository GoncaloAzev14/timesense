import { JWT } from 'next-auth/jwt';
import KeycloakProvider from 'next-auth/providers/keycloak';
import { EXPIRE_MARGIN_MINUTES } from '../constants';

const keycloakUris = {
    authorization_endpoint: `${process.env.OIDC_ISSUER}/protocol/openid-connect/auth`,
    token_endpoint: `${process.env.OIDC_ISSUER}/protocol/openid-connect/token`,
    introspection_endpoint: `${process.env.OIDC_ISSUER}/protocol/openid-connect/token/introspect`,
    userinfo_endpoint: `${process.env.OIDC_ISSUER}/protocol/openid-connect/userinfo`,
    end_session_endpoint: `${process.env.OIDC_ISSUER}/protocol/openid-connect/logout`,
};

const keycloakProvider = KeycloakProvider({
    issuer: process.env.OIDC_ISSUER,
    clientId: process.env.OIDC_CLIENT_ID || '',
    clientSecret: process.env.OIDC_CLIENT_SECRET || '',
    accessTokenUrl: keycloakUris.token_endpoint,
    requestTokenUrl: keycloakUris.authorization_endpoint,
    profileUrl: keycloakUris.userinfo_endpoint,
});

/**
 *
 * @param token Jwt object
 * @returns Returns new token object
 */
const handleRefreshTokenKeycloak = async (token: JWT) => {
    try {
        if (Date.now() > token.refreshTokenExpired) throw Error;
        const details = {
            client_id: process.env.OIDC_CLIENT_ID || '',
            client_secret: process.env.OIDC_CLIENT_SECRET || '',
            grant_type: 'refresh_token',
            refresh_token: token.refreshToken,
        };
        const formBody: string[] = [];
        Object.entries(details).forEach(([key, value]: [string, any]) => {
            const encodedKey = encodeURIComponent(key);
            const encodedValue = encodeURIComponent(value);
            formBody.push(encodedKey + '=' + encodedValue);
        });
        const formData = formBody.join('&');

        // call to the issuer to try to obtain a new token
        const response = await fetch(keycloakUris.token_endpoint, {
            method: 'POST',
            headers: {
                'Content-Type':
                    'application/x-www-form-urlencoded;charset=UTF-8',
            },
            body: formData,
        });
        const refreshedTokens = await response.json();
        if (!response.ok) throw refreshedTokens;

        return {
            ...token,
            accessToken: refreshedTokens.id_token,
            refreshToken: refreshedTokens.refresh_token,
            accessTokenExpired:
                refreshedTokens.expires_at - 60 * EXPIRE_MARGIN_MINUTES,
            refreshTokenExpired:
                Date.now() + (refreshedTokens.refresh_expires_in - 15) * 1000,
        };
    } catch (error) {
        console.log(error);
        return {
            ...token,
            error: 'RefreshAccessTokenError',
        };
    }
};

export default function buildKeycloakProvider() {
    return {
        provider: keycloakProvider,
        refreshTokenHandler: handleRefreshTokenKeycloak,
        getAccessToken: (token: any) => token.id_token,
        getRefreshToken: (token: any) => token.refresh_token,
        getAccessTokenExpiredMs: (token: any) => token.expires_at * 1000,
        getRefreshTokenExpiredMs: (token: any) =>
            Date.now() + token.refresh_expires_in * 1000,
    };
}
