// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint, type=warning, deprecated_member_use, deprecated_member_use_from_same_package
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'requests.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// GENERATED CODE - DO NOT MODIFY BY HAND
// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$LoginRequest {

 String get identifier; String get password;
/// Create a copy of LoginRequest
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$LoginRequestCopyWith<LoginRequest> get copyWith => _$LoginRequestCopyWithImpl<LoginRequest>(this as LoginRequest, _$identity);

  /// Serializes this LoginRequest to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  final _this = this as LoginRequest;
  return identical(this, other) || (other.runtimeType == runtimeType&&other is LoginRequest&&(identical(other.identifier, _this.identifier) || other.identifier == _this.identifier)&&(identical(other.password, _this.password) || other.password == _this.password));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
  final _this = this as LoginRequest;
  return Object.hash(runtimeType,_this.identifier,_this.password);
}

@override
String toString() {
  final _this = this as LoginRequest;
  return 'LoginRequest(identifier: ${_this.identifier}, password: ${_this.password})';
}


}

/// @nodoc
abstract mixin class $LoginRequestCopyWith<$Res>  {
  factory $LoginRequestCopyWith(LoginRequest value, $Res Function(LoginRequest) _then) = _$LoginRequestCopyWithImpl;
@useResult
$Res call({
 String identifier, String password
});




}
/// @nodoc
class _$LoginRequestCopyWithImpl<$Res>
    implements $LoginRequestCopyWith<$Res> {
  _$LoginRequestCopyWithImpl(this._self, this._then);

  final LoginRequest _self;
  final $Res Function(LoginRequest) _then;

/// Create a copy of LoginRequest
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? identifier = null,Object? password = null,}) {
  return _then(LoginRequest(
identifier: null == identifier ? _self.identifier : identifier // ignore: cast_nullable_to_non_nullable
as String,password: null == password ? _self.password : password // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [LoginRequest].
extension LoginRequestPatterns on LoginRequest {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _LoginRequest value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _LoginRequest() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _LoginRequest value)  $default,){
final _that = this;
switch (_that) {
case _LoginRequest():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _LoginRequest value)?  $default,){
final _that = this;
switch (_that) {
case _LoginRequest() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String identifier,  String password)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _LoginRequest() when $default != null:
return $default(_that.identifier,_that.password);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String identifier,  String password)  $default,) {final _that = this;
switch (_that) {
case _LoginRequest():
return $default(_that.identifier,_that.password);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String identifier,  String password)?  $default,) {final _that = this;
switch (_that) {
case _LoginRequest() when $default != null:
return $default(_that.identifier,_that.password);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _LoginRequest implements LoginRequest {
  const _LoginRequest({required this.identifier, required this.password});
  factory _LoginRequest.fromJson(Map<String, dynamic> json) => _$LoginRequestFromJson(json);

@override final  String identifier;
@override final  String password;

/// Create a copy of LoginRequest
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$LoginRequestCopyWith<_LoginRequest> get copyWith => __$LoginRequestCopyWithImpl<_LoginRequest>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$LoginRequestToJson(this, );
}

@override
bool operator ==(Object other) {
    return identical(this, other) || (other.runtimeType == runtimeType&&other is _LoginRequest&&(identical(other.identifier, identifier) || other.identifier == identifier)&&(identical(other.password, password) || other.password == password));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
    return Object.hash(runtimeType,identifier,password);
}

@override
String toString() {
    return 'LoginRequest(identifier: $identifier, password: $password)';
}


}

/// @nodoc
abstract mixin class _$LoginRequestCopyWith<$Res> implements $LoginRequestCopyWith<$Res> {
  factory _$LoginRequestCopyWith(_LoginRequest value, $Res Function(_LoginRequest) _then) = __$LoginRequestCopyWithImpl;
@override @useResult
$Res call({
 String identifier, String password
});




}
/// @nodoc
class __$LoginRequestCopyWithImpl<$Res>
    implements _$LoginRequestCopyWith<$Res> {
  __$LoginRequestCopyWithImpl(this._self, this._then);

  final _LoginRequest _self;
  final $Res Function(_LoginRequest) _then;

/// Create a copy of LoginRequest
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? identifier = null,Object? password = null,}) {
  return _then(_LoginRequest(
identifier: null == identifier ? _self.identifier : identifier // ignore: cast_nullable_to_non_nullable
as String,password: null == password ? _self.password : password // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}


/// @nodoc
mixin _$ChangePasswordRequest {

 String get password;@JsonKey(includeIfNull: false) String? get currentPassword;
/// Create a copy of ChangePasswordRequest
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ChangePasswordRequestCopyWith<ChangePasswordRequest> get copyWith => _$ChangePasswordRequestCopyWithImpl<ChangePasswordRequest>(this as ChangePasswordRequest, _$identity);

  /// Serializes this ChangePasswordRequest to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  final _this = this as ChangePasswordRequest;
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ChangePasswordRequest&&(identical(other.password, _this.password) || other.password == _this.password)&&(identical(other.currentPassword, _this.currentPassword) || other.currentPassword == _this.currentPassword));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
  final _this = this as ChangePasswordRequest;
  return Object.hash(runtimeType,_this.password,_this.currentPassword);
}

@override
String toString() {
  final _this = this as ChangePasswordRequest;
  return 'ChangePasswordRequest(password: ${_this.password}, currentPassword: ${_this.currentPassword})';
}


}

/// @nodoc
abstract mixin class $ChangePasswordRequestCopyWith<$Res>  {
  factory $ChangePasswordRequestCopyWith(ChangePasswordRequest value, $Res Function(ChangePasswordRequest) _then) = _$ChangePasswordRequestCopyWithImpl;
@useResult
$Res call({
 String password,@JsonKey(includeIfNull: false) String? currentPassword
});




}
/// @nodoc
class _$ChangePasswordRequestCopyWithImpl<$Res>
    implements $ChangePasswordRequestCopyWith<$Res> {
  _$ChangePasswordRequestCopyWithImpl(this._self, this._then);

  final ChangePasswordRequest _self;
  final $Res Function(ChangePasswordRequest) _then;

/// Create a copy of ChangePasswordRequest
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? password = null,Object? currentPassword = freezed,}) {
  return _then(ChangePasswordRequest(
password: null == password ? _self.password : password // ignore: cast_nullable_to_non_nullable
as String,currentPassword: freezed == currentPassword ? _self.currentPassword : currentPassword // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}

}


/// Adds pattern-matching-related methods to [ChangePasswordRequest].
extension ChangePasswordRequestPatterns on ChangePasswordRequest {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ChangePasswordRequest value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ChangePasswordRequest() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ChangePasswordRequest value)  $default,){
final _that = this;
switch (_that) {
case _ChangePasswordRequest():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ChangePasswordRequest value)?  $default,){
final _that = this;
switch (_that) {
case _ChangePasswordRequest() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String password, @JsonKey(includeIfNull: false)  String? currentPassword)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ChangePasswordRequest() when $default != null:
return $default(_that.password,_that.currentPassword);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String password, @JsonKey(includeIfNull: false)  String? currentPassword)  $default,) {final _that = this;
switch (_that) {
case _ChangePasswordRequest():
return $default(_that.password,_that.currentPassword);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String password, @JsonKey(includeIfNull: false)  String? currentPassword)?  $default,) {final _that = this;
switch (_that) {
case _ChangePasswordRequest() when $default != null:
return $default(_that.password,_that.currentPassword);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _ChangePasswordRequest implements ChangePasswordRequest {
  const _ChangePasswordRequest({required this.password, @JsonKey(includeIfNull: false) this.currentPassword});
  factory _ChangePasswordRequest.fromJson(Map<String, dynamic> json) => _$ChangePasswordRequestFromJson(json);

@override final  String password;
@override@JsonKey(includeIfNull: false) final  String? currentPassword;

/// Create a copy of ChangePasswordRequest
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ChangePasswordRequestCopyWith<_ChangePasswordRequest> get copyWith => __$ChangePasswordRequestCopyWithImpl<_ChangePasswordRequest>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$ChangePasswordRequestToJson(this, );
}

@override
bool operator ==(Object other) {
    return identical(this, other) || (other.runtimeType == runtimeType&&other is _ChangePasswordRequest&&(identical(other.password, password) || other.password == password)&&(identical(other.currentPassword, currentPassword) || other.currentPassword == currentPassword));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
    return Object.hash(runtimeType,password,currentPassword);
}

@override
String toString() {
    return 'ChangePasswordRequest(password: $password, currentPassword: $currentPassword)';
}


}

/// @nodoc
abstract mixin class _$ChangePasswordRequestCopyWith<$Res> implements $ChangePasswordRequestCopyWith<$Res> {
  factory _$ChangePasswordRequestCopyWith(_ChangePasswordRequest value, $Res Function(_ChangePasswordRequest) _then) = __$ChangePasswordRequestCopyWithImpl;
@override @useResult
$Res call({
 String password,@JsonKey(includeIfNull: false) String? currentPassword
});




}
/// @nodoc
class __$ChangePasswordRequestCopyWithImpl<$Res>
    implements _$ChangePasswordRequestCopyWith<$Res> {
  __$ChangePasswordRequestCopyWithImpl(this._self, this._then);

  final _ChangePasswordRequest _self;
  final $Res Function(_ChangePasswordRequest) _then;

/// Create a copy of ChangePasswordRequest
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? password = null,Object? currentPassword = freezed,}) {
  return _then(_ChangePasswordRequest(
password: null == password ? _self.password : password // ignore: cast_nullable_to_non_nullable
as String,currentPassword: freezed == currentPassword ? _self.currentPassword : currentPassword // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}


}


/// @nodoc
mixin _$ResetPasswordRequest {

 String get temporaryPassword;
/// Create a copy of ResetPasswordRequest
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ResetPasswordRequestCopyWith<ResetPasswordRequest> get copyWith => _$ResetPasswordRequestCopyWithImpl<ResetPasswordRequest>(this as ResetPasswordRequest, _$identity);

  /// Serializes this ResetPasswordRequest to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  final _this = this as ResetPasswordRequest;
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ResetPasswordRequest&&(identical(other.temporaryPassword, _this.temporaryPassword) || other.temporaryPassword == _this.temporaryPassword));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
  final _this = this as ResetPasswordRequest;
  return Object.hash(runtimeType,_this.temporaryPassword);
}

@override
String toString() {
  final _this = this as ResetPasswordRequest;
  return 'ResetPasswordRequest(temporaryPassword: ${_this.temporaryPassword})';
}


}

/// @nodoc
abstract mixin class $ResetPasswordRequestCopyWith<$Res>  {
  factory $ResetPasswordRequestCopyWith(ResetPasswordRequest value, $Res Function(ResetPasswordRequest) _then) = _$ResetPasswordRequestCopyWithImpl;
@useResult
$Res call({
 String temporaryPassword
});




}
/// @nodoc
class _$ResetPasswordRequestCopyWithImpl<$Res>
    implements $ResetPasswordRequestCopyWith<$Res> {
  _$ResetPasswordRequestCopyWithImpl(this._self, this._then);

  final ResetPasswordRequest _self;
  final $Res Function(ResetPasswordRequest) _then;

/// Create a copy of ResetPasswordRequest
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? temporaryPassword = null,}) {
  return _then(ResetPasswordRequest(
temporaryPassword: null == temporaryPassword ? _self.temporaryPassword : temporaryPassword // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [ResetPasswordRequest].
extension ResetPasswordRequestPatterns on ResetPasswordRequest {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ResetPasswordRequest value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ResetPasswordRequest() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ResetPasswordRequest value)  $default,){
final _that = this;
switch (_that) {
case _ResetPasswordRequest():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ResetPasswordRequest value)?  $default,){
final _that = this;
switch (_that) {
case _ResetPasswordRequest() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String temporaryPassword)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ResetPasswordRequest() when $default != null:
return $default(_that.temporaryPassword);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String temporaryPassword)  $default,) {final _that = this;
switch (_that) {
case _ResetPasswordRequest():
return $default(_that.temporaryPassword);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String temporaryPassword)?  $default,) {final _that = this;
switch (_that) {
case _ResetPasswordRequest() when $default != null:
return $default(_that.temporaryPassword);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _ResetPasswordRequest implements ResetPasswordRequest {
  const _ResetPasswordRequest({required this.temporaryPassword});
  factory _ResetPasswordRequest.fromJson(Map<String, dynamic> json) => _$ResetPasswordRequestFromJson(json);

@override final  String temporaryPassword;

/// Create a copy of ResetPasswordRequest
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ResetPasswordRequestCopyWith<_ResetPasswordRequest> get copyWith => __$ResetPasswordRequestCopyWithImpl<_ResetPasswordRequest>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$ResetPasswordRequestToJson(this, );
}

@override
bool operator ==(Object other) {
    return identical(this, other) || (other.runtimeType == runtimeType&&other is _ResetPasswordRequest&&(identical(other.temporaryPassword, temporaryPassword) || other.temporaryPassword == temporaryPassword));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
    return Object.hash(runtimeType,temporaryPassword);
}

@override
String toString() {
    return 'ResetPasswordRequest(temporaryPassword: $temporaryPassword)';
}


}

/// @nodoc
abstract mixin class _$ResetPasswordRequestCopyWith<$Res> implements $ResetPasswordRequestCopyWith<$Res> {
  factory _$ResetPasswordRequestCopyWith(_ResetPasswordRequest value, $Res Function(_ResetPasswordRequest) _then) = __$ResetPasswordRequestCopyWithImpl;
@override @useResult
$Res call({
 String temporaryPassword
});




}
/// @nodoc
class __$ResetPasswordRequestCopyWithImpl<$Res>
    implements _$ResetPasswordRequestCopyWith<$Res> {
  __$ResetPasswordRequestCopyWithImpl(this._self, this._then);

  final _ResetPasswordRequest _self;
  final $Res Function(_ResetPasswordRequest) _then;

/// Create a copy of ResetPasswordRequest
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? temporaryPassword = null,}) {
  return _then(_ResetPasswordRequest(
temporaryPassword: null == temporaryPassword ? _self.temporaryPassword : temporaryPassword // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}


/// @nodoc
mixin _$CreateTenantRequest {

 String get name; String get slug;
/// Create a copy of CreateTenantRequest
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CreateTenantRequestCopyWith<CreateTenantRequest> get copyWith => _$CreateTenantRequestCopyWithImpl<CreateTenantRequest>(this as CreateTenantRequest, _$identity);

  /// Serializes this CreateTenantRequest to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  final _this = this as CreateTenantRequest;
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CreateTenantRequest&&(identical(other.name, _this.name) || other.name == _this.name)&&(identical(other.slug, _this.slug) || other.slug == _this.slug));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
  final _this = this as CreateTenantRequest;
  return Object.hash(runtimeType,_this.name,_this.slug);
}

@override
String toString() {
  final _this = this as CreateTenantRequest;
  return 'CreateTenantRequest(name: ${_this.name}, slug: ${_this.slug})';
}


}

/// @nodoc
abstract mixin class $CreateTenantRequestCopyWith<$Res>  {
  factory $CreateTenantRequestCopyWith(CreateTenantRequest value, $Res Function(CreateTenantRequest) _then) = _$CreateTenantRequestCopyWithImpl;
@useResult
$Res call({
 String name, String slug
});




}
/// @nodoc
class _$CreateTenantRequestCopyWithImpl<$Res>
    implements $CreateTenantRequestCopyWith<$Res> {
  _$CreateTenantRequestCopyWithImpl(this._self, this._then);

  final CreateTenantRequest _self;
  final $Res Function(CreateTenantRequest) _then;

/// Create a copy of CreateTenantRequest
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? name = null,Object? slug = null,}) {
  return _then(CreateTenantRequest(
name: null == name ? _self.name : name // ignore: cast_nullable_to_non_nullable
as String,slug: null == slug ? _self.slug : slug // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [CreateTenantRequest].
extension CreateTenantRequestPatterns on CreateTenantRequest {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CreateTenantRequest value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CreateTenantRequest() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CreateTenantRequest value)  $default,){
final _that = this;
switch (_that) {
case _CreateTenantRequest():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CreateTenantRequest value)?  $default,){
final _that = this;
switch (_that) {
case _CreateTenantRequest() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String name,  String slug)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CreateTenantRequest() when $default != null:
return $default(_that.name,_that.slug);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String name,  String slug)  $default,) {final _that = this;
switch (_that) {
case _CreateTenantRequest():
return $default(_that.name,_that.slug);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String name,  String slug)?  $default,) {final _that = this;
switch (_that) {
case _CreateTenantRequest() when $default != null:
return $default(_that.name,_that.slug);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _CreateTenantRequest implements CreateTenantRequest {
  const _CreateTenantRequest({required this.name, required this.slug});
  factory _CreateTenantRequest.fromJson(Map<String, dynamic> json) => _$CreateTenantRequestFromJson(json);

@override final  String name;
@override final  String slug;

/// Create a copy of CreateTenantRequest
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CreateTenantRequestCopyWith<_CreateTenantRequest> get copyWith => __$CreateTenantRequestCopyWithImpl<_CreateTenantRequest>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$CreateTenantRequestToJson(this, );
}

@override
bool operator ==(Object other) {
    return identical(this, other) || (other.runtimeType == runtimeType&&other is _CreateTenantRequest&&(identical(other.name, name) || other.name == name)&&(identical(other.slug, slug) || other.slug == slug));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
    return Object.hash(runtimeType,name,slug);
}

@override
String toString() {
    return 'CreateTenantRequest(name: $name, slug: $slug)';
}


}

/// @nodoc
abstract mixin class _$CreateTenantRequestCopyWith<$Res> implements $CreateTenantRequestCopyWith<$Res> {
  factory _$CreateTenantRequestCopyWith(_CreateTenantRequest value, $Res Function(_CreateTenantRequest) _then) = __$CreateTenantRequestCopyWithImpl;
@override @useResult
$Res call({
 String name, String slug
});




}
/// @nodoc
class __$CreateTenantRequestCopyWithImpl<$Res>
    implements _$CreateTenantRequestCopyWith<$Res> {
  __$CreateTenantRequestCopyWithImpl(this._self, this._then);

  final _CreateTenantRequest _self;
  final $Res Function(_CreateTenantRequest) _then;

/// Create a copy of CreateTenantRequest
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? name = null,Object? slug = null,}) {
  return _then(_CreateTenantRequest(
name: null == name ? _self.name : name // ignore: cast_nullable_to_non_nullable
as String,slug: null == slug ? _self.slug : slug // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}


/// @nodoc
mixin _$UpdateTenantRequest {

 String get name; String get slug;
/// Create a copy of UpdateTenantRequest
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$UpdateTenantRequestCopyWith<UpdateTenantRequest> get copyWith => _$UpdateTenantRequestCopyWithImpl<UpdateTenantRequest>(this as UpdateTenantRequest, _$identity);

  /// Serializes this UpdateTenantRequest to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  final _this = this as UpdateTenantRequest;
  return identical(this, other) || (other.runtimeType == runtimeType&&other is UpdateTenantRequest&&(identical(other.name, _this.name) || other.name == _this.name)&&(identical(other.slug, _this.slug) || other.slug == _this.slug));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
  final _this = this as UpdateTenantRequest;
  return Object.hash(runtimeType,_this.name,_this.slug);
}

@override
String toString() {
  final _this = this as UpdateTenantRequest;
  return 'UpdateTenantRequest(name: ${_this.name}, slug: ${_this.slug})';
}


}

/// @nodoc
abstract mixin class $UpdateTenantRequestCopyWith<$Res>  {
  factory $UpdateTenantRequestCopyWith(UpdateTenantRequest value, $Res Function(UpdateTenantRequest) _then) = _$UpdateTenantRequestCopyWithImpl;
@useResult
$Res call({
 String name, String slug
});




}
/// @nodoc
class _$UpdateTenantRequestCopyWithImpl<$Res>
    implements $UpdateTenantRequestCopyWith<$Res> {
  _$UpdateTenantRequestCopyWithImpl(this._self, this._then);

  final UpdateTenantRequest _self;
  final $Res Function(UpdateTenantRequest) _then;

/// Create a copy of UpdateTenantRequest
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? name = null,Object? slug = null,}) {
  return _then(UpdateTenantRequest(
name: null == name ? _self.name : name // ignore: cast_nullable_to_non_nullable
as String,slug: null == slug ? _self.slug : slug // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [UpdateTenantRequest].
extension UpdateTenantRequestPatterns on UpdateTenantRequest {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _UpdateTenantRequest value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _UpdateTenantRequest() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _UpdateTenantRequest value)  $default,){
final _that = this;
switch (_that) {
case _UpdateTenantRequest():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _UpdateTenantRequest value)?  $default,){
final _that = this;
switch (_that) {
case _UpdateTenantRequest() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String name,  String slug)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _UpdateTenantRequest() when $default != null:
return $default(_that.name,_that.slug);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String name,  String slug)  $default,) {final _that = this;
switch (_that) {
case _UpdateTenantRequest():
return $default(_that.name,_that.slug);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String name,  String slug)?  $default,) {final _that = this;
switch (_that) {
case _UpdateTenantRequest() when $default != null:
return $default(_that.name,_that.slug);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _UpdateTenantRequest implements UpdateTenantRequest {
  const _UpdateTenantRequest({required this.name, required this.slug});
  factory _UpdateTenantRequest.fromJson(Map<String, dynamic> json) => _$UpdateTenantRequestFromJson(json);

@override final  String name;
@override final  String slug;

/// Create a copy of UpdateTenantRequest
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$UpdateTenantRequestCopyWith<_UpdateTenantRequest> get copyWith => __$UpdateTenantRequestCopyWithImpl<_UpdateTenantRequest>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$UpdateTenantRequestToJson(this, );
}

@override
bool operator ==(Object other) {
    return identical(this, other) || (other.runtimeType == runtimeType&&other is _UpdateTenantRequest&&(identical(other.name, name) || other.name == name)&&(identical(other.slug, slug) || other.slug == slug));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
    return Object.hash(runtimeType,name,slug);
}

@override
String toString() {
    return 'UpdateTenantRequest(name: $name, slug: $slug)';
}


}

/// @nodoc
abstract mixin class _$UpdateTenantRequestCopyWith<$Res> implements $UpdateTenantRequestCopyWith<$Res> {
  factory _$UpdateTenantRequestCopyWith(_UpdateTenantRequest value, $Res Function(_UpdateTenantRequest) _then) = __$UpdateTenantRequestCopyWithImpl;
@override @useResult
$Res call({
 String name, String slug
});




}
/// @nodoc
class __$UpdateTenantRequestCopyWithImpl<$Res>
    implements _$UpdateTenantRequestCopyWith<$Res> {
  __$UpdateTenantRequestCopyWithImpl(this._self, this._then);

  final _UpdateTenantRequest _self;
  final $Res Function(_UpdateTenantRequest) _then;

/// Create a copy of UpdateTenantRequest
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? name = null,Object? slug = null,}) {
  return _then(_UpdateTenantRequest(
name: null == name ? _self.name : name // ignore: cast_nullable_to_non_nullable
as String,slug: null == slug ? _self.slug : slug // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}


/// @nodoc
mixin _$UpdateUserRequest {

 String get username; List<String> get roles;
/// Create a copy of UpdateUserRequest
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$UpdateUserRequestCopyWith<UpdateUserRequest> get copyWith => _$UpdateUserRequestCopyWithImpl<UpdateUserRequest>(this as UpdateUserRequest, _$identity);

  /// Serializes this UpdateUserRequest to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  final _this = this as UpdateUserRequest;
  return identical(this, other) || (other.runtimeType == runtimeType&&other is UpdateUserRequest&&(identical(other.username, _this.username) || other.username == _this.username)&&const DeepCollectionEquality().equals(other.roles, _this.roles));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
  final _this = this as UpdateUserRequest;
  return Object.hash(runtimeType,_this.username,const DeepCollectionEquality().hash(_this.roles));
}

@override
String toString() {
  final _this = this as UpdateUserRequest;
  return 'UpdateUserRequest(username: ${_this.username}, roles: ${_this.roles})';
}


}

/// @nodoc
abstract mixin class $UpdateUserRequestCopyWith<$Res>  {
  factory $UpdateUserRequestCopyWith(UpdateUserRequest value, $Res Function(UpdateUserRequest) _then) = _$UpdateUserRequestCopyWithImpl;
@useResult
$Res call({
 String username, List<String> roles
});




}
/// @nodoc
class _$UpdateUserRequestCopyWithImpl<$Res>
    implements $UpdateUserRequestCopyWith<$Res> {
  _$UpdateUserRequestCopyWithImpl(this._self, this._then);

  final UpdateUserRequest _self;
  final $Res Function(UpdateUserRequest) _then;

/// Create a copy of UpdateUserRequest
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? username = null,Object? roles = null,}) {
  return _then(UpdateUserRequest(
username: null == username ? _self.username : username // ignore: cast_nullable_to_non_nullable
as String,roles: null == roles ? _self.roles : roles // ignore: cast_nullable_to_non_nullable
as List<String>,
  ));
}

}


/// Adds pattern-matching-related methods to [UpdateUserRequest].
extension UpdateUserRequestPatterns on UpdateUserRequest {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _UpdateUserRequest value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _UpdateUserRequest() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _UpdateUserRequest value)  $default,){
final _that = this;
switch (_that) {
case _UpdateUserRequest():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _UpdateUserRequest value)?  $default,){
final _that = this;
switch (_that) {
case _UpdateUserRequest() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String username,  List<String> roles)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _UpdateUserRequest() when $default != null:
return $default(_that.username,_that.roles);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String username,  List<String> roles)  $default,) {final _that = this;
switch (_that) {
case _UpdateUserRequest():
return $default(_that.username,_that.roles);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String username,  List<String> roles)?  $default,) {final _that = this;
switch (_that) {
case _UpdateUserRequest() when $default != null:
return $default(_that.username,_that.roles);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _UpdateUserRequest implements UpdateUserRequest {
  const _UpdateUserRequest({required this.username,  List<String> roles = const <String>[]}): _roles = roles;
  factory _UpdateUserRequest.fromJson(Map<String, dynamic> json) => _$UpdateUserRequestFromJson(json);

@override final  String username;
 final  List<String> _roles;
@override@JsonKey() List<String> get roles {
  if (_roles is EqualUnmodifiableListView) return _roles;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_roles);
}


/// Create a copy of UpdateUserRequest
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$UpdateUserRequestCopyWith<_UpdateUserRequest> get copyWith => __$UpdateUserRequestCopyWithImpl<_UpdateUserRequest>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$UpdateUserRequestToJson(this, );
}

@override
bool operator ==(Object other) {
    return identical(this, other) || (other.runtimeType == runtimeType&&other is _UpdateUserRequest&&(identical(other.username, username) || other.username == username)&&const DeepCollectionEquality().equals(other.roles, _roles));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
    return Object.hash(runtimeType,username,const DeepCollectionEquality().hash(_roles));
}

@override
String toString() {
    return 'UpdateUserRequest(username: $username, roles: $roles)';
}


}

/// @nodoc
abstract mixin class _$UpdateUserRequestCopyWith<$Res> implements $UpdateUserRequestCopyWith<$Res> {
  factory _$UpdateUserRequestCopyWith(_UpdateUserRequest value, $Res Function(_UpdateUserRequest) _then) = __$UpdateUserRequestCopyWithImpl;
@override @useResult
$Res call({
 String username, List<String> roles
});




}
/// @nodoc
class __$UpdateUserRequestCopyWithImpl<$Res>
    implements _$UpdateUserRequestCopyWith<$Res> {
  __$UpdateUserRequestCopyWithImpl(this._self, this._then);

  final _UpdateUserRequest _self;
  final $Res Function(_UpdateUserRequest) _then;

/// Create a copy of UpdateUserRequest
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? username = null,Object? roles = null,}) {
  return _then(_UpdateUserRequest(
username: null == username ? _self.username : username // ignore: cast_nullable_to_non_nullable
as String,roles: null == roles ? _self._roles : roles // ignore: cast_nullable_to_non_nullable
as List<String>,
  ));
}


}


/// @nodoc
mixin _$CreateRoleRequest {

 String get code; String get scope; List<String> get permissions;
/// Create a copy of CreateRoleRequest
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CreateRoleRequestCopyWith<CreateRoleRequest> get copyWith => _$CreateRoleRequestCopyWithImpl<CreateRoleRequest>(this as CreateRoleRequest, _$identity);

  /// Serializes this CreateRoleRequest to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  final _this = this as CreateRoleRequest;
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CreateRoleRequest&&(identical(other.code, _this.code) || other.code == _this.code)&&(identical(other.scope, _this.scope) || other.scope == _this.scope)&&const DeepCollectionEquality().equals(other.permissions, _this.permissions));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
  final _this = this as CreateRoleRequest;
  return Object.hash(runtimeType,_this.code,_this.scope,const DeepCollectionEquality().hash(_this.permissions));
}

@override
String toString() {
  final _this = this as CreateRoleRequest;
  return 'CreateRoleRequest(code: ${_this.code}, scope: ${_this.scope}, permissions: ${_this.permissions})';
}


}

/// @nodoc
abstract mixin class $CreateRoleRequestCopyWith<$Res>  {
  factory $CreateRoleRequestCopyWith(CreateRoleRequest value, $Res Function(CreateRoleRequest) _then) = _$CreateRoleRequestCopyWithImpl;
@useResult
$Res call({
 String code, String scope, List<String> permissions
});




}
/// @nodoc
class _$CreateRoleRequestCopyWithImpl<$Res>
    implements $CreateRoleRequestCopyWith<$Res> {
  _$CreateRoleRequestCopyWithImpl(this._self, this._then);

  final CreateRoleRequest _self;
  final $Res Function(CreateRoleRequest) _then;

/// Create a copy of CreateRoleRequest
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? code = null,Object? scope = null,Object? permissions = null,}) {
  return _then(CreateRoleRequest(
code: null == code ? _self.code : code // ignore: cast_nullable_to_non_nullable
as String,scope: null == scope ? _self.scope : scope // ignore: cast_nullable_to_non_nullable
as String,permissions: null == permissions ? _self.permissions : permissions // ignore: cast_nullable_to_non_nullable
as List<String>,
  ));
}

}


/// Adds pattern-matching-related methods to [CreateRoleRequest].
extension CreateRoleRequestPatterns on CreateRoleRequest {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CreateRoleRequest value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CreateRoleRequest() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CreateRoleRequest value)  $default,){
final _that = this;
switch (_that) {
case _CreateRoleRequest():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CreateRoleRequest value)?  $default,){
final _that = this;
switch (_that) {
case _CreateRoleRequest() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String code,  String scope,  List<String> permissions)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CreateRoleRequest() when $default != null:
return $default(_that.code,_that.scope,_that.permissions);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String code,  String scope,  List<String> permissions)  $default,) {final _that = this;
switch (_that) {
case _CreateRoleRequest():
return $default(_that.code,_that.scope,_that.permissions);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String code,  String scope,  List<String> permissions)?  $default,) {final _that = this;
switch (_that) {
case _CreateRoleRequest() when $default != null:
return $default(_that.code,_that.scope,_that.permissions);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _CreateRoleRequest implements CreateRoleRequest {
  const _CreateRoleRequest({required this.code, required this.scope,  List<String> permissions = const <String>[]}): _permissions = permissions;
  factory _CreateRoleRequest.fromJson(Map<String, dynamic> json) => _$CreateRoleRequestFromJson(json);

@override final  String code;
@override final  String scope;
 final  List<String> _permissions;
@override@JsonKey() List<String> get permissions {
  if (_permissions is EqualUnmodifiableListView) return _permissions;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_permissions);
}


/// Create a copy of CreateRoleRequest
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CreateRoleRequestCopyWith<_CreateRoleRequest> get copyWith => __$CreateRoleRequestCopyWithImpl<_CreateRoleRequest>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$CreateRoleRequestToJson(this, );
}

@override
bool operator ==(Object other) {
    return identical(this, other) || (other.runtimeType == runtimeType&&other is _CreateRoleRequest&&(identical(other.code, code) || other.code == code)&&(identical(other.scope, scope) || other.scope == scope)&&const DeepCollectionEquality().equals(other.permissions, _permissions));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
    return Object.hash(runtimeType,code,scope,const DeepCollectionEquality().hash(_permissions));
}

@override
String toString() {
    return 'CreateRoleRequest(code: $code, scope: $scope, permissions: $permissions)';
}


}

/// @nodoc
abstract mixin class _$CreateRoleRequestCopyWith<$Res> implements $CreateRoleRequestCopyWith<$Res> {
  factory _$CreateRoleRequestCopyWith(_CreateRoleRequest value, $Res Function(_CreateRoleRequest) _then) = __$CreateRoleRequestCopyWithImpl;
@override @useResult
$Res call({
 String code, String scope, List<String> permissions
});




}
/// @nodoc
class __$CreateRoleRequestCopyWithImpl<$Res>
    implements _$CreateRoleRequestCopyWith<$Res> {
  __$CreateRoleRequestCopyWithImpl(this._self, this._then);

  final _CreateRoleRequest _self;
  final $Res Function(_CreateRoleRequest) _then;

/// Create a copy of CreateRoleRequest
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? code = null,Object? scope = null,Object? permissions = null,}) {
  return _then(_CreateRoleRequest(
code: null == code ? _self.code : code // ignore: cast_nullable_to_non_nullable
as String,scope: null == scope ? _self.scope : scope // ignore: cast_nullable_to_non_nullable
as String,permissions: null == permissions ? _self._permissions : permissions // ignore: cast_nullable_to_non_nullable
as List<String>,
  ));
}


}


/// @nodoc
mixin _$CreatePermissionRequest {

 String get code; String get scope;
/// Create a copy of CreatePermissionRequest
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CreatePermissionRequestCopyWith<CreatePermissionRequest> get copyWith => _$CreatePermissionRequestCopyWithImpl<CreatePermissionRequest>(this as CreatePermissionRequest, _$identity);

  /// Serializes this CreatePermissionRequest to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  final _this = this as CreatePermissionRequest;
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CreatePermissionRequest&&(identical(other.code, _this.code) || other.code == _this.code)&&(identical(other.scope, _this.scope) || other.scope == _this.scope));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
  final _this = this as CreatePermissionRequest;
  return Object.hash(runtimeType,_this.code,_this.scope);
}

@override
String toString() {
  final _this = this as CreatePermissionRequest;
  return 'CreatePermissionRequest(code: ${_this.code}, scope: ${_this.scope})';
}


}

/// @nodoc
abstract mixin class $CreatePermissionRequestCopyWith<$Res>  {
  factory $CreatePermissionRequestCopyWith(CreatePermissionRequest value, $Res Function(CreatePermissionRequest) _then) = _$CreatePermissionRequestCopyWithImpl;
@useResult
$Res call({
 String code, String scope
});




}
/// @nodoc
class _$CreatePermissionRequestCopyWithImpl<$Res>
    implements $CreatePermissionRequestCopyWith<$Res> {
  _$CreatePermissionRequestCopyWithImpl(this._self, this._then);

  final CreatePermissionRequest _self;
  final $Res Function(CreatePermissionRequest) _then;

/// Create a copy of CreatePermissionRequest
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? code = null,Object? scope = null,}) {
  return _then(CreatePermissionRequest(
code: null == code ? _self.code : code // ignore: cast_nullable_to_non_nullable
as String,scope: null == scope ? _self.scope : scope // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [CreatePermissionRequest].
extension CreatePermissionRequestPatterns on CreatePermissionRequest {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CreatePermissionRequest value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CreatePermissionRequest() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CreatePermissionRequest value)  $default,){
final _that = this;
switch (_that) {
case _CreatePermissionRequest():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CreatePermissionRequest value)?  $default,){
final _that = this;
switch (_that) {
case _CreatePermissionRequest() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String code,  String scope)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CreatePermissionRequest() when $default != null:
return $default(_that.code,_that.scope);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String code,  String scope)  $default,) {final _that = this;
switch (_that) {
case _CreatePermissionRequest():
return $default(_that.code,_that.scope);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String code,  String scope)?  $default,) {final _that = this;
switch (_that) {
case _CreatePermissionRequest() when $default != null:
return $default(_that.code,_that.scope);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _CreatePermissionRequest implements CreatePermissionRequest {
  const _CreatePermissionRequest({required this.code, required this.scope});
  factory _CreatePermissionRequest.fromJson(Map<String, dynamic> json) => _$CreatePermissionRequestFromJson(json);

@override final  String code;
@override final  String scope;

/// Create a copy of CreatePermissionRequest
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CreatePermissionRequestCopyWith<_CreatePermissionRequest> get copyWith => __$CreatePermissionRequestCopyWithImpl<_CreatePermissionRequest>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$CreatePermissionRequestToJson(this, );
}

@override
bool operator ==(Object other) {
    return identical(this, other) || (other.runtimeType == runtimeType&&other is _CreatePermissionRequest&&(identical(other.code, code) || other.code == code)&&(identical(other.scope, scope) || other.scope == scope));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
    return Object.hash(runtimeType,code,scope);
}

@override
String toString() {
    return 'CreatePermissionRequest(code: $code, scope: $scope)';
}


}

/// @nodoc
abstract mixin class _$CreatePermissionRequestCopyWith<$Res> implements $CreatePermissionRequestCopyWith<$Res> {
  factory _$CreatePermissionRequestCopyWith(_CreatePermissionRequest value, $Res Function(_CreatePermissionRequest) _then) = __$CreatePermissionRequestCopyWithImpl;
@override @useResult
$Res call({
 String code, String scope
});




}
/// @nodoc
class __$CreatePermissionRequestCopyWithImpl<$Res>
    implements _$CreatePermissionRequestCopyWith<$Res> {
  __$CreatePermissionRequestCopyWithImpl(this._self, this._then);

  final _CreatePermissionRequest _self;
  final $Res Function(_CreatePermissionRequest) _then;

/// Create a copy of CreatePermissionRequest
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? code = null,Object? scope = null,}) {
  return _then(_CreatePermissionRequest(
code: null == code ? _self.code : code // ignore: cast_nullable_to_non_nullable
as String,scope: null == scope ? _self.scope : scope // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}


/// @nodoc
mixin _$CreateUserRequest {

 String get username; String? get tenantId; String? get email; String get temporaryPassword; List<String> get roles;
/// Create a copy of CreateUserRequest
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CreateUserRequestCopyWith<CreateUserRequest> get copyWith => _$CreateUserRequestCopyWithImpl<CreateUserRequest>(this as CreateUserRequest, _$identity);

  /// Serializes this CreateUserRequest to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  final _this = this as CreateUserRequest;
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CreateUserRequest&&(identical(other.username, _this.username) || other.username == _this.username)&&(identical(other.tenantId, _this.tenantId) || other.tenantId == _this.tenantId)&&(identical(other.email, _this.email) || other.email == _this.email)&&(identical(other.temporaryPassword, _this.temporaryPassword) || other.temporaryPassword == _this.temporaryPassword)&&const DeepCollectionEquality().equals(other.roles, _this.roles));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
  final _this = this as CreateUserRequest;
  return Object.hash(runtimeType,_this.username,_this.tenantId,_this.email,_this.temporaryPassword,const DeepCollectionEquality().hash(_this.roles));
}

@override
String toString() {
  final _this = this as CreateUserRequest;
  return 'CreateUserRequest(username: ${_this.username}, tenantId: ${_this.tenantId}, email: ${_this.email}, temporaryPassword: ${_this.temporaryPassword}, roles: ${_this.roles})';
}


}

/// @nodoc
abstract mixin class $CreateUserRequestCopyWith<$Res>  {
  factory $CreateUserRequestCopyWith(CreateUserRequest value, $Res Function(CreateUserRequest) _then) = _$CreateUserRequestCopyWithImpl;
@useResult
$Res call({
 String username, String? tenantId, String? email, String temporaryPassword, List<String> roles
});




}
/// @nodoc
class _$CreateUserRequestCopyWithImpl<$Res>
    implements $CreateUserRequestCopyWith<$Res> {
  _$CreateUserRequestCopyWithImpl(this._self, this._then);

  final CreateUserRequest _self;
  final $Res Function(CreateUserRequest) _then;

/// Create a copy of CreateUserRequest
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? username = null,Object? tenantId = freezed,Object? email = freezed,Object? temporaryPassword = null,Object? roles = null,}) {
  return _then(CreateUserRequest(
username: null == username ? _self.username : username // ignore: cast_nullable_to_non_nullable
as String,tenantId: freezed == tenantId ? _self.tenantId : tenantId // ignore: cast_nullable_to_non_nullable
as String?,email: freezed == email ? _self.email : email // ignore: cast_nullable_to_non_nullable
as String?,temporaryPassword: null == temporaryPassword ? _self.temporaryPassword : temporaryPassword // ignore: cast_nullable_to_non_nullable
as String,roles: null == roles ? _self.roles : roles // ignore: cast_nullable_to_non_nullable
as List<String>,
  ));
}

}


/// Adds pattern-matching-related methods to [CreateUserRequest].
extension CreateUserRequestPatterns on CreateUserRequest {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CreateUserRequest value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CreateUserRequest() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CreateUserRequest value)  $default,){
final _that = this;
switch (_that) {
case _CreateUserRequest():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CreateUserRequest value)?  $default,){
final _that = this;
switch (_that) {
case _CreateUserRequest() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String username,  String? tenantId,  String? email,  String temporaryPassword,  List<String> roles)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CreateUserRequest() when $default != null:
return $default(_that.username,_that.tenantId,_that.email,_that.temporaryPassword,_that.roles);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String username,  String? tenantId,  String? email,  String temporaryPassword,  List<String> roles)  $default,) {final _that = this;
switch (_that) {
case _CreateUserRequest():
return $default(_that.username,_that.tenantId,_that.email,_that.temporaryPassword,_that.roles);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String username,  String? tenantId,  String? email,  String temporaryPassword,  List<String> roles)?  $default,) {final _that = this;
switch (_that) {
case _CreateUserRequest() when $default != null:
return $default(_that.username,_that.tenantId,_that.email,_that.temporaryPassword,_that.roles);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _CreateUserRequest implements CreateUserRequest {
  const _CreateUserRequest({required this.username, this.tenantId, this.email, required this.temporaryPassword,  List<String> roles = const <String>[]}): _roles = roles;
  factory _CreateUserRequest.fromJson(Map<String, dynamic> json) => _$CreateUserRequestFromJson(json);

@override final  String username;
@override final  String? tenantId;
@override final  String? email;
@override final  String temporaryPassword;
 final  List<String> _roles;
@override@JsonKey() List<String> get roles {
  if (_roles is EqualUnmodifiableListView) return _roles;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_roles);
}


/// Create a copy of CreateUserRequest
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CreateUserRequestCopyWith<_CreateUserRequest> get copyWith => __$CreateUserRequestCopyWithImpl<_CreateUserRequest>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$CreateUserRequestToJson(this, );
}

@override
bool operator ==(Object other) {
    return identical(this, other) || (other.runtimeType == runtimeType&&other is _CreateUserRequest&&(identical(other.username, username) || other.username == username)&&(identical(other.tenantId, tenantId) || other.tenantId == tenantId)&&(identical(other.email, email) || other.email == email)&&(identical(other.temporaryPassword, temporaryPassword) || other.temporaryPassword == temporaryPassword)&&const DeepCollectionEquality().equals(other.roles, _roles));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode {
    return Object.hash(runtimeType,username,tenantId,email,temporaryPassword,const DeepCollectionEquality().hash(_roles));
}

@override
String toString() {
    return 'CreateUserRequest(username: $username, tenantId: $tenantId, email: $email, temporaryPassword: $temporaryPassword, roles: $roles)';
}


}

/// @nodoc
abstract mixin class _$CreateUserRequestCopyWith<$Res> implements $CreateUserRequestCopyWith<$Res> {
  factory _$CreateUserRequestCopyWith(_CreateUserRequest value, $Res Function(_CreateUserRequest) _then) = __$CreateUserRequestCopyWithImpl;
@override @useResult
$Res call({
 String username, String? tenantId, String? email, String temporaryPassword, List<String> roles
});




}
/// @nodoc
class __$CreateUserRequestCopyWithImpl<$Res>
    implements _$CreateUserRequestCopyWith<$Res> {
  __$CreateUserRequestCopyWithImpl(this._self, this._then);

  final _CreateUserRequest _self;
  final $Res Function(_CreateUserRequest) _then;

/// Create a copy of CreateUserRequest
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? username = null,Object? tenantId = freezed,Object? email = freezed,Object? temporaryPassword = null,Object? roles = null,}) {
  return _then(_CreateUserRequest(
username: null == username ? _self.username : username // ignore: cast_nullable_to_non_nullable
as String,tenantId: freezed == tenantId ? _self.tenantId : tenantId // ignore: cast_nullable_to_non_nullable
as String?,email: freezed == email ? _self.email : email // ignore: cast_nullable_to_non_nullable
as String?,temporaryPassword: null == temporaryPassword ? _self.temporaryPassword : temporaryPassword // ignore: cast_nullable_to_non_nullable
as String,roles: null == roles ? _self._roles : roles // ignore: cast_nullable_to_non_nullable
as List<String>,
  ));
}


}

// dart format on
