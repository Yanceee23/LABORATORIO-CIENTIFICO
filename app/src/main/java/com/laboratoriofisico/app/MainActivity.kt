package com.laboratoriofisico.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import com.laboratoriofisico.app.configuracion.ConfiguracionDefaults
import com.laboratoriofisico.app.core.navigation.LaboratorioNavHost
import com.laboratoriofisico.app.core.theme.LaboratorioFisicoTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as LaboratorioApp).container

        setContent {
            val configuracion by appContainer.configuracionRepository.observe()
                .collectAsState(initial = ConfiguracionDefaults.crear())

            LaboratorioFisicoTheme(
                darkTheme = configuracion.temaOscuro
            ) {
                LaboratorioNavHost(appContainer = appContainer)
            }
        }
    }
}
