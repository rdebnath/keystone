/// The shared list-screen widgets: search, filtering, sorting, paging and the states around them.
///
/// These exist so a screen **opts into** the list rules of `docs/UX_GUIDELINES.md` §1 instead of
/// re-implementing them: the 300 ms search debounce, the reset to the first page on a new query, the two
/// distinct empty states, keeping the previous rows visible while the next page loads, the pager — and the
/// fact that the list state lives in the URL.
library;

import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../models/envelopes.dart';
import '../models/list_query.dart';
import 'errors.dart';
import 'panels.dart';

/// Reads and writes a list screen's [ListQuery] in the URL query string
/// (`docs/UX_GUIDELINES.md` §1.12).
///
/// The URL is read **once**, when a screen first builds, and written on every change. Refresh,
/// back/forward and a link pasted to a colleague then all preserve the search term, the filters, the page,
/// the size and the sort.
class ListQueryLocation {
  const ListQueryLocation._();

  /// The query the current location carries, or [ListQuery.initial] when the screen has no URL (a test
  /// hosting it outside a router).
  static ListQuery read(BuildContext context) {
    if (GoRouter.maybeOf(context) == null) {
      return ListQuery.initial;
    }
    return fromQueryParameters(GoRouterState.of(context).uri.queryParameters);
  }

  /// What a set of query parameters means. Absent or blank values fall back to the defaults, so a
  /// hand-edited URL reads like a sane one — the backend validates everything again anyway.
  static ListQuery fromQueryParameters(Map<String, String> parameters) {
    return ListQuery(
      tenantId: _nonEmpty(parameters['tenantId']),
      search: parameters['q']?.trim() ?? '',
      page: _int(parameters['page'], 0),
      size: _int(parameters['size'], ListQuery.initial.size),
      sort: _nonEmpty(parameters['sort']),
      order: parameters['order'] == 'desc' ? 'desc' : 'asc',
      scope: _nonEmpty(parameters['scope']),
      access: _nonEmpty(parameters['access']),
    );
  }

  /// Replaces the current location with [path] carrying [query].
  ///
  /// `replace` rather than `push`: paging should not fill the history with twenty entries, but
  /// back/forward should still step between the queries the user ran.
  static void write(BuildContext context, String path, ListQuery query) {
    if (GoRouter.maybeOf(context) == null) {
      return;
    }
    context.replace(
      Uri(path: path, queryParameters: query.toQueryParameters()).toString(),
    );
  }

  static String? _nonEmpty(String? value) {
    final trimmed = value?.trim();
    return trimmed == null || trimmed.isEmpty ? null : trimmed;
  }

  static int _int(String? value, int fallback) {
    final parsed = int.tryParse(value?.trim() ?? '');
    return parsed == null || parsed < 0 ? fallback : parsed;
  }
}

/// The row above a list: the search box, the screen's own filters and the sort control, laid out the same
/// way on every list screen. The result summary and the pager belong to [PagedListView], which is what
/// knows the totals.
class ListToolbar extends StatelessWidget {
  const ListToolbar({
    super.key,
    required this.search,
    this.filters = const <Widget>[],
    this.trailing,
  });

  final Widget search;
  final List<Widget> filters;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
      child: Wrap(
        spacing: 12,
        runSpacing: 12,
        // Aligned by their **tops**, not their centres. The controls have different heights — a filter with
        // helper text is taller than one without, and the sort control's direction toggle lives inside its
        // field — and centring them puts the labels and the underlines of neighbouring controls at different
        // heights, which reads as a broken toolbar.
        crossAxisAlignment: WrapCrossAlignment.start,
        children: <Widget>[
          SizedBox(width: 320, child: search),
          ...filters,
          if (trailing != null) trailing!,
        ],
      ),
    );
  }
}

/// One entry of the sort control: [value] is the `sort` parameter the server accepts, or null for the
/// resource's own default order.
class SortOption {
  const SortOption(this.value, this.label);

  final String? value;
  final String label;
}

/// The sort control: one of the keys the **server** accepts, plus a direction toggle.
///
/// The console renders `ListTile` rows rather than a table, so a dropdown is the honest control. The
/// ordering itself happens in SQL — the client never re-sorts a page it holds, which would reorder only the
/// rows on screen (`docs/UX_GUIDELINES.md` §1.11).
class SortSelect extends StatelessWidget {
  const SortSelect({
    super.key,
    required this.query,
    required this.onQueryChanged,
    required this.options,
  });

  final ListQuery query;
  final ValueChanged<ListQuery> onQueryChanged;

  /// The keys this resource can be sorted by, the first being the default order.
  final List<SortOption> options;

  @override
  Widget build(BuildContext context) {
    final descending = query.order == 'desc';
    return SizedBox(
      width: 240,
      child: DropdownButtonFormField<String?>(
        initialValue: query.sort,
        isExpanded: true,
        decoration: InputDecoration(
          labelText: 'Sort',
          // The direction toggle is part of the **field**, so it lines up with the value and the underline
          // instead of floating beside the control at its own height.
          suffixIcon: IconButton(
            padding: EdgeInsets.zero,
            iconSize: 20,
            tooltip: descending ? 'Sort ascending' : 'Sort descending',
            icon: Icon(descending ? Icons.arrow_downward : Icons.arrow_upward),
            // Direction only means something for a real key: the default order is the server's, with its own
            // tiebreakers, and reversing it is not one of them.
            onPressed: query.sort == null
                ? null
                : () => onQueryChanged(
                    query.withSort(query.sort, descending ? 'asc' : 'desc'),
                  ),
          ),
          // Tight constraints: a suffix icon must not make this field taller than its neighbours, or the
          // underlines drift apart again.
          suffixIconConstraints: const BoxConstraints(minWidth: 32, minHeight: 24),
        ),
        items: options
            .map(
              (option) => DropdownMenuItem<String?>(
                value: option.value,
                child: Text(option.label),
              ),
            )
            .toList(growable: false),
        onChanged: (selected) =>
            onQueryChanged(query.withSort(selected, query.order)),
      ),
    );
  }
}

/// The pager: the page label, first/previous/next/last, and the page size.
///
/// The ends are **disabled, not hidden** (`docs/UX_GUIDELINES.md` §1.14): a control that disappears is a
/// layout that jumps, and a user who cannot tell whether they are on the first page.
class PaginationBar<T> extends StatelessWidget {
  const PaginationBar({
    super.key,
    required this.paged,
    required this.query,
    required this.onQueryChanged,
  });

  final Paged<T> paged;
  final ListQuery query;
  final ValueChanged<ListQuery> onQueryChanged;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 0, 8, 8),
      child: Row(
        children: <Widget>[
          Text(paged.pageLabel, style: Theme.of(context).textTheme.bodySmall),
          const Spacer(),
          IconButton(
            tooltip: 'First page',
            icon: const Icon(Icons.first_page),
            onPressed: paged.hasPrevious
                ? () => onQueryChanged(query.onPage(0))
                : null,
          ),
          IconButton(
            tooltip: 'Previous page',
            icon: const Icon(Icons.chevron_left),
            onPressed: paged.hasPrevious
                ? () => onQueryChanged(query.onPage(paged.page - 1))
                : null,
          ),
          IconButton(
            tooltip: 'Next page',
            icon: const Icon(Icons.chevron_right),
            onPressed: paged.hasNext
                ? () => onQueryChanged(query.onPage(paged.page + 1))
                : null,
          ),
          IconButton(
            tooltip: 'Last page',
            icon: const Icon(Icons.last_page),
            onPressed: paged.hasNext
                ? () => onQueryChanged(query.onPage(paged.totalPages - 1))
                : null,
          ),
          const SizedBox(width: 8),
          DropdownButton<int>(
            value: query.size,
            underline: const SizedBox.shrink(),
            items: const <int>[25, 50, 100]
                .map(
                  (size) => DropdownMenuItem<int>(
                    value: size,
                    child: Text('$size / page'),
                  ),
                )
                .toList(growable: false),
            onChanged: (size) =>
                size == null ? null : onQueryChanged(query.withSize(size)),
          ),
        ],
      ),
    );
  }
}

/// The body of a paged list: loading, error, the two empty states, the rows and the pager.
///
/// It implements `docs/UX_GUIDELINES.md` §1.7–1.11 and §1.15 in one place, so every list screen shows the
/// same states and none of them forgets to:
///
/// * tell **nothing here yet** apart from **nothing matches** (the second echoes the term and offers to
///   clear it; the first is the screen's own "create the first one");
/// * keep the **previous rows on screen** while the next page loads, instead of blanking to a spinner;
/// * offer **Retry against the same query** when a page fails;
/// * show the **range and the total**, and a pager that is disabled — not hidden — at the ends.
class PagedListView<T> extends StatefulWidget {
  const PagedListView({
    super.key,
    required this.value,
    required this.query,
    required this.onQueryChanged,
    required this.onRetry,
    required this.itemBuilder,
    required this.emptyIcon,
    required this.emptyMessage,
  });

  /// The provider's state for [query].
  final AsyncValue<Paged<T>> value;

  /// The query [value] belongs to — the one the pager and *Clear search* rewrite.
  final ListQuery query;

  /// Applied to the whole query: another page, another size, or a cleared search.
  final ValueChanged<ListQuery> onQueryChanged;

  /// Re-runs the same query (the screen owns the provider, so it owns the retry).
  final VoidCallback onRetry;

  final Widget Function(BuildContext context, T item) itemBuilder;

  /// Shown when the collection itself is empty (`No tenants yet.`). The screen offers the create action
  /// outside this panel; the panel only states the fact.
  final IconData emptyIcon;
  final String emptyMessage;

  @override
  State<PagedListView<T>> createState() => _PagedListViewState<T>();
}

class _PagedListViewState<T> extends State<PagedListView<T>> {
  /// The last page we managed to show, so that paging or refreshing never blanks the list (§1.9).
  Paged<T>? _shown;
  ListQuery? _shownFor;

  @override
  void initState() {
    super.initState();
    _remember();
  }

  @override
  void didUpdateWidget(PagedListView<T> oldWidget) {
    super.didUpdateWidget(oldWidget);
    _remember();
  }

  void _remember() {
    final data = widget.value.valueOrNull;
    if (data != null) {
      _shown = data;
      _shownFor = widget.query;
    }
  }

  /// The page to keep on screen while a new one loads: the last one, but only for the **same result set**.
  /// When the search or a filter changed, the old rows are not "the previous page" — they answer a
  /// different question, and showing them would be a lie.
  Paged<T>? _carried() {
    final shown = _shown;
    final shownFor = _shownFor;
    if (shown == null || shownFor == null || !_sameResultSet(shownFor, widget.query)) {
      return null;
    }
    return shown;
  }

  static bool _sameResultSet(ListQuery a, ListQuery b) =>
      a.search == b.search &&
      a.tenantId == b.tenantId &&
      a.scope == b.scope &&
      a.sort == b.sort &&
      a.order == b.order;

  @override
  Widget build(BuildContext context) {
    final data = widget.value.valueOrNull;
    final page = data ?? _carried();
    if (page == null) {
      return widget.value.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => _errorPanel(error),
        data: (_) => const SizedBox.shrink(),
      );
    }
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: <Widget>[
        // The previous page stays visible while the next one loads, under a thin progress bar.
        if (data == null) const LinearProgressIndicator(minHeight: 2),
        if (page.rangeLabel != null) _summary(context, page),
        Expanded(
          child: page.items.isEmpty
              ? _emptyPanel(page)
              : ListView.builder(
                  itemCount: page.items.length,
                  itemBuilder: (context, index) =>
                      widget.itemBuilder(context, page.items[index]),
                ),
        ),
        if (page.items.isNotEmpty)
          PaginationBar<T>(
            paged: page,
            query: widget.query,
            onQueryChanged: widget.onQueryChanged,
          ),
      ],
    );
  }

  Widget _summary(BuildContext context, Paged<T> page) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 4, 16, 4),
      child: Text(
        '${page.rangeLabel} · ${page.pageLabel}',
        style: Theme.of(context).textTheme.bodySmall,
      ),
    );
  }

  /// Which empty state this is matters: a search that matched nothing is a different situation from an
  /// empty collection, and only the first should offer to clear the search (§1.8).
  Widget _emptyPanel(Paged<T> page) {
    if (page.totalElements > 0) {
      // Rows exist, just not on this page: it went stale (rows were deleted while it was open). Offer the
      // real last page rather than pretending the list is empty.
      return MessagePanel(
        icon: Icons.find_in_page_outlined,
        message: 'Nothing on this page — the list changed while it was open.',
        actionLabel: 'Go to the last page',
        onAction: () =>
            widget.onQueryChanged(widget.query.onPage(page.totalPages - 1)),
      );
    }
    if (widget.query.search.isNotEmpty) {
      return MessagePanel(
        icon: Icons.search_off,
        message: 'Nothing matches "${widget.query.search}".',
        actionLabel: 'Clear search',
        onAction: () => widget.onQueryChanged(widget.query.withSearch('')),
      );
    }
    return MessagePanel(icon: widget.emptyIcon, message: widget.emptyMessage);
  }

  Widget _errorPanel(Object error) {
    return MessagePanel(
      icon: Icons.error_outline,
      message: apiErrorMessage(error, 'Failed to load this list.'),
      actionLabel: 'Retry',
      onAction: widget.onRetry,
    );
  }
}


///
/// Typing is reported once per pause ([debounce], 300 ms by default) rather than once per keystroke, and
/// the term is trimmed. The clear button reports an empty search **and** empties the box; the caller's
/// *Clear search* action has the same effect by passing `value: ''`.
class SearchField extends StatefulWidget {
  const SearchField({
    super.key,
    required this.value,
    required this.onChanged,
    this.hintText,
    this.debounce = const Duration(milliseconds: 300),
  });

  /// The current term; the caller's query is the source of truth.
  final String value;

  /// Called with the trimmed term once the user stops typing (or immediately when cleared).
  final ValueChanged<String> onChanged;

  /// Names the searched fields (`Search name, slug or country`) — the columns differ per resource, so a
  /// bare "Search" would leave the user guessing (`docs/UX_GUIDELINES.md` §1.3).
  final String? hintText;

  final Duration debounce;

  @override
  State<SearchField> createState() => _SearchFieldState();
}

class _SearchFieldState extends State<SearchField> {
  late final TextEditingController _controller = TextEditingController(
    text: widget.value,
  );
  Timer? _timer;

  @override
  void didUpdateWidget(SearchField oldWidget) {
    super.didUpdateWidget(oldWidget);
    // React only to an *external* change of the term (the "Clear search" action), so typing is never
    // interrupted by our own state coming back to us.
    if (widget.value != oldWidget.value && widget.value != _controller.text) {
      _controller.text = widget.value;
    }
  }

  @override
  void dispose() {
    _timer?.cancel();
    _controller.dispose();
    super.dispose();
  }

  void _onChanged(String value) {
    setState(() {}); // the clear button appears as soon as there is something to clear
    _timer?.cancel();
    _timer = Timer(widget.debounce, () => widget.onChanged(value.trim()));
  }

  void _clear() {
    _timer?.cancel();
    setState(_controller.clear);
    widget.onChanged('');
  }

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: _controller,
      onChanged: _onChanged,
      textInputAction: TextInputAction.search,
      decoration: InputDecoration(
        labelText: 'Search',
        // The label always floats, like the dropdowns beside it: a label that starts inside the box and
        // moves up on the first keystroke leaves the toolbar looking misaligned while the box is empty.
        floatingLabelBehavior: FloatingLabelBehavior.always,
        hintText: widget.hintText,
        prefixIcon: const Icon(Icons.search),
        suffixIcon: _controller.text.isEmpty
            ? null
            : IconButton(
                icon: const Icon(Icons.clear),
                tooltip: 'Clear search',
                onPressed: _clear,
              ),
      ),
    );
  }
}
