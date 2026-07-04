import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/agent_status.dart';
import '../models/runtime.dart';
import '../providers/api_provider.dart';
import '../services/api_service.dart';
import '../widgets/agent_list_tile.dart';

/// Screen that lists agent statuses for the available runtimes.
class AgentsScreen extends StatefulWidget {
  const AgentsScreen({
    super.key,
    this.api,
  });

  /// Optional API client. When omitted the screen reads [ApiProvider] from context.
  final InfinityApi? api;

  @override
  State<AgentsScreen> createState() => _AgentsScreenState();
}

class _AgentsScreenState extends State<AgentsScreen> {
  List<AgentStatus> _agents = [];
  bool _isLoading = false;
  String? _error;

  InfinityApi get _api =>
      widget.api ?? Provider.of<ApiProvider>(context, listen: false).api;

  ApiProvider? get _provider =>
      widget.api == null ? Provider.of<ApiProvider>(context, listen: false) : null;

  @override
  void initState() {
    super.initState();
    _loadAgents();
  }

  Future<void> _loadAgents() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final agents = <AgentStatus>[];
      final runtimeId = _provider?.currentRuntimeId;
      if (runtimeId != null) {
        final status = await _api.getLatestStatus(runtimeId);
        if (status != null) {
          agents.add(status);
        }
      } else {
        final runtimes = await _api.listRuntimes();
        for (final runtime in runtimes) {
          final status = await _api.getLatestStatus(runtime.id);
          if (status != null) {
            agents.add(status);
          }
        }
      }
      if (mounted) {
        setState(() {
          _agents = agents;
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

  @override
  Widget build(BuildContext context) {
    if (_isLoading && _agents.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }

    return RefreshIndicator(
      onRefresh: _loadAgents,
      child: _agents.isEmpty
          ? LayoutBuilder(
              builder: (context, constraints) {
                return SingleChildScrollView(
                  physics: const AlwaysScrollableScrollPhysics(),
                  child: ConstrainedBox(
                    constraints: BoxConstraints(minHeight: constraints.maxHeight),
                    child: Center(
                      child: Padding(
                        padding: const EdgeInsets.all(16),
                        child: Text(
                          _error ?? 'No agents found.\nPull to refresh.',
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
              itemCount: _agents.length,
              itemBuilder: (context, index) {
                return AgentListTile(agent: _agents[index]);
              },
            ),
    );
  }
}
