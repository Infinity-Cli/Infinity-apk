import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:infinity_apk/models/agent_status.dart';
import 'package:infinity_apk/models/log_entry.dart';
import 'package:infinity_apk/models/remote_command.dart';
import 'package:infinity_apk/models/runtime.dart';
import 'package:infinity_apk/services/api_service.dart';

void main() {
  group('MockInfinityApi', () {
    late MockInfinityApi api;

    setUp(() {
      api = MockInfinityApi();
    });

    test('records registerRuntime and returns canned runtimes', () async {
      await api.registerRuntime('runtime-1', name: 'Test Runtime');
      expect(api.calls.last['method'], 'registerRuntime');

      final runtimes = await api.listRuntimes();
      expect(runtimes, hasLength(1));
      expect(runtimes.first.id, 'runtime-1');
      expect(runtimes.first.name, 'Test Runtime');
    });

    test('stores status and retrieves it', () async {
      await api.postStatus('runtime-1', 'running', payload: {'step': 2});
      final status = await api.getLatestStatus('runtime-1');
      expect(status, isNotNull);
      expect(status!.status, 'running');
      expect(status.payload?['step'], 2);
    });

    test('stores logs and retrieves them', () async {
      await api.postLog('runtime-1', 'INFO', 'hello');
      final logs = await api.getLogs('runtime-1');
      expect(logs, hasLength(1));
      expect(logs.first.message, 'hello');
    });

    test('stores commands and retrieves pending', () async {
      await api.sendCommand('runtime-1', 'restart');
      final pending = await api.getPendingCommands('runtime-1');
      expect(pending, hasLength(1));
      expect(pending.first.type, 'restart');
    });
  });

  group('HttpInfinityApi', () {
    const baseUrl = 'http://localhost:8000';
    const authHeaders = {'Authorization': 'Bearer test-token'};

    test('listRuntimes parses runtimes and sends auth header', () async {
      final client = MockClient((request) async {
        expect(request.method, 'GET');
        expect(request.url.path, '/runtimes');
        expect(request.headers['Authorization'], 'Bearer test-token');

        return http.Response(
          jsonEncode([
            {
              'id': 'r1',
              'name': 'Runtime One',
              'online': true,
              'last_seen': '2024-01-01T00:00:00.000Z',
            },
          ]),
          200,
        );
      });

      final api = HttpInfinityApi(
        baseUrl: baseUrl,
        authHeaders: authHeaders,
        client: client,
      );

      final runtimes = await api.listRuntimes();
      expect(runtimes, hasLength(1));
      expect(runtimes.first.id, 'r1');
      expect(runtimes.first.name, 'Runtime One');
      expect(runtimes.first.online, isTrue);
    });

    test('postStatus sends correct payload', () async {
      final client = MockClient((request) async {
        expect(request.method, 'POST');
        expect(request.url.path, '/status/runtime-2');
        final body = jsonDecode(request.body) as Map<String, dynamic>;
        expect(body['status'], 'online');
        expect(body['runtime_id'], 'runtime-2');
        expect(body['progress'], 0.5);
        return http.Response(jsonEncode(body), 200);
      });

      final api = HttpInfinityApi(
        baseUrl: baseUrl,
        authHeaders: authHeaders,
        client: client,
      );

      await api.postStatus('runtime-2', 'online', payload: {'progress': 0.5});
    });

    test('getLatestStatus parses status document', () async {
      final client = MockClient((request) async {
        expect(request.method, 'GET');
        expect(request.url.path, '/status/runtime-3');
        return http.Response(
          jsonEncode({
            'runtime_id': 'runtime-3',
            'status': 'idle',
            'progress': 0.75,
          }),
          200,
        );
      });

      final api = HttpInfinityApi(
        baseUrl: baseUrl,
        authHeaders: authHeaders,
        client: client,
      );

      final status = await api.getLatestStatus('runtime-3');
      expect(status, isNotNull);
      expect(status!.agentId, 'runtime-3');
      expect(status.status, 'idle');
      expect(status.progress, 0.75);
    });

    test('postLog sends correct payload', () async {
      final client = MockClient((request) async {
        expect(request.method, 'POST');
        expect(request.url.path, '/logs');
        final body = jsonDecode(request.body) as Map<String, dynamic>;
        expect(body['runtime_id'], 'runtime-4');
        expect(body['level'], 'ERROR');
        expect(body['message'], 'boom');
        return http.Response(jsonEncode({'id': 'log-1', 'log': body}), 200);
      });

      final api = HttpInfinityApi(
        baseUrl: baseUrl,
        authHeaders: authHeaders,
        client: client,
      );

      await api.postLog('runtime-4', 'ERROR', 'boom');
    });

    test('getLogs filters by runtime id', () async {
      final client = MockClient((request) async {
        expect(request.method, 'GET');
        expect(request.url.path, '/logs');
        expect(request.url.queryParameters['runtime_id'], 'runtime-5');
        expect(request.url.queryParameters['limit'], '50');
        return http.Response(
          jsonEncode([
            {
              'id': 'log-1',
              'runtime_id': 'runtime-5',
              'level': 'INFO',
              'message': 'hello',
              'timestamp': '2024-01-01T00:00:00.000Z',
            },
          ]),
          200,
        );
      });

      final api = HttpInfinityApi(
        baseUrl: baseUrl,
        authHeaders: authHeaders,
        client: client,
      );

      final logs = await api.getLogs('runtime-5', limit: 50);
      expect(logs, hasLength(1));
      expect(logs.first.runtimeId, 'runtime-5');
    });

    test('sendCommand enqueues a command', () async {
      final client = MockClient((request) async {
        expect(request.method, 'POST');
        expect(request.url.path, '/commands');
        final body = jsonDecode(request.body) as Map<String, dynamic>;
        expect(body['runtime_id'], 'runtime-6');
        expect(body['type'], 'run');
        expect(body['args'], ['a', 'b']);
        return http.Response(
          jsonEncode({'id': 'cmd-1', 'command': body}),
          200,
        );
      });

      final api = HttpInfinityApi(
        baseUrl: baseUrl,
        authHeaders: authHeaders,
        client: client,
      );

      await api.sendCommand('runtime-6', 'run', payload: {'args': ['a', 'b']});
    });

    test('getPendingCommands filters by runtime id', () async {
      final client = MockClient((request) async {
        expect(request.method, 'GET');
        expect(request.url.path, '/commands/pending');
        expect(request.url.queryParameters['runtime_id'], 'runtime-7');
        return http.Response(
          jsonEncode([
            {
              'id': 'cmd-1',
              'runtime_id': 'runtime-7',
              'type': 'stop',
              'status': 'pending',
            },
            {
              'id': 'cmd-2',
              'runtime_id': 'other-runtime',
              'type': 'restart',
              'status': 'pending',
            },
          ]),
          200,
        );
      });

      final api = HttpInfinityApi(
        baseUrl: baseUrl,
        authHeaders: authHeaders,
        client: client,
      );

      final commands = await api.getPendingCommands('runtime-7');
      expect(commands, hasLength(1));
      expect(commands.first.id, 'cmd-1');
    });

    test('throws InfinityApiException on non-2xx responses', () async {
      final client = MockClient((request) async {
        return http.Response('not found', 404);
      });

      final api = HttpInfinityApi(
        baseUrl: baseUrl,
        authHeaders: authHeaders,
        client: client,
      );

      expect(
        () => api.listRuntimes(),
        throwsA(isA<InfinityApiException>()),
      );
    });
  });
}
