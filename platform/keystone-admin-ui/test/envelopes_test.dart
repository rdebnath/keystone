import 'package:flutter_test/flutter_test.dart';
import 'package:keystone_admin_ui/keystone_admin_ui.dart';

/// The two response envelopes: a page of a list and the complete set behind a picker
/// (`docs/CODING_GUIDELINES_BACKEND.md` §8, `docs/UX_GUIDELINES.md` §1).
void main() {
  group('Paged', () {
    test('should_read_the_rows_and_the_totals_the_server_sent', () {
      final paged = Paged<Tenant>.fromJson(const {
        'items': [
          {'id': 't1', 'name': 'Acme', 'slug': 'acme'},
        ],
        'page': 1,
        'size': 25,
        'totalElements': 142,
        'totalPages': 6,
        'hasNext': true,
        'hasPrevious': true,
      }, Tenant.fromJson);

      expect(paged.items.single.name, 'Acme');
      expect(paged.page, 1);
      expect(paged.size, 25);
      expect(paged.totalElements, 142);
      expect(paged.totalPages, 6);
      expect(paged.hasNext, isTrue);
      expect(paged.hasPrevious, isTrue);
    });

    test('should_take_the_totals_as_sent_rather_than_recomputing_them', () {
      // The server says this is the last page; the client must not argue with arithmetic of its own.
      final paged = Paged<Tenant>.fromJson(const {
        'items': [
          {'id': 't1', 'name': 'Acme'},
        ],
        'page': 0,
        'size': 25,
        'totalElements': 7,
        'totalPages': 1,
        'hasNext': false,
        'hasPrevious': false,
      }, Tenant.fromJson);

      expect(paged.pageLabel, 'Page 1 of 1');
      expect(paged.rangeLabel, '1–1 of 7');
    });

    test('should_label_the_range_of_a_page_from_its_offset', () {
      final paged = Paged<Tenant>.fromJson(const {
        'items': [
          {'id': 't1', 'name': 'Acme'},
          {'id': 't2', 'name': 'Globex'},
        ],
        'page': 2,
        'size': 25,
        'totalElements': 142,
        'totalPages': 6,
        'hasNext': true,
        'hasPrevious': true,
      }, Tenant.fromJson);

      // Row 51 is the first of page 2 (`page × size + 1`), not `items.length`.
      expect(paged.rangeLabel, '51–52 of 142');
      expect(paged.pageLabel, 'Page 3 of 6');
    });

    test('should_report_a_page_past_the_end_as_such', () {
      final paged = Paged<Tenant>.fromJson(const {
        'items': <dynamic>[],
        'page': 9,
        'size': 25,
        'totalElements': 7,
        'totalPages': 1,
        'hasNext': false,
        'hasPrevious': true,
      }, Tenant.fromJson);

      // Rows exist, just not on this page: the screen offers the real last page instead of "no rows".
      expect(paged.isBeyondEnd, isTrue);
      expect(paged.rangeLabel, isNull);
    });

    test('should_tolerate_an_empty_envelope', () {
      final paged = Paged<Tenant>.fromJson(
        const <String, dynamic>{},
        Tenant.fromJson,
      );

      expect(paged.items, isEmpty);
      expect(paged.totalElements, 0);
      expect(paged.pageLabel, 'Page 1 of 1');
      expect(paged.isBeyondEnd, isFalse);
    });
  });

  group('OptionList', () {
    test('should_read_the_choices_and_the_truncation_flag', () {
      final options = OptionList<Role>.fromJson(const {
        'items': [
          {'id': 'r1', 'code': 'tenant-admin', 'scope': 'TENANT'},
        ],
        'truncated': true,
      }, Role.fromJson);

      expect(options.items.single.code, 'tenant-admin');
      // The cap was hit: the console must say so rather than present a cut list as complete.
      expect(options.truncated, isTrue);
    });

    test('should_default_to_complete_when_the_flag_is_absent', () {
      final options = OptionList<Role>.fromJson(
        const {'items': <dynamic>[]},
        Role.fromJson,
      );

      expect(options.items, isEmpty);
      expect(options.truncated, isFalse);
    });
  });
}
