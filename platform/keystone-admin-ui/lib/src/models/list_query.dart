/// The list query — what a list screen asks the server for, and where that request lives in the URL.
library;

import 'package:freezed_annotation/freezed_annotation.dart';

import 'models.dart';

part 'list_query.freezed.dart';
part 'list_query.g.dart';

/// The state of **one list screen**: what to show, which page of it, and how it is ordered.
///
/// It is deliberately one value rather than six separate fields, because the same value is
/// (a) the **provider family key** — `freezed` gives it value equality, so a rebuild does not refetch and
/// two widgets asking for the same query share one request; (b) the **URL payload**, so a filtered list
/// survives a refresh and can be linked; and (c) the "did the query change?" check.
///
/// Every mutator that changes *which rows* are in the result set (search, filter, sort, size) also resets
/// the page to the first one: leaving the user on page 7 of a now-2-page result set is the bug these lists
/// exist to avoid (`docs/UX_GUIDELINES.md` §1.4).
@freezed
abstract class ListQuery with _$ListQuery {
  const ListQuery._();

  const factory ListQuery({
    /// A **platform-plane** filter; a tenant console never sends one, because its routes carry no tenant.
    String? tenantId,
    @Default('') String search,
    @Default(0) int page,
    @Default(25) int size,

    /// The sort key the server accepts, or null for the resource's own default order.
    String? sort,
    @Default('asc') String order,

    /// The `scope` filter of the roles and permissions lists (`PLATFORM`/`TENANT`), null for every scope.
    String? scope,

    /// The `access` filter of the permissions list — the level a code ends in (`read-only` / `read-write`),
    /// null for both. It is a property of the code the row shows, which is what makes it a filter the user
    /// can see the effect of (`docs/UX_GUIDELINES.md` §1.6); the wildcard `*` carries no level and is in
    /// neither level's set.
    String? access,
  }) = _ListQuery;

  factory ListQuery.fromJson(Map<String, dynamic> json) => _$ListQueryFromJson(json);

  /// The first page, unfiltered: where a screen starts when the URL carries no query.
  static const ListQuery initial = ListQuery();

  /// The query a **role's permission picker** starts from: the role's owner and scope, first page, the
  /// resource's own (catalogue-first) order.
  ///
  /// The owner and the scope are *seeded*, not offered as filters the user could widen: every row this query
  /// returns is one the backend accepts for that role (`RoleService.grantableTo` filters by the same owner and
  /// requires the same `scope`), whereas a row outside them would be refused on save with "Unknown permission"
  /// or "Permission scope mismatch". The picker therefore exposes search, the owner filter — where more than
  /// one owner is grantable — and sorting, and states the scope as text.
  ///
  /// [ownerId] is the role's owner: `null` for a global role on the platform plane, a tenant id for a
  /// tenant-owned role. A role with **no** owner is seeded with the reserved platform tenant id, i.e. the
  /// global catalogue, because that is all a global role may hold — its grants are handed to every tenant
  /// that holds it. On the **tenant** plane the value is dropped by `ConsoleScope.permissions` anyway: that
  /// route has no tenant parameter to name, and derives the owner from the caller.
  factory ListQuery.permissionsFor({String? ownerId, required String scope}) =>
      ListQuery(tenantId: ownerId ?? Tenant.platformId, scope: scope);

  /// The request's query parameters, omitting what the server's defaults already mean — a blank search is
  /// no search, and an absent sort key is the resource's default order. Sending `q=` for "no search" would
  /// put noise in the URL and in the server's logs.
  Map<String, String> toQueryParameters() => <String, String>{
        if (search.isNotEmpty) 'q': search,
        'page': '$page',
        'size': '$size',
        if (sort != null) 'sort': sort!,
        if (sort != null) 'order': order,
        if (tenantId != null) 'tenantId': tenantId!,
        if (scope != null) 'scope': scope!,
        if (access != null) 'access': access!,
      };

  /// The query with a new search term, back on the first page.
  ListQuery withSearch(String search) => ListQuery(
        tenantId: tenantId,
        search: search,
        size: size,
        sort: sort,
        order: order,
        scope: scope,
        access: access,
      );

  /// The query with a new tenant filter (null = every tenant), back on the first page.
  ListQuery withTenant(String? tenantId) => ListQuery(
        tenantId: tenantId,
        search: search,
        size: size,
        sort: sort,
        order: order,
        scope: scope,
        access: access,
      );

  /// The query with a new scope filter (null = every scope), back on the first page.
  ListQuery withScope(String? scope) => ListQuery(
        tenantId: tenantId,
        search: search,
        size: size,
        sort: sort,
        order: order,
        scope: scope,
        access: access,
      );

  /// The query with a new access-level filter — the level a permission code ends in (`read-only` /
  /// `read-write`; null = both), back on the first page.
  ListQuery withAccess(String? access) => ListQuery(
        tenantId: tenantId,
        search: search,
        size: size,
        sort: sort,
        order: order,
        scope: scope,
        access: access,
      );

  /// The query with no tenant filter, **page preserved** — what a tenant console sends: its routes carry no
  /// tenant at all, because the backend derives it from the caller. Unlike [withTenant] this does not reset
  /// the page, since nothing about the result set changed.
  ListQuery withoutTenant() => ListQuery(
        search: search,
        page: page,
        size: size,
        sort: sort,
        order: order,
        scope: scope,
        access: access,
      );

  /// The query sorted by [sort] (null = the resource's default order) in [order], back on the first page.
  ListQuery withSort(String? sort, String order) => ListQuery(
        tenantId: tenantId,
        search: search,
        size: size,
        sort: sort,
        order: order,
        scope: scope,
        access: access,
      );

  /// The query with a different page size, back on the first page.
  ListQuery withSize(int size) => ListQuery(
        tenantId: tenantId,
        search: search,
        size: size,
        sort: sort,
        order: order,
        scope: scope,
        access: access,
      );

  /// The query on another page. The only way a query moves forward or back through a result set.
  ListQuery onPage(int page) => ListQuery(
        tenantId: tenantId,
        search: search,
        page: page,
        size: size,
        sort: sort,
        order: order,
        scope: scope,
        access: access,
      );
}
