/// A single log entry persisted by Infinity-api.
class LogEntry {
  final String id;
  final String runtimeId;
  final String level;
  final String message;
  final DateTime timestamp;
  final Map<String, dynamic>? payload;

  const LogEntry({
    required this.id,
    required this.runtimeId,
    required this.level,
    required this.message,
    required this.timestamp,
    this.payload,
  });

  factory LogEntry.fromJson(Map<String, dynamic> json) {
    return LogEntry(
      id: (json['id'] as String?) ?? '',
      runtimeId: (json['runtime_id'] as String?) ?? '',
      level: (json['level'] as String?) ?? 'INFO',
      message: (json['message'] as String?) ?? '',
      timestamp: _parseDateTime(json['timestamp']) ?? DateTime.now().toUtc(),
      payload: json['payload'] as Map<String, dynamic>?,
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'runtime_id': runtimeId,
        'level': level,
        'message': message,
        'timestamp': timestamp.toIso8601String(),
        if (payload != null) 'payload': payload,
      };

  static DateTime? _parseDateTime(dynamic value) {
    if (value == null) return null;
    if (value is DateTime) return value.toUtc();
    if (value is String) {
      try {
        return DateTime.parse(value).toUtc();
      } on FormatException {
        return null;
      }
    }
    return null;
  }
}
