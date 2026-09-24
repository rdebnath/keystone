import 'package:flutter/material.dart';

/// Placeholder inventory home — shown to tenant users after login.
class InventoryHomeScreen extends StatelessWidget {
  const InventoryHomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Inventory')),
      body: const Center(child: Text('Inventory items (to be added).')),
    );
  }
}
