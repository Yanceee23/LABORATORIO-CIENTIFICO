package com.laboratoriofisico.app.backend.mapper

import com.laboratoriofisico.app.backend.local.entity.ConfiguracionEntity
import com.laboratoriofisico.app.backend.local.entity.AnalisisMicroscopioEntity
import com.laboratoriofisico.app.backend.local.entity.DocumentoEntity
import com.laboratoriofisico.app.backend.local.entity.EjercicioEstudioEntity
import com.laboratoriofisico.app.backend.local.entity.ExperimentoEntity
import com.laboratoriofisico.app.backend.local.entity.HistorialEstudioEntity
import com.laboratoriofisico.app.backend.local.entity.MedicionEntity
import com.laboratoriofisico.app.backend.local.entity.MedicionWithResultados
import com.laboratoriofisico.app.backend.local.entity.PreguntaEstudioEntity
import com.laboratoriofisico.app.backend.local.entity.ResultadoEntity
import com.laboratoriofisico.app.backend.local.entity.SeccionDocumentoEntity
import com.laboratoriofisico.app.backend.local.entity.SensorReadingEntity
import com.laboratoriofisico.app.backend.local.entity.TarjetaEstudioEntity
import com.laboratoriofisico.app.backend.local.entity.UbicacionEntity
import com.laboratoriofisico.app.backend.local.entity.UsuarioEntity
import com.laboratoriofisico.app.backend.model.Configuracion
import com.laboratoriofisico.app.backend.model.AnalisisMicroscopio
import com.laboratoriofisico.app.backend.model.Documento
import com.laboratoriofisico.app.backend.model.EjercicioEstudio
import com.laboratoriofisico.app.backend.model.EstadoExperimento
import com.laboratoriofisico.app.backend.model.Experimento
import com.laboratoriofisico.app.backend.model.HistorialEstudio
import com.laboratoriofisico.app.backend.model.Medicion
import com.laboratoriofisico.app.backend.model.MedicionConResultados
import com.laboratoriofisico.app.backend.model.PreguntaEstudio
import com.laboratoriofisico.app.backend.model.Resultado
import com.laboratoriofisico.app.backend.model.SeccionDocumento
import com.laboratoriofisico.app.backend.model.TarjetaEstudio
import com.laboratoriofisico.app.backend.model.TipoAnalisisMicroscopio
import com.laboratoriofisico.app.backend.model.Ubicacion
import com.laboratoriofisico.app.backend.model.Usuario
import com.laboratoriofisico.app.sensores.model.SensorReading
import com.laboratoriofisico.app.sensores.model.SensorType

fun UsuarioEntity.toDomain(): Usuario = Usuario(
    id = id,
    nombre = nombre,
    email = email,
    rol = rol,
    fechaCreacion = fechaCreacion
)

fun Usuario.toEntity(passwordHash: String = ""): UsuarioEntity = UsuarioEntity(
    id = id,
    nombre = nombre,
    email = email,
    rol = rol,
    fechaCreacion = fechaCreacion,
    passwordHash = passwordHash
)

fun UbicacionEntity.toDomain(): Ubicacion = Ubicacion(
    id = id,
    usuarioId = usuarioId,
    nombre = nombre,
    latitud = latitud,
    longitud = longitud,
    descripcion = descripcion
)

fun Ubicacion.toEntity(): UbicacionEntity = UbicacionEntity(
    id = id,
    usuarioId = usuarioId,
    nombre = nombre,
    latitud = latitud,
    longitud = longitud,
    descripcion = descripcion
)

fun ExperimentoEntity.toDomain(): Experimento = Experimento(
    id = id,
    titulo = titulo,
    descripcion = descripcion,
    usuarioId = usuarioId,
    ubicacionId = ubicacionId,
    estado = runCatching { EstadoExperimento.valueOf(estado) }
        .getOrDefault(EstadoExperimento.BORRADOR),
    fechaCreacion = fechaCreacion
)

fun Experimento.toEntity(): ExperimentoEntity = ExperimentoEntity(
    id = id,
    titulo = titulo,
    descripcion = descripcion,
    usuarioId = usuarioId,
    ubicacionId = ubicacionId,
    estado = estado.name,
    fechaCreacion = fechaCreacion
)

fun MedicionEntity.toDomain(): Medicion = Medicion(
    id = id,
    experimentoId = experimentoId,
    usuarioId = usuarioId,
    nombre = nombre,
    valor = valor,
    unidad = unidad,
    timestamp = timestamp
)

fun Medicion.toEntity(): MedicionEntity = MedicionEntity(
    id = id,
    experimentoId = experimentoId,
    usuarioId = usuarioId,
    nombre = nombre,
    valor = valor,
    unidad = unidad,
    timestamp = timestamp
)

fun ResultadoEntity.toDomain(): Resultado = Resultado(
    id = id,
    medicionId = medicionId,
    usuarioId = usuarioId,
    valorCalculado = valorCalculado,
    unidad = unidad,
    notas = notas,
    timestamp = timestamp
)

fun Resultado.toEntity(): ResultadoEntity = ResultadoEntity(
    id = id,
    medicionId = medicionId,
    usuarioId = usuarioId,
    valorCalculado = valorCalculado,
    unidad = unidad,
    notas = notas,
    timestamp = timestamp
)

fun AnalisisMicroscopioEntity.toDomain(): AnalisisMicroscopio = AnalisisMicroscopio(
    id = id,
    experimentoId = experimentoId,
    tipoExperimento = TipoAnalisisMicroscopio.entries.firstOrNull { it.key == tipoExperimento }
        ?: TipoAnalisisMicroscopio.YOGUR,
    subtipo = subtipo,
    imagenPath = imagenPath,
    analisis = analisis,
    resumenParaComparacion = resumenParaComparacion,
    promptVersion = promptVersion,
    timestamp = timestamp
)

fun AnalisisMicroscopio.toEntity(): AnalisisMicroscopioEntity = AnalisisMicroscopioEntity(
    id = id,
    experimentoId = experimentoId,
    tipoExperimento = tipoExperimento.key,
    subtipo = subtipo,
    imagenPath = imagenPath,
    analisis = analisis,
    resumenParaComparacion = resumenParaComparacion,
    promptVersion = promptVersion,
    timestamp = timestamp
)

fun ConfiguracionEntity.toDomain(): Configuracion = Configuracion(
    id = id,
    usuarioId = usuarioId,
    temaOscuro = temaOscuro,
    idioma = idioma,
    unidadesMetricas = unidadesMetricas,
    ultimaSincronizacion = ultimaSincronizacion
)

fun Configuracion.toEntity(): ConfiguracionEntity = ConfiguracionEntity(
    id = id,
    usuarioId = usuarioId,
    temaOscuro = temaOscuro,
    idioma = idioma,
    unidadesMetricas = unidadesMetricas,
    ultimaSincronizacion = ultimaSincronizacion
)

fun MedicionWithResultados.toDomain(): MedicionConResultados = MedicionConResultados(
    medicion = medicion.toDomain(),
    resultados = resultados.map { it.toDomain() }
)

fun SensorReadingEntity.toDomain(): SensorReading = SensorReading(
    id = id,
    sensorType = runCatching { SensorType.valueOf(sensorType) }
        .getOrDefault(SensorType.ACCELEROMETER),
    timestamp = timestamp,
    valueX = valueX,
    valueY = valueY,
    valueZ = valueZ,
    valueScalar = valueScalar,
    altitude = altitude,
    accuracy = accuracy,
    speed = speed,
    unit = unit,
    metadata = metadata
)

fun SensorReading.toEntity(usuarioId: Long): SensorReadingEntity = SensorReadingEntity(
    id = id,
    usuarioId = usuarioId,
    sensorType = sensorType.name,
    timestamp = timestamp,
    valueX = valueX,
    valueY = valueY,
    valueZ = valueZ,
    valueScalar = valueScalar,
    altitude = altitude,
    accuracy = accuracy,
    speed = speed,
    unit = unit,
    metadata = metadata
)

fun DocumentoEntity.toDomain(): Documento = Documento(
    id = id,
    usuarioId = usuarioId,
    nombre = nombre,
    nombreArchivo = nombreArchivo,
    rutaArchivo = rutaArchivo,
    extension = extension,
    tamano = tamano,
    numeroPaginas = numeroPaginas,
    fechaImportacion = fechaImportacion,
    estadoProcesamiento = estadoProcesamiento
)

fun Documento.toEntity(): DocumentoEntity = DocumentoEntity(
    id = id,
    usuarioId = usuarioId,
    nombre = nombre,
    nombreArchivo = nombreArchivo,
    rutaArchivo = rutaArchivo,
    extension = extension,
    tamano = tamano,
    numeroPaginas = numeroPaginas,
    fechaImportacion = fechaImportacion,
    estadoProcesamiento = estadoProcesamiento
)

fun SeccionDocumentoEntity.toDomain(): SeccionDocumento = SeccionDocumento(
    id = id,
    documentoId = documentoId,
    numeroPagina = numeroPagina,
    titulo = titulo,
    contenido = contenido,
    orden = orden
)

fun SeccionDocumento.toEntity(): SeccionDocumentoEntity = SeccionDocumentoEntity(
    id = id,
    documentoId = documentoId,
    numeroPagina = numeroPagina,
    titulo = titulo,
    contenido = contenido,
    orden = orden
)

fun PreguntaEstudioEntity.toDomain(): PreguntaEstudio = PreguntaEstudio(
    id = id,
    documentoId = documentoId,
    pregunta = pregunta,
    respuesta = respuesta,
    explicacion = explicacion,
    dificultad = dificultad,
    fechaCreacion = fechaCreacion
)

fun PreguntaEstudio.toEntity(): PreguntaEstudioEntity = PreguntaEstudioEntity(
    id = id,
    documentoId = documentoId,
    pregunta = pregunta,
    respuesta = respuesta,
    explicacion = explicacion,
    dificultad = dificultad,
    fechaCreacion = fechaCreacion
)

fun EjercicioEstudioEntity.toDomain(): EjercicioEstudio = EjercicioEstudio(
    id = id,
    documentoId = documentoId,
    enunciado = enunciado,
    datos = datos,
    formula = formula,
    solucion = solucion,
    explicacion = explicacion,
    dificultad = dificultad,
    fechaCreacion = fechaCreacion
)

fun EjercicioEstudio.toEntity(): EjercicioEstudioEntity = EjercicioEstudioEntity(
    id = id,
    documentoId = documentoId,
    enunciado = enunciado,
    datos = datos,
    formula = formula,
    solucion = solucion,
    explicacion = explicacion,
    dificultad = dificultad,
    fechaCreacion = fechaCreacion
)

fun TarjetaEstudioEntity.toDomain(): TarjetaEstudio = TarjetaEstudio(
    id = id,
    documentoId = documentoId,
    frente = frente,
    reverso = reverso,
    dificultad = dificultad,
    estado = estado,
    fechaCreacion = fechaCreacion
)

fun TarjetaEstudio.toEntity(): TarjetaEstudioEntity = TarjetaEstudioEntity(
    id = id,
    documentoId = documentoId,
    frente = frente,
    reverso = reverso,
    dificultad = dificultad,
    estado = estado,
    fechaCreacion = fechaCreacion
)

fun HistorialEstudioEntity.toDomain(): HistorialEstudio = HistorialEstudio(
    id = id,
    documentoId = documentoId,
    tipoActividad = tipoActividad,
    resultado = resultado,
    fecha = fecha,
    tiempoEstudio = tiempoEstudio
)

fun HistorialEstudio.toEntity(): HistorialEstudioEntity = HistorialEstudioEntity(
    id = id,
    documentoId = documentoId,
    tipoActividad = tipoActividad,
    resultado = resultado,
    fecha = fecha,
    tiempoEstudio = tiempoEstudio
)
