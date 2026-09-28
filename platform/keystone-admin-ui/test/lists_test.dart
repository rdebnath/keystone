import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:keystone_admin_ui/keystone_admin_ui.dart';

/// The shared list widgets: the states a list screen must show and the controls it must offer
/// (`docs/UX_GUIDELINES.md` §1.7–1.11, §1.14, §1.15).
void main() {
  /// A page holding [names] rows, on [page] of a [total]-row result set.
  Paged<Tenant> tenants({
    required int page,
    required int total,
    List<String> names = const ['Acme'],
  }) => Paged<Tenant>(
    items: names
        .map((name) => Tenant(id: 'tenant-$name', name: name, slug: name))
        .toList(growable: false),
    page: page,
    size: 25,
    totalElements: total,
    totalPages: total == 0 ? 0 : (total / 25).ceil(),
    hasNext: (page + 1) * 25 < total,
    hasPrevious: page > 0,
  );

  Future<void> pumpList(
    WidgetTester tester, {
    required AsyncValue<Paged<Tenant>> value,
    required ListQuery query,
    required ValueChanged<ListQuery> onQueryChanged,
    VoidCallback? onRetry,
  }) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: PagedListView<Tenant>(
            value: value,
            query: query,
            onQueryChanged: onQueryChanged,
            onRetry: onRetry ?? () {},
            emptyIcon: Icons.apartment_outlined,
            emptyMessage: 'No tenants yet.',
            itemBuilder: (context, tenant) => ListTile(title: Text(tenant.name)),
          ),
        ),
      ),
    );
  }

  group('PagedListView', () {
    testWidgets('should_show_the_range_and_the_page_of_the_result_set', (
      tester,
    ) async {
      await pumpList(
        tester,
        value: AsyncValue.data(tenants(page: 0, total: 142)),
        query: const ListQuery(),
        onQueryChanged: (_) {},
      );

      expect(find.text('1–1 of 142 · Page 1 of 6'), findsOneWidget);
      expect(find.text('Acme'), findsOneWidget);
    });

    testWidgets('should_tell_an_empty_collection_apart_from_a_search_match', (
      tester,
    ) async {
      // Nothing exists yet: the screen's own create action belongs here, not a "clear search".
      await pumpList(
        tester,
        value: AsyncValue.data(tenants(page: 0, total: 0, names: const [])),
        query: const ListQuery(),
        onQueryChanged: (_) {},
      );
      expect(find.text('No tenants yet.'), findsOneWidget);
      expect(find.text('Clear search'), findsNothing);

      // Nothing matches: the term is echoed back and can be cleared.
      await pumpList(
        tester,
        value: AsyncValue.data(tenants(page: 0, total: 0, names: const [])),
        query: const ListQuery(search: 'acme'),
        onQueryChanged: (_) {},
      );
      expect(find.text('Nothing matches "acme".'), findsOneWidget);
      expect(find.text('Clear search'), findsOneWidget);
    });

    testWidgets('should_offer_to_clear_the_search', (tester) async {
      ListQuery? reported;
      await pumpList(
        tester,
        value: AsyncValue.data(tenants(page: 0, total: 0, names: const [])),
        query: const ListQuery(search: 'acme', page: 2),
        onQueryChanged: (query) => reported = query,
      );

      await tester.tap(find.text('Clear search'));
      await tester.pump();

      expect(reported?.search, '');
      expect(reported?.page, 0);
    });

    testWidgets('should_offer_the_real_last_page_when_the_page_is_stale', (
      tester,
    ) async {
      ListQuery? reported;
      await pumpList(
        tester,
        value: AsyncValue.data(tenants(page: 9, total: 3, names: const [])),
        query: const ListQuery(page: 9),
        onQueryChanged: (query) => reported = query,
      );

      expect(
        find.text('Nothing on this page — the list changed while it was open.'),
        findsOneWidget,
      );
      await tester.tap(find.text('Go to the last page'));
      await tester.pump();

      expect(reported?.page, 0);
    });

    testWidgets('should_retry_the_same_query_after_a_failure', (tester) async {
      var retried = 0;
      await pumpList(
        tester,
        value: const AsyncValue.error('boom', StackTrace.empty),
        query: const ListQuery(search: 'acme', page: 1),
        onQueryChanged: (_) {},
        onRetry: () => retried++,
      );

      expect(find.text('Failed to load this list.'), findsOneWidget);
      await tester.tap(find.text('Retry'));
      await tester.pump();

      expect(retried, 1);
    });

    testWidgets('should_keep_the_previous_page_while_the_next_one_loads', (
      tester,
    ) async {
      // The same result set, another page: the rows stay visible under a progress bar (§1.9).
      await pumpList(
        tester,
        value: AsyncValue.data(tenants(page: 0, total: 142)),
        query: const ListQuery(),
        onQueryChanged: (_) {},
      );
      await pumpList(
        tester,
        value: const AsyncValue.loading(),
        query: const ListQuery(page: 1),
        onQueryChanged: (_) {},
      );

      expect(find.text('Acme'), findsOneWidget);
      expect(find.byType(LinearProgressIndicator), findsOneWidget);
      expect(find.byType(CircularProgressIndicator), findsNothing);
    });

    testWidgets('should_not_keep_the_previous_page_when_the_term_changed', (
      tester,
    ) async {
      await pumpList(
        tester,
        value: AsyncValue.data(tenants(page: 0, total: 142)),
        query: const ListQuery(),
        onQueryChanged: (_) {},
      );
      await pumpList(
        tester,
        value: const AsyncValue.loading(),
        query: const ListQuery(search: 'globex'),
        onQueryChanged: (_) {},
      );

      // Those rows answered a different question; showing them would be a lie.
      expect(find.text('Acme'), findsNothing);
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
    });
  });

  group('PaginationBar', () {
    testWidgets('should_disable_the_ends_rather_than_hide_them', (
      tester,
    ) async {
      ListQuery? reported;
      await pumpList(
        tester,
        value: AsyncValue.data(tenants(page: 0, total: 142)),
        query: const ListQuery(),
        onQueryChanged: (query) => reported = query,
      );

      // First page: the first/previous controls are present but disabled, so the bar does not jump (§1.14).
      expect(
        tester
            .widget<IconButton>(find.widgetWithIcon(IconButton, Icons.first_page))
            .onPressed,
        isNull,
      );
      expect(
        tester
            .widget<IconButton>(find.widgetWithIcon(IconButton, Icons.chevron_left))
            .onPressed,
        isNull,
      );
      // …and the next control is enabled, so the disabled state is a state, not a missing control.
      expect(
        tester
            .widget<IconButton>(find.widgetWithIcon(IconButton, Icons.chevron_right))
            .onPressed,
        isNotNull,
      );

      await tester.tap(find.byTooltip('Next page'));
      await tester.pump();
      expect(reported?.page, 1);

      await tester.tap(find.byTooltip('Last page'));
      await tester.pump();
      expect(reported?.page, 5);
    });

    testWidgets('should_change_the_page_size_and_return_to_the_first_page', (
      tester,
    ) async {
      ListQuery? reported;
      await pumpList(
        tester,
        value: AsyncValue.data(tenants(page: 3, total: 142)),
        query: const ListQuery(page: 3),
        onQueryChanged: (query) => reported = query,
      );

      await tester.tap(find.text('25 / page'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('100 / page').last);
      await tester.pumpAndSettle();

      expect(reported?.size, 100);
      expect(reported?.page, 0);
    });
  });

  group('ListToolbar', () {
    testWidgets('should_line_up_controls_of_different_heights', (tester) async {
      // The regression this guards: a filter carrying helper text is taller than a plain field, and the sort
      // control's direction toggle lives inside its own field. Centre-aligning them put a label and an
      // underline of one control between two lines of its neighbour.
      // A wide surface keeps every control on one row, which is where alignment is visible.
      tester.view.physicalSize = const Size(1400, 900);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ListToolbar(
              search: const SearchField(value: '', onChanged: _noop),
              filters: <Widget>[
                SizedBox(
                  width: 240,
                  child: DropdownButtonFormField<String?>(
                    initialValue: null,
                    isExpanded: true,
                    decoration: const InputDecoration(
                      labelText: 'Tenant',
                      helperText: 'Keystone holds the platform users',
                    ),
                    items: const <DropdownMenuItem<String?>>[
                      DropdownMenuItem<String?>(
                        value: null,
                        child: Text('All tenants'),
                      ),
                    ],
                    onChanged: (_) {},
                  ),
                ),
              ],
              trailing: SortSelect(
                query: const ListQuery(),
                onQueryChanged: (_) {},
                options: const <SortOption>[
                  SortOption(null, 'Username (default)'),
                ],
              ),
            ),
          ),
        ),
      );

      // Every label sits on the same line, whatever the control's total height.
      final labelTop = tester.getTopLeft(find.text('Tenant')).dy;
      expect(tester.getTopLeft(find.text('Search')).dy, equals(labelTop));
      expect(tester.getTopLeft(find.text('Sort')).dy, equals(labelTop));

      // …and so do the values, which is what makes the underlines line up.
      expect(
        tester.getTopLeft(find.text('All tenants')).dy,
        equals(tester.getTopLeft(find.text('Username (default)')).dy),
      );
    });

    testWidgets('should_keep_a_helper_text_below_its_own_field', (
      tester,
    ) async {
      // A helper must not be what pushes a control out of line: it hangs below the field it explains.
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ListToolbar(
              search: const SearchField(value: '', onChanged: _noop),
              filters: <Widget>[
                SizedBox(
                  width: 240,
                  child: DropdownButtonFormField<String?>(
                    initialValue: null,
                    decoration: const InputDecoration(
                      labelText: 'Tenant',
                      helperText: 'Keystone holds the platform users',
                    ),
                    items: const <DropdownMenuItem<String?>>[
                      DropdownMenuItem<String?>(
                        value: null,
                        child: Text('All tenants'),
                      ),
                    ],
                    onChanged: (_) {},
                  ),
                ),
              ],
            ),
          ),
        ),
      );

      expect(
        tester.getTopLeft(find.text('Keystone holds the platform users')).dy,
        greaterThan(tester.getTopLeft(find.text('All tenants')).dy),
      );
    });
  });

  group('SearchField', () {
    testWidgets('should_report_once_per_pause_not_once_per_keystroke', (
      tester,
    ) async {
      final reported = <String>[];
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SearchField(
              value: '',
              hintText: 'Search name, slug or country',
              onChanged: reported.add,
            ),
          ),
        ),
      );

      await tester.enterText(find.byType(TextField), 'a');
      await tester.pump(const Duration(milliseconds: 100));
      await tester.enterText(find.byType(TextField), 'ac');
      await tester.pump(const Duration(milliseconds: 100));
      await tester.enterText(find.byType(TextField), ' acme ');
      // Still inside the debounce window: nothing has been sent yet.
      expect(reported, isEmpty);

      await tester.pump(const Duration(milliseconds: 350));
      expect(reported, ['acme']);
    });

    testWidgets('should_name_what_it_searches_and_offer_to_clear_it', (
      tester,
    ) async {
      final reported = <String>[];
      Widget field(String value) => MaterialApp(
        home: Scaffold(
          body: SearchField(
            value: value,
            hintText: 'Search name, slug or country',
            onChanged: reported.add,
          ),
        ),
      );

      // The label is always visible — it floats above the box even while the box is empty, like the filters
      // beside it — and the hint names the searched columns until something is typed (§1.3).
      await tester.pumpWidget(field(''));
      expect(find.text('Search'), findsOneWidget);
      expect(find.text('Search name, slug or country'), findsOneWidget);

      // A filled box offers to clear itself.
      await tester.pumpWidget(field('acme'));
      expect(find.byTooltip('Clear search'), findsOneWidget);

      await tester.tap(find.byTooltip('Clear search'));
      await tester.pump();

      expect(reported, ['']);
      expect(find.byTooltip('Clear search'), findsNothing);
    });

    testWidgets('should_empty_the_box_when_the_term_is_cleared_elsewhere', (
      tester,
    ) async {
      // A "Clear search" action outside the field must empty the field too, not leave the old term in it.
      await tester.pumpWidget(
        MaterialApp(home: Scaffold(body: SearchField(value: 'acme', onChanged: _noop))),
      );
      expect(find.text('acme'), findsOneWidget);

      await tester.pumpWidget(
        MaterialApp(home: Scaffold(body: SearchField(value: '', onChanged: _noop))),
      );

      expect(find.text('acme'), findsNothing);
    });
  });
}

/// A no-op handler for the fields pumped without one.
void _noop(String _) {}
