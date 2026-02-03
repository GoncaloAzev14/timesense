import { describe, expect, test, beforeEach, vi } from 'vitest';
import ServiceBase, { ServiceError } from '@/lib//service-base';
import { User } from 'next-auth';
import createFetchMock from 'vitest-fetch-mock';

let fetchMocker = createFetchMock(vi);
fetchMocker.enableMocks();

const USER: User = {

    id: '1',
    sub: 'me',
    email_verified: true,
    name: 'me',
    preferred_username: 'me',
    given_name: 'me',
    family_name: 'me',
    email: 'me@test',
    telephone: '555-1234',
    org_name: 'company',
    image: 'urn:123432412',
};

type ResponsePayload = {
    a: number;
    b: string;
    c: { dt: Date };
    d: { c: { dt: Date } };
    dt: Date;
    dta: Date[];
    dts: { dt: Date }[];
};

describe('service-base', () => {
    var service: ServiceBase;
    beforeEach(() => {
        fetchMocker.doMock();
        service = new ServiceBase({
            accessToken: 'token',
            user: USER,
            error: '',
            expires: 'never',
        });
    });

    test('can call an endpoint', async () => {
        fetchMocker.mockOnce('its a test');
        expect(
            await service.callEndPoint('GET', 'http://nothing.at.nowhere:3000')
        ).toBeInstanceOf(Response);
    });

    test('bearer token is passed', async () => {
        fetchMocker.mockOnce(req => {
            if (req.headers.get('Authorization') === 'Bearer token') {
                return 'its a test';
            } else {
                return { status: 401, body: '' };
            }
        });
        let response = await service.callEndPoint(
            'GET',
            'http://nothing.at.nowhere:3000'
        );
        expect(response).toBeInstanceOf(Response);
        expect(response.status).toBe(200);
    });

    test('throws exception if incorrect token is passed', async () => {
        fetchMocker.mockOnce(req => {
            if (req.headers.get('Authorization') === 'Bearer other_token') {
                return '{"a":"its a test"}';
            } else {
                return { status: 401, body: '' };
            }
        });
        try {
            await service.callJsonEndPoint<any>(
                'GET',
                'http://nothing.at.nowhere:3000'
            );
        } catch (e) {
            expect(e).toBeInstanceOf(Error);
            expect((e as Error).message).toBe('EndpointAccessTokenError');
            return;
        }
        throw new Error('should have thrown an exception');
    });

    test('calling an endpoint with error throws exception', async () => {
        fetchMocker.mockResponse(() => ({ status: 404, body: '' }));
        let response = await service.callEndPoint(
            'GET',
            'http://nothing.at.nowhere:3000'
        );
        expect(response).toBeInstanceOf(Response);
        expect(response.status).toBe(404);
    });

    test('can get json content', async () => {
        fetchMocker.mockOnce('{"a":1,"b":"a"}');
        let x = await service.callJsonEndPoint<ResponsePayload>(
            'GET',
            'http://nothing.at.nowhere:3000'
        );
        expect(x.a).toBe(1);
        expect(x.b).toBe('a');
    });

    test('sends the proper content type when a body is provided', async () => {
        fetchMocker.mockOnce(req => {
            if (req.headers.get('Content-Type') === 'application/json') {
                return '{"b":"ok"}';
            } else {
                return { status: 400, body: '{"reason":"Invalid json"}' };
            }
        });
        let response = await service.callJsonEndPoint<ResponsePayload>(
            'POST',
            'http://nothing.at.nowhere:3000',
            { a: 1 }
        );
        expect(response.b).toBe('ok');
    });

    test('throws exception if the server returns an error', async () => {
        fetchMocker.mockOnce(() => ({
            status: 400,
            body: '{"validation":"Some error"}',
        }));
        try {
            let response = await service.callJsonEndPoint<ResponsePayload>(
                'POST',
                'http://nothing.at.nowhere:3000',
                { a: 1 }
            );
        } catch (e) {
            expect(e).toBeInstanceOf(ServiceError);
            expect((e as ServiceError).message).toBe('Failed to call endpoint');
            expect((e as ServiceError).statusCode).toBe(400);
            expect((e as ServiceError).getContentAsJson()).toEqual(
                '{"validation":"Some error"}',
            );
            return;
        }
        throw new Error('should have thrown an exception');
    });

    test('can get json content with null values', async () => {
        fetchMocker.mockOnce('{"a":null,"b":"a"}');
        let x = await service.callJsonEndPoint<ResponsePayload>(
            'GET',
            'http://nothing.at.nowhere:3000'
        );
        expect(x.a).toBe(null);
        expect(x.b).toBe('a');
    });

    test('can fix json dates', async () => {
        fetchMocker.mockOnce('{"a":1,"dt":"2024-02-03T04:05:06.789+00:00"}');
        let x = await service.callJsonEndPoint<ResponsePayload>(
            'GET',
            'http://nothing.at.nowhere:3000'
        );
        expect(x.a).toBe(1);
        expect(x.dt).toEqual(new Date(2024, 1, 3, 4, 5, 6, 789));
    });

    test('can fix json dates in sub-objects', async () => {
        fetchMocker.mockOnce(
            '{"a":1,"c":{"dt":"2024-02-03T04:05:06.789+00:00"}}'
        );
        let x = await service.callJsonEndPoint<ResponsePayload>(
            'GET',
            'http://nothing.at.nowhere:3000'
        );
        expect(x.a).toBe(1);
        expect(x.c.dt).toEqual(new Date(2024, 1, 3, 4, 5, 6, 789));
        fetchMocker.mockOnce(
            '{"a":1,"d":{"c":{"dt":"2024-02-03T04:05:06.789+00:00"}}}'
        );
        x = await service.callJsonEndPoint<ResponsePayload>(
            'GET',
            'http://nothing.at.nowhere:3000'
        );
        expect(x.d.c.dt).toEqual(new Date(2024, 1, 3, 4, 5, 6, 789));
    });

    test('can fix json dates in arrays', async () => {
        fetchMocker.mockOnce('{"a":1,"dta":["2024-02-03T04:05:06.789+00:00"]}');
        let x = await service.callJsonEndPoint<ResponsePayload>(
            'GET',
            'http://nothing.at.nowhere:3000'
        );
        expect(x.a).toBe(1);
        expect(x.dta[0]).toEqual(new Date(2024, 1, 3, 4, 5, 6, 789));
    });

    test('can fix json dates in arrays of objects', async () => {
        fetchMocker.mockOnce(
            '{"a":1,"dts":[{"dt":"2024-02-03T04:05:06.789+00:00"}]}'
        );
        let x = await service.callJsonEndPoint<ResponsePayload>(
            'GET',
            'http://nothing.at.nowhere:3000'
        );
        expect(x.a).toBe(1);
        expect(x.dts[0].dt).toEqual(new Date(2024, 1, 3, 4, 5, 6, 789));
    });

    test('throws exception if the server returns invalid json', async () => {
        fetchMocker.mockOnce('{"a":1,invalid json');
        try {
            await service.callJsonEndPoint<any>(
                'GET',
                'http://nothing.at.nowhere:3000'
            );
        } catch (e) {
            expect(e).toBeInstanceOf(ServiceError);
            expect((e as Error).message).toSatisfy((x: string) =>
                x.startsWith('Failed to parse response')
            );
            return;
        }
        throw new Error('should have thrown an exception');
    });
});
