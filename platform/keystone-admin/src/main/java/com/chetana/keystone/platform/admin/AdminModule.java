package com.chetana.keystone.platform.admin;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.chetana.keystone.platform.admin.auth.AuthFilter;
import com.chetana.keystone.platform.admin.auth.AuthHandler;
import com.chetana.keystone.platform.admin.auth.JwtTokenAuthenticator;
import com.chetana.keystone.platform.admin.auth.LoginService;
import com.chetana.keystone.platform.admin.auth.PermissionGuard;
import com.chetana.keystone.platform.admin.auth.TenantResolver;
import com.chetana.keystone.platform.admin.auth.TokenAuthenticator;
import com.chetana.keystone.platform.admin.identity.MeService;
import com.chetana.keystone.platform.admin.identity.PermissionResolver;
import com.chetana.keystone.platform.admin.permission.PermissionHandler;
import com.chetana.keystone.platform.admin.permission.PermissionService;
import com.chetana.keystone.platform.admin.role.RoleHandler;
import com.chetana.keystone.platform.admin.role.RoleService;
import com.chetana.keystone.platform.admin.supabase.SupabaseAdminClient;
import com.chetana.keystone.platform.admin.supabase.SupabaseHttpAdminClient;
import com.chetana.keystone.platform.admin.tenant.TenantHandler;
import com.chetana.keystone.platform.admin.tenant.TenantService;
import com.chetana.keystone.platform.admin.user.UserHandler;
import com.chetana.keystone.platform.admin.user.UserService;
import com.chetana.keystone.web.RouteConfigurer;

/**
 * Wires the platform admin console library: its services and its routes. Common infrastructure
 * ({@code IdGenerator}, {@code DateTimeService}) is bound by the hosting application, not here.
 */
public final class AdminModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(TokenAuthenticator.class).to(JwtTokenAuthenticator.class);
        bind(SupabaseAdminClient.class).to(SupabaseHttpAdminClient.class);

        bind(PermissionResolver.class);
        bind(PermissionGuard.class);
        bind(TenantResolver.class);
        bind(LoginService.class);
        bind(MeService.class);
        bind(TenantService.class);
        bind(RoleService.class);
        bind(PermissionService.class);
        bind(UserService.class);
        bind(BootstrapRunner.class);

        Multibinder<RouteConfigurer> routes = Multibinder.newSetBinder(binder(), RouteConfigurer.class);
        routes.addBinding().to(AuthFilter.class);
        routes.addBinding().to(AuthHandler.class);
        routes.addBinding().to(HealthHandler.class);
        routes.addBinding().to(MeHandler.class);
        routes.addBinding().to(TenantHandler.class);
        routes.addBinding().to(RoleHandler.class);
        routes.addBinding().to(PermissionHandler.class);
        routes.addBinding().to(UserHandler.class);
    }
}
