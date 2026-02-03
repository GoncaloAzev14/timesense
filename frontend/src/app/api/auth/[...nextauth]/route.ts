import NextAuth, { AuthOptions } from 'next-auth';
import { EXPIRE_MARGIN_MINUTES } from '@/lib/auth/constants';
import { getAuthProvider } from '@/auth';

/**
 * Options are passed to NextAuth.js when initializing it in an API route
 */
const authOptions: AuthOptions = {
    providers: [getAuthProvider().provider],
    jwt: {
        secret: 'SECRET',
    },
    secret: process.env.SECRET,
    session: {
        strategy: 'jwt',
    },
    callbacks: {
        async session({ session, token }) {
            // populate the session with the user data to be able to call the
            // backend services. This includes the current state of the session
            // and the current access token
            if (token) {
                session.user = token.user;
                session.error = token.error;
                session.accessToken = token.accessToken;
            }
            return session;
        },

        async signIn({ user, account }) {
            if (account && user) {
                return true;
            }
            // TODO: provide a sensible redirect URL
            return '';
        },

        async jwt({ token, user, account }) {
            // Initial sign in
            if (account && user) {
                const {
                    access_token,
                    id_token,
                    refresh_token,
                    ...restInitialToken
                } = account;
                console.log(
                    'Account token for the initial signin:',
                    restInitialToken
                );

                token.provider = account.provider;
                // Add access_token, refresh_token and expirations to the token
                // right after signin
                token.accessToken = getAuthProvider().getAccessToken(account);
                token.refreshToken = getAuthProvider().getRefreshToken(account);
                token.accessTokenExpired =
                    getAuthProvider().getAccessTokenExpiredMs(account) -
                    60000 * EXPIRE_MARGIN_MINUTES;
                token.refreshTokenExpired =
                    getAuthProvider().getRefreshTokenExpiredMs(account) - 15000;
                token.user = user;
                return token;
            }

            // Return previous token if the access token has not expired yet
            if (Date.now() < token.accessTokenExpired) {
                console.log("Reusing the token because it hasn't expired yet");
                return token;
            }

            // Access token has expired, try to update it
            try {
                console.log('Refreshing the token');
                if (!getAuthProvider()) {
                    throw new Error('Provider not defined');
                }
                const refreshTokenHandler =
                    getAuthProvider().refreshTokenHandler;
                if (!refreshTokenHandler) {
                    throw new Error(`Provider not found: ${token.provider}`);
                }
                return await refreshTokenHandler(token);
            } catch (e) {
                console.log('Error while refreshing token', e);
                throw new Error('Some problem while refreshing the token.');
            }
        },
    },
};

/**
 * Initialize NextAuth with a Route Handler
 */
const handler = NextAuth(authOptions);

const wrapper = async (req: any, res: any) => {
    req.getCookies();
    if ('getUrl' in req) {
        req.getUrl();
    }
    return handler(req, res);
};
/**
 * NextAuth.js needs the GET and POST handlers to function properly, so we export those two.
 */
export { handler as GET, handler as POST };
