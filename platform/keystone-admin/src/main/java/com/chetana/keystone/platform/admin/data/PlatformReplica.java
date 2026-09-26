package com.chetana.keystone.platform.admin.data;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Qualifies the platform-admin read-replica {@code DataSource}/{@code DSLContext}. Complements
 * {@link Platform}, which remains the read-write (primary) qualifier for backward compatibility.
 */
@BindingAnnotation
@Target({FIELD, PARAMETER, METHOD})
@Retention(RUNTIME)
public @interface PlatformReplica {
}
