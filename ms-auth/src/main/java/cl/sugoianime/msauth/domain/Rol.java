package cl.sugoianime.msauth.domain;

public enum Rol {
    USUARIO,
    PREMIUM,
    ADMIN;

    /** Spring Security espera el prefijo ROLE_ en las authorities. */
    public String comoAuthority() {
        return "ROLE_" + name();
    }
}
