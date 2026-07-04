import 'package:flutter/material.dart';

import '../models/agent_status.dart';

/// List tile that displays an agent's id, status, and progress.
class AgentListTile extends StatelessWidget {
  const AgentListTile({
    super.key,
    required this.agent,
  });

  final AgentStatus agent;

  Color _statusColor(String status) {
    final lowered = status.toLowerCase();
    if (lowered == 'online' || lowered == 'active' || lowered == 'running') {
      return Colors.green;
    }
    if (lowered == 'offline' || lowered == 'error') {
      return Colors.red;
    }
    if (lowered == 'paused' || lowered == 'warning') {
      return Colors.orange;
    }
    return Colors.grey;
  }

  @override
  Widget build(BuildContext context) {
    final progress = agent.progress ?? 0.0;
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    agent.agentId,
                    style: Theme.of(context).textTheme.titleMedium,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: _statusColor(agent.status).withOpacity(0.15),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Text(
                    agent.status,
                    style: TextStyle(
                      color: _statusColor(agent.status),
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            LinearProgressIndicator(
              value: progress.clamp(0.0, 1.0),
              backgroundColor: Colors.grey.shade300,
              valueColor: AlwaysStoppedAnimation<Color>(
                _statusColor(agent.status),
              ),
            ),
            const SizedBox(height: 4),
            Text(
              '${(progress * 100).toStringAsFixed(0)}%',
              style: Theme.of(context).textTheme.bodySmall,
            ),
          ],
        ),
      ),
    );
  }
}
