import 'package:flutter/material.dart';

/// A password input that can be revealed in place: one [TextFormField] plus a trailing visibility
/// toggle, so a user can check what they typed — the value, the focus and the submitted body are
/// unaffected by toggling.
///
/// It stays a single form field internally (the toggle is the field's `suffixIcon`), so it drops in
/// wherever a password `TextFormField` was used: the login screen, the first-login gate, the
/// change-password dialog, the reset dialog and the user editor.
class PasswordField extends StatefulWidget {
  const PasswordField({
    super.key,
    required this.controller,
    required this.labelText,
    this.helperText,
    this.validator,
    this.autofocus = false,
    this.textInputAction,
    this.onFieldSubmitted,
    this.focusNode,
  });

  final TextEditingController controller;

  final String labelText;

  final String? helperText;

  final FormFieldValidator<String>? validator;

  final bool autofocus;

  final TextInputAction? textInputAction;

  final ValueChanged<String>? onFieldSubmitted;

  final FocusNode? focusNode;

  @override
  State<PasswordField> createState() => _PasswordFieldState();
}

class _PasswordFieldState extends State<PasswordField> {
  bool _visible = false;

  @override
  Widget build(BuildContext context) {
    return TextFormField(
      controller: widget.controller,
      focusNode: widget.focusNode,
      autofocus: widget.autofocus,
      obscureText: !_visible,
      textInputAction: widget.textInputAction,
      onFieldSubmitted: widget.onFieldSubmitted,
      validator: widget.validator,
      decoration: InputDecoration(
        labelText: widget.labelText,
        helperText: widget.helperText,
        suffixIcon: IconButton(
          icon: Icon(_visible ? Icons.visibility_off : Icons.visibility),
          tooltip: _visible ? 'Hide password' : 'Show password',
          onPressed: () => setState(() => _visible = !_visible),
        ),
      ),
    );
  }
}
