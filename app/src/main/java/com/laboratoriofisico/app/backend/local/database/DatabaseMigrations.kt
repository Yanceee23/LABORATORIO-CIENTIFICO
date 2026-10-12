package com.laboratoriofisico.app.backend.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sensor_readings (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sensorType TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                valueX REAL,
                valueY REAL,
                valueZ REAL,
                valueScalar REAL,
                altitude REAL,
                accuracy REAL,
                speed REAL,
                unit TEXT NOT NULL,
                metadata TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_sensor_readings_sensorType ON sensor_readings(sensorType)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_sensor_readings_timestamp ON sensor_readings(timestamp)"
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE usuarios ADD COLUMN passwordHash TEXT NOT NULL DEFAULT ''"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_usuarios_email ON usuarios(email)"
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS documentos (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                usuarioId INTEGER NOT NULL,
                nombre TEXT NOT NULL,
                nombreArchivo TEXT NOT NULL,
                rutaArchivo TEXT NOT NULL,
                extension TEXT NOT NULL,
                tamano INTEGER NOT NULL,
                numeroPaginas INTEGER NOT NULL,
                fechaImportacion INTEGER NOT NULL,
                estadoProcesamiento TEXT NOT NULL,
                FOREIGN KEY(usuarioId) REFERENCES usuarios(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_documentos_usuarioId ON documentos(usuarioId)"
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS secciones_documento (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                documentoId INTEGER NOT NULL,
                numeroPagina INTEGER NOT NULL,
                titulo TEXT NOT NULL,
                contenido TEXT NOT NULL,
                orden INTEGER NOT NULL,
                FOREIGN KEY(documentoId) REFERENCES documentos(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_secciones_documento_documentoId ON secciones_documento(documentoId)"
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_secciones_documento_documentoId_orden
            ON secciones_documento(documentoId, orden)
            """.trimIndent()
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS preguntas_estudio (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                documentoId INTEGER NOT NULL,
                pregunta TEXT NOT NULL,
                respuesta TEXT NOT NULL,
                explicacion TEXT NOT NULL,
                dificultad TEXT NOT NULL,
                fechaCreacion INTEGER NOT NULL,
                FOREIGN KEY(documentoId) REFERENCES documentos(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_preguntas_estudio_documentoId ON preguntas_estudio(documentoId)"
        )
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ejercicios_estudio (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                documentoId INTEGER NOT NULL,
                enunciado TEXT NOT NULL,
                datos TEXT NOT NULL,
                formula TEXT NOT NULL,
                solucion TEXT NOT NULL,
                explicacion TEXT NOT NULL,
                dificultad TEXT NOT NULL,
                fechaCreacion INTEGER NOT NULL,
                FOREIGN KEY(documentoId) REFERENCES documentos(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_ejercicios_estudio_documentoId ON ejercicios_estudio(documentoId)"
        )
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS tarjetas_estudio (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                documentoId INTEGER NOT NULL,
                frente TEXT NOT NULL,
                reverso TEXT NOT NULL,
                dificultad TEXT NOT NULL,
                estado TEXT NOT NULL,
                fechaCreacion INTEGER NOT NULL,
                FOREIGN KEY(documentoId) REFERENCES documentos(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_tarjetas_estudio_documentoId ON tarjetas_estudio(documentoId)"
        )
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS historial_estudio (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                documentoId INTEGER NOT NULL,
                tipoActividad TEXT NOT NULL,
                resultado TEXT NOT NULL,
                fecha INTEGER NOT NULL,
                tiempoEstudio INTEGER NOT NULL,
                FOREIGN KEY(documentoId) REFERENCES documentos(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_historial_estudio_documentoId ON historial_estudio(documentoId)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_historial_estudio_fecha ON historial_estudio(fecha)"
        )
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS conversaciones_ia (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                usuarioId INTEGER NOT NULL,
                documentoId INTEGER,
                pregunta TEXT NOT NULL,
                respuesta TEXT NOT NULL,
                fechaHora INTEGER NOT NULL,
                FOREIGN KEY(usuarioId) REFERENCES usuarios(id) ON DELETE CASCADE,
                FOREIGN KEY(documentoId) REFERENCES documentos(id) ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_conversaciones_ia_usuarioId ON conversaciones_ia(usuarioId)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_conversaciones_ia_documentoId ON conversaciones_ia(documentoId)"
        )
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS analisis_microscopio (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                experimentoId INTEGER NOT NULL,
                tipoExperimento TEXT NOT NULL,
                subtipo TEXT,
                imagenPath TEXT NOT NULL,
                analisis TEXT NOT NULL,
                resumenParaComparacion TEXT NOT NULL,
                promptVersion INTEGER NOT NULL,
                timestamp INTEGER NOT NULL,
                FOREIGN KEY(experimentoId) REFERENCES experimentos(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_analisis_microscopio_experimentoId ON analisis_microscopio(experimentoId)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_analisis_microscopio_tipoExperimento ON analisis_microscopio(tipoExperimento)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_analisis_microscopio_timestamp ON analisis_microscopio(timestamp)"
        )
    }
}

/**
 * v12: roles, userId en tablas huérfanas, emails normalizados, sin borrar datos.
 * Datos sin dueño se asignan al primer usuario (o se crea uno de migración).
 */
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1) Asegurar al menos un usuario para backfill.
        db.execSQL(
            """
            INSERT INTO usuarios (nombre, email, rol, fechaCreacion, passwordHash)
            SELECT 'Usuario migrado', 'migrado@laboratorio.local', 'registrado',
                   ${System.currentTimeMillis()}, ''
            WHERE NOT EXISTS (SELECT 1 FROM usuarios LIMIT 1)
            """.trimIndent()
        )

        // 2) Roles legacy → registrado.
        db.execSQL(
            """
            UPDATE usuarios SET rol = 'registrado'
            WHERE lower(trim(rol)) IN ('estudiante', 'usuario', 'admin', '')
               OR rol IS NULL
            """.trimIndent()
        )

        // 3) Normalizar emails y resolver duplicados en Kotlin-equivalente vía SQL temporal.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS _email_norm (
                id INTEGER PRIMARY KEY NOT NULL,
                email_original TEXT NOT NULL,
                email_norm TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO _email_norm (id, email_original, email_norm)
            SELECT id, email, lower(trim(email)) FROM usuarios
            """.trimIndent()
        )

        // Marcar duplicados: conservar el menor id.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS _email_keep AS
            SELECT email_norm, MIN(id) AS keep_id
            FROM _email_norm
            GROUP BY email_norm
            """.trimIndent()
        )
        db.execSQL(
            """
            UPDATE usuarios
            SET email = (
                SELECT email_norm FROM _email_norm WHERE _email_norm.id = usuarios.id
            )
            WHERE id IN (SELECT keep_id FROM _email_keep)
            """.trimIndent()
        )
        // Duplicados: añadir sufijo +dup{id} para no perder filas.
        db.execSQL(
            """
            UPDATE usuarios
            SET email = (
                SELECT
                    CASE
                        WHEN instr(n.email_norm, '@') > 0 THEN
                            substr(n.email_norm, 1, instr(n.email_norm, '@') - 1)
                            || '+dup' || n.id
                            || substr(n.email_norm, instr(n.email_norm, '@'))
                        ELSE n.email_norm || '+dup' || n.id
                    END
                FROM _email_norm n
                WHERE n.id = usuarios.id
            )
            WHERE id NOT IN (SELECT keep_id FROM _email_keep)
            """.trimIndent()
        )
        db.execSQL("DROP TABLE IF EXISTS _email_norm")
        db.execSQL("DROP TABLE IF EXISTS _email_keep")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_usuarios_email ON usuarios(email)")

        val firstUserId = queryFirstUserId(db)

        // 4) ubicaciones + usuarioId
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ubicaciones_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                usuarioId INTEGER NOT NULL,
                nombre TEXT NOT NULL,
                latitud REAL,
                longitud REAL,
                descripcion TEXT NOT NULL,
                FOREIGN KEY(usuarioId) REFERENCES usuarios(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO ubicaciones_new (id, usuarioId, nombre, latitud, longitud, descripcion)
            SELECT id, $firstUserId, nombre, latitud, longitud, descripcion FROM ubicaciones
            """.trimIndent()
        )
        db.execSQL("DROP TABLE ubicaciones")
        db.execSQL("ALTER TABLE ubicaciones_new RENAME TO ubicaciones")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_ubicaciones_usuarioId ON ubicaciones(usuarioId)")

        // 5) configuracion por usuario
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS configuracion_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                usuarioId INTEGER NOT NULL,
                temaOscuro INTEGER NOT NULL,
                idioma TEXT NOT NULL,
                unidadesMetricas INTEGER NOT NULL,
                ultimaSincronizacion INTEGER,
                FOREIGN KEY(usuarioId) REFERENCES usuarios(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO configuracion_new (usuarioId, temaOscuro, idioma, unidadesMetricas, ultimaSincronizacion)
            SELECT $firstUserId,
                   COALESCE((SELECT temaOscuro FROM configuracion WHERE id = 1), 0),
                   COALESCE((SELECT idioma FROM configuracion WHERE id = 1), 'es'),
                   COALESCE((SELECT unidadesMetricas FROM configuracion WHERE id = 1), 1),
                   (SELECT ultimaSincronizacion FROM configuracion WHERE id = 1)
            """.trimIndent()
        )
        db.execSQL("DROP TABLE configuracion")
        db.execSQL("ALTER TABLE configuracion_new RENAME TO configuracion")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_configuracion_usuarioId ON configuracion(usuarioId)"
        )

        // 6) sensor_readings + usuarioId
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sensor_readings_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                usuarioId INTEGER NOT NULL,
                sensorType TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                valueX REAL,
                valueY REAL,
                valueZ REAL,
                valueScalar REAL,
                altitude REAL,
                accuracy REAL,
                speed REAL,
                unit TEXT NOT NULL,
                metadata TEXT,
                FOREIGN KEY(usuarioId) REFERENCES usuarios(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO sensor_readings_new (
                id, usuarioId, sensorType, timestamp, valueX, valueY, valueZ,
                valueScalar, altitude, accuracy, speed, unit, metadata
            )
            SELECT id, $firstUserId, sensorType, timestamp, valueX, valueY, valueZ,
                   valueScalar, altitude, accuracy, speed, unit, metadata
            FROM sensor_readings
            """.trimIndent()
        )
        db.execSQL("DROP TABLE sensor_readings")
        db.execSQL("ALTER TABLE sensor_readings_new RENAME TO sensor_readings")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sensor_readings_usuarioId ON sensor_readings(usuarioId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sensor_readings_sensorType ON sensor_readings(sensorType)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sensor_readings_timestamp ON sensor_readings(timestamp)")

        // 7) mediciones + usuarioId (desde experimento o primer usuario)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS mediciones_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                experimentoId INTEGER NOT NULL,
                usuarioId INTEGER NOT NULL,
                nombre TEXT NOT NULL,
                valor REAL NOT NULL,
                unidad TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                FOREIGN KEY(experimentoId) REFERENCES experimentos(id) ON DELETE CASCADE,
                FOREIGN KEY(usuarioId) REFERENCES usuarios(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO mediciones_new (id, experimentoId, usuarioId, nombre, valor, unidad, timestamp)
            SELECT m.id, m.experimentoId,
                   COALESCE(e.usuarioId, $firstUserId),
                   m.nombre, m.valor, m.unidad, m.timestamp
            FROM mediciones m
            LEFT JOIN experimentos e ON e.id = m.experimentoId
            """.trimIndent()
        )
        db.execSQL("DROP TABLE mediciones")
        db.execSQL("ALTER TABLE mediciones_new RENAME TO mediciones")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_mediciones_experimentoId ON mediciones(experimentoId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_mediciones_usuarioId ON mediciones(usuarioId)")

        // 8) resultados + usuarioId
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS resultados_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                medicionId INTEGER NOT NULL,
                usuarioId INTEGER NOT NULL,
                valorCalculado REAL NOT NULL,
                unidad TEXT NOT NULL,
                notas TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                FOREIGN KEY(medicionId) REFERENCES mediciones(id) ON DELETE CASCADE,
                FOREIGN KEY(usuarioId) REFERENCES usuarios(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO resultados_new (id, medicionId, usuarioId, valorCalculado, unidad, notas, timestamp)
            SELECT r.id, r.medicionId,
                   COALESCE(m.usuarioId, $firstUserId),
                   r.valorCalculado, r.unidad, r.notas, r.timestamp
            FROM resultados r
            LEFT JOIN mediciones m ON m.id = r.medicionId
            """.trimIndent()
        )
        db.execSQL("DROP TABLE resultados")
        db.execSQL("ALTER TABLE resultados_new RENAME TO resultados")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_resultados_medicionId ON resultados(medicionId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_resultados_usuarioId ON resultados(usuarioId)")
    }

    private fun queryFirstUserId(db: SupportSQLiteDatabase): Long {
        db.query("SELECT id FROM usuarios ORDER BY id ASC LIMIT 1").use { cursor ->
            if (cursor.moveToFirst()) return cursor.getLong(0)
        }
        return 1L
    }
}

/** v13: ejercicios resueltos con foto (Tutor Fase 1), sin borrar datos. */
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ejercicios_resueltos (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                usuarioId INTEGER NOT NULL,
                fotoPath TEXT NOT NULL,
                materia TEXT NOT NULL,
                tema TEXT NOT NULL,
                enunciado TEXT NOT NULL,
                datosJson TEXT NOT NULL,
                incognita TEXT NOT NULL,
                formula TEXT NOT NULL,
                pasosJson TEXT NOT NULL,
                resultadoValor REAL NOT NULL,
                resultadoUnidad TEXT NOT NULL,
                verificacion TEXT NOT NULL,
                confianza TEXT NOT NULL,
                fechaCreacion INTEGER NOT NULL,
                FOREIGN KEY(usuarioId) REFERENCES usuarios(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_ejercicios_resueltos_usuarioId ON ejercicios_resueltos(usuarioId)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_ejercicios_resueltos_fechaCreacion ON ejercicios_resueltos(fechaCreacion)"
        )
    }
}

/** v14: campos legibles de fórmula + progreso del juego Aprender jugando. */
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE ejercicios_resueltos ADD COLUMN formulaLegible TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE ejercicios_resueltos ADD COLUMN variablesJson TEXT NOT NULL DEFAULT '[]'")
        db.execSQL("ALTER TABLE ejercicios_resueltos ADD COLUMN explicacionFormula TEXT NOT NULL DEFAULT ''")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS juego_ejercicio_progreso (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                usuarioId INTEGER NOT NULL,
                ejercicioId INTEGER NOT NULL,
                intentos INTEGER NOT NULL,
                estrellas INTEGER NOT NULL,
                pistasUsadas INTEGER NOT NULL,
                completo INTEGER NOT NULL,
                fechaActualizacion INTEGER NOT NULL,
                FOREIGN KEY(usuarioId) REFERENCES usuarios(id) ON DELETE CASCADE,
                FOREIGN KEY(ejercicioId) REFERENCES ejercicios_resueltos(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_juego_ejercicio_progreso_usuarioId ON juego_ejercicio_progreso(usuarioId)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_juego_ejercicio_progreso_ejercicioId ON juego_ejercicio_progreso(ejercicioId)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_juego_ejercicio_progreso_usuarioId_ejercicioId ON juego_ejercicio_progreso(usuarioId, ejercicioId)"
        )
    }
}

