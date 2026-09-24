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

  group('Tenant', () {
    test('should parse slug', () {
      final tenant = Tenant.fromJson({'id': 'i', 'name': 'Acme', 'slug': 'acme'});
      expect(tenant.slug, 'acme');
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
}
