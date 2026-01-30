package com.example.geodedemo.security;

import lombok.extern.slf4j.Slf4j;
import org.apache.geode.security.AuthenticationFailedException;
import org.apache.geode.security.ResourcePermission;
import org.apache.geode.security.SecurityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Geode Security Configuration.
 * Implements authentication and authorization for Geode cluster access.
 *
 * This is a sample implementation for demonstration purposes.
 * In production, integrate with LDAP, OAuth, or enterprise identity provider.
 */
@Slf4j
@Configuration
public class GeodeSecurityConfig implements SecurityManager {

    // Sample users with their credentials and permissions
    private static final Map<String, UserCredentials> USERS = new HashMap<>();

    static {
        // Admin user - full access
        USERS.put("admin", new UserCredentials("admin", "admin123",
            Set.of(Permission.DATA_READ, Permission.DATA_WRITE, Permission.DATA_MANAGE,
                   Permission.CLUSTER_READ, Permission.CLUSTER_WRITE, Permission.CLUSTER_MANAGE)));

        // Read-only user
        USERS.put("reader", new UserCredentials("reader", "reader123",
            Set.of(Permission.DATA_READ, Permission.CLUSTER_READ)));

        // Application user - read/write data
        USERS.put("app", new UserCredentials("app", "app123",
            Set.of(Permission.DATA_READ, Permission.DATA_WRITE, Permission.CLUSTER_READ)));

        // Operator user - cluster management
        USERS.put("operator", new UserCredentials("operator", "operator123",
            Set.of(Permission.DATA_READ, Permission.CLUSTER_READ, Permission.CLUSTER_WRITE)));
    }

    @Override
    public Object authenticate(Properties credentials) throws AuthenticationFailedException {
        String username = credentials.getProperty("security-username");
        String password = credentials.getProperty("security-password");

        log.debug("Authenticating user: {}", username);

        if (username == null || password == null) {
            throw new AuthenticationFailedException("Username or password is missing");
        }

        UserCredentials user = USERS.get(username);
        if (user == null || !user.password.equals(password)) {
            log.warn("Authentication failed for user: {}", username);
            throw new AuthenticationFailedException("Invalid credentials for user: " + username);
        }

        log.info("User authenticated: {}", username);
        return user;
    }

    @Override
    public boolean authorize(Object principal, ResourcePermission permission) {
        if (principal == null) {
            log.warn("Authorization failed: no principal");
            return false;
        }

        UserCredentials user = (UserCredentials) principal;
        Permission required = mapPermission(permission);

        boolean authorized = user.permissions.contains(required);
        log.debug("Authorization check - User: {}, Permission: {}, Authorized: {}",
            user.username, required, authorized);

        return authorized;
    }

    private Permission mapPermission(ResourcePermission permission) {
        String resource = permission.getResource().name();
        String operation = permission.getOperation().name();

        if ("DATA".equals(resource)) {
            switch (operation) {
                case "READ": return Permission.DATA_READ;
                case "WRITE": return Permission.DATA_WRITE;
                case "MANAGE": return Permission.DATA_MANAGE;
            }
        } else if ("CLUSTER".equals(resource)) {
            switch (operation) {
                case "READ": return Permission.CLUSTER_READ;
                case "WRITE": return Permission.CLUSTER_WRITE;
                case "MANAGE": return Permission.CLUSTER_MANAGE;
            }
        }
        return Permission.DATA_READ; // Default
    }

    @Override
    public void close() {
        log.info("Security manager closed");
    }

    // Permission enum
    enum Permission {
        DATA_READ,
        DATA_WRITE,
        DATA_MANAGE,
        CLUSTER_READ,
        CLUSTER_WRITE,
        CLUSTER_MANAGE
    }

    // User credentials class
    static class UserCredentials {
        final String username;
        final String password;
        final Set<Permission> permissions;

        UserCredentials(String username, String password, Set<Permission> permissions) {
            this.username = username;
            this.password = password;
            this.permissions = permissions;
        }
    }
}
