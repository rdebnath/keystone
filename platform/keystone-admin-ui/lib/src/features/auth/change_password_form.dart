import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/errors.dart';
import '../../core/log.dart';
import '../../core/password_field.dart';
import '../../core/providers.dart';
import 'auth_service.dart';

/// The change-password fields, shared by the two hosts so the flows cannot drift: the forced first-login
/// gate ([ChangePasswordScreen], `requiresCurrentPassword: false`) and the voluntary dialog
/// (`showChangePasswordDialog`, `requiresCurrentPassword: true`).
///
/// The backend mirrors this rule: it demands (and verifies against Supabase Auth) `currentPassword`
/// unless the caller is in the forced first-login state, where the password just used to sign in is the
/// proof. A rejected current password comes back as the RFC 9457 `detail` and is shown as is.
class ChangePasswordForm extends ConsumerStatefulWidget {
  const ChangePasswordForm({
    super.key,
    required this.requiresCurrentPassword,
    required this.onSaved,
    this.submitLabel = 'Save password',
  });

  /// Adds the "Current password" field — the voluntary flow's proof of ownership.
  final bool requiresCurrentPassword;

  /// Called after a successful change (and after `/me` is refreshed).
  final VoidCallback onSaved;

  final String submitLabel;

  @override
  ConsumerState<ChangePasswordForm> createState() => _ChangePasswordFormState();
}

class _ChangePasswordFormState extends ConsumerState<ChangePasswordForm> {
  final _formKey = GlobalKey<FormState>();
  final _current = TextEditingController();
  final _password = TextEditingController();
  final _confirm = TextEditingController();
  final _passwordFocus = FocusNode();
  final _confirmFocus = FocusNode();
  bool _submitting = false;

  @override
  void dispose() {
    _current.dispose();
    _password.dispose();
    _confirm.dispose();
    _passwordFocus.dispose();
    _confirmFocus.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    // Re-entrancy guard: Enter and a button activation can both land on the same frame.
    if (_submitting || !_formKey.currentState!.validate()) {
      return;
    }
    if (_password.text != _confirm.text) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Passwords do not match.')));
      return;
    }
    setState(() => _submitting = true);
    try {
      await ref
          .read(authServiceProvider)
          .changePassword(
            _password.text,
            currentPassword: widget.requiresCurrentPassword
                ? _current.text
                : null,
          );
      ref.invalidate(meProvider);
      if (mounted) {
        setState(() => _submitting = false);
      }
      widget.onSaved();
    } catch (error) {
      log.e('password change failed', error: error);
      if (mounted) {
        setState(() => _submitting = false);
        showApiError(context, error, 'Could not change password.');
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Form(
      key: _formKey,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (widget.requiresCurrentPassword) ...[
            PasswordField(
              controller: _current,
              autofocus: true,
              textInputAction: TextInputAction.next,
              onFieldSubmitted: (_) => _passwordFocus.requestFocus(),
              labelText: 'Current password',
            ),
            const SizedBox(height: 12),
          ],
          PasswordField(
            controller: _password,
            focusNode: _passwordFocus,
            autofocus: !widget.requiresCurrentPassword,
            textInputAction: TextInputAction.next,
            onFieldSubmitted: (_) => _confirmFocus.requestFocus(),
            labelText: 'New password',
            validator: (value) => value == null || value.length < 8
                ? 'At least 8 characters'
                : null,
          ),
          const SizedBox(height: 12),
          PasswordField(
            controller: _confirm,
            focusNode: _confirmFocus,
            textInputAction: TextInputAction.done,
            onFieldSubmitted: (_) => _submit(),
            labelText: 'Confirm password',
            validator: (value) =>
                value == null || value.isEmpty ? 'Required' : null,
          ),
          const SizedBox(height: 24),
          FilledButton(
            onPressed: _submitting ? null : _submit,
            child: Text(widget.submitLabel),
          ),
        ],
      ),
    );
  }
}
