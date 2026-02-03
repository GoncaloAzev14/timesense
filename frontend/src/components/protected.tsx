import { useSession, signIn } from 'next-auth/react';
import { ReactNode } from 'react';
import Loading from './load';

interface IProtectedPage {
    role?: string;
    children: ReactNode;
    provider: string;
}

/**
 *
 * @param role The role the user needs to have to access this page
 * @param children The page that needs to be authenticated
 * @returns ReactNode
 */
export default function ProtectedPage({
    role,
    children,
    provider,
}: IProtectedPage) {
    const { status, data: session } = useSession({
        required: true,
        onUnauthenticated() {
            (async () => {
                console.log(
                    'User is unauthenticated, triggering the signin',
                    session
                );
                signIn(provider);
            })();
        },
    });

    if (status === 'loading')
        return (
            <>
                <Loading></Loading>
            </>
        );

    if (role) {
        //TODO Validate that the user can access this page
    }

    return <>{children}</>;
}
