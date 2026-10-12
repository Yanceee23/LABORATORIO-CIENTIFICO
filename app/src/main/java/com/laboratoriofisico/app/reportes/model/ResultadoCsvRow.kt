package com.laboratoriofisico.app.reportes.model

data class ResultadoCsvRow(
    val experimento: String,
    val tipo: String,
    val valor: String,
    val unidad: String,
    val fecha: String,
    val hora: String
)

fun ResultadoExportData.toCsvRow(): ResultadoCsvRow {
    val date = java.util.Date(resultado.timestamp)
    val fecha = ResultadoCsvFormats.date.format(date)
    val hora = ResultadoCsvFormats.time.format(date)
    return ResultadoCsvRow(
        experimento = experimento.titulo,
        tipo = medicion.nombre,
        valor = formatCsvNumber(resultado.valorCalculado),
        unidad = resultado.unidad,
        fecha = fecha,
        hora = hora
    )
}

private object ResultadoCsvFormats {
    val date = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
    val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
}

private fun formatCsvNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else "%.4f".format(value).trimEnd('0').trimEnd('.')
