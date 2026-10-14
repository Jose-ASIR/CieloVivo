package com.raiz.toc.data

/** Psicoeducación basada en la terapia de exposición con prevención de respuesta (EPR). */
data class Lesson(val title: String, val summary: String, val paragraphs: List<String>)

object Content {

    val lessons = listOf(
        Lesson(
            title = "Qué es el TOC",
            summary = "Obsesiones, compulsiones y por qué no eres tus pensamientos.",
            paragraphs = listOf(
                "El trastorno obsesivo-compulsivo afecta a cerca del 2 % de la población. Tiene dos partes: las obsesiones (pensamientos, imágenes o impulsos intrusivos que generan angustia, asco o la sensación de que algo \"no está bien\") y las compulsiones (lo que haces para quitarte ese malestar o evitar que pase algo malo).",
                "Casi todo el mundo tiene pensamientos intrusivos raros o desagradables. La diferencia en el TOC no está en el pensamiento, sino en la importancia que se le da y en lo que se hace después para neutralizarlo.",
                "Tener un pensamiento horrible no dice nada de quién eres. De hecho, el TOC suele atacar justo lo que más valoras: por eso esos pensamientos te angustian tanto.",
                "Las compulsiones pueden ser visibles (lavar, comprobar, ordenar, repetir, preguntar) o mentales (rezar, contar, repasar recuerdos, analizar, \"neutralizar\" un pensamiento con otro). Las mentales también son rituales y también mantienen el problema.",
            ),
        ),
        Lesson(
            title = "El círculo que lo mantiene",
            summary = "Por qué los rituales alivian hoy y empeoran el TOC mañana.",
            paragraphs = listOf(
                "1. Disparador: algo (una situación, un objeto, un pensamiento) activa la obsesión.\n2. Malestar: aparecen la ansiedad, la duda, el asco o la sensación de incompletitud.\n3. Ritual: haces algo para calmarlo o para asegurarte.\n4. Alivio: el malestar baja… durante un rato.",
                "Ese alivio es la trampa. El cerebro aprende dos cosas falsas: que el peligro era real y que solo te salvaste gracias al ritual. Así, la próxima vez la alarma suena más fuerte y el ritual parece todavía más necesario.",
                "Evitar situaciones funciona igual: como nunca te expones, nunca compruebas que podías soportarlo y que lo temido no pasaba.",
                "Por eso tratar el TOC \"desde la raíz\" no significa encontrar un pensamiento perfecto que te deje tranquilo, sino romper el círculo: dejar de hacer el ritual para que el cerebro pueda aprender otra cosa.",
            ),
        ),
        Lesson(
            title = "La terapia que funciona: EPR",
            summary = "Exposición con prevención de respuesta, el tratamiento psicológico de primera elección.",
            paragraphs = listOf(
                "La exposición con prevención de respuesta (EPR, o ERP en inglés) es el tratamiento psicológico con más evidencia para el TOC. Las guías clínicas la recomiendan como primera opción, sola o junto con medicación (normalmente un antidepresivo ISRS), y la mayoría de las personas que la completan mejoran de forma importante.",
                "Exposición: te acercas a propósito, poco a poco, a lo que dispara la obsesión.\nPrevención de respuesta: no haces el ritual, ni visible ni mental, ni pides que te tranquilicen.",
                "Se trabaja con una escalera: una lista de situaciones ordenadas por el malestar que esperas (de 0 a 100). Se empieza por peldaños asumibles (30-50) y se sube cuando los de abajo ya no imponen tanto.",
                "Hacerlo con un profesional (psicólogo clínico o psiquiatra con formación en EPR) multiplica las probabilidades de éxito, sobre todo si el TOC es intenso. Esta app es una herramienta para practicar entre sesiones o para empezar, no un sustituto.",
            ),
        ),
        Lesson(
            title = "Aprender, no solo aguantar",
            summary = "El modelo de aprendizaje inhibitorio: qué hace que la exposición funcione.",
            paragraphs = listOf(
                "Durante años se pensó que la exposición funcionaba porque la ansiedad \"se cansaba\" y bajaba (habituación). Hoy sabemos que lo más importante es otra cosa: aprender algo nuevo que compita con el miedo.",
                "Por eso, antes de cada práctica la app te pide una predicción concreta (\"si no me lavo, enfermaré\") y cuánto te la crees. Después compruebas si pasó. La sorpresa de ver que no ocurrió, o que pudiste tolerarlo, es lo que reentrena el cerebro.",
                "No hace falta que la ansiedad baje para que la práctica cuente. A veces terminas igual de nervioso y aun así has aprendido que puedes con ello.",
                "Varía las exposiciones: distintos lugares, horas y peldaños, alternando fácil y difícil. Lo aprendido en un solo contexto se generaliza peor.",
                "Evita las \"conductas de seguridad\" durante la práctica: distraerte a propósito, tocar con un pañuelo, repetirte que todo irá bien. Son rituales disfrazados que impiden el aprendizaje.",
            ),
        ),
        Lesson(
            title = "Aceptar la incertidumbre",
            summary = "El TOC exige certeza total. La salida es renunciar a ella.",
            paragraphs = listOf(
                "El TOC te promete que, si compruebas una vez más, por fin estarás seguro. Esa certeza nunca llega, porque nadie puede tener certeza absoluta de casi nada.",
                "En lugar de discutir con la obsesión o intentar demostrar que es falsa, prueba a responder con aceptación: \"Puede que sí, puede que no. Elijo vivir con la duda\".",
                "No es resignación: es dejar de pagar el precio del ritual a cambio de una seguridad que no existe.",
            ),
        ),
        Lesson(
            title = "Rituales mentales y rumiación",
            summary = "Analizar, repasar y neutralizar también son compulsiones.",
            paragraphs = listOf(
                "Muchos rituales no se ven: repasar si hiciste algo mal, comparar tus sentimientos, rezar de una forma concreta, sustituir una imagen mala por una buena, buscar en internet, analizar \"por qué he pensado eso\".",
                "Pista para detectarlos: si lo haces para quitarte la duda o el malestar, y te deja con ganas de volver a hacerlo, probablemente es un ritual.",
                "Para cortarlos: date cuenta (\"estoy analizando otra vez\"), etiquétalo (\"esto es el TOC\") y devuelve la atención a lo que estabas haciendo, aunque la duda siga ahí. No hace falta echar el pensamiento: déjalo estar.",
            ),
        ),
        Lesson(
            title = "Pedir que te tranquilicen",
            summary = "La búsqueda de tranquilización es un ritual muy común.",
            paragraphs = listOf(
                "Preguntar \"¿seguro que cerré?\", \"¿tú crees que soy mala persona?\" o confesar algo para quedarte tranquilo funciona igual que lavarse las manos: alivia un rato y refuerza el TOC.",
                "Explica a las personas cercanas cómo ayudarte. Una respuesta útil, pactada de antemano y dicha con cariño, puede ser: \"Te quiero, y creo que eso es el TOC preguntando. ¿Qué podemos hacer en vez de contestar?\"",
                "Que la familia participe en los rituales o los facilite (acomodación) es muy habitual y se asocia a peores resultados. Reducirla poco a poco, de acuerdo con la persona, forma parte del tratamiento.",
            ),
        ),
        Lesson(
            title = "Aplazar el ritual",
            summary = "Una forma de empezar cuando resistirlo del todo parece imposible.",
            paragraphs = listOf(
                "Si no puedes evitar el ritual, prueba a aplazarlo: 5 minutos, luego 10, luego 30. Muchas veces, al pasar el tiempo, el impulso baja solo y ya no hace falta.",
                "Otra opción es recortarlo: lavarte una vez en lugar de cinco, comprobar una vez y marcharte, hacerlo a propósito \"mal\". Cada recorte es una pequeña exposición.",
                "Surfear el impulso: el impulso es como una ola. Sube, llega a un pico y baja. Obsérvalo en el cuerpo sin obedecerlo, respirando despacio, y nota cómo cambia.",
            ),
        ),
        Lesson(
            title = "Usar bien el acompañante IA",
            summary = "Una ayuda para soltar rituales, no una nueva fuente de certezas.",
            paragraphs = listOf(
                "El acompañante IA (opcional) está diseñado para ayudarte a aplazar, recortar y dejar rituales, y a convertir el miedo a no hacerlos en experimentos: \"si no lo hago, ¿qué temo que pase? Vamos a comprobarlo\".",
                "No te dirá que todo está bien ni comprobará cosas por ti. Si le pides certezas, te lo señalará con cariño: preguntarle a una IA una y otra vez es otra forma de buscar tranquilización, y eso alimenta el TOC.",
                "Úsalo en el momento del impulso (desde \"Tengo un impulso ahora\" o durante una exposición), para preparar un plan gradual con \"Soltar un ritual\" o para generar un resumen de tu progreso que puedas enseñar a tu terapeuta.",
                "Es una IA: puede equivocarse y no sustituye a un profesional. Revisa siempre los pasos que te propone y descarta los que no te encajen.",
            ),
        ),
        Lesson(
            title = "Medicación y otras ayudas",
            summary = "Cuándo pedir ayuda profesional y qué opciones existen.",
            paragraphs = listOf(
                "Los antidepresivos ISRS (y la clomipramina) son el tratamiento farmacológico de primera línea. Suelen necesitar dosis más altas y más tiempo (8-12 semanas) que en la depresión. Solo un médico puede valorarlos; no dejes ni cambies una medicación por tu cuenta.",
                "La terapia de aceptación y compromiso (ACT) y la terapia cognitiva basada en la inferencia son alternativas o complementos con evidencia creciente.",
                "Pide ayuda profesional si el TOC te ocupa más de una hora al día, interfiere en tu trabajo, estudios o relaciones, o si notas tristeza intensa o pensamientos de hacerte daño.",
                "En España puedes empezar por tu médico de cabecera, que puede derivarte a salud mental. Si estás en crisis o piensas en hacerte daño, llama al 024 (línea de atención a la conducta suicida, gratuita y 24 h) o al 112.",
            ),
        ),
        Lesson(
            title = "Recaídas",
            summary = "Los altibajos son normales y no borran lo aprendido.",
            paragraphs = listOf(
                "El TOC tiende a subir con el estrés, el cansancio o los cambios. Un mal día no significa que hayas vuelto al principio: lo aprendido sigue ahí, solo hay que reactivarlo.",
                "Si notas que vuelven rituales antiguos, vuelve a la escalera: practica unos días los peldaños que ya dominabas.",
                "Seguir haciendo exposiciones de vez en cuando, aunque estés bien, es la mejor protección.",
            ),
        ),
    )

    /** Frases para responder a la obsesión sin discutir con ella. */
    val uncertaintyPhrases = listOf(
        "Puede que sí, puede que no. Puedo vivir con la duda.",
        "Esto es el TOC hablando. No necesito contestarle.",
        "No tengo que estar seguro para seguir con mi día.",
        "El malestar es incómodo, no peligroso. Va a pasar.",
        "Un pensamiento no es un hecho ni una orden.",
        "Si hago el ritual, el TOC gana fuerza. Si espero, la pierde.",
        "Dejo que el pensamiento esté ahí mientras hago otra cosa.",
        "Prefiero la incomodidad de hoy a la cárcel de siempre.",
        "Ya he resistido otras veces y no pasó lo que temía.",
        "No busco sentirme bien ahora; busco ser libre después.",
    )

    /** Guía mientras se aplaza un ritual, en orden. */
    val urgeGuide = listOf(
        "Nómbralo: \"Tengo el impulso de hacer un ritual\". No eres el impulso.",
        "Nota dónde está en el cuerpo: pecho, estómago, manos… Descríbelo sin juzgarlo.",
        "Respira con el círculo: inspira cuando crece, suelta el aire cuando se encoge.",
        "No discutas con el pensamiento ni intentes demostrar que es falso. Déjalo estar.",
        "El impulso es una ola: sube, llega al pico y baja. Solo tienes que surfearla.",
        "Vuelve poco a poco a lo que estabas haciendo, con la duda incluida.",
    )

    /** Recordatorios antes de una exposición. */
    val exposureRules = listOf(
        "No hagas el ritual, tampoco en la cabeza (contar, rezar, repasar, neutralizar).",
        "No pidas que te tranquilicen ni busques información para quedarte seguro.",
        "No te distraigas a propósito: deja que el malestar esté ahí.",
        "Quédate el tiempo suficiente para comprobar tu predicción (orientativo: 10-30 min).",
    )

    /** Ideas de peldaños por tema para quien no sabe por dónde empezar. */
    val stepIdeas = mapOf(
        Theme.CONTAMINATION to listOf(
            "Tocar el pomo de una puerta y no lavarme durante 1 hora",
            "Sentarme en un banco público y comer después sin lavarme",
        ),
        Theme.CHECKING to listOf(
            "Cerrar la puerta una sola vez y marcharme sin volver",
            "Apagar el fuego, mirar una vez y salir de la cocina",
        ),
        Theme.HARM to listOf(
            "Usar un cuchillo de cocina con alguien cerca",
            "Conducir sin volver a mirar por el retrovisor si atropellé a alguien",
        ),
        Theme.SYMMETRY to listOf(
            "Dejar un cuadro torcido todo el día",
            "Escribir una palabra \"mal\" y no repetirla",
        ),
        Theme.INTRUSIVE to listOf(
            "Escribir el pensamiento intrusivo en un papel y leerlo en voz alta",
            "Escuchar un audio con el pensamiento sin neutralizarlo",
        ),
        Theme.RELATIONSHIP to listOf(
            "No comparar a mi pareja con otras personas durante un día",
            "No preguntar \"¿me quieres?\" para quedarme tranquilo",
        ),
        Theme.HEALTH to listOf(
            "No buscar síntomas en internet durante un día",
            "No palparme el bulto para comprobarlo",
        ),
    )

    /**
     * Autoevaluación semanal propia, inspirada en las dimensiones clásicas de gravedad del
     * TOC (tiempo, malestar, interferencia, resistencia y control). Cada respuesta va de 0 a 4.
     */
    val checkQuestions = listOf(
        "¿Cuánto tiempo al día te han ocupado %s?" to listOf("Nada", "Menos de 1 h", "1-3 h", "3-8 h", "Más de 8 h"),
        "¿Cuánto te han interferido %s en tu vida (trabajo, estudios, relaciones)?" to listOf("Nada", "Un poco", "Bastante, pero manejable", "Mucho", "Me incapacita"),
        "¿Cuánto malestar te han causado %s?" to listOf("Ninguno", "Poco", "Moderado", "Mucho", "Extremo"),
        "¿Cuánto has intentado resistir %s?" to listOf("Siempre lo intento", "Casi siempre", "A veces", "Rara vez", "Me dejo llevar del todo"),
        "¿Cuánto control has tenido sobre %s?" to listOf("Total", "Bastante", "Algo", "Poco", "Ninguno"),
    )
}
