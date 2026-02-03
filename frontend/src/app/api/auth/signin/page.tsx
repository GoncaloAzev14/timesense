import { getAuthProviderId } from '@/auth';
import { signIn } from 'next-auth/react';

export default async function SignIn({
    searchParams,
}: {
    searchParams?: Promise<{ [key: string]: string | string[] | undefined }>;
}) {
    // reference the searchParams to prevent static builfd
    const values = await searchParams;
    signIn(getAuthProviderId());
    return <></>;
}
