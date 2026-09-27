import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:keystone_admin_ui/keystone_admin_ui.dart';

/// Records the change-password call (or fails it, so the dialog's error path can be exercised) without
/// reaching the backend. The unused REST client and secure storage are never touched, so the double needs
/// neither `dio` nor a platform channel.
final class _RecordingAuthService implements AuthService {
  final List<ChangePasswordRequest> requests = [];
  DioException? failure;

  @override
  ApiClient get api => throw UnsupportedError('not used in this test');

  @override
  TokenStorage get storage => throw UnsupportedError('not used in this test');

  @override
  Future<void> signIn(String identifier, String password) async {}

  @override
  Future<void> changePassword(
    String password, {
    String? currentPassword,
  }) async {
    if (failure != null) {
      throw failure!;
    }
    requests.add(
      ChangePasswordRequest(
        password: password,
        currentPassword: currentPassword,
      ),
    );
  }

  @override
  Future<void> signOut() async {}
}

/// The dialog as the console opens it: from a button on a normal screen.
Future<_RecordingAuthService> _pumpDialog(
  WidgetTester tester, {
  _RecordingAuthService? auth,
}) async {
  final authService = auth ?? _RecordingAuthService();
  await tester.pumpWidget(
    ProviderScope(
      overrides: [
        authServiceProvider.overrideWithValue(authService),
        meProvider.overrideWith((ref) async => null),
      ],
      child: MaterialApp(
        home: Scaffold(
          body: Builder(
            builder: (context) => ElevatedButton(
              onPressed: () => showChangePasswordDialog(context),
              child: const Text('open dialog'),
            ),
          ),
        ),
      ),
    ),
  );
  await tester.tap(find.text('open dialog'));
  await tester.pumpAndSettle();
  return authService;
}

/// A rejected current password as the backend answers it (RFC 9457 `detail`).
DioException _rejectedCurrentPassword() {
  final options = RequestOptions(path: '/api/v1/me/password');
  return DioException(
    requestOptions: options,
    response: Response<Map<String, dynamic>>(
      requestOptions: options,
      statusCode: 422,
      data: const {
        'title': 'VALIDATION',
        'status': 422,
        'detail': 'Current password is incorrect.',
      },
    ),
  );
}

void main() {
  group('Change password dialog', () {
    testWidgets('should_ask_for_the_current_password_first', (tester) async {
      await _pumpDialog(tester);

      expect(find.text('Change password'), findsWidgets);
      expect(find.text('Current password'), findsOneWidget);
      expect(find.text('New password'), findsOneWidget);
      expect(find.text('Confirm password'), findsOneWidget);
    });

    testWidgets('should_send_the_current_and_the_new_password_together', (
      tester,
    ) async {
      final authService = await _pumpDialog(tester);

      await tester.enterText(find.byType(TextFormField).at(0), 'old-secret');
      await tester.enterText(find.byType(TextFormField).at(1), 'new-secret');
      await tester.enterText(find.byType(TextFormField).at(2), 'new-secret');
      await tester.tap(find.text('Change password').last);
      await tester.pumpAndSettle();

      expect(authService.requests, hasLength(1));
      expect(authService.requests.single.password, 'new-secret');
      expect(authService.requests.single.currentPassword, 'old-secret');
      // The dialog closes on success.
      expect(find.text('Current password'), findsNothing);
    });

    testWidgets('should_keep_the_dialog_open_and_show_the_rejection', (
      tester,
    ) async {
      final authService = _RecordingAuthService()
        ..failure = _rejectedCurrentPassword();
      await _pumpDialog(tester, auth: authService);

      await tester.enterText(find.byType(TextFormField).at(0), 'wrong-secret');
      await tester.enterText(find.byType(TextFormField).at(1), 'new-secret');
      await tester.enterText(find.byType(TextFormField).at(2), 'new-secret');
      await tester.tap(find.text('Change password').last);
      await tester.pumpAndSettle();

      expect(authService.requests, isEmpty);
      expect(find.text('Current password is incorrect.'), findsOneWidget);
      expect(find.text('Current password'), findsOneWidget);
    });

    testWidgets('should_not_submit_when_the_confirmation_differs', (
      tester,
    ) async {
      final authService = await _pumpDialog(tester);

      await tester.enterText(find.byType(TextFormField).at(0), 'old-secret');
      await tester.enterText(find.byType(TextFormField).at(1), 'new-secret');
      await tester.enterText(find.byType(TextFormField).at(2), 'other-secret');
      await tester.tap(find.text('Change password').last);
      await tester.pumpAndSettle();

      expect(authService.requests, isEmpty);
      expect(find.text('Passwords do not match.'), findsOneWidget);
    });
  });
}
