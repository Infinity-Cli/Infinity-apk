/// A registered Infinity runtime.
class Runtime {
  final String id;
  final String? name;
  final bool online;
  final DateTime? lastSeen;

  const Runtime({
    required this.id,
    this.name,
    this.online = false,
    this.lastSeen,
  });

  factory Runtime.fromJson(Map<String, dynamic> json) {
    return Runtime(
      id: (json['runtime_id'] as String?) ?? (json['id'] as String? ?? ''),
      name: json['name'] as String?,
      online: json['online'] as bool? ?? false,
      lastSeen: _parseDateTime(json['last_seen'] ?? json['timestamp']),
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        if (name != null) 'name': name,
        'online': online,
        if (lastSeen != null) 'last_seen': lastSeen!.toIso8601String(),
      };

  static DateTime? _parseDateTime(dynamic value) {
    if (value == null) return null;
    if (value is DateTime) return value;
    if (value is String) {
      try {
        return DateTime.parse(value);
      } on FormatException {
        return null;
      }
    }
    return null;
  }
}
