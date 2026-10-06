# AloTube para Android

App de Android que contiene AloTube (`app/src/main/assets/alotube.html`) dentro de un WebView.
Por ser una app nativa mejora varias cosas respecto al archivo HTML suelto:

- Las páginas y descargas ya no dependen de proxies públicos: la app hace esas peticiones ella misma.
- Las descargas se guardan directamente en **Descargas/AloTube** del teléfono.
- El botón de imagen (fondo personalizado) abre el selector de fotos del teléfono.
- Video en pantalla completa y botón "atrás" de Android.
- Menú ⋮ → **YouTube oficial**: abre m.youtube.com dentro de la app (es la web normal: con anuncios y sin iniciar sesión, porque Google no permite el login en WebViews).

Sigue usando los servidores Piped/Invidious para buscar y reproducir sin anuncios.
Dentro de la app no funciona conectar la cuenta de Google (usa Chrome con una dirección https para eso).

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
3. Cuando termine (unos 5 a 10 minutos), abre la ejecución y descarga el artefacto **AloTube-apk**
   (dentro del zip está `app-debug.apk`).

## Instalar
Copia el APK al teléfono y ábrelo. Android pedirá permitir "instalar apps desconocidas" para tu
administrador de archivos o navegador. El APK está firmado con la clave de depuración: sirve para uso
personal, no para Play Store.

## Si algo falla
Esta versión no se compiló con el SDK real de Android (solo se revisó el código). Si Gradle da un
error, copia el mensaje del registro y se corrige.
