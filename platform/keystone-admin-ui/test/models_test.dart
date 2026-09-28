import 'package:flutter_test/flutter_test.dart';
import 'package:keystone_admin_ui/keystone_admin_ui.dart';

void main() {
  group('Me', () {
    test('should flag platform admin when tenantId is null', () {
      final me = Me.fromJson({
        'sub': 's',
        'tenantId': null,
        'mustChangePassword': true,
        'permissions': ['*'],
      });
      expect(me.isPlatformAdmin, isTrue);
      expect(me.permissions, ['*']);
    });

    test('should flag tenant user when tenantId is set', () {
      final me = Me.fromJson({
        'sub': 's',
        'tenantId': 't',
        'mustChangePassword': false,
        'permissions': const [],
      });
      expect(me.isPlatformAdmin, isFalse);
      expect(me.tenantId, 't');
    });
  });

  group('Me permissions', () {
    Me meWith(List<String> permissions) =>
        Me.fromJson({'sub': 's', 'permissions': permissions});

    test('should_let_the_wildcard_allow_everything', () {
      final me = meWith([Permission.wildcard]);

      expect(me.allows('platform:tenant:read-only'), isTrue);
      expect(me.allowsResource('platform:user'), isTrue);
      expect(me.canWrite('platform:role'), isTrue);
    });

    test('should_treat_read_write_as_a_read_grant', () {
      final me = meWith(['platform:tenant:read-write']);

      expect(me.allowsResource('platform:tenant'), isTrue);
      expect(me.canWrite('platform:tenant'), isTrue);
    });

    test('should_not_treat_read_only_as_a_write_grant', () {
      final me = meWith(['platform:tenant:read-only']);

      expect(me.allowsResource('platform:tenant'), isTrue);
      expect(me.canWrite('platform:tenant'), isFalse);
    });

    test('should_not_let_one_resource_grant_another', () {
      final me = meWith(['platform:tenant:read-write']);

      expect(me.allowsResource('platform:user'), isFalse);
      expect(me.allowsResource('tenant:user'), isFalse);
    });

    test('should_let_the_platform_plane_grant_anything', () {
      // `CallerScope.requireGrantable` exempts the platform plane, so the picker must not disable anything
      // for it — including the wildcard row in the global catalogue.
      final me = Me(sub: 's', permissions: const ['platform:role:read-write']);

      expect(me.canGrant('tenant:user:read-write'), isTrue);
      expect(me.canGrant(Permission.wildcard), isTrue);
    });

    test('should_grant_a_code_the_tenant_caller_holds', () {
      final me = Me(
        sub: 's',
        tenantId: 't1',
        permissions: const ['tenant:role:read-write'],
      );

      expect(me.canGrant('tenant:role:read-write'), isTrue);
      expect(me.canGrant('tenant:user:read-write'), isFalse);
    });

    test('should_treat_a_write_grant_as_covering_the_read_only_code', () {
      // The guard's own implication: `CallerScope.holds` accepts the read-only sibling of a write grant.
      final me = Me(
        sub: 's',
        tenantId: 't1',
        permissions: const ['tenant:user:read-write'],
      );

      expect(me.canGrant('tenant:user:read-only'), isTrue);
      // …and never the other way round.
      final readOnly = Me(
        sub: 's',
        tenantId: 't1',
        permissions: const ['tenant:user:read-only'],
      );
      expect(readOnly.canGrant('tenant:user:read-write'), isFalse);
    });

    test('should_never_grant_the_wildcard_nor_a_code_with_no_level', () {
      // A tenant caller may not hand out `*`, and a code without an access level is not a held code either.
      final me = Me(
        sub: 's',
        tenantId: 't1',
        permissions: const ['*'],
      );

      expect(me.canGrant(Permission.wildcard), isFalse);
      expect(me.canGrant('tenant:user:read-write'), isFalse);
      expect(me.canGrant('tenant:user'), isFalse);
    });

    test('should_default_permissions_and_username_when_absent', () {
      final me = Me.fromJson({'sub': 's'});

      expect(me.username, '');
      expect(me.permissions, isEmpty);
      expect(me.allowsResource('platform:tenant'), isFalse);
    });
  });

  group('Seeded administrative roles', () {
    test('should_flag_the_immutable_global_and_tenant_admin_roles', () {
      // Mirrors `PermissionCatalog.isSeededAdminRole`, so the console offers no edit the server would refuse.
      expect(
        isSeededAdminRole(
          const Role(id: 'r', code: 'platform-admin', scope: 'PLATFORM'),
        ),
        isTrue,
      );
      expect(
        isSeededAdminRole(
          const Role(
            id: 'r',
            code: 'admin',
            scope: 'TENANT',
            tenantId: 'acme',
          ),
        ),
        isTrue,
      );
    });

    test('should_not_flag_a_role_of_another_code_owner_or_plane', () {
      expect(
        isSeededAdminRole(
          const Role(
            id: 'r',
            code: 'platform-admin',
            scope: 'TENANT',
            tenantId: 'acme',
          ),
        ),
        isFalse,
      );
      expect(
        isSeededAdminRole(
          const Role(id: 'r', code: 'admin', scope: 'PLATFORM'),
        ),
        isFalse,
      );
      expect(
        isSeededAdminRole(const Role(id: 'r', code: 'auditor', scope: 'TENANT')),
        isFalse,
      );
    });
  });

  group('Tenant', () {
    test('should_parse_slug', () {
      final tenant = Tenant.fromJson({
        'id': 'i',
        'name': 'Acme',
        'slug': 'acme',
      });
      expect(tenant.slug, 'acme');
    });

    test('should_default_to_a_customer_tenant', () {
      final tenant = Tenant.fromJson({
        'id': 'i',
        'name': 'Acme',
        'slug': 'acme',
      });

      expect(tenant.platform, isFalse);
      expect(tenant.isPlatform, isFalse);
      expect(tenant.userPlane, 'acme');
    });

    test('should_flag_the_platform_tenant', () {
      final tenant = Tenant.fromJson({
        'id': '00000000-0000-0000-0000-000000000000',
        'name': 'Keystone',
        'slug': 'keystone',
        'platform': true,
        'createdAt': null,
        'updatedAt': null,
      });

      expect(tenant.isPlatform, isTrue);
      expect(tenant.userPlane, 'Platform');
      expect(tenant.createdAt, '');
    });
  });

  group('Role', () {
    test('should_flag_the_platform_scope', () {
      final platform = Role.fromJson({
        'id': 'i',
        'code': 'platform-admin',
        'scope': 'PLATFORM',
      });
      final tenant = Role.fromJson({
        'id': 'i',
        'code': 'tenant-admin',
        'scope': 'TENANT',
      });

      expect(platform.isPlatformScope, isTrue);
      expect(tenant.isPlatformScope, isFalse);
    });
  });

  group('User', () {
    test('should parse username and email', () {
      final user = User.fromJson({
        'id': 'i',
        'sub': 's',
        'username': 'alice',
        'email': 'alice@acme.com',
        'roles': const ['role-a'],
      });
      expect(user.username, 'alice');
      expect(user.email, 'alice@acme.com');
      expect(user.roles, ['role-a']);
    });
  });

  group('Permission', () {
    test('should read the access level from the code suffix', () {
      final readOnly = Permission.fromJson({
        'id': 'i',
        'code': 'platform:tenant:read-only',
        'scope': 'PLATFORM',
      });
      final readWrite = Permission.fromJson({
        'id': 'i',
        'code': 'platform:user:read-write',
        'scope': 'PLATFORM',
      });

      expect(readOnly.access, PermissionAccess.readOnly);
      expect(readOnly.access?.label, 'read-only');
      expect(readWrite.access, PermissionAccess.readWrite);
      expect(readWrite.access?.label, 'read/write');
    });

    test('should read the wildcard as read/write', () {
      final wildcard = Permission.fromJson({
        'id': 'i',
        'code': '*',
        'scope': 'PLATFORM',
      });

      expect(wildcard.access, PermissionAccess.readWrite);
      expect(wildcard.access?.label, 'read/write');
    });

    test('should have no access level for a code without a level suffix', () {
      final legacy = Permission.fromJson({
        'id': 'i',
        'code': 'platform:tenant:create',
        'scope': 'PLATFORM',
      });

      expect(legacy.access, isNull);
    });
  });

  group('Session', () {
    test('should parse tokens', () {
      final session = Session.fromJson({
        'accessToken': 'access',
        'refreshToken': 'refresh',
        'tokenType': 'bearer',
        'expiresIn': 3600,
      });
      expect(session.accessToken, 'access');
      expect(session.refreshToken, 'refresh');
      expect(session.expiresIn, 3600);
    });
  });

  group('Password request models', () {
    test(
      'should_omit_the_current_password_when_the_forced_flow_sends_none',
      () {
        expect(ChangePasswordRequest(password: 'new-secret').toJson(), {
          'password': 'new-secret',
        });
      },
    );

    test('should_carry_the_current_password_for_a_voluntary_change', () {
      expect(
        ChangePasswordRequest(
          password: 'new-secret',
          currentPassword: 'old-secret',
        ).toJson(),
        {'password': 'new-secret', 'currentPassword': 'old-secret'},
      );
    });

    test('should_serialize_the_reset_password_request', () {
      expect(ResetPasswordRequest(temporaryPassword: 'temp-secret').toJson(), {
        'temporaryPassword': 'temp-secret',
      });
    });
  });

  group('Role and permission ownership', () {
    test('should_treat_an_absent_owner_as_the_global_catalog', () {
      final role = Role.fromJson({
        'id': 'r1',
        'code': 'platform-admin',
        'scope': 'PLATFORM',
      });
      expect(role.isGlobal, isTrue);
      expect(role.tenantId, isNull);
      expect(role.ownerLabel, 'Global');

      final permission = Permission.fromJson({
        'id': 'p1',
        'code': 'tenant:user:read-write',
        'scope': 'TENANT',
      });
      expect(permission.isGlobal, isTrue);
    });
  });

  group('Country and phone number', () {
    test('should_parse_a_tenants_country_and_default_it_to_none', () {
      expect(
        Tenant.fromJson({
          'id': 'i',
          'name': 'Acme',
          'slug': 'acme',
          'country': 'IN',
        }).country,
        'IN',
      );
      expect(
        Tenant.fromJson({'id': 'i', 'name': 'Acme', 'slug': 'acme'}).country,
        isNull,
      );
    });

    test('should_parse_a_users_phone_number_and_default_it_to_none', () {
      final user = User.fromJson({
        'id': 'u1',
        'sub': 'sub-1',
        'username': 'alice',
        'email': 'alice@acme.com',
        'phoneNumber': '+919876543210',
      });
      expect(user.phoneNumber, '+919876543210');

      final none = User.fromJson({
        'id': 'u2',
        'sub': 'sub-2',
        'username': 'bob',
        'email': 'bob@acme.com',
      });
      expect(none.phoneNumber, isNull);
    });

    test('should_carry_the_country_in_a_create_and_an_update_request', () {
      expect(
        CreateTenantRequest(name: 'Acme', slug: 'acme', country: 'IN').toJson(),
        {'name': 'Acme', 'slug': 'acme', 'country': 'IN'},
      );
      // An omitted country clears the recorded one: the update takes the same full body as create.
      expect(UpdateTenantRequest(name: 'Acme', slug: 'acme').toJson(), {
        'name': 'Acme',
        'slug': 'acme',
        'country': null,
      });
    });

    test('should_carry_the_phone_number_in_a_create_and_an_update_request', () {
      expect(
        CreateUserRequest(
          username: 'bob',
          temporaryPassword: 'temporary',
          phoneNumber: '+91 98765 43210',
        ).toJson(),
        {
          'username': 'bob',
          'tenantId': null,
          'email': null,
          'phoneNumber': '+91 98765 43210',
          'temporaryPassword': 'temporary',
          'roles': <String>[],
        },
      );
      expect(UpdateUserRequest(username: 'bob').toJson(), {
        'username': 'bob',
        'phoneNumber': null,
        'roles': <String>[],
      });
    });
  });
}
