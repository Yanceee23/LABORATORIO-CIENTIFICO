package com.laboratoriofisico.app.core.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.laboratoriofisico.app.R
import com.laboratoriofisico.app.backend.auth.RegisteredUserGuard
import com.laboratoriofisico.app.backend.model.TipoAnalisisMicroscopio
import com.laboratoriofisico.app.backend.session.AppSession
import com.laboratoriofisico.app.core.di.AppContainer
import com.laboratoriofisico.app.frontend.ui.components.CuentaRequeridaCard
import com.laboratoriofisico.app.frontend.ui.components.LabBottomNavigation
import com.laboratoriofisico.app.frontend.ui.components.LabGlobalHeader
import com.laboratoriofisico.app.frontend.ui.components.LabScreenTitle
import com.laboratoriofisico.app.frontend.ui.screens.CapturaMagnetometroScreen
import com.laboratoriofisico.app.frontend.ui.screens.EstablecerPasswordScreen
import com.laboratoriofisico.app.frontend.ui.screens.ExperimentoDetailScreen
import com.laboratoriofisico.app.frontend.ui.screens.ExperimentoListScreen
import com.laboratoriofisico.app.frontend.ui.screens.LoginScreen
import com.laboratoriofisico.app.frontend.ui.screens.MedicionListScreen
import com.laboratoriofisico.app.frontend.ui.screens.RegistroScreen
import com.laboratoriofisico.app.frontend.ui.screens.SensorDiagnosticScreen
import com.laboratoriofisico.app.frontend.ui.screens.UsuarioScreen
import com.laboratoriofisico.app.frontend.ui.screens.WelcomeScreen
import com.laboratoriofisico.app.frontend.ui.screens.ajustes.AcercaDeScreen
import com.laboratoriofisico.app.frontend.ui.screens.ajustes.AjustesScreen
import com.laboratoriofisico.app.frontend.ui.screens.documentos.DocumentosListScreen
import com.laboratoriofisico.app.frontend.ui.screens.documentos.ImportarPdfScreen
import com.laboratoriofisico.app.frontend.ui.screens.documentos.PdfViewerScreen
import com.laboratoriofisico.app.frontend.ui.screens.estudio.EstudioHubScreen
import com.laboratoriofisico.app.frontend.ui.screens.experimentos.CameraExperimentScreen
import com.laboratoriofisico.app.frontend.ui.screens.experimentos.ExperimentosCatalogScreen
import com.laboratoriofisico.app.frontend.ui.screens.experimentos.LaboratorioHubScreen
import com.laboratoriofisico.app.frontend.ui.screens.experimentos.LaboratorioMagneticoScreen
import com.laboratoriofisico.app.frontend.ui.screens.experimentos.MedirHubScreen
import com.laboratoriofisico.app.frontend.ui.screens.experimentos.MicroscopioAnalisisScreen
import com.laboratoriofisico.app.frontend.ui.screens.experimentos.ObjectTrackingScreen
import com.laboratoriofisico.app.frontend.ui.screens.experimentos.SensorMedicionScreen
import com.laboratoriofisico.app.frontend.ui.screens.experimentos.SensorPhysicsExperimentScreen
import com.laboratoriofisico.app.frontend.ui.screens.inicio.HomeScreen
import com.laboratoriofisico.app.frontend.ui.screens.resultados.ResultadoDetailScreen
import com.laboratoriofisico.app.frontend.ui.screens.resultados.ResultadosListScreen
import com.laboratoriofisico.app.frontend.ui.screens.tutoria.AprenderMateriaScreen
import com.laboratoriofisico.app.frontend.ui.screens.tutoria.MisEjerciciosScreen
import com.laboratoriofisico.app.frontend.ui.screens.tutoria.ResolverEjercicioScreen
import com.laboratoriofisico.app.frontend.ui.screens.tutoria.TutorHubScreen
import com.laboratoriofisico.app.frontend.viewmodel.CameraExperimentViewModel
import com.laboratoriofisico.app.frontend.viewmodel.CapturaMagnetometroViewModel
import com.laboratoriofisico.app.frontend.viewmodel.ConfiguracionViewModel
import com.laboratoriofisico.app.frontend.viewmodel.DocumentosListViewModel
import com.laboratoriofisico.app.frontend.viewmodel.EstablecerPasswordViewModel
import com.laboratoriofisico.app.frontend.viewmodel.EstudioViewModel
import com.laboratoriofisico.app.frontend.viewmodel.ExperimentoDetailViewModel
import com.laboratoriofisico.app.frontend.viewmodel.ExperimentoListViewModel
import com.laboratoriofisico.app.frontend.viewmodel.ExperimentoSensorKind
import com.laboratoriofisico.app.frontend.viewmodel.ImportarPdfViewModel
import com.laboratoriofisico.app.frontend.viewmodel.LaboratorioMagneticoViewModel
import com.laboratoriofisico.app.frontend.viewmodel.LoginViewModel
import com.laboratoriofisico.app.frontend.viewmodel.MedicionViewModel
import com.laboratoriofisico.app.frontend.viewmodel.MedirHubViewModel
import com.laboratoriofisico.app.frontend.viewmodel.MicroscopioAnalisisViewModel
import com.laboratoriofisico.app.frontend.viewmodel.MisEjerciciosViewModel
import com.laboratoriofisico.app.frontend.viewmodel.ObjectTrackingViewModel
import com.laboratoriofisico.app.frontend.viewmodel.PdfViewerViewModel
import com.laboratoriofisico.app.frontend.viewmodel.RegistroViewModel
import com.laboratoriofisico.app.frontend.viewmodel.ResolverEjercicioViewModel
import com.laboratoriofisico.app.frontend.viewmodel.ResultadoDetailViewModel
import com.laboratoriofisico.app.frontend.viewmodel.ResultadosListViewModel
import com.laboratoriofisico.app.frontend.viewmodel.SensorDiagnosticViewModel
import com.laboratoriofisico.app.frontend.viewmodel.SensorMedicionViewModel
import com.laboratoriofisico.app.frontend.viewmodel.SensorPhysicsExperimentViewModel
import com.laboratoriofisico.app.frontend.viewmodel.TutorViewModel
import com.laboratoriofisico.app.frontend.viewmodel.UsuarioViewModel
import com.laboratoriofisico.app.frontend.viewmodel.WelcomeViewModel
import com.laboratoriofisico.app.experimentos.sensores.SensorPhysicsExperimentType
import kotlinx.coroutines.launch

private val bottomRoutes = setOf(
    NavRoutes.HOME,
    NavRoutes.EXPERIMENTOS_HUB,
    NavRoutes.MEDIR_HUB,
    NavRoutes.LABORATORIO_HUB,
    NavRoutes.LABORATORIO_MAGNETICO
)

private val routesWithGlobalHeader = bottomRoutes + setOf(
    NavRoutes.EXPERIMENTOS,
    NavRoutes.DOCUMENTOS,
    NavRoutes.ESTUDIO,
    NavRoutes.TUTOR,
    NavRoutes.MEDIR_SONIDO,
    NavRoutes.MEDIR_FRECUENCIA,
    NavRoutes.ANALIZAR_ONDAS,
    NavRoutes.MEDIR_MOVIMIENTO,
    NavRoutes.MEDIR_DISTANCIA,
    NavRoutes.MEDIR_ACELERACION_OBJETO,
    NavRoutes.ESTUDIAR_CAIDA,
    NavRoutes.PENDULO,
    NavRoutes.ANALISIS_LUZ,
    NavRoutes.REFLEXION_LUZ,
    NavRoutes.RESULTADOS,
    NavRoutes.AJUSTES,
    NavRoutes.ACERCA_DE,
    NavRoutes.USUARIO,
    NavRoutes.SENSORES_DIAGNOSTICO,
    NavRoutes.IMPORTAR_PDF
)

@Composable
fun LaboratorioNavHost(
    appContainer: AppContainer,
    modifier: Modifier = Modifier
) {
    val session by appContainer.sessionRepository.session.collectAsStateWithLifecycle(AppSession.None)
    var isSessionLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(appContainer.sessionRepository) {
        appContainer.sessionRepository.session.collect {
            isSessionLoaded = true
        }
    }

    if (!isSessionLoaded) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    when (session) {
        is AppSession.None -> AuthNavHost(appContainer = appContainer, modifier = modifier)
        is AppSession.Guest,
        is AppSession.User -> MainNavHost(
            appContainer = appContainer,
            session = session,
            modifier = modifier
        )
    }
}

@Composable
private fun AuthNavHost(appContainer: AppContainer, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = NavRoutes.WELCOME, modifier = modifier) {
        composable(NavRoutes.WELCOME) {
            val vm: WelcomeViewModel = viewModel(factory = appContainer.welcomeViewModelFactory())
            WelcomeScreen(
                onLogin = { navController.navigate(NavRoutes.LOGIN) },
                onRegister = { navController.navigate(NavRoutes.REGISTRO) },
                onGuest = { vm.enterAsGuest() }
            )
        }
        composable(NavRoutes.LOGIN) {
            val vm: LoginViewModel = viewModel(factory = appContainer.loginViewModelFactory())
            LoginScreen(
                viewModel = vm,
                onLoginSuccess = {},
                onNavigateToRegistro = { navController.navigate(NavRoutes.REGISTRO) },
                onNeedsPasswordReset = { userId, email ->
                    navController.navigate("${NavRoutes.ESTABLECER_PASSWORD}/$userId?email=$email")
                }
            )
        }
        composable(NavRoutes.REGISTRO) {
            val vm: RegistroViewModel = viewModel(factory = appContainer.registroViewModelFactory())
            RegistroScreen(
                viewModel = vm,
                onRegistroSuccess = {},
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable(
            route = "${NavRoutes.ESTABLECER_PASSWORD}/{userId}?email={email}",
            arguments = listOf(
                navArgument("userId") { type = NavType.LongType },
                navArgument("email") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { entry ->
            val userId = entry.arguments?.getLong("userId") ?: return@composable
            val email = entry.arguments?.getString("email").orEmpty()
            val vm: EstablecerPasswordViewModel = viewModel(
                factory = appContainer.establecerPasswordViewModelFactory(userId)
            )
            EstablecerPasswordScreen(
                viewModel = vm,
                email = email,
                onSuccess = {},
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun MainNavHost(
    appContainer: AppContainer,
    session: AppSession,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route?.substringBefore("?")
    val showBottomBar = currentRoute in bottomRoutes
    val isDocumentoLectura = NavRoutes.documentoSubRoute(currentRoute)
    val showGlobalHeader = !isDocumentoLectura && (
        currentRoute == null || currentRoute in routesWithGlobalHeader ||
            currentRoute.startsWith("experimento/") ||
            currentRoute.startsWith("documentos/") ||
            currentRoute.startsWith("resultado/")
        )

    fun goToAuthWelcome() {
        scope.launch { appContainer.authRepository.logout() }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (showGlobalHeader) {
                LabGlobalHeader(
                    onNavigateToResultados = { navController.navigate(NavRoutes.RESULTADOS) },
                    onNavigateToAjustes = { navController.navigate(NavRoutes.AJUSTES) },
                    onNavigateToTutor = { navController.navigate(NavRoutes.TUTOR) },
                    onNavigateToDocumentos = { navController.navigate(NavRoutes.DOCUMENTOS) },
                    onNavigateToEstudio = { navController.navigate(NavRoutes.ESTUDIO) },
                    onLogoClick = {
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                LabBottomNavigation(
                    currentRoute = currentRoute,
                    onItemClick = { item ->
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(NavRoutes.HOME) {
                HomeScreen(
                    onNavigateToExperimentos = { navController.navigate(NavRoutes.EXPERIMENTOS_HUB) },
                    onNavigateToMedir = { navController.navigate(NavRoutes.MEDIR_HUB) },
                    onNavigateToLaboratorio = { navController.navigate(NavRoutes.LABORATORIO_HUB) },
                    onNavigateToMagnetico = { navController.navigate(NavRoutes.LABORATORIO_MAGNETICO) },
                    onNavigateToDocumentos = { navController.navigate(NavRoutes.DOCUMENTOS) },
                    onNavigateToEstudio = { navController.navigate(NavRoutes.ESTUDIO) },
                    onNavigateToTutor = { navController.navigate(NavRoutes.TUTOR) },
                    onLogout = { goToAuthWelcome() }
                )
            }

            composable(NavRoutes.EXPERIMENTOS_HUB) {
                ExperimentosCatalogScreen(
                    onNavigateBack = {
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onExperimentoClick = { tipo -> navController.navigate(experimentoRoute(tipo)) }
                )
            }

            composable(NavRoutes.EXPERIMENTOS) {
                val vm: ExperimentoListViewModel = viewModel(factory = appContainer.experimentoListViewModelFactory())
                ExperimentoListScreen(
                    viewModel = vm,
                    onExperimentoClick = { id -> navController.navigate(NavRoutes.experimentoDetalle(id)) }
                )
            }

            composable(NavRoutes.DOCUMENTOS) {
                if (!RegisteredUserGuard.canAccessDocumentos(session)) {
                    AccesoRestringidoContent(
                        mensaje = stringResource(R.string.rol_bloqueado_documentos),
                        onCrearCuenta = { goToAuthWelcome() }
                    )
                } else {
                    val vm: DocumentosListViewModel = viewModel(factory = appContainer.documentosListViewModelFactory())
                    DocumentosListScreen(
                        viewModel = vm,
                        onNavigateToImportar = { navController.navigate(NavRoutes.IMPORTAR_PDF) },
                        onDocumentoClick = { id -> navController.navigate(NavRoutes.pdfVisor(id)) },
                        onNavigateToBuscar = { id -> navController.navigate(NavRoutes.pdfBuscar(id)) }
                    )
                }
            }

            composable(NavRoutes.ESTUDIO) {
                if (!RegisteredUserGuard.canAccessEstudio(session)) {
                    AccesoRestringidoContent(
                        mensaje = stringResource(R.string.rol_bloqueado_estudio),
                        onCrearCuenta = { goToAuthWelcome() }
                    )
                } else {
                    val vm: EstudioViewModel = viewModel(factory = appContainer.estudioViewModelFactory())
                    EstudioHubScreen(viewModel = vm)
                }
            }

            composable(NavRoutes.TUTOR) {
                if (!RegisteredUserGuard.canAccessTutor(session)) {
                    AccesoRestringidoContent(
                        mensaje = stringResource(R.string.rol_bloqueado_tutor),
                        onCrearCuenta = { goToAuthWelcome() }
                    )
                } else {
                    val vm: TutorViewModel = viewModel(factory = appContainer.tutorViewModelFactory())
                    TutorHubScreen(
                        viewModel = vm,
                        onAprenderJugando = { navController.navigate(NavRoutes.TUTOR_APRENDER) },
                        onResolverConFoto = { navController.navigate(NavRoutes.TUTOR_RESOLVER_FOTO) },
                        onMisEjercicios = { navController.navigate(NavRoutes.TUTOR_MIS_EJERCICIOS) }
                    )
                }
            }

            composable(NavRoutes.TUTOR_APRENDER) {
                if (!RegisteredUserGuard.canAccessTutor(session)) {
                    AccesoRestringidoContent(
                        mensaje = stringResource(R.string.rol_bloqueado_tutor),
                        onCrearCuenta = { goToAuthWelcome() }
                    )
                } else {
                    AprenderMateriaScreen(onNavigateBack = { navController.popBackStack() })
                }
            }

            composable(NavRoutes.TUTOR_RESOLVER_FOTO) {
                if (!RegisteredUserGuard.canAccessTutor(session)) {
                    AccesoRestringidoContent(
                        mensaje = stringResource(R.string.rol_bloqueado_tutor),
                        onCrearCuenta = { goToAuthWelcome() }
                    )
                } else {
                    val vm: ResolverEjercicioViewModel = viewModel(
                        factory = appContainer.resolverEjercicioViewModelFactory(null)
                    )
                    ResolverEjercicioScreen(
                        viewModel = vm,
                        onNavigateBack = { navController.popBackStack() },
                        onAprenderJugando = { id ->
                            navController.navigate(NavRoutes.tutorJuego(id))
                        }
                    )
                }
            }

            composable(NavRoutes.TUTOR_MIS_EJERCICIOS) {
                if (!RegisteredUserGuard.canAccessTutor(session)) {
                    AccesoRestringidoContent(
                        mensaje = stringResource(R.string.rol_bloqueado_tutor),
                        onCrearCuenta = { goToAuthWelcome() }
                    )
                } else {
                    val vm: MisEjerciciosViewModel = viewModel(
                        factory = appContainer.misEjerciciosViewModelFactory()
                    )
                    MisEjerciciosScreen(
                        viewModel = vm,
                        onNavigateBack = { navController.popBackStack() },
                        onAbrirDetalle = { id ->
                            navController.navigate(NavRoutes.tutorEjercicioDetalle(id))
                        }
                    )
                }
            }

            composable(
                route = NavRoutes.TUTOR_EJERCICIO_DETALLE,
                arguments = listOf(navArgument("ejercicioId") { type = NavType.LongType })
            ) { entry ->
                if (!RegisteredUserGuard.canAccessTutor(session)) {
                    AccesoRestringidoContent(
                        mensaje = stringResource(R.string.rol_bloqueado_tutor),
                        onCrearCuenta = { goToAuthWelcome() }
                    )
                    return@composable
                }
                val id = entry.arguments?.getLong("ejercicioId") ?: return@composable
                val vm: ResolverEjercicioViewModel = viewModel(
                    factory = appContainer.resolverEjercicioViewModelFactory(id)
                )
                ResolverEjercicioScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onAprenderJugando = { eid ->
                        navController.navigate(NavRoutes.tutorJuego(eid))
                    }
                )
            }

            composable(
                route = NavRoutes.TUTOR_JUEGO,
                arguments = listOf(navArgument("ejercicioId") { type = NavType.LongType })
            ) { entry ->
                if (!RegisteredUserGuard.canAccessTutor(session)) {
                    AccesoRestringidoContent(
                        mensaje = stringResource(R.string.rol_bloqueado_tutor),
                        onCrearCuenta = { goToAuthWelcome() }
                    )
                    return@composable
                }
                val id = entry.arguments?.getLong("ejercicioId") ?: return@composable
                val vm: com.laboratoriofisico.app.frontend.viewmodel.JuegoEjercicioViewModel = viewModel(
                    factory = appContainer.juegoEjercicioViewModelFactory(id)
                )
                com.laboratoriofisico.app.frontend.ui.screens.tutoria.JuegoEjercicioScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onSiguienteEjercicio = {
                        navController.navigate(NavRoutes.TUTOR_MIS_EJERCICIOS) {
                            popUpTo(NavRoutes.TUTOR) { inclusive = false }
                        }
                    }
                )
            }

            composable(NavRoutes.IMPORTAR_PDF) {
                if (!RegisteredUserGuard.canAccessDocumentos(session)) {
                    AccesoRestringidoContent(
                        mensaje = stringResource(R.string.rol_bloqueado_documentos),
                        onCrearCuenta = { goToAuthWelcome() }
                    )
                } else {
                    val vm: ImportarPdfViewModel = viewModel(factory = appContainer.importarPdfViewModelFactory())
                    ImportarPdfScreen(
                        viewModel = vm,
                        onNavigateBack = { navController.popBackStack() },
                        onImportComplete = { id ->
                            navController.popBackStack()
                            navController.navigate(NavRoutes.pdfVisor(id))
                        }
                    )
                }
            }

            composable(
                route = NavRoutes.PDF_VISOR,
                arguments = listOf(navArgument("documentoId") { type = NavType.LongType })
            ) { entry ->
                if (!RegisteredUserGuard.canAccessDocumentos(session)) {
                    AccesoRestringidoContent(
                        mensaje = stringResource(R.string.rol_bloqueado_documentos),
                        onCrearCuenta = { goToAuthWelcome() }
                    )
                    return@composable
                }
                val id = entry.arguments?.getLong("documentoId") ?: return@composable
                val vm: PdfViewerViewModel = viewModel(factory = appContainer.pdfViewerViewModelFactory(id))
                PdfViewerScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = NavRoutes.PDF_BUSCAR,
                arguments = listOf(navArgument("documentoId") { type = NavType.LongType })
            ) { entry ->
                if (!RegisteredUserGuard.canAccessDocumentos(session)) {
                    AccesoRestringidoContent(
                        mensaje = stringResource(R.string.rol_bloqueado_documentos),
                        onCrearCuenta = { goToAuthWelcome() }
                    )
                    return@composable
                }
                val id = entry.arguments?.getLong("documentoId") ?: return@composable
                val vm: PdfViewerViewModel = viewModel(factory = appContainer.pdfViewerViewModelFactory(id))
                PdfViewerScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    abrirBusqueda = true
                )
            }

            composable(NavRoutes.MEDIR_HUB) {
                val vm: MedirHubViewModel = viewModel(factory = appContainer.medirHubViewModelFactory())
                MedirHubScreen(viewModel = vm, onNavigateBack = null)
            }

            composable(NavRoutes.LABORATORIO_HUB) {
                if (!RegisteredUserGuard.canAccessLaboratorio(session)) {
                    AccesoRestringidoContent(
                        mensaje = stringResource(R.string.rol_bloqueado_laboratorio),
                        onCrearCuenta = { goToAuthWelcome() }
                    )
                } else {
                    LaboratorioHubScreen(
                        onNavigateBack = null,
                        onExperimentoClick = { tipo -> navController.navigate(experimentoRoute(tipo)) }
                    )
                }
            }

            composable(NavRoutes.LABORATORIO_MAGNETICO) {
                val vm: LaboratorioMagneticoViewModel = viewModel(factory = appContainer.laboratorioMagneticoViewModelFactory())
                LaboratorioMagneticoScreen(viewModel = vm, onNavigateBack = null)
            }

            composable(NavRoutes.MEDIR_SONIDO) {
                sensorMedicionScreen(appContainer, ExperimentoSensorKind.SONIDO) { navController.popBackStack() }
            }
            composable(NavRoutes.MEDIR_FRECUENCIA) {
                if (!RegisteredUserGuard.canAccessLaboratorio(session)) {
                    AccesoRestringidoContent(
                        mensaje = stringResource(R.string.rol_bloqueado_laboratorio),
                        onCrearCuenta = { goToAuthWelcome() }
                    )
                } else {
                    sensorMedicionScreen(appContainer, ExperimentoSensorKind.FRECUENCIA) { navController.popBackStack() }
                }
            }
            composable(NavRoutes.ANALIZAR_ONDAS) {
                if (!RegisteredUserGuard.canAccessLaboratorio(session)) {
                    AccesoRestringidoContent(
                        mensaje = stringResource(R.string.rol_bloqueado_laboratorio),
                        onCrearCuenta = { goToAuthWelcome() }
                    )
                } else {
                    sensorMedicionScreen(appContainer, ExperimentoSensorKind.ONDAS) { navController.popBackStack() }
                }
            }
            composable(NavRoutes.MEDIR_MOVIMIENTO) {
                sensorMedicionScreen(appContainer, ExperimentoSensorKind.MOVIMIENTO) { navController.popBackStack() }
            }
            composable(NavRoutes.MEDIR_DISTANCIA) {
                sensorMedicionScreen(appContainer, ExperimentoSensorKind.DISTANCIA) { navController.popBackStack() }
            }
            composable(NavRoutes.MEDIR_ACELERACION_OBJETO) {
                sensorPhysicsExperimentScreen(
                    appContainer = appContainer,
                    type = SensorPhysicsExperimentType.OBJECT_ACCELERATION,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(NavRoutes.ESTUDIAR_CAIDA) {
                sensorPhysicsExperimentScreen(
                    appContainer = appContainer,
                    type = SensorPhysicsExperimentType.FREE_FALL,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(NavRoutes.PENDULO) {
                if (!RegisteredUserGuard.canAccessLaboratorio(session)) {
                    AccesoRestringidoContent(
                        mensaje = stringResource(R.string.rol_bloqueado_laboratorio),
                        onCrearCuenta = { goToAuthWelcome() }
                    )
                } else {
                    sensorPhysicsExperimentScreen(
                        appContainer = appContainer,
                        type = SensorPhysicsExperimentType.PENDULUM,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
            composable(NavRoutes.ANALISIS_LUZ) {
                sensorMedicionScreen(appContainer, ExperimentoSensorKind.LUZ) { navController.popBackStack() }
            }

            composable(NavRoutes.REFLEXION_LUZ) {
                if (!RegisteredUserGuard.canAccessLaboratorio(session)) {
                    AccesoRestringidoContent(
                        mensaje = stringResource(R.string.rol_bloqueado_laboratorio),
                        onCrearCuenta = { goToAuthWelcome() }
                    )
                } else {
                    sensorMedicionScreen(appContainer, ExperimentoSensorKind.REFLEXION) { navController.popBackStack() }
                }
            }

            composable(NavRoutes.EXPERIMENTO_CAMARA) {
                if (!RegisteredUserGuard.canAccessLaboratorio(session)) {
                    AccesoRestringidoContent(
                        mensaje = stringResource(R.string.rol_bloqueado_laboratorio),
                        onCrearCuenta = { goToAuthWelcome() }
                    )
                } else {
                    val vm: CameraExperimentViewModel = viewModel(
                        factory = appContainer.cameraExperimentViewModelFactory()
                    )
                    CameraExperimentScreen(
                        viewModel = vm,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }

            composable(NavRoutes.ANALISIS_OBJETOS) {
                val vm: ObjectTrackingViewModel = viewModel(
                    factory = appContainer.objectTrackingViewModelFactory()
                )
                ObjectTrackingScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = NavRoutes.MICROSCOPIO_ANALISIS,
                arguments = listOf(navArgument("tipo") { type = NavType.StringType })
            ) { entry ->
                if (!RegisteredUserGuard.canAccessLaboratorio(session)) {
                    AccesoRestringidoContent(
                        mensaje = stringResource(R.string.rol_bloqueado_laboratorio),
                        onCrearCuenta = { goToAuthWelcome() }
                    )
                    return@composable
                }
                val tipoKey = entry.arguments?.getString("tipo").orEmpty()
                val tipo = TipoAnalisisMicroscopio.entries.firstOrNull { it.key == tipoKey }
                    ?: TipoAnalisisMicroscopio.YOGUR
                val vm: MicroscopioAnalisisViewModel = viewModel(
                    factory = appContainer.microscopioAnalisisViewModelFactory(tipo)
                )
                MicroscopioAnalisisScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.RESULTADOS) {
                val vm: ResultadosListViewModel = viewModel(factory = appContainer.resultadosListViewModelFactory())
                ResultadosListScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onResultadoClick = { id -> navController.navigate(NavRoutes.resultadoDetalle(id)) }
                )
            }

            composable(
                route = NavRoutes.RESULTADO_DETALLE,
                arguments = listOf(navArgument("resultadoId") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("resultadoId") ?: return@composable
                val vm: ResultadoDetailViewModel = viewModel(factory = appContainer.resultadoDetailViewModelFactory(id))
                ResultadoDetailScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onAnalizarConIa = {
                        navController.navigate(NavRoutes.TUTOR) {
                            popUpTo(NavRoutes.HOME)
                        }
                    }
                )
            }

            composable(NavRoutes.AJUSTES) {
                val vm: ConfiguracionViewModel = viewModel(factory = appContainer.configuracionViewModelFactory())
                AjustesScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAcercaDe = { navController.navigate(NavRoutes.ACERCA_DE) },
                    onNavigateToSensores = { navController.navigate(NavRoutes.SENSORES_DIAGNOSTICO) },
                    onNavigateToUsuario = { navController.navigate(NavRoutes.USUARIO) },
                    onLogout = { goToAuthWelcome() }
                )
            }

            composable(NavRoutes.ACERCA_DE) {
                AcercaDeScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(
                route = NavRoutes.EXPERIMENTO_DETALLE,
                arguments = listOf(navArgument("experimentoId") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("experimentoId") ?: return@composable
                val vm: ExperimentoDetailViewModel = viewModel(factory = appContainer.experimentoDetailViewModelFactory(id))
                ExperimentoDetailScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMediciones = { eid -> navController.navigate(NavRoutes.mediciones(eid)) },
                    onNavigateToCapturaMagnetometro = { eid -> navController.navigate(NavRoutes.capturaMagnetometro(eid)) }
                )
            }

            composable(
                route = NavRoutes.CAPTURA_MAGNETOMETRO,
                arguments = listOf(navArgument("experimentoId") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("experimentoId") ?: return@composable
                val vm: CapturaMagnetometroViewModel = viewModel(factory = appContainer.capturaMagnetometroViewModelFactory(id))
                CapturaMagnetometroScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
            }

            composable(
                route = NavRoutes.MEDICIONES,
                arguments = listOf(navArgument("experimentoId") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("experimentoId") ?: return@composable
                val vm: MedicionViewModel = viewModel(factory = appContainer.medicionViewModelFactory(id))
                MedicionListScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
            }

            composable(NavRoutes.USUARIO) {
                val vm: UsuarioViewModel = viewModel(factory = appContainer.usuarioViewModelFactory())
                UsuarioScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onLogout = { goToAuthWelcome() }
                )
            }

            composable(NavRoutes.SENSORES_DIAGNOSTICO) {
                val vm: SensorDiagnosticViewModel = viewModel(factory = appContainer.sensorDiagnosticViewModelFactory())
                SensorDiagnosticScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun AccesoRestringidoContent(
    mensaje: String,
    onCrearCuenta: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LabScreenTitle(
            title = stringResource(R.string.rol_acceso_restringido_titulo),
            subtitle = null
        )
        CuentaRequeridaCard(
            onCrearCuentaOIniciar = onCrearCuenta,
            mensaje = mensaje
        )
    }
}

@Composable
private fun sensorMedicionScreen(
    appContainer: AppContainer,
    kind: ExperimentoSensorKind,
    onNavigateBack: () -> Unit
) {
    val vm: SensorMedicionViewModel = viewModel(
        factory = appContainer.sensorMedicionViewModelFactory(kind)
    )
    SensorMedicionScreen(viewModel = vm, onNavigateBack = onNavigateBack)
}

private fun experimentoRoute(tipo: String): String = when (tipo) {
    "sonido" -> NavRoutes.MEDIR_SONIDO
    "frecuencia" -> NavRoutes.MEDIR_FRECUENCIA
    "ondas" -> NavRoutes.ANALIZAR_ONDAS
    "movimiento" -> NavRoutes.MEDIR_MOVIMIENTO
    "distancia" -> NavRoutes.MEDIR_DISTANCIA
    "aceleracion-objeto" -> NavRoutes.MEDIR_ACELERACION_OBJETO
    "caida" -> NavRoutes.ESTUDIAR_CAIDA
    "pendulo" -> NavRoutes.PENDULO
    "luz" -> NavRoutes.ANALISIS_LUZ
    "reflexion-luz" -> NavRoutes.REFLEXION_LUZ
    "camara" -> NavRoutes.EXPERIMENTO_CAMARA
    "objetos" -> NavRoutes.ANALISIS_OBJETOS
    "microscopio-yogur" -> NavRoutes.microscopioAnalisis(TipoAnalisisMicroscopio.YOGUR.key)
    "microscopio-piel" -> NavRoutes.microscopioAnalisis(TipoAnalisisMicroscopio.PIEL.key)
    "microscopio-hoja" -> NavRoutes.microscopioAnalisis(TipoAnalisisMicroscopio.HOJA.key)
    "microscopio-sangre" -> NavRoutes.microscopioAnalisis(TipoAnalisisMicroscopio.SANGRE.key)
    else -> NavRoutes.MEDIR_HUB
}

@Composable
private fun sensorPhysicsExperimentScreen(
    appContainer: AppContainer,
    type: SensorPhysicsExperimentType,
    onNavigateBack: () -> Unit
) {
    val vm: SensorPhysicsExperimentViewModel = viewModel(
        factory = appContainer.sensorPhysicsExperimentViewModelFactory()
    )
    SensorPhysicsExperimentScreen(
        viewModel = vm,
        type = type,
        onNavigateBack = onNavigateBack
    )
}
