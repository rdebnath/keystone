import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:keystone_admin_ui/keystone_admin_ui.dart';

/// Records auth calls instead of reaching the backend. The unused REST client and secure
/// storage are never touched, so the double needs neither `dio` nor a platform channel.
final class _RecordingAuthService implements AuthService {
  final List<LoginRequest> logins = [];
  final List<ChangePasswordRequest> passwordChanges = [];

  @override
  ApiClient get api => throw UnsupportedError('not used in this test');

  @override
  TokenStorage get storage => throw UnsupportedError('not used in this test');

  @override
  Future<void> signIn(String identifier, String password) async {
    logins.add(LoginRequest(identifier: identifier, password: password));
  }

  @override
  Future<void> changePassword(
    String password, {
    String? currentPassword,
  }) async {
    passwordChanges.add(
      ChangePasswordRequest(
        password: password,
        currentPassword: currentPassword,
      ),
    );
  }

  @override
  Future<void> signOut() async {}
}

Future<_RecordingAuthService> _pumpScreen(
  WidgetTester tester,
  Widget screen, {
  AppBranding? branding,
}) async {
  final authService = _RecordingAuthService();
  await tester.pumpWidget(
    ProviderScope(
      overrides: [
        authServiceProvider.overrideWithValue(authService),
        if (branding != null) appBrandingProvider.overrideWithValue(branding),
      ],
      child: MaterialApp(home: screen),
    ),
  );
  return authService;
}

void main() {
  group('LoginScreen', () {
    testWidgets(
      'should_show_the_platform_default_branding_when_the_host_injects_none',
      (tester) async {
        await _pumpScreen(tester, const LoginScreen());

        expect(find.text('Keystone'), findsOneWidget);
      },
    );

    testWidgets('should_show_the_branding_injected_by_the_host_app', (
      tester,
    ) async {
      await _pumpScreen(
        tester,
        const LoginScreen(),
        branding: const AppBranding(title: 'Keystone - Inventory Management'),
      );

      expect(find.text('Keystone - Inventory Management'), findsOneWidget);
    });

    testWidgets('should_sign_in_when_enter_is_pressed_in_the_password_field', (
      tester,
    ) async {
      final authService = await _pumpScreen(tester, const LoginScreen());
      await tester.enterText(find.byType(TextFormField).at(0), 'alice@acme');
      await tester.enterText(find.byType(TextFormField).at(1), 'secret');

      await tester.testTextInput.receiveAction(TextInputAction.done);
      await tester.pump();

      expect(authService.logins.single.identifier, 'alice@acme');
      expect(authService.logins.single.password, 'secret');
    });

    testWidgets(
      'should_move_focus_to_the_password_field_when_enter_is_pressed_in_the_username_field',
      (tester) async {
        await _pumpScreen(tester, const LoginScreen());
        await tester.enterText(find.byType(TextFormField).at(0), 'alice@acme');

        await tester.testTextInput.receiveAction(TextInputAction.next);
        await tester.pump();

        final passwordField = tester.widget<EditableText>(
          find.byType(EditableText).at(1),
        );
        expect(passwordField.focusNode.hasFocus, isTrue);
      },
    );

    testWidgets(
      'should_not_sign_in_when_enter_is_pressed_and_the_form_is_empty',
      (tester) async {
        final authService = await _pumpScreen(tester, const LoginScreen());

        await tester.showKeyboard(find.byType(TextFormField).at(1));
        await tester.testTextInput.receiveAction(TextInputAction.done);
        await tester.pump();

        expect(authService.logins, isEmpty);
        expect(find.text('Username is required'), findsOneWidget);
        expect(find.text('Password is required'), findsOneWidget);
      },
    );

    testWidgets('should_still_submit_the_password_after_revealing_it', (
      tester,
    ) async {
      final authService = await _pumpScreen(tester, const LoginScreen());
      await tester.enterText(
        find.byType(TextFormField).at(0),
        'admin@keystone',
      );
      await tester.enterText(find.byType(TextFormField).at(1), 'hunter2secret');

      await tester.tap(find.byTooltip('Show password'));
      await tester.pump();

      // The reveal is a rendering switch: the value that will be submitted is the one typed.
      expect(
        tester
            .widget<EditableText>(find.byType(EditableText).at(1))
            .obscureText,
        isFalse,
      );
      expect(find.text('hunter2secret'), findsOneWidget);

      await tester.testTextInput.receiveAction(TextInputAction.done);
      await tester.pump();

      expect(authService.logins.single.identifier, 'admin@keystone');
      expect(authService.logins.single.password, 'hunter2secret');
    });
  });

  group('ChangePasswordScreen', () {
    testWidgets(
      'should_change_the_password_when_enter_is_pressed_in_the_confirm_field',
      (tester) async {
        final authService = await _pumpScreen(
          tester,
          const ChangePasswordScreen(),
        );
        await tester.enterText(find.byType(TextFormField).at(0), 'new-secret');
        await tester.enterText(find.byType(TextFormField).at(1), 'new-secret');

        await tester.testTextInput.receiveAction(TextInputAction.done);
        await tester.pump();

        // The forced flow sends no current password: the one just used to sign in is the proof.
        expect(authService.passwordChanges.single.password, 'new-secret');
        expect(authService.passwordChanges.single.currentPassword, isNull);
      },
    );

    testWidgets('should_reveal_the_typed_password_without_changing_it', (
      tester,
    ) async {
      final authService = await _pumpScreen(
        tester,
        const ChangePasswordScreen(),
      );
      await tester.enterText(find.byType(TextFormField).at(0), 'new-secret');
      await tester.enterText(find.byType(TextFormField).at(1), 'new-secret');

      await tester.tap(find.byTooltip('Show password').first);
      await tester.pump();

      expect(
        tester
            .widget<EditableText>(find.byType(EditableText).at(0))
            .obscureText,
        isFalse,
      );
      expect(find.text('new-secret'), findsNWidgets(2));

      await tester.testTextInput.receiveAction(TextInputAction.done);
      await tester.pump();

      expect(authService.passwordChanges.single.password, 'new-secret');
    });

    testWidgets(
      'should_move_focus_to_the_confirm_field_when_enter_is_pressed_in_the_new_password_field',
      (tester) async {
        await _pumpScreen(tester, const ChangePasswordScreen());
        await tester.enterText(find.byType(TextFormField).at(0), 'new-secret');

        await tester.testTextInput.receiveAction(TextInputAction.next);
        await tester.pump();

        final confirmField = tester.widget<EditableText>(
          find.byType(EditableText).at(1),
        );
        expect(confirmField.focusNode.hasFocus, isTrue);
      },
    );
  });
}
