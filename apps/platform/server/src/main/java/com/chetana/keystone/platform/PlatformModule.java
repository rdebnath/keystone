package com.chetana.keystone.platform;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.chetana.keystone.common.id.IdGenerator;
import com.chetana.keystone.common.id.UuidIdGenerator;
import com.chetana.keystone.common.time.DateTimeService;
import com.chetana.keystone.common.time.SystemDateTimeService;
import com.chetana.keystone.platform.auth.AuthFilter;
import com.chetana.keystone.platform.auth.JwtTokenAuthenticator;
import com.chetana.keystone.platform.auth.PermissionGuard;
import com.chetana.keystone.platform.auth.TokenAuthenticator;
import com.chetana.keystone.platform.identity.MeService;
import com.chetana.keystone.platform.identity.PermissionResolver;
import com.chetana.keystone.platform.permission.PermissionHandler;
import com.chetana.keystone.platform.permission.PermissionService;
import com.chetana.keystone.platform.role.RoleHandler;
import com.chetana.keystone.platform.role.RoleService;
import com.chetana.keystone.platform.supabase.SupabaseAdminClient;
import com.chetana.keystone.platform.supabase.SupabaseHttpAdminClient;
import com.chetana.keystone.platform.tenant.TenantHandler;
import com.chetana.keystone.platform.tenant.TenantService;
import com.chetana.keystone.platform.user.UserHandler;
import com.chetana.keystone.platform.user.UserService;
import com.chetana.keystone.web.RouteConfigurer;

/**
 * Wires the platform admin console: its services and its routes.
 */
public final class PlatformModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(IdGenerator.class).to(UuidIdGenerator.class);
        bind(DateTimeService.class).to(SystemDateTimeService.class);

        bind(TokenAuthenticator.class).to(JwtTokenAuthenticator.class);
        bind(SupabaseAdminClient.class).to(SupabaseHttpAdminClient.class);

        bind(PermissionResolver.class);
        bind(PermissionGuard.class);
        bind(MeService.class);
        bind(TenantService.class);
        bind(RoleService.class);
        bind(PermissionService.class);
        bind(UserService.class);
        bind(BootstrapRunner.class);

        Multibinder<RouteConfigurer> routes = Multibinder.newSetBinder(binder(), RouteConfigurer.class);
        routes.addBinding().to(AuthFilter.class);
        routes.addBinding().to(HealthHandler.class);
        routes.addBinding().to(MeHandler.class);
        routes.addBinding().to(TenantHandler.class);
        routes.addBinding().to(RoleHandler.class);
        routes.addBinding().to(PermissionHandler.class);
        routes.addBinding().to(UserHandler.class);
    }
}
