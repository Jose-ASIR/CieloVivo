package com.raiz.toc.ai

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.PermissionDeniedException
import com.anthropic.errors.RateLimitException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.models.messages.JsonOutputFormat
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.MessageParam
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.StopReason
import com.raiz.toc.data.Theme
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Modelos que se pueden elegir en Ajustes. El primero es el recomendado. */
enum class AiModel(val id: String, val label: String) {
    OPUS("claude-opus-5-5", "Claude Opus 5.5 (recomendado)"),
    SONNET("claude-sonnet-5-5", "Claude Sonnet 5.5 (más barato)"),
    HAIKU("claude-haiku-5-5", "Claude Haiku 5.5 (el más barato)"),
}

data class ChatTurn(val fromUser: Boolean, val text: String)

@Serializable
data class SuggestedStep(
    val situation: String,
    val ritual: String,
    val expectedDistress: Int,
    val theme: Theme,
    val prediction: String,
)

@Serializable
data class StepPlan(val intro: String, val steps: List<SuggestedStep>)

/** Error ya traducido para mostrarlo tal cual. */
class AiException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Cliente de Claude para las funciones de IA de Raíz. Todas las llamadas son bloqueantes:
 * hay que hacerlas fuera del hilo principal.
 */
class ClaudeCoach(apiKey: String, private val model: AiModel, baseUrl: String? = null) : AutoCloseable {

    private val client: AnthropicClient = AnthropicOkHttpClient.builder()
        .apiKey(apiKey)
        .apply { if (baseUrl != null) baseUrl(baseUrl) }
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Conversación con el acompañante en streaming: [onText] recibe el texto acumulado
     * cada vez que llega un trozo nuevo. Devuelve la respuesta completa.
     */
    fun chat(history: List<ChatTurn>, context: String?, onText: (String) -> Unit): String = guard {
        val params = base(OutputConfig.Effort.MEDIUM)
            .system(Prompts.coachWithContext(context))
            .messages(history.map { turn ->
                MessageParam.builder()
                    .role(if (turn.fromUser) MessageParam.Role.USER else MessageParam.Role.ASSISTANT)
                    .content(turn.text)
                    .build()
            })
            .build()

        val out = StringBuilder()
        var stop: StopReason? = null
        client.messages().createStreaming(params).use { stream ->
            stream.stream().forEach { event ->
                event.contentBlockDelta().flatMap { it.delta().text() }.ifPresent {
                    out.append(it.text())
                    onText(out.toString())
                }
                event.messageDelta().flatMap { it.delta().stopReason() }.ifPresent { stop = it }
            }
        }
        if (stop == StopReason.REFUSAL && out.isBlank()) throw AiException(REFUSAL_TEXT)
        out.toString().trim()
    }

    /** Peldaños para la escalera a partir de una descripción libre del TOC. */
    fun suggestLadder(description: String): StepPlan =
        plan(Prompts.ladder, "Así es mi TOC:\n$description")

    /** Plan gradual para relajar un ritual concreto. */
    fun planRitual(ritual: String, fear: String): StepPlan =
        plan(Prompts.ritualPlan, "Ritual que quiero soltar: $ritual\nLo que temo que pase si no lo hago: $fear")

    fun suggestPrediction(situation: String, ritual: String): String = guard {
        val params = base(OutputConfig.Effort.LOW)
            .system(Prompts.prediction)
            .addUserMessage("Situación: $situation\nRitual que no haré: $ritual")
            .build()
        text(client.messages().create(params)).trim().trim('"', '“', '”')
    }

    /** Resumen de progreso para la persona y su terapeuta, a partir de [Digest]. */
    fun summarize(digest: String): String = guard {
        val params = base(OutputConfig.Effort.MEDIUM)
            .system(Prompts.summary)
            .addUserMessage("Estos son mis registros:\n\n$digest")
            .build()
        text(client.messages().create(params)).trim()
    }

    override fun close() = client.close()

    private fun plan(system: String, user: String): StepPlan = guard {
        val params = base(OutputConfig.Effort.MEDIUM, JsonOutputFormat.builder().schema(PLAN_SCHEMA).build())
            .system(system)
            .addUserMessage(user)
            .build()
        val raw = text(client.messages().create(params))
        runCatching { json.decodeFromString(StepPlan.serializer(), raw) }
            .getOrElse { throw AiException("La respuesta de la IA no tenía el formato esperado. Inténtalo de nuevo.", it) }
            .let { p -> p.copy(steps = p.steps.map { it.copy(expectedDistress = it.expectedDistress.coerceIn(0, 100)) }) }
    }

    private fun base(effort: OutputConfig.Effort, format: JsonOutputFormat? = null): MessageCreateParams.Builder {
        val output = OutputConfig.builder().effort(effort)
        if (format != null) output.format(format)
        val b = MessageCreateParams.builder()
            .model(model.id)
            .maxTokens(MAX_TOKENS)
            .outputConfig(output.build())
        // Si los filtros de seguridad rechazan una petición legítima (p. ej. al hablar de
        // pensamientos intrusivos), el servidor la reintenta con el modelo de respaldo adecuado.
        if (model != AiModel.HAIKU) {
            b.putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
        }
        return b
    }

    private fun text(message: Message): String {
        val text = message.content().mapNotNull { block -> block.text().map { it.text() }.orElse(null) }.joinToString("")
        if (message.stopReason().orElse(null) == StopReason.REFUSAL && text.isBlank()) throw AiException(REFUSAL_TEXT)
        if (text.isBlank()) throw AiException("La IA no ha devuelto respuesta. Inténtalo de nuevo.")
        return text
    }

    private inline fun <T> guard(block: () -> T): T = try {
        block()
    } catch (e: AiException) {
        throw e
    } catch (e: UnauthorizedException) {
        throw AiException("La clave de API no es válida. Revísala en Ajustes de IA.", e)
    } catch (e: PermissionDeniedException) {
        throw AiException("Tu clave de API no tiene permiso para usar este modelo.", e)
    } catch (e: RateLimitException) {
        throw AiException("Demasiadas peticiones o sin saldo en tu cuenta de Anthropic. Espera un poco y vuelve a intentarlo.", e)
    } catch (e: AnthropicServiceException) {
        throw AiException("El servicio de IA ha devuelto un error (${e.statusCode()}). Inténtalo más tarde.", e)
    } catch (e: AnthropicIoException) {
        throw AiException("No hay conexión con el servicio de IA. Comprueba tu internet.", e)
    }

    companion object {
        private const val MAX_TOKENS = 16_000L

        const val REFUSAL_TEXT =
            "La IA no ha podido responder a esto. Prueba a decirlo de otra forma. Si lo estás pasando muy mal, llama al 024 o al 112."

        private val STEP_SCHEMA = mapOf(
            "type" to "object",
            "additionalProperties" to false,
            "required" to listOf("situation", "ritual", "expectedDistress", "theme", "prediction"),
            "properties" to mapOf(
                "situation" to mapOf("type" to "string", "description" to "Situación concreta de exposición"),
                "ritual" to mapOf("type" to "string", "description" to "Ritual que no se hará o se limitará"),
                "expectedDistress" to mapOf("type" to "integer", "description" to "Malestar esperado de 0 a 100"),
                "theme" to mapOf("type" to "string", "enum" to Theme.entries.map { it.name }),
                "prediction" to mapOf("type" to "string", "description" to "Predicción temida, en primera persona"),
            ),
        )

        private val PLAN_SCHEMA: JsonOutputFormat.Schema = JsonOutputFormat.Schema.builder()
            .putAdditionalProperty("type", JsonValue.from("object"))
            .putAdditionalProperty("additionalProperties", JsonValue.from(false))
            .putAdditionalProperty("required", JsonValue.from(listOf("intro", "steps")))
            .putAdditionalProperty(
                "properties",
                JsonValue.from(
                    mapOf(
                        "intro" to mapOf("type" to "string"),
                        "steps" to mapOf("type" to "array", "items" to STEP_SCHEMA),
                    ),
                ),
            )
            .build()
    }
}
