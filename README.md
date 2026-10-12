# Laboratorio Científico

App de Android para experimentos con los sensores del celular, documentos PDF y tutor con Gemini.

## Clonar y abrir

1. Instala [Android Studio](https://developer.android.com/studio) (trae JDK 17 y el Android SDK).
2. En una carpeta vacía, clona el repositorio:

```bash
git clone https://github.com/Yanceee23/LABORATORIO-CIENTIFICO.git
cd LABORATORIO-CIENTIFICO
```

3. Copia el ejemplo y pon tu clave:

```bash
# Windows (PowerShell)
Copy-Item local.properties.example local.properties
```

```bash
# macOS o Linux
cp local.properties.example local.properties
```

4. Abre `local.properties` y cambia esta línea. No compartas ese archivo: no está en Git.

```properties
GEMINI_API_KEY=PON TU CLAVE DE GEMINI AQUI
```

La clave se crea gratis en [Google AI Studio](https://aistudio.google.com/app/apikey). Cada persona usa la suya.

5. En Android Studio: **File → Open** y elige la carpeta `LABORATORIO-CIENTIFICO`.
6. Espera a que termine **Sync**. Si pide la ruta del SDK, Android Studio la escribe en `sdk.dir` dentro de `local.properties`.
7. Conecta un celular con depuración USB y pulsa **Run**.

Sin clave la app abre, pero Tutor y Estudio no responden hasta que pegues la tuya y vuelvas a compilar.

## Frontend

La interfaz está dentro de la app Android, hecha con Jetpack Compose. No hay otro proyecto web.

Carpeta:

`app/src/main/java/com/laboratoriofisico/app/frontend/`

| Carpeta | Qué es |
| --- | --- |
| `ui/screens/inicio` | Pantalla de inicio |
| `ui/screens/experimentos` | Experimentos, luz, cámara y seguimiento |
| `ui/screens/tutoria` | Tutor IA y Aprender jugando |
| `ui/screens/estudio` | Estudio |
| `ui/screens/documentos` | PDF |
| `ui/screens/resultados` | Resultados |
| `ui/screens/ajustes` | Ajustes |
| `ui/components` | Botones, tarjetas y barra de navegación |
| `viewmodel` | Lo que cada pantalla muestra y guarda |

Las rutas entre pantallas están en `core/navigation/LaboratorioNavHost.kt`.

Los sensores, la base de datos y Gemini están fuera del frontend: `sensores/`, `backend/` e `ia/`.
