package com.chetana.keystone.common.id;

import java.util.UUID;

/**
 * Port for generating identifiers.
 *
 * <p>Keeps id creation injectable and testable instead of scattering
 * {@code UUID.randomUUID()} through the codebase.
 */
@FunctionalInterface
public interface IdGenerator {

    UUID nextId();
}
