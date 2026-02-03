import { JWT } from 'next-auth/jwt';
import AzureADProvider from 'next-auth/providers/azure-ad';
import { EXPIRE_MARGIN_MINUTES } from '../constants';
import assert from 'assert';

const azureUris = {
    authorization_endpoint: `${process.env.OIDC_ISSUER}/protocol/openid-connect/auth`,
    token_endpoint: `${process.env.OIDC_ISSUER}/../token`,
    introspection_endpoint: `${process.env.OIDC_ISSUER}/protocol/openid-connect/token/introspect`,
    userinfo_endpoint: `${process.env.OIDC_ISSUER}/protocol/openid-connect/userinfo`,
    end_session_endpoint: `${process.env.OIDC_ISSUER}/logout`,
};

const azureProvider = AzureADProvider({
    clientId: process.env.OIDC_CLIENT_ID || '',
    clientSecret: process.env.OIDC_CLIENT_SECRET || '',
    tenantId: process.env.OIDC_TENANT_ID || '',
    authorization: {
        params: {
            // Include offline_access scope to get the refresh token
            // Include the default scope for the app so that the access token
            // works in the backend
            scope: `openid profile email offline_access ${process.env.OIDC_CLIENT_ID}/.default`,
        },
    },
});

/**
 *
 * @param token Jwt object
 * @returns Returns new token object
 */
const handleRefreshTokenAzure = async (token: JWT) => {
    try {
        if (Date.now() > token.refreshTokenExpired) {
            throw Error('Refresh token has already expired!');
        }

        const details = {
            client_id: process.env.OIDC_CLIENT_ID || '',
            client_secret: process.env.OIDC_CLIENT_SECRET || '',
            tenantId: process.env.OIDC_TENANT_ID || '',
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
        const response = await fetch(azureUris.token_endpoint, {
            method: 'POST',
            headers: {
                'Content-Type':
                    'application/x-www-form-urlencoded;charset=UTF-8',
            },
            body: formData,
        });
        const refreshedTokens = await response.json();
        if (!response.ok) throw refreshedTokens;
        const { access_token, refresh_token, ...rest } = refreshedTokens;
        assert(access_token, 'No access token in the response');
        assert(refresh_token, 'No refresh token in the response');
        console.log('Refreshed tokens additional data:', rest);
        return {
            ...token,
            accessToken: refreshedTokens.access_token,
            refreshToken: refreshedTokens.refresh_token,
            accessTokenExpired:
                parseInt(refreshedTokens.expires_on) -
                60 * EXPIRE_MARGIN_MINUTES,
            refreshTokenExpired:
                Date.now() +
                ((refreshedTokens.refresh_expires_in || 3600) - 15) * 1000,
        };
    } catch (error) {
        console.log(error);
        return {
            ...token,
            error: 'RefreshAccessTokenError',
        };
    }
};

export default function buildAzureAdProvider() {
    return {
        provider: azureProvider,
        refreshTokenHandler: handleRefreshTokenAzure,
        getAccessToken: (token: any) => token.access_token,
        getRefreshToken: (token: any) => token.refresh_token,
        getAccessTokenExpiredMs: (token: any) => token.expires_at * 1000,
        getRefreshTokenExpiredMs: (token: any) =>
            Date.now() + token.refresh_expires_in * 1000,
    };
}
