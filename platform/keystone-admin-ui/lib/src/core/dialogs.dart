import 'package:flutter/material.dart';

/// Splits a comma-separated input into trimmed, non-empty values.
List<String> splitList(String value) {
  return value
      .split(',')
      .map((e) => e.trim())
      .where((e) => e.isNotEmpty)
      .toList();
}

/// Shows a single-field prompt dialog and returns the entered text, or null on cancel.
Future<String?> promptText(
  BuildContext context, {
  required String title,
  required String label,
}) {
  final controller = TextEditingController();
  return showDialog<String>(
    context: context,
    builder: (ctx) => AlertDialog(
      title: Text(title),
      content: TextField(
        controller: controller,
        autofocus: true,
        decoration: InputDecoration(labelText: label),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(ctx),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: () => Navigator.pop(ctx, controller.text),
          child: const Text('Create'),
        ),
      ],
    ),
  );
}
