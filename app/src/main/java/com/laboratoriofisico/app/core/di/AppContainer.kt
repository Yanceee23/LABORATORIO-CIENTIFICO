package com.laboratoriofisico.app.core.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.laboratoriofisico.app.backend.auth.FirebaseCuentas
import com.laboratoriofisico.app.backend.auth.RegisteredUserGuard
import com.laboratoriofisico.app.backend.local.database.LaboratorioDatabase
import com.laboratoriofisico.app.backend.model.TipoAnalisisMicroscopio
import com.laboratoriofisico.app.backend.repository.AnalisisMicroscopioRepository
import com.laboratoriofisico.app.backend.repository.AuthRepository
import com.laboratoriofisico.app.backend.repository.CapturaMagnetometroRepository
import com.laboratoriofisico.app.backend.repository.ConfiguracionRepository
import com.laboratoriofisico.app.backend.repository.ConversacionIARepository
import com.laboratoriofisico.app.backend.repository.DocumentoRepository
import com.laboratoriofisico.app.backend.repository.EjercicioEstudioRepository
import com.laboratoriofisico.app.backend.repository.EjercicioResueltoRepository
import com.laboratoriofisico.app.backend.repository.ExperimentoRepository
import com.laboratoriofisico.app.backend.repository.HistorialEstudioRepository
import com.laboratoriofisico.app.backend.repository.MedicionRepository
import com.laboratoriofisico.app.backend.repository.PreguntaEstudioRepository
import com.laboratoriofisico.app.backend.repository.ResultadoRepository
import com.laboratoriofisico.app.backend.repository.MedicionSensorRepository
import com.laboratoriofisico.app.backend.repository.ObjectTrackingMeasurementRepository
import com.laboratoriofisico.app.backend.repository.SeedDataRepository
import com.laboratoriofisico.app.backend.repository.SeccionDocumentoRepository
import com.laboratoriofisico.app.backend.repository.SensorPhysicsExperimentRepository
import com.laboratoriofisico.app.backend.repository.SensorReadingRepository
import com.laboratoriofisico.app.backend.repository.TarjetaEstudioRepository
import com.laboratoriofisico.app.backend.repository.UbicacionRepository
import com.laboratoriofisico.app.backend.repository.UsuarioRepository
import com.laboratoriofisico.app.backend.session.SessionRepository
import com.laboratoriofisico.app.estudio.EstudioContentService
import com.laboratoriofisico.app.estudio.EstudioTextoLocalRepository
import com.laboratoriofisico.app.estudio.GeminiEstudioContentService
import com.laboratoriofisico.app.fisica.DefaultFisicaEngine
import com.laboratoriofisico.app.fisica.FisicaEngine
import com.laboratoriofisico.app.frontend.viewmodel.BuscarDocumentoViewModel
import com.laboratoriofisico.app.frontend.viewmodel.CameraExperimentViewModel
import com.laboratoriofisico.app.frontend.viewmodel.CapturaMagnetometroViewModel
import com.laboratoriofisico.app.frontend.viewmodel.ConfiguracionViewModel
import com.laboratoriofisico.app.frontend.viewmodel.DocumentosListViewModel
import com.laboratoriofisico.app.frontend.viewmodel.EstudioViewModel
import com.laboratoriofisico.app.frontend.viewmodel.EstablecerPasswordViewModel
import com.laboratoriofisico.app.frontend.viewmodel.ExperimentoDetailViewModel
import com.laboratoriofisico.app.frontend.viewmodel.ExperimentoListViewModel
import com.laboratoriofisico.app.frontend.viewmodel.ExperimentoSensorKind
import com.laboratoriofisico.app.frontend.viewmodel.HomeViewModel
import com.laboratoriofisico.app.frontend.viewmodel.ImportarPdfViewModel
import com.laboratoriofisico.app.frontend.viewmodel.LaboratorioMagneticoViewModel
import com.laboratoriofisico.app.frontend.viewmodel.MedirHubViewModel
import com.laboratoriofisico.app.frontend.viewmodel.MicroscopioAnalisisViewModel
import com.laboratoriofisico.app.frontend.viewmodel.MisEjerciciosViewModel
import com.laboratoriofisico.app.frontend.viewmodel.ObjectTrackingViewModel
import com.laboratoriofisico.app.frontend.viewmodel.LoginViewModel
import com.laboratoriofisico.app.frontend.viewmodel.MedicionViewModel
import com.laboratoriofisico.app.frontend.viewmodel.PdfViewerViewModel
import com.laboratoriofisico.app.frontend.viewmodel.RegistroViewModel
import com.laboratoriofisico.app.frontend.viewmodel.ResolverEjercicioViewModel
import com.laboratoriofisico.app.frontend.viewmodel.ResultadoDetailViewModel
import com.laboratoriofisico.app.frontend.viewmodel.ResultadosListViewModel
import com.laboratoriofisico.app.frontend.viewmodel.SensorDiagnosticViewModel
import com.laboratoriofisico.app.frontend.viewmodel.SensorPhysicsExperimentViewModel
import com.laboratoriofisico.app.frontend.viewmodel.SensorMedicionViewModel
import com.laboratoriofisico.app.frontend.viewmodel.TutorViewModel
import com.laboratoriofisico.app.frontend.viewmodel.UsuarioViewModel
import com.laboratoriofisico.app.frontend.viewmodel.WelcomeViewModel
import com.laboratoriofisico.app.ia.IaService
import com.laboratoriofisico.app.ia.gemini.DocumentoContextoHelper
import com.laboratoriofisico.app.ia.gemini.GeminiClient
import com.laboratoriofisico.app.ia.gemini.GeminiIaService
import com.laboratoriofisico.app.tutor.ejercicio.EjercicioLocalVerifier
import com.laboratoriofisico.app.tutor.ejercicio.ResolverEjercicioService
import com.laboratoriofisico.app.reportes.RealReporteService
import com.laboratoriofisico.app.reportes.ReporteDataRepository
import com.laboratoriofisico.app.reportes.ReporteService
import com.laboratoriofisico.app.usb.UvcMicroscopeManager
import com.laboratoriofisico.app.pdf.PdfProcessingService
import com.laboratoriofisico.app.pdf.RealPdfProcessingService
import com.laboratoriofisico.app.mcp.McpClient
import com.laboratoriofisico.app.mcp.McpComponents
import com.laboratoriofisico.app.mcp.McpIaService
import com.laboratoriofisico.app.mcp.McpServerConfig
import com.laboratoriofisico.app.mcp.McpToolsFactory
import com.laboratoriofisico.app.sensores.LaboratorioSensorManager
import com.laboratoriofisico.app.sensores.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val database: LaboratorioDatabase = LaboratorioDatabase.create(appContext)

    val sessionRepository = SessionRepository(appContext)
    val authRepository = AuthRepository(
        database.usuarioDao(),
        sessionRepository,
        FirebaseCuentas()
    )
    val registeredUserGuard = RegisteredUserGuard(sessionRepository)

    val usuarioRepository = UsuarioRepository(database.usuarioDao(), sessionRepository)
    val ubicacionRepository = UbicacionRepository(database.ubicacionDao())
    val medicionRepository = MedicionRepository(database.medicionDao())
    val resultadoRepository = ResultadoRepository(database.resultadoDao())
    val experimentoRepository = ExperimentoRepository(
        database.experimentoDao(),
        medicionRepository
    )
    val configuracionRepository = ConfiguracionRepository(database.configuracionDao(), sessionRepository)
    val sensorReadingRepository = SensorReadingRepository(database.sensorReadingDao(), sessionRepository)
    val sensorPhysicsExperimentRepository = SensorPhysicsExperimentRepository(
        experimentoDao = database.experimentoDao(),
        experimentoRepository = experimentoRepository,
        medicionRepository = medicionRepository,
        resultadoRepository = resultadoRepository,
        sensorReadingRepository = sensorReadingRepository,
        sessionRepository = sessionRepository
    )
    val objectTrackingMeasurementRepository = ObjectTrackingMeasurementRepository(
        sessionRepository = sessionRepository,
        experimentoDao = database.experimentoDao(),
        experimentoRepository = experimentoRepository,
        medicionRepository = medicionRepository,
        resultadoRepository = resultadoRepository
    )
    val documentoRepository = DocumentoRepository(database.documentoDao())
    val seccionDocumentoRepository = SeccionDocumentoRepository(database.seccionDocumentoDao())
    val preguntaEstudioRepository = PreguntaEstudioRepository(database.preguntaEstudioDao())
    val ejercicioEstudioRepository = EjercicioEstudioRepository(database.ejercicioEstudioDao())
    val tarjetaEstudioRepository = TarjetaEstudioRepository(database.tarjetaEstudioDao())
    val historialEstudioRepository = HistorialEstudioRepository(database.historialEstudioDao())
    val conversacionIARepository = ConversacionIARepository(database.conversacionIADao())
    val analisisMicroscopioRepository = AnalisisMicroscopioRepository(
        analisisDao = database.analisisMicroscopioDao(),
        experimentoDao = database.experimentoDao(),
        sessionRepository = sessionRepository
    )
    val ejercicioResueltoRepository = EjercicioResueltoRepository(
        dao = database.ejercicioResueltoDao(),
        progresoDao = database.juegoEjercicioProgresoDao(),
        sessionRepository = sessionRepository
    )

    val sensorManager: SensorManager = LaboratorioSensorManager(appContext, applicationScope)
    val fisicaEngine: FisicaEngine = DefaultFisicaEngine()
    val geminiClient = GeminiClient()
    val ejercicioLocalVerifier = EjercicioLocalVerifier(fisicaEngine)
    val resolverEjercicioService = ResolverEjercicioService(
        geminiClient = geminiClient,
        repository = ejercicioResueltoRepository,
        sessionRepository = sessionRepository,
        verifier = ejercicioLocalVerifier
    )
    val uvcMicroscopeManager = UvcMicroscopeManager(appContext)
    val capturaMagnetometroRepository = CapturaMagnetometroRepository(
        medicionRepository = medicionRepository,
        resultadoRepository = resultadoRepository,
        fisicaEngine = fisicaEngine,
        sessionRepository = sessionRepository
    )
    val medicionSensorRepository = MedicionSensorRepository(
        sessionRepository = sessionRepository,
        experimentoDao = database.experimentoDao(),
        experimentoRepository = experimentoRepository,
        medicionRepository = medicionRepository,
        resultadoRepository = resultadoRepository,
        fisicaEngine = fisicaEngine
    )

    val pdfProcessingService: PdfProcessingService = RealPdfProcessingService(
        context = appContext,
        documentoRepository = documentoRepository,
        seccionDocumentoRepository = seccionDocumentoRepository
    )

    val iaService: IaService
    val estudioContentService: EstudioContentService
    val estudioTextoLocalRepository: EstudioTextoLocalRepository
    val reporteService: ReporteService
    val mcpClient: McpClient
    private val mcpComponents: McpComponents

    init {
        val documentoContextoHelper = DocumentoContextoHelper(
            seccionDocumentoRepository = seccionDocumentoRepository,
            documentoRepository = documentoRepository
        )
        val geminiIaService = GeminiIaService(
            geminiClient = geminiClient,
            resultadoRepository = resultadoRepository,
            medicionRepository = medicionRepository,
            documentoContextoHelper = documentoContextoHelper
        )
        estudioContentService = GeminiEstudioContentService(
            geminiClient = geminiClient,
            documentoContextoHelper = documentoContextoHelper,
            preguntaEstudioRepository = preguntaEstudioRepository,
            ejercicioEstudioRepository = ejercicioEstudioRepository,
            tarjetaEstudioRepository = tarjetaEstudioRepository
        )
        mcpComponents = McpToolsFactory.build(
            sessionRepository = sessionRepository,
            documentoRepository = documentoRepository,
            seccionDocumentoRepository = seccionDocumentoRepository,
            pdfProcessingService = pdfProcessingService,
            documentoContextoHelper = documentoContextoHelper,
            fisicaEngine = fisicaEngine,
            estudioContentService = estudioContentService,
            resultadoRepository = resultadoRepository,
            medicionRepository = medicionRepository,
            iaService = geminiIaService
        )
        mcpClient = mcpComponents.client
        iaService = McpIaService(
            delegate = geminiIaService,
            preguntaSobrePdf = mcpComponents.preguntaSobrePdf,
            resolverEjercicio = mcpComponents.resolverEjercicio,
            analizarExperimento = mcpComponents.analizarExperimento,
            mcpClient = mcpClient
        )
        estudioTextoLocalRepository = EstudioTextoLocalRepository(appContext)
        reporteService = RealReporteService(
            context = appContext,
            reporteDataRepository = ReporteDataRepository(
                resultadoRepository = resultadoRepository,
                medicionRepository = medicionRepository,
                experimentoRepository = experimentoRepository,
                usuarioRepository = usuarioRepository,
                ubicacionRepository = ubicacionRepository,
                documentoRepository = documentoRepository,
                historialEstudioRepository = historialEstudioRepository
            )
        )
    }

    private val seedDataRepository = SeedDataRepository(
        database = database,
        usuarioDao = database.usuarioDao(),
        ubicacionDao = database.ubicacionDao(),
        experimentoDao = database.experimentoDao(),
        medicionDao = database.medicionDao(),
        resultadoDao = database.resultadoDao(),
        configuracionDao = database.configuracionDao(),
        documentoDao = database.documentoDao(),
        seccionDocumentoDao = database.seccionDocumentoDao()
    )

    suspend fun initialize() {
        seedDataRepository.seedIfEmpty()
        mcpClient.conectar(McpServerConfig.LOCAL_ENDPOINT)
    }

    fun loginViewModelFactory(): ViewModelProvider.Factory =
        simpleFactory { LoginViewModel(authRepository) }

    fun welcomeViewModelFactory(): ViewModelProvider.Factory =
        simpleFactory { WelcomeViewModel(authRepository) }

    fun establecerPasswordViewModelFactory(userId: Long): ViewModelProvider.Factory =
        simpleFactory { EstablecerPasswordViewModel(authRepository, userId) }

    fun registroViewModelFactory(): ViewModelProvider.Factory =
        simpleFactory { RegistroViewModel(authRepository) }

    fun homeViewModelFactory(): ViewModelProvider.Factory =
        simpleFactory {
            HomeViewModel(
                experimentoRepository = experimentoRepository,
                documentoRepository = documentoRepository,
                usuarioRepository = usuarioRepository,
                sessionRepository = sessionRepository
            )
        }

    fun experimentoListViewModelFactory(): ViewModelProvider.Factory =
        simpleFactory {
            ExperimentoListViewModel(
                experimentoRepository = experimentoRepository,
                sessionRepository = sessionRepository
            )
        }

    fun experimentoDetailViewModelFactory(experimentoId: Long): ViewModelProvider.Factory =
        simpleFactory {
            ExperimentoDetailViewModel(
                experimentoId = experimentoId,
                experimentoRepository = experimentoRepository,
                ubicacionRepository = ubicacionRepository,
                resultadoRepository = resultadoRepository,
                reporteService = reporteService,
                sessionRepository = sessionRepository
            )
        }

    fun medicionViewModelFactory(experimentoId: Long): ViewModelProvider.Factory =
        simpleFactory {
            MedicionViewModel(
                experimentoId = experimentoId,
                medicionRepository = medicionRepository,
                sessionRepository = sessionRepository
            )
        }

    fun usuarioViewModelFactory(): ViewModelProvider.Factory =
        simpleFactory { UsuarioViewModel(usuarioRepository, authRepository) }

    fun configuracionViewModelFactory(): ViewModelProvider.Factory =
        simpleFactory { ConfiguracionViewModel(configuracionRepository) }

    fun sensorDiagnosticViewModelFactory(): ViewModelProvider.Factory =
        simpleFactory {
            SensorDiagnosticViewModel(
                sensorManager = sensorManager,
                sensorReadingRepository = sensorReadingRepository
            )
        }

    fun capturaMagnetometroViewModelFactory(experimentoId: Long): ViewModelProvider.Factory =
        simpleFactory {
            CapturaMagnetometroViewModel(
                experimentoId = experimentoId,
                sensorManager = sensorManager,
                capturaMagnetometroRepository = capturaMagnetometroRepository
            )
        }

    fun cameraExperimentViewModelFactory(): ViewModelProvider.Factory =
        simpleFactory {
            CameraExperimentViewModel(
                sensorManager = sensorManager,
                appContext = appContext
            )
        }

    fun microscopioAnalisisViewModelFactory(tipo: TipoAnalisisMicroscopio): ViewModelProvider.Factory =
        simpleFactory {
            MicroscopioAnalisisViewModel(
                tipo = tipo,
                appContext = appContext,
                analisisRepository = analisisMicroscopioRepository,
                geminiClient = geminiClient,
                uvcMicroscopeManager = uvcMicroscopeManager,
                medicionRepository = medicionRepository,
                resultadoRepository = resultadoRepository,
                sessionRepository = sessionRepository
            )
        }

    fun objectTrackingViewModelFactory(): ViewModelProvider.Factory =
        simpleFactory {
            ObjectTrackingViewModel(
                sensorManager = sensorManager,
                measurementRepository = objectTrackingMeasurementRepository,
                appContext = appContext,
                geminiClient = geminiClient
            )
        }

    fun medirHubViewModelFactory(): ViewModelProvider.Factory =
        simpleFactory {
            MedirHubViewModel(
                sensorManager = sensorManager,
                medicionRepository = medicionRepository,
                sessionRepository = sessionRepository
            )
        }

    fun laboratorioMagneticoViewModelFactory(): ViewModelProvider.Factory =
        simpleFactory {
            LaboratorioMagneticoViewModel(
                sensorManager = sensorManager,
                fisicaEngine = fisicaEngine
            )
        }

    fun sensorMedicionViewModelFactory(kind: ExperimentoSensorKind): ViewModelProvider.Factory =
        simpleFactory {
            SensorMedicionViewModel(
                sensorManager = sensorManager,
                fisicaEngine = fisicaEngine,
                medicionSensorRepository = medicionSensorRepository,
                kind = kind
            )
        }

    fun sensorPhysicsExperimentViewModelFactory(): ViewModelProvider.Factory =
        simpleFactory {
            SensorPhysicsExperimentViewModel(
                sensorManager = sensorManager,
                repository = sensorPhysicsExperimentRepository,
                sessionRepository = sessionRepository
            )
        }

    fun resultadosListViewModelFactory(): ViewModelProvider.Factory =
        simpleFactory {
            ResultadosListViewModel(
                resultadoRepository = resultadoRepository,
                reporteService = reporteService,
                sessionRepository = sessionRepository
            )
        }

    fun resultadoDetailViewModelFactory(resultadoId: Long): ViewModelProvider.Factory =
        simpleFactory {
            ResultadoDetailViewModel(
                resultadoId = resultadoId,
                resultadoRepository = resultadoRepository,
                reporteService = reporteService,
                sessionRepository = sessionRepository
            )
        }

    fun documentosListViewModelFactory(): ViewModelProvider.Factory =
        simpleFactory { DocumentosListViewModel(documentoRepository, sessionRepository) }

    fun importarPdfViewModelFactory(): ViewModelProvider.Factory =
        simpleFactory { ImportarPdfViewModel(pdfProcessingService, sessionRepository) }

    fun pdfViewerViewModelFactory(documentoId: Long): ViewModelProvider.Factory =
        simpleFactory {
            PdfViewerViewModel(documentoId, documentoRepository, pdfProcessingService)
        }

    fun buscarDocumentoViewModelFactory(documentoId: Long): ViewModelProvider.Factory =
        simpleFactory { BuscarDocumentoViewModel(documentoId, pdfProcessingService) }

    fun estudioViewModelFactory(): ViewModelProvider.Factory =
        simpleFactory {
            EstudioViewModel(
                estudioContentService = estudioContentService,
                documentoRepository = documentoRepository,
                preguntaEstudioRepository = preguntaEstudioRepository,
                ejercicioEstudioRepository = ejercicioEstudioRepository,
                tarjetaEstudioRepository = tarjetaEstudioRepository,
                historialEstudioRepository = historialEstudioRepository,
                estudioTextoLocalRepository = estudioTextoLocalRepository,
                reporteService = reporteService,
                sessionRepository = sessionRepository
            )
        }

    fun tutorViewModelFactory(): ViewModelProvider.Factory =
        simpleFactory {
            TutorViewModel(
                iaService = iaService,
                conversacionIARepository = conversacionIARepository,
                documentoRepository = documentoRepository,
                resultadoRepository = resultadoRepository,
                sessionRepository = sessionRepository
            )
        }

    fun resolverEjercicioViewModelFactory(ejercicioId: Long?): ViewModelProvider.Factory =
        simpleFactory {
            ResolverEjercicioViewModel(
                appContext = appContext,
                service = resolverEjercicioService,
                ejercicioIdInicial = ejercicioId
            )
        }

    fun misEjerciciosViewModelFactory(): ViewModelProvider.Factory =
        simpleFactory { MisEjerciciosViewModel(ejercicioResueltoRepository) }

    fun juegoEjercicioViewModelFactory(ejercicioId: Long): ViewModelProvider.Factory =
        simpleFactory {
            com.laboratoriofisico.app.frontend.viewmodel.JuegoEjercicioViewModel(
                ejercicioId = ejercicioId,
                repository = ejercicioResueltoRepository,
                verifier = ejercicioLocalVerifier
            )
        }

    private fun <T : ViewModel> simpleFactory(creator: () -> T): ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <VM : ViewModel> create(modelClass: Class<VM>): VM {
                return creator() as VM
            }
        }
}
