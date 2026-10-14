# Raíz: entrenamiento para salir del círculo del TOC

App Android (Jetpack Compose) para practicar **exposición con prevención de respuesta (EPR)**, el tratamiento psicológico con más evidencia para el trastorno obsesivo-compulsivo.

> **Aviso.** Ninguna app cura el TOC por sí sola. Raíz es una herramienta de autoayuda para practicar entre sesiones de terapia o como primer paso mientras se busca ayuda profesional. Si hay pensamientos de hacerse daño: **024** (España) o **112**.

## 1. Cómo funciona el TOC (resumen de la investigación)

```
 Disparador ──► Obsesión ──► Malestar (ansiedad, duda, asco, "no está bien")
                                  │
                                  ▼
                 Ritual / evitación / pedir que te tranquilicen
                                  │
                                  ▼
             Alivio breve  ──►  el cerebro aprende que el peligro era real
                                  │      y que solo el ritual lo evitó
                                  └──────────► la próxima obsesión llega más fuerte
```

- **Los pensamientos intrusivos son universales.** En el TOC el problema es la importancia que se les da y lo que se hace para neutralizarlos.
- **El "motor" es el ritual** (visible o mental: comprobar, lavar, contar, rezar, repasar, analizar, preguntar). Alivia a corto plazo y mantiene el trastorno por refuerzo negativo. Por eso tratarlo "desde la raíz" significa **romper el círculo**, no encontrar la respuesta que dé certeza total.
- **EPR**: exponerse de forma gradual a lo que dispara la obsesión **sin hacer el ritual**. Es el tratamiento psicológico de primera elección, con efectos grandes en los metaanálisis (en torno a un 60 % de reducción de síntomas). Combinada con ISRS supera a la medicación sola.
- **Aprendizaje inhibitorio** (modelo actual): la exposición funciona sobre todo porque **desmiente expectativas** ("si no me lavo, enfermaré") y crea un aprendizaje nuevo que compite con el miedo. No hace falta que la ansiedad baje durante la práctica. Ayuda variar contextos y no usar conductas de seguridad.
- **Apps**: la evidencia de las apps de EPR autoguiadas aún es limitada (pilotos y un ECA pequeño con efectos modestos). Funcionan mejor junto a un terapeuta.

## 2. Qué hace la app

| Pantalla | Para qué |
|---|---|
| **Hoy** | Botón **"Tengo un impulso ahora"**, siguiente práctica sugerida, frase del día, racha y resumen semanal. |
| **Impulso** | Nombrar el disparador → **aplazar el ritual** 5/10/15/30 min con temporizador → respiración guiada y "surfear la ola" → registrar sin juzgar (resistido / recortado / hecho). |
| **Escalera** | Jerarquía de exposición: situación, ritual que no se hará, tema y malestar esperado (0-100), con ideas por tema. |
| **Exposición** | 1) Predicción concreta y cuánto se cree. 2) Práctica cronometrada anotando el malestar cada minuto, sin ritual. 3) ¿Ocurrió? ¿Cuánto se cree ahora? ¿Qué he aprendido? |
| **Acompañante IA** (opcional) | Chat con Claude para el momento del impulso o de la exposición: ayuda a aplazar, recortar y soltar el ritual y a convertir el miedo en un experimento. Se abre por encima del temporizador, que sigue corriendo. |
| **Soltar un ritual** (IA) | Describes el ritual y lo que temes; la IA propone 5-8 pasos graduales (aplazar → acortar → hacerlo "imperfecto" → no hacerlo), cada uno con su predicción, y eliges cuáles añadir a la escalera. |
| **Escalera con IA** | Describes tu TOC con tus palabras y la IA sugiere 6-10 peldaños con malestar esperado, tema y predicción. |
| **Predicción con IA** | En la preparación de una exposición, "Ayúdame a formularla" propone una predicción concreta y comprobable. |
| **Resumen con IA** | Analiza tus registros (patrones, predicciones desmentidas, siguientes pasos) y lo comparte con tu terapeuta. |
| **Aprender** | 11 lecciones: qué es el TOC, el círculo, EPR, aprendizaje inhibitorio, incertidumbre, rituales mentales, búsqueda de tranquilización y acomodación familiar, aplazar el ritual, medicación y recaídas. |
| **Progreso** | Peldaños consolidados, creencia antes/después de cada exposición, diario de aprendizajes, impulsos y **chequeo semanal** orientativo (0-40). |

Reglas de progreso (`data/Progress.kt`):
- Un peldaño se **consolida** tras 3 prácticas sin ritual y cuando la última vez la creencia en la predicción ha bajado a ≤ 30 %. No se exige que la ansiedad baje.
- La app sugiere el peldaño más bajo aún no consolidado y avisa si las últimas 3 prácticas fueron iguales (para variar contextos).
- El chequeo semanal es propio (tiempo, interferencia, malestar, resistencia y control; obsesiones y compulsiones). **No es una escala clínica ni un diagnóstico.**

## 3. Estructura

```
raiz/src/main/java/com/raiz/toc/
├── MainActivity.kt
├── data/
│   ├── Models.kt      Peldaños, sesiones, impulsos, chequeos (kotlinx.serialization)
│   ├── Progress.kt    Consolidación, sugerencias, estadísticas, rachas (sin Android, con tests)
│   ├── Content.kt     Lecciones, frases de aceptación, guía de impulsos, ideas por tema
│   └── Store.kt       JSON privado con escritura atómica
├── ai/
│   ├── ClaudeCoach.kt Cliente de Claude: chat en streaming, planes estructurados, predicción, resumen
│   ├── Prompts.kt     Instrucciones (sin tranquilización, centradas en EPR)
│   └── Digest.kt      Resumen compacto de los registros para la IA
└── ui/                Compose: Hoy, Escalera, Aprender, Progreso + flujos a pantalla completa
```

## 4. La IA (opcional)

Usa la API de Claude (Anthropic) con el **SDK oficial de Java** (`com.anthropic:anthropic-java`) y la clave de API de la propia persona, que se introduce en *Hoy → ⚙ Ajustes de IA* y se guarda solo en las preferencias privadas de la app. Modelo por defecto: **Claude Opus 5.5**; en ajustes se puede elegir Sonnet 5.5 o Haiku 5.5, que son más baratos.

**Diseño clínico.** El mayor riesgo de una IA con TOC es que se convierta en un ritual más: preguntarle "¿seguro que no pasa nada?" una y otra vez. Por eso las instrucciones (`ai/Prompts.kt`):
- prohíben tranquilizar sobre el contenido de la obsesión, comprobar o analizar su significado; cuando detecta búsqueda de certeza, la nombra con cariño y devuelve la incertidumbre;
- se centran en *relajar el ritual* (aplazar, acortar, hacerlo imperfecto, quitar conductas de seguridad) y en *perder el miedo a no hacerlo* (predicción → experimento, la ola del impulso, valores);
- tratan los pensamientos intrusivos como lo que son, sin alarmarse, pero derivan al 024/112 ante ideas reales de hacerse daño.

**Detalles técnicos** (`ai/ClaudeCoach.kt`):
- El chat va en *streaming* (el texto aparece mientras se genera). Los planes usan *structured outputs* con un esquema JSON y se validan con kotlinx.serialization.
- Esfuerzo `medium` para el chat, los planes y el resumen; `low` para la sugerencia de predicción.
- *Fallback* del lado del servidor (`fallbacks: "default"`): si un filtro de seguridad rechaza por error una petición legítima (por ejemplo, al hablar de pensamientos intrusivos), Anthropic la reintenta con el modelo adecuado. Si aun así se rechaza, la app lo dice y recuerda el 024.
- Los errores (clave inválida, sin saldo, sin conexión) se traducen a mensajes claros.
- `ClaudeCoachTest` comprueba las peticiones y respuestas contra un servidor HTTP local falso, sin gastar llamadas reales.

**Privacidad.** Sin clave, nada sale del teléfono. Con la IA activada, solo se envía lo que escribes en el chat o en los formularios de IA. El resumen de registros (`ai/Digest.kt`) solo se envía al pedir el resumen o si activas "Compartir mis registros con el acompañante" (desactivado por defecto). `allowBackup=false`; los datos están en `files/raiz.json`.

## 5. Compilar

```bash
./gradlew :raiz:assembleDebug        # APK en raiz/build/outputs/apk/debug/
./gradlew :raiz:testDebugUnitTest    # tests de Progress, Store, Digest y ClaudeCoach
```

GitHub Actions compila las dos apps del repositorio en cada push; el APK sale como artefacto **Raiz-apk**.

## 6. Fuentes

- [ERP combinada con farmacoterapia en el TOC: revisión sistemática y metaanálisis (2022)](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC9520065/)
- [Abramowitz et al., avances recientes en TCC para el TOC (2018)](https://jonabram.web.unc.edu/wp-content/uploads/sites/2968/2018/05/Recent-advances-in-CBT-for-OCD-2018.pdf)
- [Metaanálisis de protocolos estrictos de EPR (UCL)](https://discovery-pp.ucl.ac.uk/id/eprint/10135430)
- [Terapia basada en EPR para TOC y Tourette: metaanálisis (2022)](https://dx.doi.org/10.31083/j.jin2103097)
- [ECA de una app autoguiada para síntomas de TOC (JMIR mHealth, 2024)](https://mhealth.jmir.org/2024/1/e63316)
- [Boisseau et al., EPR guiada por app: estudio piloto](https://sites.northwestern.edu/ocdanxietylab/files/2019/10/Boisseau-et-al-App-Guided.pdf)

## 7. Ideas para siguientes versiones

1. Recordatorio diario de práctica (WorkManager + notificaciones).
2. Exposición imaginal: grabar y reproducir un guion del pensamiento temido en bucle.
3. Exportar el registro en PDF para llevarlo a terapia.
4. Modo familia: guía para dejar de acomodar los rituales.
