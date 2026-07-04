import 'package:flutter/material.dart';

/// A small colored dot and text that reflects an online/offline state.
class ConnectionStatusIndicator extends StatelessWidget {
  const ConnectionStatusIndicator({
    super.key,
    required this.online,
    this.label,
  });

  final bool online;
  final String? label;

  @override
  Widget build(BuildContext context) {
    final color = online ? Colors.green : Colors.red;
    final text = label ?? (online ? 'Online' : 'Offline');
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 12,
          height: 12,
          decoration: BoxDecoration(
            color: color,
            shape: BoxShape.circle,
          ),
        ),
        const SizedBox(width: 8),
        Text(
          text,
          style: TextStyle(color: color, fontWeight: FontWeight.bold),
        ),
      ],
    );
  }
}
