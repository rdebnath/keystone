import 'package:flutter_test/flutter_test.dart';
import 'package:keystone_admin_ui/src/core/dialogs.dart';

void main() {
  test('should split and trim comma-separated values', () {
    expect(splitList('a, b ,,c'), ['a', 'b', 'c']);
  });

  test('should return empty list for blank input', () {
    expect(splitList('  '), isEmpty);
  });
}
