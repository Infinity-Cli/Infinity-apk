import 'package:flutter/material.dart';

/// A compact chip that color-codes a log level.
class LogLevelChip extends StatelessWidget {
  const LogLevelChip({
    super.key,
    required this.level,
  });

  final String level;

  Color get _color {
    final lowered = level.toLowerCase();
    if (lowered.contains('error') || lowered.contains('severe') || lowered.contains('fatal')) {
      return Colors.red;
    }
    if (lowered.contains('warn')) {
      return Colors.orange;
    }
    if (lowered.contains('info')) {
      return Colors.blue;
    }
    if (lowered.contains('debug')) {
      return Colors.purple;
    }
    return Colors.grey;
  }

  @override
  Widget build(BuildContext context) {
    return Chip(
      label: Text(
        level.toUpperCase(),
        style: const TextStyle(fontSize: 10, fontWeight: FontWeight.bold),
      ),
      backgroundColor: _color.withOpacity(0.15),
      labelStyle: TextStyle(color: _color),
      padding: EdgeInsets.zero,
      materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
    );
  }
}
