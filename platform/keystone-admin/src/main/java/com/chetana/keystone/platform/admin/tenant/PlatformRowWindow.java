package com.chetana.keystone.platform.admin.tenant;

import com.chetana.keystone.common.query.PageRequest;

/**
 * How a requested page of tenants maps onto the rows actually in the {@code tenants} table, given that the
 * synthetic platform row ({@link com.chetana.keystone.platform.admin.PlatformSchema#PLATFORM_TENANT_ID})
 * is always the first row of the list.
 *
 * <p>The list the console sees is {@code [platform] + tenants}, which is one row longer than the table, so
 * page windows do not line up with a plain {@code LIMIT}/{@code OFFSET}: page 1 holds one fewer real row
 * (the platform row takes a slot), and every later page starts one row earlier in the table. When the
 * search term cannot match the platform row, the row is not part of the list at all and the window is the
 * plain one.
 *
 * <p>This is pure arithmetic with no database, so the mapping is unit-tested for every page/size
 * combination instead of being inferred from an integration test.
 */
record PlatformRowWindow(int realOffset, int realLimit, boolean includesPlatformRow) {

    /**
     * Maps the requested page onto the persisted rows.
     *
     * @param page              the requested window
     * @param platformIncluded  whether the synthetic row is part of the (search-filtered) list
     * @param realTotal         the number of persisted tenants matching the same filter
     */
    static PlatformRowWindow of(PageRequest page, boolean platformIncluded, long realTotal) {
        if (!platformIncluded) {
            return new PlatformRowWindow(page.offset(), page.size(), false);
        }
        long logicalTotal = realTotal + 1;
        if (page.offset() >= logicalTotal) {
            // Past the end: nothing to fetch, and the platform row is not on this page either.
            return new PlatformRowWindow(0, 0, false);
        }
        if (page.offset() == 0) {
            // Page 1: the platform row occupies its first slot, so one fewer real row fits in the page.
            return new PlatformRowWindow(0, page.size() - 1, true);
        }
        // Later pages: the platform row shifted every real row by one, so the window starts one row earlier.
        return new PlatformRowWindow(page.offset() - 1, page.size(), false);
    }
}
