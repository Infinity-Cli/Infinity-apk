/// Latest status reported for an Infinity agent/runtime.
class AgentStatus {
  final String agentId;
  final String status;
  final double? progress;
  final Map<String, dynamic>? payload;

  const AgentStatus({
    required this.agentId,
    required this.status,
    this.progress,
    this.payload,
  });

  factory AgentStatus.fromJson(Map<String, dynamic> json) {
    final rawProgress = json['progress'];
    return AgentStatus(
      agentId: (json['agent_id'] as String?) ??
          (json['agentId'] as String?) ??
          (json['runtime_id'] as String? ?? ''),
      status: (json['status'] as String?) ?? 'unknown',
      progress: rawProgress is num ? rawProgress.toDouble() : null,
      payload: json['payload'] as Map<String, dynamic>?,
    );
  }

  Map<String, dynamic> toJson() => {
        'agent_id': agentId,
        'status': status,
        if (progress != null) 'progress': progress,
        if (payload != null) 'payload': payload,
      };
}
