import 'dart:async';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/runtime.dart';
import '../providers/api_provider.dart';
import '../services/api_service.dart';
import '../services/websocket_service.dart';
import '../widgets/connection_status_indicator.dart';
import '../widgets/progress_summary_card.dart';
import 'agents_screen.dart';
import 'logs_screen.dart';

/// Main dashboard screen with runtime controls and tab navigation.
class DashboardScreen extends StatefulWidget {
  const DashboardScreen({
    super.key,
    this.api,
    this.webSocket,
  });

  /// Optional API client. When omitted the screen reads [ApiProvider] from context.
  final InfinityApi? api;

  /// Optional WebSocket used to reflect connection state.
  final InfinityWebSocket? webSocket;

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  int _tabIndex = 0;
  bool _online = false;
  String? _currentRuntimeId;
  List<Runtime> _runtimes = [];
  bool _isLoadingRuntimes = false;
  String? _error;
  StreamSubscription? _wsSubscription;
  Timer? _connectionPoller;

  InfinityApi get _api =>
      widget.api ?? Provider.of<ApiProvider>(context, listen: false).api;

  ApiProvider? get _provider =>
      widget.api == null ? Provider.of<ApiProvider>(context, listen: false) : null;

  @override
  void initState() {
    super.initState();
    _currentRuntimeId = _provider?.currentRuntimeId;
    _loadRuntimes();
    _updateOnlineStatus();

    final ws = widget.webSocket;
    if (ws != null) {
      _wsSubscription = ws.events.listen(
        _onWebSocketEvent,
        onError: (_) => _updateOnlineStatus(),
        onDone: _updateOnlineStatus,
      );
      // Also poll for connection changes in case the mock does not emit events.
      _connectionPoller = Timer.periodic(
        const Duration(milliseconds: 100),
        (_) => _updateOnlineStatus(),
      );
    } else {
      _online = _provider?.online ?? false;
    }
  }

  @override
  void dispose() {
    _wsSubscription?.cancel();
    _connectionPoller?.cancel();
    super.dispose();
  }

  void _onWebSocketEvent(dynamic event) {
    _updateOnlineStatus();
  }

  void _updateOnlineStatus() {
    final ws = widget.webSocket;
    final online = ws?.isConnected ?? _provider?.online ?? false;
    if (online != _online && mounted) {
      setState(() => _online = online);
    }
  }

  Future<void> _loadRuntimes() async {
    setState(() {
      _isLoadingRuntimes = true;
      _error = null;
    });
    try {
      final runtimes = await _api.listRuntimes();
      if (mounted) {
        setState(() {
          _runtimes = runtimes;
          _isLoadingRuntimes = false;
          if (_currentRuntimeId == null && runtimes.isNotEmpty) {
            _currentRuntimeId = runtimes.first.id;
            _provider?.setCurrentRuntimeId(_currentRuntimeId!);
          }
        });
      }
    } on Exception catch (e) {
      if (mounted) {
        setState(() {
          _error = e.toString();
          _isLoadingRuntimes = false;
        });
      }
    }
  }

  Future<void> _sendCommand(String type) async {
    final runtimeId = _currentRuntimeId;
    if (runtimeId == null) {
      _showError('No runtime selected');
      return;
    }
    try {
      await _api.sendCommand(runtimeId, type);
    } on Exception catch (e) {
      _showError(e.toString());
    }
  }

  void _showError(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message)),
    );
  }

  void _setRuntimeId(String? runtimeId) {
    if (runtimeId == null) return;
    setState(() => _currentRuntimeId = runtimeId);
    _provider?.setCurrentRuntimeId(runtimeId);
  }

  Widget _buildDashboardTab() {
    return RefreshIndicator(
      onRefresh: _loadRuntimes,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        'Connection',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      ConnectionStatusIndicator(online: _online),
                    ],
                  ),
                  const SizedBox(height: 16),
                  Text(
                    'Runtime',
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  const SizedBox(height: 8),
                  if (_isLoadingRuntimes)
                    const LinearProgressIndicator()
                  else if (_runtimes.isEmpty)
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        TextField(
                          decoration: const InputDecoration(
                            labelText: 'Runtime ID',
                            hintText: 'Enter runtime id',
                          ),
                          onChanged: _setRuntimeId,
                        ),
                        if (_currentRuntimeId != null)
                          Padding(
                            padding: const EdgeInsets.only(top: 8),
                            child: Text(
                              'Current: $_currentRuntimeId',
                              style: Theme.of(context).textTheme.bodySmall,
                            ),
                          ),
                      ],
                    )
                  else
                    DropdownButtonFormField<String>(
                      value: _currentRuntimeId,
                      isExpanded: true,
                      decoration: const InputDecoration(
                        labelText: 'Selected runtime',
                        border: OutlineInputBorder(),
                      ),
                      items: _runtimes.map((runtime) {
                        return DropdownMenuItem<String>(
                          value: runtime.id,
                          child: Text(
                            runtime.name ?? runtime.id,
                            overflow: TextOverflow.ellipsis,
                          ),
                        );
                      }).toList(),
                      onChanged: _setRuntimeId,
                    ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          ProgressSummaryCard(
            title: 'Overall Progress',
            subtitle: _currentRuntimeId != null
                ? 'Runtime: $_currentRuntimeId'
                : 'No runtime selected',
          ),
          const SizedBox(height: 16),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              ElevatedButton.icon(
                key: const Key('pauseButton'),
                onPressed: _currentRuntimeId == null
                    ? null
                    : () => _sendCommand('pause'),
                icon: const Icon(Icons.pause),
                label: const Text('Pause'),
              ),
              ElevatedButton.icon(
                key: const Key('resumeButton'),
                onPressed: _currentRuntimeId == null
                    ? null
                    : () => _sendCommand('resume'),
                icon: const Icon(Icons.play_arrow),
                label: const Text('Resume'),
              ),
              ElevatedButton.icon(
                key: const Key('startButton'),
                onPressed: _currentRuntimeId == null
                    ? null
                    : () => _sendCommand('start'),
                icon: const Icon(Icons.restart_alt),
                label: const Text('Start'),
              ),
            ],
          ),
          if (_error != null) ...[
            const SizedBox(height: 16),
            Text(
              _error!,
              style: TextStyle(color: Theme.of(context).colorScheme.error),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildBody() {
    switch (_tabIndex) {
      case 0:
        return _buildDashboardTab();
      case 1:
        return AgentsScreen(api: widget.api);
      case 2:
        return LogsScreen(api: widget.api);
      default:
        return _buildDashboardTab();
    }
  }

  @override
  Widget build(BuildContext context) {
    final titles = const ['Dashboard', 'Agents', 'Logs'];
    return Scaffold(
      appBar: AppBar(
        title: Text(titles[_tabIndex]),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 16),
            child: ConnectionStatusIndicator(online: _online),
          ),
        ],
      ),
      body: _buildBody(),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _tabIndex,
        onTap: (index) => setState(() => _tabIndex = index),
        items: const [
          BottomNavigationBarItem(
            icon: Icon(Icons.dashboard),
            label: 'Dashboard',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.people),
            label: 'Agents',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.article),
            label: 'Logs',
          ),
        ],
      ),
    );
  }
}
