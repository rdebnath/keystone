import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/branding.dart';
import '../../core/log.dart';
import '../../core/password_field.dart';
import '../../core/providers.dart';
import 'auth_service.dart';

class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _identifier = TextEditingController();
  final _password = TextEditingController();
  final _passwordFocus = FocusNode();
  bool _submitting = false;

  @override
  void dispose() {
    _identifier.dispose();
    _password.dispose();
    _passwordFocus.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    // Re-entrancy guard: Enter and a button activation can both land on the same frame.
    if (_submitting || !_formKey.currentState!.validate()) {
      return;
    }
    setState(() => _submitting = true);
    try {
      await ref
          .read(authServiceProvider)
          .signIn(_identifier.text.trim(), _password.text);
      ref.read(signedInProvider.notifier).state = true;
      ref.invalidate(meProvider);
      // The router redirects once the auth state (and /me) resolve.
    } catch (e) {
      log.e('sign-in failed', error: e);
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('Sign in failed.')));
      }
    } finally {
      if (mounted) {
        setState(() => _submitting = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final title = ref.watch(appBrandingProvider).title;
    return Scaffold(
      body: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 400),
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Form(
              key: _formKey,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(
                    title,
                    textAlign: TextAlign.center,
                    style: Theme.of(context).textTheme.headlineMedium,
                  ),
                  const SizedBox(height: 32),
                  TextFormField(
                    controller: _identifier,
                    autofocus: true,
                    textInputAction: TextInputAction.next,
                    onFieldSubmitted: (_) => _passwordFocus.requestFocus(),
                    decoration: const InputDecoration(
                      labelText: 'Username',
                      hintText: 'username@tenantid',
                    ),
                    validator: (v) =>
                        v == null || v.isEmpty ? 'Username is required' : null,
                  ),
                  const SizedBox(height: 12),
                  PasswordField(
                    controller: _password,
                    focusNode: _passwordFocus,
                    textInputAction: TextInputAction.done,
                    onFieldSubmitted: (_) => _submit(),
                    labelText: 'Password',
                    validator: (v) =>
                        v == null || v.isEmpty ? 'Password is required' : null,
                  ),
                  const SizedBox(height: 24),
                  FilledButton(
                    onPressed: _submitting ? null : _submit,
                    child: const Text('Sign in'),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
