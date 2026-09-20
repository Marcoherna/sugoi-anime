package cl.sugoianime.mscatalogo.domain;

public enum EstadoAnime {
    EN_EMISION,
    FINALIZADO,
    PROXIMAMENTE,
    DESCONOCIDO;

    /** Traduce el campo "status" que devuelve Jikan a nuestro enum (RF-04). */
    public static EstadoAnime desdeJikan(String status) {
        if (status == null) {
            return DESCONOCIDO;
        }
        return switch (status.toLowerCase()) {
            case "currently airing"  -> EN_EMISION;
            case "finished airing"   -> FINALIZADO;
            case "not yet aired"     -> PROXIMAMENTE;
            default                  -> DESCONOCIDO;
        };
    }
}
