import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:keystone_admin_ui/keystone_admin_ui.dart';

/// Pumps one [PasswordField] so the reveal toggle and the value it keeps can be observed.
Future<TextEditingController> _pumpField(WidgetTester tester) async {
  final controller = TextEditingController();
  addTearDown(controller.dispose);

  await tester.pumpWidget(
    MaterialApp(
      home: Scaffold(
        body: Form(
          child: PasswordField(
            controller: controller,
            labelText: 'Password',
            validator: (value) =>
                value == null || value.isEmpty ? 'Required' : null,
          ),
        ),
      ),
    ),
  );
  return controller;
}

/// Whether the field currently masks what was typed.
bool _obscured(WidgetTester tester) =>
    tester.widget<EditableText>(find.byType(EditableText)).obscureText;

void main() {
  group('PasswordField', () {
    testWidgets('should_mask_the_password_by_default', (tester) async {
      final controller = await _pumpField(tester);

      await tester.enterText(find.byType(TextFormField), 'hunter2secret');

      // `obscureText` is the rendering switch the toggle flips; the value itself is untouched.
      expect(_obscured(tester), isTrue);
      expect(controller.text, 'hunter2secret');
      expect(find.byTooltip('Show password'), findsOneWidget);
    });

    testWidgets('should_reveal_the_password_when_the_toggle_is_tapped', (
      tester,
    ) async {
      await _pumpField(tester);
      await tester.enterText(find.byType(TextFormField), 'hunter2secret');

      await tester.tap(find.byTooltip('Show password'));
      await tester.pump();

      expect(_obscured(tester), isFalse);
      expect(find.text('hunter2secret'), findsOneWidget);
      expect(find.byTooltip('Hide password'), findsOneWidget);
    });

    testWidgets('should_mask_it_again_on_a_second_tap_and_keep_the_value', (
      tester,
    ) async {
      final controller = await _pumpField(tester);
      await tester.enterText(find.byType(TextFormField), 'hunter2secret');

      await tester.tap(find.byTooltip('Show password'));
      await tester.pump();
      await tester.tap(find.byTooltip('Hide password'));
      await tester.pump();

      expect(_obscured(tester), isTrue);
      expect(controller.text, 'hunter2secret');
    });
  });
}
