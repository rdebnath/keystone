/// The two response **envelopes** a console consumes: a page of a list and the complete set behind a
/// picker.
///
/// They are hand-written immutable generics rather than `freezed` classes, deliberately:
/// `json_serializable` generates field-level code per class, so a generic envelope would have to be copied
/// once per resource (`TenantPage`, `UserPage`, …) and those copies would drift the first time the envelope
/// grows a field. Parsing still happens here, at the client boundary, and `items` is mapped into models in
/// the same statement — so no `Map` outlives the call (`docs/CODING_GUIDELINES_FRONTEND.md` §14).
library;

/// One page of a paged list, exactly as the backend's list contract returns it
/// (`docs/CODING_GUIDELINES_BACKEND.md` §8).
class Paged<T> {
  const Paged({
    required this.items,
    required this.page,
    required this.size,
    required this.totalElements,
    required this.totalPages,
    required this.hasNext,
    required this.hasPrevious,
  });

  /// Parses the envelope, mapping each element with [fromJson]. The totals are taken **as sent**: the
  /// client never recomputes `totalPages`, so it cannot disagree with the server about the last page.
  factory Paged.fromJson(
    Map<String, dynamic> json,
    T Function(Map<String, dynamic>) fromJson,
  ) {
    final items = (json['items'] as List<dynamic>? ?? const <dynamic>[])
        .map((item) => fromJson(item as Map<String, dynamic>))
        .toList(growable: false);
    return Paged<T>(
      items: items,
      page: _int(json['page']),
      size: _int(json['size']),
      totalElements: _int(json['totalElements']),
      totalPages: _int(json['totalPages']),
      hasNext: json['hasNext'] as bool? ?? false,
      hasPrevious: json['hasPrevious'] as bool? ?? false,
    );
  }

  /// The rows of this page.
  final List<T> items;

  /// The 0-based page index this window came from.
  final int page;

  /// The page size the window was taken with.
  final int size;

  /// How many rows matched the query in total — across every page, not just this one.
  final int totalElements;

  final int totalPages;
  final bool hasNext;
  final bool hasPrevious;

  /// Whether the page is **past the end** of the result set: rows exist, but none on this page. A stale
  /// page is not an error (the server answers `200` with the real totals), so a screen offers the last
  /// real page instead of pretending there is nothing to see.
  bool get isBeyondEnd => items.isEmpty && totalElements > 0 && page >= totalPages;

  /// The 1-based range label (`1–25 of 142`), or null when there is nothing to label.
  String? get rangeLabel {
    if (totalElements == 0 || items.isEmpty) {
      return null;
    }
    return '${page * size + 1}–${page * size + items.length} of $totalElements';
  }

  /// The 1-based page label (`Page 2 of 6`).
  String get pageLabel => 'Page ${page + 1} of ${totalPages < 1 ? 1 : totalPages}';

  static int _int(Object? value) => value is num ? value.toInt() : 0;
}

/// The **complete** set of choices behind a picker, as the resource's `/options` endpoint returns it.
///
/// A picker cannot be fed by [Paged]: it would silently offer only the first page. [truncated] says the
/// endpoint's cap was reached, and must be surfaced rather than hidden — a console that quietly shows the
/// first 500 tenants is worse than one that says so (`docs/UX_GUIDELINES.md` §1.13).
class OptionList<T> {
  const OptionList({required this.items, required this.truncated});

  factory OptionList.fromJson(
    Map<String, dynamic> json,
    T Function(Map<String, dynamic>) fromJson,
  ) {
    return OptionList<T>(
      items: (json['items'] as List<dynamic>? ?? const <dynamic>[])
          .map((item) => fromJson(item as Map<String, dynamic>))
          .toList(growable: false),
      truncated: json['truncated'] as bool? ?? false,
    );
  }

  final List<T> items;

  /// Whether the backend cap cut the set short — the client must say so, never present it as complete.
  final bool truncated;

  /// The options, or an empty list when a caller wants a plain `List` (a dropdown's `items`).
  List<T> get values => items;
}
