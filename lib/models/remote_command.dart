/// A command queued in Infinity-api for a runtime to process.
class RemoteCommand {
  final String id;
  final String runtimeId;
  final String type;
  final Map<String, dynamic>? payload;
  final String status;

  const RemoteCommand({
    required this.id,
    required this.runtimeId,
    required this.type,
    this.payload,
    required this.status,
  });

  factory RemoteCommand.fromJson(Map<String, dynamic> json) {
    return RemoteCommand(
      id: (json['id'] as String?) ?? '',
      runtimeId: (json['runtime_id'] as String?) ?? '',
      type: (json['type'] as String?) ?? '',
      payload: json['payload'] as Map<String, dynamic>?,
      status: (json['status'] as String?) ?? 'pending',
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'runtime_id': runtimeId,
        'type': type,
        if (payload != null) 'payload': payload,
        'status': status,
      };
}
