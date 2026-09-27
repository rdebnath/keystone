// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint, type=warning, deprecated_member_use, deprecated_member_use_from_same_package
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'models.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// GENERATED CODE - DO NOT MODIFY BY HAND
// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$Me {

 String get sub; String get username; String? get tenantId; bool get mustChangePassword; List<String> get permissions;
/// Create a copy of Me
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$MeCopyWith<Me> get copyWith => _$MeCopyWithImpl<Me>(this as Me, _$identity);

  /// Serializes this Me to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  final _this = this as Me;
  return identical(this, other) || (other.runtimeType == runtimeType&&other is Me&&(identical(other.sub, _this.sub) || other.sub == _this.sub)&&(identical(other.username, _this.username) || other.username == _this.username)&&(identical(other.tenantId, _this.tenantId) || other.tenantId == _this.tenantId)&&(identical(other.mustChangePassword, _this.mustChangePassword) || other.mustChangePassword == _this.mustChangePassword)&&const DeepCollectionEquality().equals(other.permissions, _this.permissions));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
  final _this = this as Me;
  return Object.hash(runtimeType,_this.sub,_this.username,_this.tenantId,_this.mustChangePassword,const DeepCollectionEquality().hash(_this.permissions));
}

@override
String toString() {
  final _this = this as Me;
  return 'Me(sub: ${_this.sub}, username: ${_this.username}, tenantId: ${_this.tenantId}, mustChangePassword: ${_this.mustChangePassword}, permissions: ${_this.permissions})';
}


}

/// @nodoc
abstract mixin class $MeCopyWith<$Res>  {
  factory $MeCopyWith(Me value, $Res Function(Me) _then) = _$MeCopyWithImpl;
@useResult
$Res call({
 String sub, String username, String? tenantId, bool mustChangePassword, List<String> permissions
});




}
/// @nodoc
class _$MeCopyWithImpl<$Res>
    implements $MeCopyWith<$Res> {
  _$MeCopyWithImpl(this._self, this._then);

  final Me _self;
  final $Res Function(Me) _then;

/// Create a copy of Me
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? sub = null,Object? username = null,Object? tenantId = freezed,Object? mustChangePassword = null,Object? permissions = null,}) {
  return _then(Me(
sub: null == sub ? _self.sub : sub // ignore: cast_nullable_to_non_nullable
as String,username: null == username ? _self.username : username // ignore: cast_nullable_to_non_nullable
as String,tenantId: freezed == tenantId ? _self.tenantId : tenantId // ignore: cast_nullable_to_non_nullable
as String?,mustChangePassword: null == mustChangePassword ? _self.mustChangePassword : mustChangePassword // ignore: cast_nullable_to_non_nullable
as bool,permissions: null == permissions ? _self.permissions : permissions // ignore: cast_nullable_to_non_nullable
as List<String>,
  ));
}

}


/// Adds pattern-matching-related methods to [Me].
extension MePatterns on Me {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _Me value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _Me() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _Me value)  $default,){
final _that = this;
switch (_that) {
case _Me():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _Me value)?  $default,){
final _that = this;
switch (_that) {
case _Me() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String sub,  String username,  String? tenantId,  bool mustChangePassword,  List<String> permissions)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _Me() when $default != null:
return $default(_that.sub,_that.username,_that.tenantId,_that.mustChangePassword,_that.permissions);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String sub,  String username,  String? tenantId,  bool mustChangePassword,  List<String> permissions)  $default,) {final _that = this;
switch (_that) {
case _Me():
return $default(_that.sub,_that.username,_that.tenantId,_that.mustChangePassword,_that.permissions);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String sub,  String username,  String? tenantId,  bool mustChangePassword,  List<String> permissions)?  $default,) {final _that = this;
switch (_that) {
case _Me() when $default != null:
return $default(_that.sub,_that.username,_that.tenantId,_that.mustChangePassword,_that.permissions);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _Me extends Me {
  const _Me({required this.sub, this.username = '', this.tenantId, this.mustChangePassword = false,  List<String> permissions = const <String>[]}): _permissions = permissions,super._();
  factory _Me.fromJson(Map<String, dynamic> json) => _$MeFromJson(json);

@override final  String sub;
@override@JsonKey() final  String username;
@override final  String? tenantId;
@override@JsonKey() final  bool mustChangePassword;
 final  List<String> _permissions;
@override@JsonKey() List<String> get permissions {
  if (_permissions is EqualUnmodifiableListView) return _permissions;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_permissions);
}


/// Create a copy of Me
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$MeCopyWith<_Me> get copyWith => __$MeCopyWithImpl<_Me>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$MeToJson(this, );
}

@override
bool operator ==(Object other) {
    return identical(this, other) || (other.runtimeType == runtimeType&&other is _Me&&(identical(other.sub, sub) || other.sub == sub)&&(identical(other.username, username) || other.username == username)&&(identical(other.tenantId, tenantId) || other.tenantId == tenantId)&&(identical(other.mustChangePassword, mustChangePassword) || other.mustChangePassword == mustChangePassword)&&const DeepCollectionEquality().equals(other.permissions, _permissions));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
    return Object.hash(runtimeType,sub,username,tenantId,mustChangePassword,const DeepCollectionEquality().hash(_permissions));
}

@override
String toString() {
    return 'Me(sub: $sub, username: $username, tenantId: $tenantId, mustChangePassword: $mustChangePassword, permissions: $permissions)';
}


}

/// @nodoc
abstract mixin class _$MeCopyWith<$Res> implements $MeCopyWith<$Res> {
  factory _$MeCopyWith(_Me value, $Res Function(_Me) _then) = __$MeCopyWithImpl;
@override @useResult
$Res call({
 String sub, String username, String? tenantId, bool mustChangePassword, List<String> permissions
});




}
/// @nodoc
class __$MeCopyWithImpl<$Res>
    implements _$MeCopyWith<$Res> {
  __$MeCopyWithImpl(this._self, this._then);

  final _Me _self;
  final $Res Function(_Me) _then;

/// Create a copy of Me
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? sub = null,Object? username = null,Object? tenantId = freezed,Object? mustChangePassword = null,Object? permissions = null,}) {
  return _then(_Me(
sub: null == sub ? _self.sub : sub // ignore: cast_nullable_to_non_nullable
as String,username: null == username ? _self.username : username // ignore: cast_nullable_to_non_nullable
as String,tenantId: freezed == tenantId ? _self.tenantId : tenantId // ignore: cast_nullable_to_non_nullable
as String?,mustChangePassword: null == mustChangePassword ? _self.mustChangePassword : mustChangePassword // ignore: cast_nullable_to_non_nullable
as bool,permissions: null == permissions ? _self._permissions : permissions // ignore: cast_nullable_to_non_nullable
as List<String>,
  ));
}


}


/// @nodoc
mixin _$Tenant {

 String get id; String get name; String get slug; bool get platform; String get createdAt; String get updatedAt;
/// Create a copy of Tenant
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$TenantCopyWith<Tenant> get copyWith => _$TenantCopyWithImpl<Tenant>(this as Tenant, _$identity);

  /// Serializes this Tenant to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  final _this = this as Tenant;
  return identical(this, other) || (other.runtimeType == runtimeType&&other is Tenant&&(identical(other.id, _this.id) || other.id == _this.id)&&(identical(other.name, _this.name) || other.name == _this.name)&&(identical(other.slug, _this.slug) || other.slug == _this.slug)&&(identical(other.platform, _this.platform) || other.platform == _this.platform)&&(identical(other.createdAt, _this.createdAt) || other.createdAt == _this.createdAt)&&(identical(other.updatedAt, _this.updatedAt) || other.updatedAt == _this.updatedAt));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
  final _this = this as Tenant;
  return Object.hash(runtimeType,_this.id,_this.name,_this.slug,_this.platform,_this.createdAt,_this.updatedAt);
}

@override
String toString() {
  final _this = this as Tenant;
  return 'Tenant(id: ${_this.id}, name: ${_this.name}, slug: ${_this.slug}, platform: ${_this.platform}, createdAt: ${_this.createdAt}, updatedAt: ${_this.updatedAt})';
}


}

/// @nodoc
abstract mixin class $TenantCopyWith<$Res>  {
  factory $TenantCopyWith(Tenant value, $Res Function(Tenant) _then) = _$TenantCopyWithImpl;
@useResult
$Res call({
 String id, String name, String slug, bool platform, String createdAt, String updatedAt
});




}
/// @nodoc
class _$TenantCopyWithImpl<$Res>
    implements $TenantCopyWith<$Res> {
  _$TenantCopyWithImpl(this._self, this._then);

  final Tenant _self;
  final $Res Function(Tenant) _then;

/// Create a copy of Tenant
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? id = null,Object? name = null,Object? slug = null,Object? platform = null,Object? createdAt = null,Object? updatedAt = null,}) {
  return _then(Tenant(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,name: null == name ? _self.name : name // ignore: cast_nullable_to_non_nullable
as String,slug: null == slug ? _self.slug : slug // ignore: cast_nullable_to_non_nullable
as String,platform: null == platform ? _self.platform : platform // ignore: cast_nullable_to_non_nullable
as bool,createdAt: null == createdAt ? _self.createdAt : createdAt // ignore: cast_nullable_to_non_nullable
as String,updatedAt: null == updatedAt ? _self.updatedAt : updatedAt // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [Tenant].
extension TenantPatterns on Tenant {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _Tenant value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _Tenant() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _Tenant value)  $default,){
final _that = this;
switch (_that) {
case _Tenant():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _Tenant value)?  $default,){
final _that = this;
switch (_that) {
case _Tenant() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String id,  String name,  String slug,  bool platform,  String createdAt,  String updatedAt)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _Tenant() when $default != null:
return $default(_that.id,_that.name,_that.slug,_that.platform,_that.createdAt,_that.updatedAt);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String id,  String name,  String slug,  bool platform,  String createdAt,  String updatedAt)  $default,) {final _that = this;
switch (_that) {
case _Tenant():
return $default(_that.id,_that.name,_that.slug,_that.platform,_that.createdAt,_that.updatedAt);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String id,  String name,  String slug,  bool platform,  String createdAt,  String updatedAt)?  $default,) {final _that = this;
switch (_that) {
case _Tenant() when $default != null:
return $default(_that.id,_that.name,_that.slug,_that.platform,_that.createdAt,_that.updatedAt);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _Tenant extends Tenant {
  const _Tenant({required this.id, required this.name, this.slug = '', this.platform = false, this.createdAt = '', this.updatedAt = ''}): super._();
  factory _Tenant.fromJson(Map<String, dynamic> json) => _$TenantFromJson(json);

@override final  String id;
@override final  String name;
@override@JsonKey() final  String slug;
@override@JsonKey() final  bool platform;
@override@JsonKey() final  String createdAt;
@override@JsonKey() final  String updatedAt;

/// Create a copy of Tenant
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$TenantCopyWith<_Tenant> get copyWith => __$TenantCopyWithImpl<_Tenant>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$TenantToJson(this, );
}

@override
bool operator ==(Object other) {
    return identical(this, other) || (other.runtimeType == runtimeType&&other is _Tenant&&(identical(other.id, id) || other.id == id)&&(identical(other.name, name) || other.name == name)&&(identical(other.slug, slug) || other.slug == slug)&&(identical(other.platform, platform) || other.platform == platform)&&(identical(other.createdAt, createdAt) || other.createdAt == createdAt)&&(identical(other.updatedAt, updatedAt) || other.updatedAt == updatedAt));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
    return Object.hash(runtimeType,id,name,slug,platform,createdAt,updatedAt);
}

@override
String toString() {
    return 'Tenant(id: $id, name: $name, slug: $slug, platform: $platform, createdAt: $createdAt, updatedAt: $updatedAt)';
}


}

/// @nodoc
abstract mixin class _$TenantCopyWith<$Res> implements $TenantCopyWith<$Res> {
  factory _$TenantCopyWith(_Tenant value, $Res Function(_Tenant) _then) = __$TenantCopyWithImpl;
@override @useResult
$Res call({
 String id, String name, String slug, bool platform, String createdAt, String updatedAt
});




}
/// @nodoc
class __$TenantCopyWithImpl<$Res>
    implements _$TenantCopyWith<$Res> {
  __$TenantCopyWithImpl(this._self, this._then);

  final _Tenant _self;
  final $Res Function(_Tenant) _then;

/// Create a copy of Tenant
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? id = null,Object? name = null,Object? slug = null,Object? platform = null,Object? createdAt = null,Object? updatedAt = null,}) {
  return _then(_Tenant(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,name: null == name ? _self.name : name // ignore: cast_nullable_to_non_nullable
as String,slug: null == slug ? _self.slug : slug // ignore: cast_nullable_to_non_nullable
as String,platform: null == platform ? _self.platform : platform // ignore: cast_nullable_to_non_nullable
as bool,createdAt: null == createdAt ? _self.createdAt : createdAt // ignore: cast_nullable_to_non_nullable
as String,updatedAt: null == updatedAt ? _self.updatedAt : updatedAt // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}


/// @nodoc
mixin _$Role {

 String get id; String get code; String get scope; List<String> get permissions; String get createdAt; String get updatedAt;
/// Create a copy of Role
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$RoleCopyWith<Role> get copyWith => _$RoleCopyWithImpl<Role>(this as Role, _$identity);

  /// Serializes this Role to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  final _this = this as Role;
  return identical(this, other) || (other.runtimeType == runtimeType&&other is Role&&(identical(other.id, _this.id) || other.id == _this.id)&&(identical(other.code, _this.code) || other.code == _this.code)&&(identical(other.scope, _this.scope) || other.scope == _this.scope)&&const DeepCollectionEquality().equals(other.permissions, _this.permissions)&&(identical(other.createdAt, _this.createdAt) || other.createdAt == _this.createdAt)&&(identical(other.updatedAt, _this.updatedAt) || other.updatedAt == _this.updatedAt));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
  final _this = this as Role;
  return Object.hash(runtimeType,_this.id,_this.code,_this.scope,const DeepCollectionEquality().hash(_this.permissions),_this.createdAt,_this.updatedAt);
}

@override
String toString() {
  final _this = this as Role;
  return 'Role(id: ${_this.id}, code: ${_this.code}, scope: ${_this.scope}, permissions: ${_this.permissions}, createdAt: ${_this.createdAt}, updatedAt: ${_this.updatedAt})';
}


}

/// @nodoc
abstract mixin class $RoleCopyWith<$Res>  {
  factory $RoleCopyWith(Role value, $Res Function(Role) _then) = _$RoleCopyWithImpl;
@useResult
$Res call({
 String id, String code, String scope, List<String> permissions, String createdAt, String updatedAt
});




}
/// @nodoc
class _$RoleCopyWithImpl<$Res>
    implements $RoleCopyWith<$Res> {
  _$RoleCopyWithImpl(this._self, this._then);

  final Role _self;
  final $Res Function(Role) _then;

/// Create a copy of Role
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? id = null,Object? code = null,Object? scope = null,Object? permissions = null,Object? createdAt = null,Object? updatedAt = null,}) {
  return _then(Role(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,code: null == code ? _self.code : code // ignore: cast_nullable_to_non_nullable
as String,scope: null == scope ? _self.scope : scope // ignore: cast_nullable_to_non_nullable
as String,permissions: null == permissions ? _self.permissions : permissions // ignore: cast_nullable_to_non_nullable
as List<String>,createdAt: null == createdAt ? _self.createdAt : createdAt // ignore: cast_nullable_to_non_nullable
as String,updatedAt: null == updatedAt ? _self.updatedAt : updatedAt // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [Role].
extension RolePatterns on Role {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _Role value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _Role() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _Role value)  $default,){
final _that = this;
switch (_that) {
case _Role():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _Role value)?  $default,){
final _that = this;
switch (_that) {
case _Role() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String id,  String code,  String scope,  List<String> permissions,  String createdAt,  String updatedAt)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _Role() when $default != null:
return $default(_that.id,_that.code,_that.scope,_that.permissions,_that.createdAt,_that.updatedAt);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String id,  String code,  String scope,  List<String> permissions,  String createdAt,  String updatedAt)  $default,) {final _that = this;
switch (_that) {
case _Role():
return $default(_that.id,_that.code,_that.scope,_that.permissions,_that.createdAt,_that.updatedAt);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String id,  String code,  String scope,  List<String> permissions,  String createdAt,  String updatedAt)?  $default,) {final _that = this;
switch (_that) {
case _Role() when $default != null:
return $default(_that.id,_that.code,_that.scope,_that.permissions,_that.createdAt,_that.updatedAt);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _Role extends Role {
  const _Role({required this.id, required this.code, this.scope = '',  List<String> permissions = const <String>[], this.createdAt = '', this.updatedAt = ''}): _permissions = permissions,super._();
  factory _Role.fromJson(Map<String, dynamic> json) => _$RoleFromJson(json);

@override final  String id;
@override final  String code;
@override@JsonKey() final  String scope;
 final  List<String> _permissions;
@override@JsonKey() List<String> get permissions {
  if (_permissions is EqualUnmodifiableListView) return _permissions;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_permissions);
}

@override@JsonKey() final  String createdAt;
@override@JsonKey() final  String updatedAt;

/// Create a copy of Role
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$RoleCopyWith<_Role> get copyWith => __$RoleCopyWithImpl<_Role>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$RoleToJson(this, );
}

@override
bool operator ==(Object other) {
    return identical(this, other) || (other.runtimeType == runtimeType&&other is _Role&&(identical(other.id, id) || other.id == id)&&(identical(other.code, code) || other.code == code)&&(identical(other.scope, scope) || other.scope == scope)&&const DeepCollectionEquality().equals(other.permissions, _permissions)&&(identical(other.createdAt, createdAt) || other.createdAt == createdAt)&&(identical(other.updatedAt, updatedAt) || other.updatedAt == updatedAt));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
    return Object.hash(runtimeType,id,code,scope,const DeepCollectionEquality().hash(_permissions),createdAt,updatedAt);
}

@override
String toString() {
    return 'Role(id: $id, code: $code, scope: $scope, permissions: $permissions, createdAt: $createdAt, updatedAt: $updatedAt)';
}


}

/// @nodoc
abstract mixin class _$RoleCopyWith<$Res> implements $RoleCopyWith<$Res> {
  factory _$RoleCopyWith(_Role value, $Res Function(_Role) _then) = __$RoleCopyWithImpl;
@override @useResult
$Res call({
 String id, String code, String scope, List<String> permissions, String createdAt, String updatedAt
});




}
/// @nodoc
class __$RoleCopyWithImpl<$Res>
    implements _$RoleCopyWith<$Res> {
  __$RoleCopyWithImpl(this._self, this._then);

  final _Role _self;
  final $Res Function(_Role) _then;

/// Create a copy of Role
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? id = null,Object? code = null,Object? scope = null,Object? permissions = null,Object? createdAt = null,Object? updatedAt = null,}) {
  return _then(_Role(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,code: null == code ? _self.code : code // ignore: cast_nullable_to_non_nullable
as String,scope: null == scope ? _self.scope : scope // ignore: cast_nullable_to_non_nullable
as String,permissions: null == permissions ? _self._permissions : permissions // ignore: cast_nullable_to_non_nullable
as List<String>,createdAt: null == createdAt ? _self.createdAt : createdAt // ignore: cast_nullable_to_non_nullable
as String,updatedAt: null == updatedAt ? _self.updatedAt : updatedAt // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}


/// @nodoc
mixin _$Permission {

 String get id; String get code; String get scope; String get createdAt; String get updatedAt;
/// Create a copy of Permission
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$PermissionCopyWith<Permission> get copyWith => _$PermissionCopyWithImpl<Permission>(this as Permission, _$identity);

  /// Serializes this Permission to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  final _this = this as Permission;
  return identical(this, other) || (other.runtimeType == runtimeType&&other is Permission&&(identical(other.id, _this.id) || other.id == _this.id)&&(identical(other.code, _this.code) || other.code == _this.code)&&(identical(other.scope, _this.scope) || other.scope == _this.scope)&&(identical(other.createdAt, _this.createdAt) || other.createdAt == _this.createdAt)&&(identical(other.updatedAt, _this.updatedAt) || other.updatedAt == _this.updatedAt));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
  final _this = this as Permission;
  return Object.hash(runtimeType,_this.id,_this.code,_this.scope,_this.createdAt,_this.updatedAt);
}

@override
String toString() {
  final _this = this as Permission;
  return 'Permission(id: ${_this.id}, code: ${_this.code}, scope: ${_this.scope}, createdAt: ${_this.createdAt}, updatedAt: ${_this.updatedAt})';
}


}

/// @nodoc
abstract mixin class $PermissionCopyWith<$Res>  {
  factory $PermissionCopyWith(Permission value, $Res Function(Permission) _then) = _$PermissionCopyWithImpl;
@useResult
$Res call({
 String id, String code, String scope, String createdAt, String updatedAt
});




}
/// @nodoc
class _$PermissionCopyWithImpl<$Res>
    implements $PermissionCopyWith<$Res> {
  _$PermissionCopyWithImpl(this._self, this._then);

  final Permission _self;
  final $Res Function(Permission) _then;

/// Create a copy of Permission
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? id = null,Object? code = null,Object? scope = null,Object? createdAt = null,Object? updatedAt = null,}) {
  return _then(Permission(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,code: null == code ? _self.code : code // ignore: cast_nullable_to_non_nullable
as String,scope: null == scope ? _self.scope : scope // ignore: cast_nullable_to_non_nullable
as String,createdAt: null == createdAt ? _self.createdAt : createdAt // ignore: cast_nullable_to_non_nullable
as String,updatedAt: null == updatedAt ? _self.updatedAt : updatedAt // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [Permission].
extension PermissionPatterns on Permission {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _Permission value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _Permission() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _Permission value)  $default,){
final _that = this;
switch (_that) {
case _Permission():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _Permission value)?  $default,){
final _that = this;
switch (_that) {
case _Permission() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String id,  String code,  String scope,  String createdAt,  String updatedAt)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _Permission() when $default != null:
return $default(_that.id,_that.code,_that.scope,_that.createdAt,_that.updatedAt);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String id,  String code,  String scope,  String createdAt,  String updatedAt)  $default,) {final _that = this;
switch (_that) {
case _Permission():
return $default(_that.id,_that.code,_that.scope,_that.createdAt,_that.updatedAt);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String id,  String code,  String scope,  String createdAt,  String updatedAt)?  $default,) {final _that = this;
switch (_that) {
case _Permission() when $default != null:
return $default(_that.id,_that.code,_that.scope,_that.createdAt,_that.updatedAt);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _Permission extends Permission {
  const _Permission({required this.id, required this.code, this.scope = '', this.createdAt = '', this.updatedAt = ''}): super._();
  factory _Permission.fromJson(Map<String, dynamic> json) => _$PermissionFromJson(json);

@override final  String id;
@override final  String code;
@override@JsonKey() final  String scope;
@override@JsonKey() final  String createdAt;
@override@JsonKey() final  String updatedAt;

/// Create a copy of Permission
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$PermissionCopyWith<_Permission> get copyWith => __$PermissionCopyWithImpl<_Permission>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$PermissionToJson(this, );
}

@override
bool operator ==(Object other) {
    return identical(this, other) || (other.runtimeType == runtimeType&&other is _Permission&&(identical(other.id, id) || other.id == id)&&(identical(other.code, code) || other.code == code)&&(identical(other.scope, scope) || other.scope == scope)&&(identical(other.createdAt, createdAt) || other.createdAt == createdAt)&&(identical(other.updatedAt, updatedAt) || other.updatedAt == updatedAt));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
    return Object.hash(runtimeType,id,code,scope,createdAt,updatedAt);
}

@override
String toString() {
    return 'Permission(id: $id, code: $code, scope: $scope, createdAt: $createdAt, updatedAt: $updatedAt)';
}


}

/// @nodoc
abstract mixin class _$PermissionCopyWith<$Res> implements $PermissionCopyWith<$Res> {
  factory _$PermissionCopyWith(_Permission value, $Res Function(_Permission) _then) = __$PermissionCopyWithImpl;
@override @useResult
$Res call({
 String id, String code, String scope, String createdAt, String updatedAt
});




}
/// @nodoc
class __$PermissionCopyWithImpl<$Res>
    implements _$PermissionCopyWith<$Res> {
  __$PermissionCopyWithImpl(this._self, this._then);

  final _Permission _self;
  final $Res Function(_Permission) _then;

/// Create a copy of Permission
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? id = null,Object? code = null,Object? scope = null,Object? createdAt = null,Object? updatedAt = null,}) {
  return _then(_Permission(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,code: null == code ? _self.code : code // ignore: cast_nullable_to_non_nullable
as String,scope: null == scope ? _self.scope : scope // ignore: cast_nullable_to_non_nullable
as String,createdAt: null == createdAt ? _self.createdAt : createdAt // ignore: cast_nullable_to_non_nullable
as String,updatedAt: null == updatedAt ? _self.updatedAt : updatedAt // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}


/// @nodoc
mixin _$User {

 String get id; String get sub; String get username; String get email; String? get tenantId; bool get mustChangePassword; List<String> get roles; String get createdAt; String get updatedAt;
/// Create a copy of User
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$UserCopyWith<User> get copyWith => _$UserCopyWithImpl<User>(this as User, _$identity);

  /// Serializes this User to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  final _this = this as User;
  return identical(this, other) || (other.runtimeType == runtimeType&&other is User&&(identical(other.id, _this.id) || other.id == _this.id)&&(identical(other.sub, _this.sub) || other.sub == _this.sub)&&(identical(other.username, _this.username) || other.username == _this.username)&&(identical(other.email, _this.email) || other.email == _this.email)&&(identical(other.tenantId, _this.tenantId) || other.tenantId == _this.tenantId)&&(identical(other.mustChangePassword, _this.mustChangePassword) || other.mustChangePassword == _this.mustChangePassword)&&const DeepCollectionEquality().equals(other.roles, _this.roles)&&(identical(other.createdAt, _this.createdAt) || other.createdAt == _this.createdAt)&&(identical(other.updatedAt, _this.updatedAt) || other.updatedAt == _this.updatedAt));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
  final _this = this as User;
  return Object.hash(runtimeType,_this.id,_this.sub,_this.username,_this.email,_this.tenantId,_this.mustChangePassword,const DeepCollectionEquality().hash(_this.roles),_this.createdAt,_this.updatedAt);
}

@override
String toString() {
  final _this = this as User;
  return 'User(id: ${_this.id}, sub: ${_this.sub}, username: ${_this.username}, email: ${_this.email}, tenantId: ${_this.tenantId}, mustChangePassword: ${_this.mustChangePassword}, roles: ${_this.roles}, createdAt: ${_this.createdAt}, updatedAt: ${_this.updatedAt})';
}


}

/// @nodoc
abstract mixin class $UserCopyWith<$Res>  {
  factory $UserCopyWith(User value, $Res Function(User) _then) = _$UserCopyWithImpl;
@useResult
$Res call({
 String id, String sub, String username, String email, String? tenantId, bool mustChangePassword, List<String> roles, String createdAt, String updatedAt
});




}
/// @nodoc
class _$UserCopyWithImpl<$Res>
    implements $UserCopyWith<$Res> {
  _$UserCopyWithImpl(this._self, this._then);

  final User _self;
  final $Res Function(User) _then;

/// Create a copy of User
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? id = null,Object? sub = null,Object? username = null,Object? email = null,Object? tenantId = freezed,Object? mustChangePassword = null,Object? roles = null,Object? createdAt = null,Object? updatedAt = null,}) {
  return _then(User(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,sub: null == sub ? _self.sub : sub // ignore: cast_nullable_to_non_nullable
as String,username: null == username ? _self.username : username // ignore: cast_nullable_to_non_nullable
as String,email: null == email ? _self.email : email // ignore: cast_nullable_to_non_nullable
as String,tenantId: freezed == tenantId ? _self.tenantId : tenantId // ignore: cast_nullable_to_non_nullable
as String?,mustChangePassword: null == mustChangePassword ? _self.mustChangePassword : mustChangePassword // ignore: cast_nullable_to_non_nullable
as bool,roles: null == roles ? _self.roles : roles // ignore: cast_nullable_to_non_nullable
as List<String>,createdAt: null == createdAt ? _self.createdAt : createdAt // ignore: cast_nullable_to_non_nullable
as String,updatedAt: null == updatedAt ? _self.updatedAt : updatedAt // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [User].
extension UserPatterns on User {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _User value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _User() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _User value)  $default,){
final _that = this;
switch (_that) {
case _User():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _User value)?  $default,){
final _that = this;
switch (_that) {
case _User() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String id,  String sub,  String username,  String email,  String? tenantId,  bool mustChangePassword,  List<String> roles,  String createdAt,  String updatedAt)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _User() when $default != null:
return $default(_that.id,_that.sub,_that.username,_that.email,_that.tenantId,_that.mustChangePassword,_that.roles,_that.createdAt,_that.updatedAt);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String id,  String sub,  String username,  String email,  String? tenantId,  bool mustChangePassword,  List<String> roles,  String createdAt,  String updatedAt)  $default,) {final _that = this;
switch (_that) {
case _User():
return $default(_that.id,_that.sub,_that.username,_that.email,_that.tenantId,_that.mustChangePassword,_that.roles,_that.createdAt,_that.updatedAt);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String id,  String sub,  String username,  String email,  String? tenantId,  bool mustChangePassword,  List<String> roles,  String createdAt,  String updatedAt)?  $default,) {final _that = this;
switch (_that) {
case _User() when $default != null:
return $default(_that.id,_that.sub,_that.username,_that.email,_that.tenantId,_that.mustChangePassword,_that.roles,_that.createdAt,_that.updatedAt);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _User implements User {
  const _User({required this.id, required this.sub, this.username = '', required this.email, this.tenantId, this.mustChangePassword = false,  List<String> roles = const <String>[], this.createdAt = '', this.updatedAt = ''}): _roles = roles;
  factory _User.fromJson(Map<String, dynamic> json) => _$UserFromJson(json);

@override final  String id;
@override final  String sub;
@override@JsonKey() final  String username;
@override final  String email;
@override final  String? tenantId;
@override@JsonKey() final  bool mustChangePassword;
 final  List<String> _roles;
@override@JsonKey() List<String> get roles {
  if (_roles is EqualUnmodifiableListView) return _roles;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_roles);
}

@override@JsonKey() final  String createdAt;
@override@JsonKey() final  String updatedAt;

/// Create a copy of User
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$UserCopyWith<_User> get copyWith => __$UserCopyWithImpl<_User>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$UserToJson(this, );
}

@override
bool operator ==(Object other) {
    return identical(this, other) || (other.runtimeType == runtimeType&&other is _User&&(identical(other.id, id) || other.id == id)&&(identical(other.sub, sub) || other.sub == sub)&&(identical(other.username, username) || other.username == username)&&(identical(other.email, email) || other.email == email)&&(identical(other.tenantId, tenantId) || other.tenantId == tenantId)&&(identical(other.mustChangePassword, mustChangePassword) || other.mustChangePassword == mustChangePassword)&&const DeepCollectionEquality().equals(other.roles, _roles)&&(identical(other.createdAt, createdAt) || other.createdAt == createdAt)&&(identical(other.updatedAt, updatedAt) || other.updatedAt == updatedAt));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
    return Object.hash(runtimeType,id,sub,username,email,tenantId,mustChangePassword,const DeepCollectionEquality().hash(_roles),createdAt,updatedAt);
}

@override
String toString() {
    return 'User(id: $id, sub: $sub, username: $username, email: $email, tenantId: $tenantId, mustChangePassword: $mustChangePassword, roles: $roles, createdAt: $createdAt, updatedAt: $updatedAt)';
}


}

/// @nodoc
abstract mixin class _$UserCopyWith<$Res> implements $UserCopyWith<$Res> {
  factory _$UserCopyWith(_User value, $Res Function(_User) _then) = __$UserCopyWithImpl;
@override @useResult
$Res call({
 String id, String sub, String username, String email, String? tenantId, bool mustChangePassword, List<String> roles, String createdAt, String updatedAt
});




}
/// @nodoc
class __$UserCopyWithImpl<$Res>
    implements _$UserCopyWith<$Res> {
  __$UserCopyWithImpl(this._self, this._then);

  final _User _self;
  final $Res Function(_User) _then;

/// Create a copy of User
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? id = null,Object? sub = null,Object? username = null,Object? email = null,Object? tenantId = freezed,Object? mustChangePassword = null,Object? roles = null,Object? createdAt = null,Object? updatedAt = null,}) {
  return _then(_User(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,sub: null == sub ? _self.sub : sub // ignore: cast_nullable_to_non_nullable
as String,username: null == username ? _self.username : username // ignore: cast_nullable_to_non_nullable
as String,email: null == email ? _self.email : email // ignore: cast_nullable_to_non_nullable
as String,tenantId: freezed == tenantId ? _self.tenantId : tenantId // ignore: cast_nullable_to_non_nullable
as String?,mustChangePassword: null == mustChangePassword ? _self.mustChangePassword : mustChangePassword // ignore: cast_nullable_to_non_nullable
as bool,roles: null == roles ? _self._roles : roles // ignore: cast_nullable_to_non_nullable
as List<String>,createdAt: null == createdAt ? _self.createdAt : createdAt // ignore: cast_nullable_to_non_nullable
as String,updatedAt: null == updatedAt ? _self.updatedAt : updatedAt // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}


/// @nodoc
mixin _$Session {

 String get accessToken; String get refreshToken; String get tokenType; int get expiresIn;
/// Create a copy of Session
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$SessionCopyWith<Session> get copyWith => _$SessionCopyWithImpl<Session>(this as Session, _$identity);

  /// Serializes this Session to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  final _this = this as Session;
  return identical(this, other) || (other.runtimeType == runtimeType&&other is Session&&(identical(other.accessToken, _this.accessToken) || other.accessToken == _this.accessToken)&&(identical(other.refreshToken, _this.refreshToken) || other.refreshToken == _this.refreshToken)&&(identical(other.tokenType, _this.tokenType) || other.tokenType == _this.tokenType)&&(identical(other.expiresIn, _this.expiresIn) || other.expiresIn == _this.expiresIn));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
  final _this = this as Session;
  return Object.hash(runtimeType,_this.accessToken,_this.refreshToken,_this.tokenType,_this.expiresIn);
}

@override
String toString() {
  final _this = this as Session;
  return 'Session(accessToken: ${_this.accessToken}, refreshToken: ${_this.refreshToken}, tokenType: ${_this.tokenType}, expiresIn: ${_this.expiresIn})';
}


}

/// @nodoc
abstract mixin class $SessionCopyWith<$Res>  {
  factory $SessionCopyWith(Session value, $Res Function(Session) _then) = _$SessionCopyWithImpl;
@useResult
$Res call({
 String accessToken, String refreshToken, String tokenType, int expiresIn
});




}
/// @nodoc
class _$SessionCopyWithImpl<$Res>
    implements $SessionCopyWith<$Res> {
  _$SessionCopyWithImpl(this._self, this._then);

  final Session _self;
  final $Res Function(Session) _then;

/// Create a copy of Session
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? accessToken = null,Object? refreshToken = null,Object? tokenType = null,Object? expiresIn = null,}) {
  return _then(Session(
accessToken: null == accessToken ? _self.accessToken : accessToken // ignore: cast_nullable_to_non_nullable
as String,refreshToken: null == refreshToken ? _self.refreshToken : refreshToken // ignore: cast_nullable_to_non_nullable
as String,tokenType: null == tokenType ? _self.tokenType : tokenType // ignore: cast_nullable_to_non_nullable
as String,expiresIn: null == expiresIn ? _self.expiresIn : expiresIn // ignore: cast_nullable_to_non_nullable
as int,
  ));
}

}


/// Adds pattern-matching-related methods to [Session].
extension SessionPatterns on Session {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _Session value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _Session() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _Session value)  $default,){
final _that = this;
switch (_that) {
case _Session():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _Session value)?  $default,){
final _that = this;
switch (_that) {
case _Session() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String accessToken,  String refreshToken,  String tokenType,  int expiresIn)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _Session() when $default != null:
return $default(_that.accessToken,_that.refreshToken,_that.tokenType,_that.expiresIn);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String accessToken,  String refreshToken,  String tokenType,  int expiresIn)  $default,) {final _that = this;
switch (_that) {
case _Session():
return $default(_that.accessToken,_that.refreshToken,_that.tokenType,_that.expiresIn);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String accessToken,  String refreshToken,  String tokenType,  int expiresIn)?  $default,) {final _that = this;
switch (_that) {
case _Session() when $default != null:
return $default(_that.accessToken,_that.refreshToken,_that.tokenType,_that.expiresIn);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _Session implements Session {
  const _Session({required this.accessToken, this.refreshToken = '', this.tokenType = 'bearer', this.expiresIn = 0});
  factory _Session.fromJson(Map<String, dynamic> json) => _$SessionFromJson(json);

@override final  String accessToken;
@override@JsonKey() final  String refreshToken;
@override@JsonKey() final  String tokenType;
@override@JsonKey() final  int expiresIn;

/// Create a copy of Session
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$SessionCopyWith<_Session> get copyWith => __$SessionCopyWithImpl<_Session>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$SessionToJson(this, );
}

@override
bool operator ==(Object other) {
    return identical(this, other) || (other.runtimeType == runtimeType&&other is _Session&&(identical(other.accessToken, accessToken) || other.accessToken == accessToken)&&(identical(other.refreshToken, refreshToken) || other.refreshToken == refreshToken)&&(identical(other.tokenType, tokenType) || other.tokenType == tokenType)&&(identical(other.expiresIn, expiresIn) || other.expiresIn == expiresIn));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
    return Object.hash(runtimeType,accessToken,refreshToken,tokenType,expiresIn);
}

@override
String toString() {
    return 'Session(accessToken: $accessToken, refreshToken: $refreshToken, tokenType: $tokenType, expiresIn: $expiresIn)';
}


}

/// @nodoc
abstract mixin class _$SessionCopyWith<$Res> implements $SessionCopyWith<$Res> {
  factory _$SessionCopyWith(_Session value, $Res Function(_Session) _then) = __$SessionCopyWithImpl;
@override @useResult
$Res call({
 String accessToken, String refreshToken, String tokenType, int expiresIn
});




}
/// @nodoc
class __$SessionCopyWithImpl<$Res>
    implements _$SessionCopyWith<$Res> {
  __$SessionCopyWithImpl(this._self, this._then);

  final _Session _self;
  final $Res Function(_Session) _then;

/// Create a copy of Session
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? accessToken = null,Object? refreshToken = null,Object? tokenType = null,Object? expiresIn = null,}) {
  return _then(_Session(
accessToken: null == accessToken ? _self.accessToken : accessToken // ignore: cast_nullable_to_non_nullable
as String,refreshToken: null == refreshToken ? _self.refreshToken : refreshToken // ignore: cast_nullable_to_non_nullable
as String,tokenType: null == tokenType ? _self.tokenType : tokenType // ignore: cast_nullable_to_non_nullable
as String,expiresIn: null == expiresIn ? _self.expiresIn : expiresIn // ignore: cast_nullable_to_non_nullable
as int,
  ));
}


}

// dart format on
