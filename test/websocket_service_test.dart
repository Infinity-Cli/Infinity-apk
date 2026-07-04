import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:infinity_apk/services/websocket_service.dart';

void main() {
  group('MockWebSocketService', () {
    test('emits sent messages on the events stream', () async {
      final ws = MockWebSocketService();
      await ws.connect('ws://localhost/ws/runtime-1');

      final events = <dynamic>[];
      final subscription = ws.events.listen(events.add);

      const message = <String, dynamic>{'type': 'ping'};
      await ws.send(message);
      await Future<void>.value();

      expect(ws.isConnected, isTrue);
      expect(ws.sentMessages, hasLength(1));
      expect(ws.sentMessages.first, message);
      expect(events, hasLength(1));
      expect(events.first, message);

      await subscription.cancel();
      await ws.disconnect();
    });

    test('handles disconnect and records connection state', () async {
      final ws = MockWebSocketService();
      expect(ws.isConnected, isFalse);

      await ws.connect('ws://localhost/ws/runtime-1');
      expect(ws.isConnected, isTrue);

      await ws.disconnect();
      expect(ws.isConnected, isFalse);
    });

    test('emits injected server events', () async {
      final ws = MockWebSocketService();
      await ws.connect('ws://localhost/ws/runtime-1');

      final events = <dynamic>[];
      final subscription = ws.events.listen(events.add);

      const event = <String, dynamic>{'type': 'status', 'data': {'status': 'online'}};
      ws.emit(event);
      await Future<void>.value();

      expect(events, hasLength(1));
      expect(events.first, event);

      await subscription.cancel();
      await ws.disconnect();
    });

    test('throws when sending while disconnected', () async {
      final ws = MockWebSocketService();
      expect(
        () => ws.send(<String, dynamic>{}),
        throwsA(isA<InfinityWebSocketException>()),
      );
    });
  });

  group('WebSocketChannelService', () {
    test('starts disconnected', () {
      final ws = WebSocketChannelService(maxRetries: 0);
      expect(ws.isConnected, isFalse);
      ws.dispose();
    });

    test('disconnect cancels reconnect and sets isConnected false', () async {
      final ws = WebSocketChannelService(maxRetries: null);
      await ws.connect('ws://localhost:0/ws/invalid', headers: {});
      // The connection will fail; ensure disconnect cleans up.
      await ws.disconnect();
      expect(ws.isConnected, isFalse);
    });
  });
}
