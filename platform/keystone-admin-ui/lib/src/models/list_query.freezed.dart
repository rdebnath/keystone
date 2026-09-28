// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint, type=warning, deprecated_member_use, deprecated_member_use_from_same_package
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'list_query.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// GENERATED CODE - DO NOT MODIFY BY HAND
// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$ListQuery {

/// A **platform-plane** filter; a tenant console never sends one, because its routes carry no tenant.
 String? get tenantId; String get search; int get page; int get size;/// The sort key the server accepts, or null for the resource's own default order.
 String? get sort; String get order;/// The `scope` filter of the roles and permissions lists (`PLATFORM`/`TENANT`), null for every scope.
 String? get scope;/// The `access` filter of the permissions list — the level a code ends in (`read-only` / `read-write`),
/// null for both. It is a property of the code the row shows, which is what makes it a filter the user
/// can see the effect of (`docs/UX_GUIDELINES.md` §1.6); the wildcard `*` carries no level and is in
/// neither level's set.
 String? get access;
/// Create a copy of ListQuery
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ListQueryCopyWith<ListQuery> get copyWith => _$ListQueryCopyWithImpl<ListQuery>(this as ListQuery, _$identity);

  /// Serializes this ListQuery to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  final _this = this as ListQuery;
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ListQuery&&(identical(other.tenantId, _this.tenantId) || other.tenantId == _this.tenantId)&&(identical(other.search, _this.search) || other.search == _this.search)&&(identical(other.page, _this.page) || other.page == _this.page)&&(identical(other.size, _this.size) || other.size == _this.size)&&(identical(other.sort, _this.sort) || other.sort == _this.sort)&&(identical(other.order, _this.order) || other.order == _this.order)&&(identical(other.scope, _this.scope) || other.scope == _this.scope)&&(identical(other.access, _this.access) || other.access == _this.access));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
  final _this = this as ListQuery;
  return Object.hash(runtimeType,_this.tenantId,_this.search,_this.page,_this.size,_this.sort,_this.order,_this.scope,_this.access);
}

@override
String toString() {
  final _this = this as ListQuery;
  return 'ListQuery(tenantId: ${_this.tenantId}, search: ${_this.search}, page: ${_this.page}, size: ${_this.size}, sort: ${_this.sort}, order: ${_this.order}, scope: ${_this.scope}, access: ${_this.access})';
}


}

/// @nodoc
abstract mixin class $ListQueryCopyWith<$Res>  {
  factory $ListQueryCopyWith(ListQuery value, $Res Function(ListQuery) _then) = _$ListQueryCopyWithImpl;
@useResult
$Res call({
 String? tenantId, String search, int page, int size, String? sort, String order, String? scope, String? access
});




}
/// @nodoc
class _$ListQueryCopyWithImpl<$Res>
    implements $ListQueryCopyWith<$Res> {
  _$ListQueryCopyWithImpl(this._self, this._then);

  final ListQuery _self;
  final $Res Function(ListQuery) _then;

/// Create a copy of ListQuery
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? tenantId = freezed,Object? search = null,Object? page = null,Object? size = null,Object? sort = freezed,Object? order = null,Object? scope = freezed,Object? access = freezed,}) {
  return _then(ListQuery(
tenantId: freezed == tenantId ? _self.tenantId : tenantId // ignore: cast_nullable_to_non_nullable
as String?,search: null == search ? _self.search : search // ignore: cast_nullable_to_non_nullable
as String,page: null == page ? _self.page : page // ignore: cast_nullable_to_non_nullable
as int,size: null == size ? _self.size : size // ignore: cast_nullable_to_non_nullable
as int,sort: freezed == sort ? _self.sort : sort // ignore: cast_nullable_to_non_nullable
as String?,order: null == order ? _self.order : order // ignore: cast_nullable_to_non_nullable
as String,scope: freezed == scope ? _self.scope : scope // ignore: cast_nullable_to_non_nullable
as String?,access: freezed == access ? _self.access : access // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}

}


/// Adds pattern-matching-related methods to [ListQuery].
extension ListQueryPatterns on ListQuery {
/// A variant of `map` that fallback to returning `orElse`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ListQuery value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ListQuery() when $default != null:
return $default(_that);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// Callbacks receives the raw object, upcasted.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case final Subclass2 value:
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ListQuery value)  $default,){
final _that = this;
switch (_that) {
case _ListQuery():
return $default(_that);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `map` that fallback to returning `null`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ListQuery value)?  $default,){
final _that = this;
switch (_that) {
case _ListQuery() when $default != null:
return $default(_that);case _:
  return null;

}
}
/// A variant of `when` that fallback to an `orElse` callback.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String? tenantId,  String search,  int page,  int size,  String? sort,  String order,  String? scope,  String? access)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ListQuery() when $default != null:
return $default(_that.tenantId,_that.search,_that.page,_that.size,_that.sort,_that.order,_that.scope,_that.access);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// As opposed to `map`, this offers destructuring.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case Subclass2(:final field2):
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String? tenantId,  String search,  int page,  int size,  String? sort,  String order,  String? scope,  String? access)  $default,) {final _that = this;
switch (_that) {
case _ListQuery():
return $default(_that.tenantId,_that.search,_that.page,_that.size,_that.sort,_that.order,_that.scope,_that.access);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `when` that fallback to returning `null`
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String? tenantId,  String search,  int page,  int size,  String? sort,  String order,  String? scope,  String? access)?  $default,) {final _that = this;
switch (_that) {
case _ListQuery() when $default != null:
return $default(_that.tenantId,_that.search,_that.page,_that.size,_that.sort,_that.order,_that.scope,_that.access);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _ListQuery extends ListQuery {
  const _ListQuery({this.tenantId, this.search = '', this.page = 0, this.size = 25, this.sort, this.order = 'asc', this.scope, this.access}): super._();
  factory _ListQuery.fromJson(Map<String, dynamic> json) => _$ListQueryFromJson(json);

/// A **platform-plane** filter; a tenant console never sends one, because its routes carry no tenant.
@override final  String? tenantId;
@override@JsonKey() final  String search;
@override@JsonKey() final  int page;
@override@JsonKey() final  int size;
/// The sort key the server accepts, or null for the resource's own default order.
@override final  String? sort;
@override@JsonKey() final  String order;
/// The `scope` filter of the roles and permissions lists (`PLATFORM`/`TENANT`), null for every scope.
@override final  String? scope;
/// The `access` filter of the permissions list — the level a code ends in (`read-only` / `read-write`),
/// null for both. It is a property of the code the row shows, which is what makes it a filter the user
/// can see the effect of (`docs/UX_GUIDELINES.md` §1.6); the wildcard `*` carries no level and is in
/// neither level's set.
@override final  String? access;

/// Create a copy of ListQuery
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ListQueryCopyWith<_ListQuery> get copyWith => __$ListQueryCopyWithImpl<_ListQuery>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$ListQueryToJson(this, );
}

@override
bool operator ==(Object other) {
    return identical(this, other) || (other.runtimeType == runtimeType&&other is _ListQuery&&(identical(other.tenantId, tenantId) || other.tenantId == tenantId)&&(identical(other.search, search) || other.search == search)&&(identical(other.page, page) || other.page == page)&&(identical(other.size, size) || other.size == size)&&(identical(other.sort, sort) || other.sort == sort)&&(identical(other.order, order) || other.order == order)&&(identical(other.scope, scope) || other.scope == scope)&&(identical(other.access, access) || other.access == access));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
    return Object.hash(runtimeType,tenantId,search,page,size,sort,order,scope,access);
}

@override
String toString() {
    return 'ListQuery(tenantId: $tenantId, search: $search, page: $page, size: $size, sort: $sort, order: $order, scope: $scope, access: $access)';
}


}

/// @nodoc
abstract mixin class _$ListQueryCopyWith<$Res> implements $ListQueryCopyWith<$Res> {
  factory _$ListQueryCopyWith(_ListQuery value, $Res Function(_ListQuery) _then) = __$ListQueryCopyWithImpl;
@override @useResult
$Res call({
 String? tenantId, String search, int page, int size, String? sort, String order, String? scope, String? access
});




}
/// @nodoc
class __$ListQueryCopyWithImpl<$Res>
    implements _$ListQueryCopyWith<$Res> {
  __$ListQueryCopyWithImpl(this._self, this._then);

  final _ListQuery _self;
  final $Res Function(_ListQuery) _then;

/// Create a copy of ListQuery
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? tenantId = freezed,Object? search = null,Object? page = null,Object? size = null,Object? sort = freezed,Object? order = null,Object? scope = freezed,Object? access = freezed,}) {
  return _then(_ListQuery(
tenantId: freezed == tenantId ? _self.tenantId : tenantId // ignore: cast_nullable_to_non_nullable
as String?,search: null == search ? _self.search : search // ignore: cast_nullable_to_non_nullable
as String,page: null == page ? _self.page : page // ignore: cast_nullable_to_non_nullable
as int,size: null == size ? _self.size : size // ignore: cast_nullable_to_non_nullable
as int,sort: freezed == sort ? _self.sort : sort // ignore: cast_nullable_to_non_nullable
as String?,order: null == order ? _self.order : order // ignore: cast_nullable_to_non_nullable
as String,scope: freezed == scope ? _self.scope : scope // ignore: cast_nullable_to_non_nullable
as String?,access: freezed == access ? _self.access : access // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}


}

// dart format on
