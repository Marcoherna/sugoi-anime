-- =====================================================================
-- V2: Dataset semilla. Mitigacion del riesgo R02 (rate limit de Jikan).
-- Permite demos sin dependencia de la API externa.
-- =====================================================================

INSERT INTO genero (mal_id, nombre, nombre_es) VALUES
                                                   (1,  'Action',        'Accion'),
                                                   (2,  'Adventure',     'Aventura'),
                                                   (4,  'Comedy',        'Comedia'),
                                                   (8,  'Drama',         'Drama'),
                                                   (10, 'Fantasy',       'Fantasia'),
                                                   (14, 'Horror',        'Terror'),
                                                   (22, 'Romance',       'Romance'),
                                                   (24, 'Sci-Fi',        'Ciencia Ficcion'),
                                                   (30, 'Sports',        'Deportes'),
                                                   (37, 'Supernatural',  'Sobrenatural'),
                                                   (36, 'Slice of Life', 'Recuentos de la Vida'),
                                                   (41, 'Suspense',      'Suspenso');

INSERT INTO anime (mal_id, titulo, titulo_japones, sinopsis, sinopsis_es, anio, episodios, puntuacion, estado) VALUES
                                                                                                                   (16498, 'Attack on Titan', 'Shingeki no Kyojin',
                                                                                                                    'Humanity fights for survival against giant humanoid Titans behind massive walls.',
                                                                                                                    'La humanidad lucha por sobrevivir frente a titanes gigantes tras enormes murallas.',
                                                                                                                    2013, 25, 8.54, 'FINALIZADO'),

                                                                                                                   (5114, 'Fullmetal Alchemist: Brotherhood', 'Hagane no Renkinjutsushi',
                                                                                                                    'Two brothers search for the Philosopher''s Stone to restore their bodies.',
                                                                                                                    'Dos hermanos buscan la Piedra Filosofal para recuperar sus cuerpos.',
                                                                                                                    2009, 64, 9.09, 'FINALIZADO'),

                                                                                                                   (9253, 'Steins;Gate', 'Steins;Gate',
                                                                                                                    'A group of friends discover a way to send messages to the past.',
                                                                                                                    'Un grupo de amigos descubre como enviar mensajes al pasado.',
                                                                                                                    2011, 24, 9.07, 'FINALIZADO'),

                                                                                                                   (11061, 'Hunter x Hunter', 'Hunter x Hunter',
                                                                                                                    'A young boy searches for his father while becoming a licensed Hunter.',
                                                                                                                    'Un joven busca a su padre mientras se convierte en Cazador licenciado.',
                                                                                                                    2011, 148, 9.03, 'FINALIZADO'),

                                                                                                                   (38000, 'Demon Slayer', 'Kimetsu no Yaiba',
                                                                                                                    'A boy becomes a demon slayer to avenge his family and cure his sister.',
                                                                                                                    'Un joven se convierte en cazador de demonios para vengar a su familia.',
                                                                                                                    2019, 26, 8.49, 'FINALIZADO'),

                                                                                                                   (31964, 'My Hero Academia', 'Boku no Hero Academia',
                                                                                                                    'A boy born without superpowers dreams of becoming a hero.',
                                                                                                                    'Un joven sin superpoderes suena con convertirse en heroe.',
                                                                                                                    2016, 13, 7.86, 'FINALIZADO'),

                                                                                                                   (30276, 'One Punch Man', 'One Punch Man',
                                                                                                                    'A hero who can defeat any enemy with a single punch seeks a worthy opponent.',
                                                                                                                    'Un heroe que derrota a cualquier enemigo de un golpe busca un rival digno.',
                                                                                                                    2015, 12, 8.49, 'FINALIZADO'),

                                                                                                                   (32281, 'Your Name', 'Kimi no Na wa.',
                                                                                                                    'Two teenagers discover they are mysteriously swapping bodies.',
                                                                                                                    'Dos adolescentes descubren que intercambian cuerpos misteriosamente.',
                                                                                                                    2016, 1, 8.83, 'FINALIZADO'),

                                                                                                                   (52991, 'Frieren: Beyond Journey''s End', 'Sousou no Frieren',
                                                                                                                    'An elf mage reflects on mortality after her hero party disbands.',
                                                                                                                    'Una maga elfa reflexiona sobre la mortalidad tras disolverse su grupo.',
                                                                                                                    2023, 28, 9.30, 'FINALIZADO'),

                                                                                                                   (51009, 'Jujutsu Kaisen 2nd Season', 'Jujutsu Kaisen 2nd Season',
                                                                                                                    'Sorcerers battle cursed spirits in modern Japan.',
                                                                                                                    'Hechiceros combaten espiritus malditos en el Japon moderno.',
                                                                                                                    2023, 23, 8.72, 'FINALIZADO');

-- Relacion anime <-> genero
INSERT INTO anime_genero (anime_id, genero_id)
SELECT a.id, g.id FROM anime a, genero g
WHERE (a.mal_id, g.mal_id) IN (
                               (16498,1),(16498,8),(16498,41),
                               (5114,1),(5114,2),(5114,8),
                               (9253,24),(9253,41),
                               (11061,1),(11061,2),(11061,10),
                               (38000,1),(38000,37),
                               (31964,1),(31964,4),
                               (30276,1),(30276,4),(30276,24),
                               (32281,22),(32281,8),(32281,37),
                               (52991,2),(52991,8),(52991,10),
                               (51009,1),(51009,10),(51009,37)
    );

-- =====================================================================
-- Episodios de demostracion.
-- CUMPLIMIENTO LEY 17.336 / RIESGO R04:
-- Todo el contenido es Creative Commons de Blender Foundation.
-- NO se usa material con derechos reservados.
-- =====================================================================
INSERT INTO episodio (anime_id, numero, titulo, duracion_seg, video_key, licencia, fuente_url)
SELECT a.id, 1, 'Episodio de demostracion (contenido CC)', 634,
       'demo/big_buck_bunny_1080p.mp4', 'CC-BY-3.0',
       'https://peach.blender.org/download/'
FROM anime a WHERE a.mal_id IN (16498, 5114, 9253);

INSERT INTO episodio (anime_id, numero, titulo, duracion_seg, video_key, licencia, fuente_url)
SELECT a.id, 2, 'Episodio de demostracion (contenido CC)', 888,
       'demo/sintel_1080p.mp4', 'CC-BY-3.0',
       'https://durian.blender.org/download/'
FROM anime a WHERE a.mal_id IN (16498, 5114);
