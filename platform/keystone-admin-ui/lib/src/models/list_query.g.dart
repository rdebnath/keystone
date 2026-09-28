// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'list_query.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_ListQuery _$ListQueryFromJson(Map<String, dynamic> json) => _ListQuery(
  tenantId: json['tenantId'] as String?,
  search: json['search'] as String? ?? '',
  page: (json['page'] as num?)?.toInt() ?? 0,
  size: (json['size'] as num?)?.toInt() ?? 25,
  sort: json['sort'] as String?,
  order: json['order'] as String? ?? 'asc',
  scope: json['scope'] as String?,
  access: json['access'] as String?,
);

Map<String, dynamic> _$ListQueryToJson(_ListQuery instance) =>
    <String, dynamic>{
      'tenantId': instance.tenantId,
      'search': instance.search,
      'page': instance.page,
      'size': instance.size,
      'sort': instance.sort,
      'order': instance.order,
      'scope': instance.scope,
      'access': instance.access,
    };
