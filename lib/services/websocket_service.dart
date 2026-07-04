import 'dart:async';
import 'dart:convert';

import 'package:web_socket_channel/io.dart';
import 'package:web_socket_channel/web_socket_channel.dart';

/// Exception thrown when a WebSocket operation fails.
class InfinityWebSocketException implements Exception {
  final String message;

  const InfinityWebSocketException(this.message);

  @override
  String toString() => 'InfinityWebSocketException: $message';
}

/// Platform-agnostic Infinity WebSocket client.
abstract class InfinityWebSocket {
  /// Incoming decoded events.
  Stream<dynamic> get events;

  /// Connect to [url] with optional [headers].
  Future<void> connect(String url, {Map<String, String>? headers});

  /// Send a JSON-serializable [message].
  Future<void> send(dynamic message);

  /// Disconnect and stop automatic reconnects.
  Future<void> disconnect();

  /// Whether the WebSocket is currently connected.
  bool get isConnected;
}

/// [InfinityWebSocket] implementation backed by [web_socket_channel].
class WebSocketChannelService implements InfinityWebSocket {
  WebSocketChannelService({
    this.maxRetries,
    this.heartbeatInterval = const Duration(seconds: 15),
    this.maxBackoff = const Duration(seconds: 30),
  });

  /// Maximum reconnect attempts; `null` means unlimited.
  final int? maxRetries;

  /// Interval between JSON heartbeat pings.
  final Duration heartbeatInterval;

  /// Maximum exponential backoff between reconnect attempts.
  final Duration maxBackoff;

  WebSocketChannel? _channel;
  final StreamController<dynamic> _events = StreamController<dynamic>.broadcast();

  String? _url;
  Map<String, String>? _headers;
  bool _shouldReconnect = true;
  int _retryCount = 0;
  Timer? _heartbeatTimer;
  Timer? _reconnectTimer;

  @override
  Stream<dynamic> get events => _events.stream;

  @override
  bool get isConnected {
    final channel = _channel;
    return channel != null && channel.closeCode == null;
  }

  @override
  Future<void> connect(String url, {Map<String, String>? headers}) async {
    _url = url;
    _headers = headers;
    _shouldReconnect = true;
    _retryCount = 0;
    await _connect();
  }

  Future<void> _connect() async {
    final url = _url;
    if (url == null) return;

    await _disconnectChannel();

    try {
      _channel = IOWebSocketChannel.connect(
        Uri.parse(url),
        headers: _headers,
        pingInterval: const Duration(seconds: 30),
      );

      _channel!.stream.listen(
        _onData,
        onError: _onError,
        onDone: _onDone,
        cancelOnError: false,
      );

      _retryCount = 0;
      _startHeartbeat();
    } on Exception catch (error) {
      _events.addError(error);
      _scheduleReconnect();
    }
  }

  void _onData(dynamic data) {
    dynamic decoded;
    if (data is String) {
      try {
        decoded = jsonDecode(data);
      } on FormatException {
        decoded = data;
      }
    } else {
      decoded = data;
    }

    // Absorb transport-level pongs; keep JSON pings visible for callers.
    if (decoded is Map<String, dynamic> && decoded['type'] == 'pong') {
      return;
    }

    _events.add(decoded);
  }

  void _onError(Object error) {
    _events.addError(error);
    _scheduleReconnect();
  }

  void _onDone() {
    _stopHeartbeat();
    if (_shouldReconnect) {
      _scheduleReconnect();
    }
  }

  void _scheduleReconnect() {
    _stopHeartbeat();

    if (!_shouldReconnect) return;
    if (maxRetries != null && _retryCount >= maxRetries!) return;

    final delay = Duration(
      milliseconds: min(
        const Duration(seconds: 1).inMilliseconds * (1 << _retryCount),
        maxBackoff.inMilliseconds,
      ),
    );
    _retryCount++;

    _reconnectTimer?.cancel();
    _reconnectTimer = Timer(delay, () {
      _connect();
    });
  }

  void _startHeartbeat() {
    _heartbeatTimer?.cancel();
    _heartbeatTimer = Timer.periodic(heartbeatInterval, (_) {
      if (isConnected) {
        _channel?.sink.add(jsonEncode({'type': 'ping'}));
      }
    });
  }

  void _stopHeartbeat() {
    _heartbeatTimer?.cancel();
    _heartbeatTimer = null;
  }

  Future<void> _disconnectChannel() async {
    _stopHeartbeat();
    await _channel?.sink.close();
    _channel = null;
  }

  @override
  Future<void> send(dynamic message) async {
    if (!isConnected) {
      throw const InfinityWebSocketException('not connected');
    }
    final payload = message is String ? message : jsonEncode(message);
    _channel?.sink.add(payload);
  }

  @override
  Future<void> disconnect() async {
    _shouldReconnect = false;
    _reconnectTimer?.cancel();
    _reconnectTimer = null;
    await _disconnectChannel();
  }

  /// Releases internal controllers. Call when the service is no longer needed.
  void dispose() {
    disconnect();
    if (!_events.isClosed) {
      _events.close();
    }
  }
}

int min(int a, int b) => a < b ? a : b;

/// In-memory [InfinityWebSocket] implementation for tests.
class MockWebSocketService implements InfinityWebSocket {
  final StreamController<dynamic> _events = StreamController<dynamic>.broadcast();
  final List<dynamic> sentMessages = [];
  bool _isConnected = false;

  @override
  Stream<dynamic> get events => _events.stream;

  @override
  bool get isConnected => _isConnected;

  @override
  Future<void> connect(String url, {Map<String, String>? headers}) async {
    _isConnected = true;
  }

  @override
  Future<void> send(dynamic message) async {
    if (!_isConnected) {
      throw const InfinityWebSocketException('not connected');
    }
    sentMessages.add(message);
    _events.add(message);
  }

  @override
  Future<void> disconnect() async {
    _isConnected = false;
    await _events.close();
  }

  /// Simulate an incoming event from the server.
  void emit(dynamic event) => _events.add(event);

  /// Simulate a connection state change for tests.
  ///
  /// Updates [isConnected] and optionally emits a [status] event.
  void simulateConnectionChange(bool connected, {String? status}) {
    _isConnected = connected;
    if (status != null) {
      _events.add({'type': 'status', 'status': status, 'online': connected});
    } else {
      _events.add({'type': 'connection', 'online': connected});
    }
  }
}
