/**
 * Shared test support for Keystone applications — Testcontainers base classes, JavalinTest
 * helpers, and fixtures.
 *
 * <p>Consumers depend on this module with {@code <scope>test</scope>}. Its dependencies
 * (Testcontainers, javalin-testtools, JUnit Jupiter, AssertJ) are declared at compile scope here
 * so they flow transitively to test scope in each application, keeping test infrastructure in one
 * place.
 */
package com.chetana.keystone.testing;
