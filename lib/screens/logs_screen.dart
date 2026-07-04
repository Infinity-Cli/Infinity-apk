import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../models/log_entry.dart';
import '../providers/api_provider.dart';
import '../services/api_service.dart';
import '../widgets/log_level_chip.dart';

/// Screen that displays log entries for the current runtime.
class LogsScreen extends StatefulWidget {
  const LogsScreen({
    super.key,
    this.api,
    this.limit = 100,
  });

  /// Optional API client. When omitted the screen reads [ApiProvider] from context.
  final InfinityApi? api;

  /// Maximum number of log entries to fetch.
  final int limit;

  @override
  State<LogsScreen> createState() => _LogsScreenState();
}

class _LogsScreenState extends State<LogsScreen> {
  List<LogEntry> _logs = [];
  bool _isLoading = false;
  String? _error;

  InfinityApi get _api =>
      widget.api ?? Provider.of<ApiProvider>(context, listen: false).api;

  ApiProvider? get _provider =>
      widget.api == null ? Provider.of<ApiProvider>(context, listen: false) : null;

  @override
  void initState() {
    super.initState();
    _loadLogs();
  }

  Future<void> _loadLogs() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final runtimeId = _provider?.currentRuntimeId;
      final logs = runtimeId != null
          ? await _api.getLogs(runtimeId, limit: widget.limit)
          : <LogEntry>[];
      if (mounted) {
        setState(() {
          _logs = logs;
          _isLoading = false;
        });
      }
    } on Exception catch (e) {
      if (mounted) {
        setState(() {
          _error = e.toString();
          _isLoading = false;
        });
      }
    }
  }

  void _clearLogs() {
    setState(() => _logs = []);
  }

  @override
  Widget build(BuildContext context) {
    final sortedLogs = _logs.toList()
      ..sort((a, b) => b.timestamp.compareTo(a.timestamp));

    return Scaffold(
      body: RefreshIndicator(
        onRefresh: _loadLogs,
        child: _isLoading && sortedLogs.isEmpty
            ? const Center(child: CircularProgressIndicator())
            : sortedLogs.isEmpty
                ? LayoutBuilder(
                    builder: (context, constraints) {
                      return SingleChildScrollView(
                        physics: const AlwaysScrollableScrollPhysics(),
                        child: ConstrainedBox(
                          constraints: BoxConstraints(
                            minHeight: constraints.maxHeight,
                          ),
                          child: Center(
                            child: Padding(
                              padding: const EdgeInsets.all(16),
                              child: Text(
                                _error ??
                                    'No logs available.\nPull to refresh.',
                                textAlign: TextAlign.center,
                              ),
                            ),
                          ),
                        ),
                      );
                    },
                  )
                : ListView.builder(
                    physics: const AlwaysScrollableScrollPhysics(),
                    reverse: true,
                    itemCount: sortedLogs.length,
                    itemBuilder: (context, index) {
                      final log = sortedLogs[index];
                      return _LogListTile(log: log);
                    },
                  ),
      ),
      floatingActionButton: FloatingActionButton.small(
        onPressed: _clearLogs,
        tooltip: 'Clear logs',
        child: const Icon(Icons.clear_all),
      ),
    );
  }
}

class _LogListTile extends StatelessWidget {
  const _LogListTile({required this.log});

  final LogEntry log;

  @override
  Widget build(BuildContext context) {
    final timestamp = DateFormat.Hms().format(log.timestamp.toLocal());
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                LogLevelChip(level: log.level),
                const SizedBox(width: 8),
                Text(
                  timestamp,
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ],
            ),
            const SizedBox(height: 8),
            SelectableText(log.message),
          ],
        ),
      ),
    );
  }
}
