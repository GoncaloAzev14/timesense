import buildKeycloakProvider from '@/lib/auth/providers/keycloak';
import buildAzureAdProvider from '@/lib/auth/providers/azure';

function resolveSelectedProvider() {
    switch (process.env.OIDC_TYPE) {
        case 'keycloak':
            return buildKeycloakProvider();
        case 'azure':
            return buildAzureAdProvider();
        default:
            throw Error(`Unsupported provider: ${process.env.OIDC_TYPE}`);
    }
}

export function getAuthProvider() {
    return resolveSelectedProvider();
}

export function getAuthProviderId() {
    return resolveSelectedProvider().provider.id;
}
