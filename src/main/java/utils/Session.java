package utils;

public final class Session {

    public enum Role { USER, ADMIN }

    private static Role role = Role.ADMIN; // défaut

    private Session() {}

    public static void setRole(Role r) {
        role = (r == null) ? Role.USER : r;
    }

    public static Role getRole() {
        return role;
    }

    public static boolean isAdmin() {
        return role == Role.ADMIN;
    }
}