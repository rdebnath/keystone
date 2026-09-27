import 'package:flutter/material.dart';

import '../../core/errors.dart';
import 'change_password_form.dart';

/// Opens the voluntary change-password dialog — the non-forced counterpart of `/change-password`, which
/// the router redirects away from as soon as `mustChangePassword` is false.
///
/// Every signed-in caller may change their own password; the dialog therefore asks for the current
/// password, which the backend verifies against Supabase Auth (a rejected one comes back as the RFC 9457
/// `detail`). Returns whether the password was changed.
Future<bool> showChangePasswordDialog(BuildContext context) async {
  final saved = await showDialog<bool>(
    context: context,
    builder: (dialogContext) => AlertDialog(
      title: const Text('Change password'),
      content: SizedBox(
        width: 380,
        child: SingleChildScrollView(
          child: ChangePasswordForm(
            requiresCurrentPassword: true,
            submitLabel: 'Change password',
            onSaved: () => Navigator.of(dialogContext).pop(true),
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(dialogContext).pop(false),
          child: const Text('Cancel'),
        ),
      ],
    ),
  );
  if ((saved ?? false) && context.mounted) {
    showApiSuccess(context, 'Password changed.');
  }
  return saved ?? false;
}
