package com.example.evshare.security;

/**
 * Authoritative security role constants and SpEL expression definitions
 * in compliance with docs/RBAC.md.
 */
public final class SecurityRoles {

    private SecurityRoles() {
        // Prevent instantiation
    }

    // Role Identifiers (Used in hasRole(...) checks)
    public static final String CO_OWNER = "CO_OWNER";
    public static final String STAFF = "STAFF";
    public static final String ADMIN = "ADMIN";

    // Authority Identifiers (Used in GrantedAuthority representations)
    public static final String ROLE_CO_OWNER = "ROLE_CO_OWNER";
    public static final String ROLE_STAFF = "ROLE_STAFF";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    // PreAuthorize SpEL expression templates
    public static final String HAS_ROLE_ADMIN = "hasRole('ADMIN')";
    public static final String HAS_ROLE_STAFF = "hasRole('STAFF')";
    public static final String HAS_ROLE_CO_OWNER = "hasRole('CO_OWNER')";
    public static final String HAS_STAFF_OR_ADMIN = "hasAnyRole('STAFF', 'ADMIN')";
    public static final String HAS_CO_OWNER_OR_ADMIN = "hasAnyRole('CO_OWNER', 'ADMIN')";
    public static final String HAS_ANY_ROLE = "hasAnyRole('CO_OWNER', 'STAFF', 'ADMIN')";
}
