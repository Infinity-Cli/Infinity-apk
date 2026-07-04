import 'package:flutter/foundation.dart';

import '../models/agent_status.dart';
import '../models/log_entry.dart';
import '../models/remote_command.dart';
import '../models/runtime.dart';
import '../services/api_service.dart';

/// ChangeNotifier that exposes the Infinity API client and connection state.
class ApiProvider extends ChangeNotifier {
  ApiProvider({required InfinityApi api}) : _api = api;

  final InfinityApi _api;

  Map<String, String> _authHeaders = {};
  String? _currentRuntimeId;
  bool _online = false;

  /// The underlying Infinity API client.
  InfinityApi get api => _api;

  /// Authentication headers sent with every API request.
  Map<String, String> get authHeaders => Map.unmodifiable(_authHeaders);

  /// The runtime this app is currently acting on behalf of.
  String? get currentRuntimeId => _currentRuntimeId;

  /// Whether the current runtime has reported an online status.
  bool get online => _online;

  /// Replace the authentication headers.
  void setAuthHeaders(Map<String, String> headers) {
    _authHeaders = Map<String, String>.unmodifiable(headers);
    notifyListeners();
  }

  /// Clear the authentication headers (e.g. on sign out).
  void clearAuthHeaders() {
    _authHeaders = {};
    notifyListeners();
  }

  /// Set the active runtime id.
  void setCurrentRuntimeId(String? runtimeId) {
    _currentRuntimeId = runtimeId;
    notifyListeners();
  }

  /// Update the cached online state.
  void setOnline(bool value) {
    if (_online == value) return;
    _online = value;
    notifyListeners();
  }

  /// Register the current runtime with Infinity-api.
  Future<void> registerRuntime(String runtimeId, {String? name}) async {
    setCurrentRuntimeId(runtimeId);
    await _api.registerRuntime(runtimeId, name: name);
    setOnline(true);
  }

  /// List all runtimes visible to the authenticated user.
  Future<List<Runtime>> listRuntimes() => _api.listRuntimes();

  /// Post a status update for the current runtime.
  Future<void> postStatus(
    String status, {
    Map<String, dynamic>? payload,
  }) async {
    final runtimeId = _currentRuntimeId;
    if (runtimeId == null) {
      throw StateError('No current runtime selected');
    }
    await _api.postStatus(runtimeId, status, payload: payload);
    setOnline(status.toLowerCase() == 'online');
  }

  /// Fetch the latest status document for the current runtime.
  Future<AgentStatus?> getLatestStatus() async {
    final runtimeId = _currentRuntimeId;
    if (runtimeId == null) return null;
    return _api.getLatestStatus(runtimeId);
  }

  /// Persist a log entry for the current runtime.
  Future<void> postLog(
    String level,
    String message, {
    Map<String, dynamic>? payload,
  }) async {
    final runtimeId = _currentRuntimeId;
    if (runtimeId == null) {
      throw StateError('No current runtime selected');
    }
    await _api.postLog(runtimeId, level, message, payload: payload);
  }

  /// Query logs for the current runtime.
  Future<List<LogEntry>> getLogs({int limit = 100}) async {
    final runtimeId = _currentRuntimeId;
    if (runtimeId == null) return [];
    return _api.getLogs(runtimeId, limit: limit);
  }

  /// Enqueue a command for the current runtime.
  Future<void> sendCommand(
    String type, {
    Map<String, dynamic>? payload,
  }) async {
    final runtimeId = _currentRuntimeId;
    if (runtimeId == null) {
      throw StateError('No current runtime selected');
    }
    await _api.sendCommand(runtimeId, type, payload: payload);
  }

  /// Fetch pending commands for the current runtime.
  Future<List<RemoteCommand>> getPendingCommands() async {
    final runtimeId = _currentRuntimeId;
    if (runtimeId == null) return [];
    return _api.getPendingCommands(runtimeId);
  }
}
