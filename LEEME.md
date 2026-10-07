# AloTube para Android

App de Android que contiene AloTube (`app/src/main/assets/alotube.html`) dentro de un WebView.

## Lo que aporta la app respecto al archivo HTML suelto
- Las páginas y descargas ya no dependen de proxies públicos: la app hace esas peticiones ella misma.
- Las descargas y las copias de seguridad se guardan en **Descargas/AloTube**.
- El botón de imagen (fondo personalizado) abre el selector de fotos del teléfono.
- Video en pantalla completa y botón "atrás" de Android.
- **Audio en segundo plano**: mientras suena algo, la app muestra una notificación y sigue sonando con la pantalla apagada.
- **Ventana flotante**: el botón "Ventana flotante" del reproductor pasa la app a imagen en imagen.
- Menú ⋮ › **YouTube oficial**: abre m.youtube.com dentro de la app (web normal: con anuncios y sin iniciar sesión, porque Google no permite el login en WebViews).

## Novedades de AloTube (también en el HTML suelto)
- Servidores inteligentes: recuerda cuáles funcionan y prueba primero el mejor. Ajustes › "Probar y ordenar servidores".
- Copia de seguridad: Ajustes › Exportar / Importar (historial, listas, me gusta, ajustes y, si quieres, fondo y claves).
- Reproductor: velocidad (0,5× a 2×), doble toque ±10 s, deslizar para volumen y brillo.
- Cola de reproducción con siguiente automático, repetir y aleatorio.
- Modo solo audio y ventana flotante.
- Pantalla de Ajustes: calidad y velocidad por defecto, historial de búsqueda, sugerencias, país, WiFi, conexiones.

Conectar la cuenta de Google no funciona dentro de la app (Google no lo permite en WebViews); para eso usa Chrome con una dirección https.

## Cómo generar el APK

### Opción A: Android Studio (en un computador)
1. Instala Android Studio y elige **Open** sobre esta carpeta.
2. Espera a que termine la sincronización de Gradle.
3. Menú **Build › Build Bundle(s) / APK(s) › Build APK(s)**.
4. El archivo queda en `app/build/outputs/apk/debug/app-debug.apk`.

### Opción B: en la nube con GitHub (no instalas nada)
1. Crea un repositorio en github.com y sube todo el contenido de esta carpeta.
   Si el sistema oculta la carpeta `.github`, créala en GitHub con **Add file › Create new file**,
   escribe el nombre `.github/workflows/build-apk.yml` y pega el contenido de ese archivo.
2. Entra en la pestaña **Actions › Build APK › Run workflow**.
3. Cuando termine (unos 5 a 10 minutos), descarga el artefacto **AloTube-apk** (dentro del zip está `app-debug.apk`).

## Instalar
Copia el APK al teléfono y ábrelo. Android pedirá permitir "instalar apps desconocidas".
Está firmado con la clave de depuración: sirve para uso personal, no para Play Store.
La primera vez, acepta el permiso de notificaciones (lo usa el audio en segundo plano).

## Si algo falla
Esta versión no se compiló con el SDK real de Android (el código se revisó con un compilador de Java
contra una copia simplificada de la API). Si Gradle da un error, copia el mensaje del registro y se corrige.
