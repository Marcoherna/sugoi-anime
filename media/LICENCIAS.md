# Registro de Licencias de Contenido Multimedia
## Proyecto SugoiAnime — Cumplimiento Ley N° 17.336 (Propiedad Intelectual)

Este registro documenta la procedencia y licencia de todo el contenido
multimedia alojado en el bucket S3 `sugoianime-media`.

**Política del proyecto:** solo se aloja contenido de dominio público,
bajo licencia Creative Commons, o material promocional oficial de
distribución libre. NO se aloja material con derechos reservados.

## Videos

| Archivo | Obra | Autor | Licencia | Fuente | Descargado |
|---|---|---|---|---|---|
| `demo/big_buck_bunny_720p.mp4` | Big Buck Bunny (2008) | Blender Foundation | CC BY 3.0 | https://peach.blender.org/download/ | 2026-09-08 |
| `demo/sintel_trailer.mp4` | Sintel (2010) — tráiler | Blender Foundation | CC BY 3.0 | https://durian.blender.org/download/ | 2026-09-08 |

## Atribución requerida

La licencia CC BY 3.0 exige atribución. El sistema la cumple mediante el
campo `licencia` de la tabla `episodio`, que se expone en el endpoint
`GET /episodios/{id}/reproduccion` y se muestra en la interfaz del
reproductor bajo el video.

## Portadas

Las imágenes de portada provienen de la API pública de Jikan
(MyAnimeList) y se referencian por URL externa, sin ser almacenadas ni
redistribuidas por este proyecto. Ver RF-04.

## Verificación

Cualquier evaluador puede verificar estas licencias visitando las URLs
de origen. Las obras de Blender Foundation declaran su licencia de forma
explícita en sus respectivas páginas de descarga.
