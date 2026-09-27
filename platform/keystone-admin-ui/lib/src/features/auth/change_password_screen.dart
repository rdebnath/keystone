import 'package:flutter/material.dart';

import 'change_password_form.dart';

/// Shown on first login: the user must set a new password before reaching the app.
///
/// This is the *forced* gate, so no current password is asked for — the one just used to sign in is the
/// proof, and the backend accepts the change because `users.must_change_password` is true. The voluntary
/// flow (`showChangePasswordDialog`) shares the same form with the current-password field.
class ChangePasswordScreen extends StatelessWidget {
  const ChangePasswordScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Set a new password')),
      body: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 400),
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(
                  'You must change your password before continuing.',
                  style: Theme.of(context).textTheme.bodyLarge,
                ),
                const SizedBox(height: 24),
                // The router redirects to the app once /me shows mustChangePassword == false.
                ChangePasswordForm(
                  requiresCurrentPassword: false,
                  onSaved: () {},
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
