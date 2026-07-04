import 'dart:convert';

import 'package:http/http.dart' as http;

import '../models/agent_status.dart';
import '../models/log_entry.dart';
import '../models/remote_command.dart';
import '../models/runtime.dart';

/// Exception thrown when an Infinity API call fails.
class InfinityApiException implements Exception {
  final int? statusCode;
  final String message;

  const InfinityApiException({this.statusCode, required this.message});

  @override
  String toString() => 'InfinityApiException($statusCode): $message';
}

/// Client for Infinity-api REST endpoints.
abstract class InfinityApi {
  /// Register (or re-register) a runtime by upserting its status document.
  Future<void> registerRuntime(String runtimeId, {String? name});

  /// List all runtimes known to Infinity-api.
  Future<List<Runtime>> listRuntimes();

  /// Post a status update for [runtimeId].
  Future<void> postStatus(
    String runtimeId,
    String status, {
    Map<String, dynamic>? payload,
  });

  /// Fetch the latest status document for [runtimeId].
  Future<AgentStatus?> getLatestStatus(String runtimeId);

  /// Persist a log entry for [runtimeId].
  Future<void> postLog(
    String runtimeId,
    String level,
    String message, {
    Map<String, dynamic>? payload,
  });

  /// Query log entries for [runtimeId].
  Future<List<LogEntry>> getLogs(String runtimeId, {int limit = 100});

  /// Enqueue a command for [runtimeId].
  Future<void> sendCommand(
    String runtimeId,
    String type, {
    Map<String, dynamic>? payload,
  });

  /// Fetch pending commands, optionally filtered to [runtimeId].
  Future<List<RemoteCommand>> getPendingCommands(String runtimeId);
}

/// Production HTTP implementation of [InfinityApi].
class HttpInfinityApi implements InfinityApi {
  final String baseUrl;
  final Map<String, String> authHeaders;
  final http.Client _client;
  final Duration timeout;

  HttpInfinityApi({
    required this.baseUrl,
    this.authHeaders = const {},
    http.Client? client,
    this.timeout = const Duration(seconds: 10),
  }) : _client = client ?? http.Client();

  Uri _uri(String path) {
    final normalizedBase = baseUrl.endsWith('/') ? baseUrl.substring(0, baseUrl.length - 1) : baseUrl;
    final normalizedPath = path.startsWith('/') ? path : '/$path';
    return Uri.parse('$normalizedBase$normalizedPath');
  }

  Map<String, String> get _headers => {
        'Content-Type': 'application/json',
        ...authHeaders,
      };

  Future<http.Response> _get(String path, {Map<String, String>? query}) async {
    final uri = _uri(path).replace(queryParameters: query);
    return _client.get(uri, headers: authHeaders).timeout(timeout);
  }

  Future<http.Response> _post(
    String path,
    Map<String, dynamic> body, {
    Map<String, String>? query,
  }) async {
    final uri = _uri(path).replace(queryParameters: query);
    return _client
        .post(
          uri,
          headers: _headers,
          body: jsonEncode(body),
        )
        .timeout(timeout);
  }

  dynamic _decode(http.Response response) {
    final body = response.body;
    if (body.isEmpty) return null;
    return jsonDecode(body);
  }

  void _check(http.Response response, {String context = 'API request'}) {
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw InfinityApiException(
        statusCode: response.statusCode,
        message: '$context failed: ${response.body}',
      );
    }
  }

  @override
  Future<void> registerRuntime(String runtimeId, {String? name}) async {
    final body = <String, dynamic>{
      'status': 'online',
      'online': true,
      'runtime_id': runtimeId,
      if (name != null) 'name': name,
    };
    final response = await _post('/status/$runtimeId', body);
    _check(response, context: 'registerRuntime');
  }

  @override
  Future<List<Runtime>> listRuntimes() async {
    final response = await _get('/runtimes');
    _check(response, context: 'listRuntimes');
    final data = _decode(response);
    if (data is! List) return [];
    return data
        .whereType<Map<String, dynamic>>()
        .map(Runtime.fromJson)
        .toList();
  }

  @override
  Future<void> postStatus(
    String runtimeId,
    String status, {
    Map<String, dynamic>? payload,
  }) async {
    final body = <String, dynamic>{
      if (payload != null) ...payload,
      'status': status,
      'runtime_id': runtimeId,
    };
    final response = await _post('/status/$runtimeId', body);
    _check(response, context: 'postStatus');
  }

  @override
  Future<AgentStatus?> getLatestStatus(String runtimeId) async {
    final response = await _get('/status/$runtimeId');
    _check(response, context: 'getLatestStatus');
    final data = _decode(response);
    if (data is! Map<String, dynamic>) return null;
    return AgentStatus.fromJson(data);
  }

  @override
  Future<void> postLog(
    String runtimeId,
    String level,
    String message, {
    Map<String, dynamic>? payload,
  }) async {
    final body = <String, dynamic>{
      if (payload != null) ...payload,
      'runtime_id': runtimeId,
      'level': level,
      'message': message,
    };
    final response = await _post('/logs', body);
    _check(response, context: 'postLog');
  }

  @override
  Future<List<LogEntry>> getLogs(String runtimeId, {int limit = 100}) async {
    final response = await _get('/logs', query: {
      'runtime_id': runtimeId,
      'limit': '$limit',
    });
    _check(response, context: 'getLogs');
    final data = _decode(response);
    if (data is! List) return [];
    return data
        .whereType<Map<String, dynamic>>()
        .map(LogEntry.fromJson)
        .toList();
  }

  @override
  Future<void> sendCommand(
    String runtimeId,
    String type, {
    Map<String, dynamic>? payload,
  }) async {
    final body = <String, dynamic>{
      if (payload != null) ...payload,
      'runtime_id': runtimeId,
      'type': type,
    };
    final response = await _post('/commands', body);
    _check(response, context: 'sendCommand');
  }

  @override
  Future<List<RemoteCommand>> getPendingCommands(String runtimeId) async {
    final response = await _get('/commands/pending', query: {
      'runtime_id': runtimeId,
    });
    _check(response, context: 'getPendingCommands');
    final data = _decode(response);
    if (data is! List) return [];
    return data
        .whereType<Map<String, dynamic>>()
        .map(RemoteCommand.fromJson)
        .where((command) => command.runtimeId == runtimeId)
        .toList();
  }
}

/// In-memory [InfinityApi] implementation for tests and local development.
class MockInfinityApi implements InfinityApi {
  final List<Map<String, dynamic>> calls = [];
  final List<Runtime> runtimes = [];
  AgentStatus? latestStatus;
  final List<LogEntry> logs = [];
  final List<RemoteCommand> pendingCommands = [];

  void _record(String method, Map<String, dynamic> args) {
    calls.add({'method': method, 'args': args});
  }

  @override
  Future<void> registerRuntime(String runtimeId, {String? name}) async {
    _record('registerRuntime', {'runtimeId': runtimeId, 'name': name});
    final existing = runtimes.indexWhere((r) => r.id == runtimeId);
    final runtime = Runtime(
      id: runtimeId,
      name: name,
      online: true,
      lastSeen: DateTime.now().toUtc(),
    );
    if (existing >= 0) {
      runtimes[existing] = runtime;
    } else {
      runtimes.add(runtime);
    }
  }

  @override
  Future<List<Runtime>> listRuntimes() async {
    _record('listRuntimes', {});
    return List.unmodifiable(runtimes);
  }

  @override
  Future<void> postStatus(
    String runtimeId,
    String status, {
    Map<String, dynamic>? payload,
  }) async {
    _record('postStatus', {
      'runtimeId': runtimeId,
      'status': status,
      'payload': payload,
    });
    latestStatus = AgentStatus(
      agentId: runtimeId,
      status: status,
      payload: payload,
    );
  }

  @override
  Future<AgentStatus?> getLatestStatus(String runtimeId) async {
    _record('getLatestStatus', {'runtimeId': runtimeId});
    return latestStatus;
  }

  @override
  Future<void> postLog(
    String runtimeId,
    String level,
    String message, {
    Map<String, dynamic>? payload,
  }) async {
    _record('postLog', {
      'runtimeId': runtimeId,
      'level': level,
      'message': message,
      'payload': payload,
    });
    logs.add(LogEntry(
      id: 'log-${logs.length + 1}',
      runtimeId: runtimeId,
      level: level,
      message: message,
      timestamp: DateTime.now().toUtc(),
      payload: payload,
    ));
  }

  @override
  Future<List<LogEntry>> getLogs(String runtimeId, {int limit = 100}) async {
    _record('getLogs', {'runtimeId': runtimeId, 'limit': limit});
    return logs
        .where((log) => log.runtimeId == runtimeId)
        .take(limit)
        .toList();
  }

  @override
  Future<void> sendCommand(
    String runtimeId,
    String type, {
    Map<String, dynamic>? payload,
  }) async {
    _record('sendCommand', {
      'runtimeId': runtimeId,
      'type': type,
      'payload': payload,
    });
    pendingCommands.add(RemoteCommand(
      id: 'cmd-${pendingCommands.length + 1}',
      runtimeId: runtimeId,
      type: type,
      payload: payload,
      status: 'pending',
    ));
  }

  @override
  Future<List<RemoteCommand>> getPendingCommands(String runtimeId) async {
    _record('getPendingCommands', {'runtimeId': runtimeId});
    return pendingCommands
        .where((command) => command.runtimeId == runtimeId)
        .toList();
  }

  /// Convenience accessor for commands sent via [sendCommand].
  List<Map<String, dynamic>> get sentCommands => calls
      .where((call) => call['method'] == 'sendCommand')
      .map((call) => (call['args'] as Map<String, dynamic>? ?? {}))
      .toList();
}
