package com.raiz.toc.ai

import com.raiz.toc.data.Theme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

/** Prueba las peticiones y el análisis de respuestas contra un servidor local falso. */
class ClaudeCoachTest {
    private lateinit var server: MockWebServer
    private var status = 200
    private var contentType = "application/json"
    private var body = ""

    @Before
    fun start() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun stop() = server.shutdown()

    private fun coach(model: AiModel = AiModel.OPUS): ClaudeCoach {
        server.enqueue(MockResponse().setResponseCode(status).setHeader("Content-Type", contentType).setBody(body))
        return ClaudeCoach("clave-de-prueba", model, server.url("/").toString().trimEnd('/'))
    }

    /** Cabeceras (en minúsculas) y cuerpo JSON de la única petición recibida. */
    private fun request(): Pair<Map<String, String>, JsonObject> {
        assertEquals(1, server.requestCount)
        val r = server.takeRequest()
        val headers = r.headers.names().associate { it.lowercase() to r.headers.values(it).joinToString(",") }
        return headers to Json.parseToJsonElement(r.body.readUtf8()).jsonObject
    }

    private fun message(text: String, stop: String = "end_turn") = """
        {"id":"msg_1","type":"message","role":"assistant","model":"claude-opus-5-5",
         "content":[{"type":"text","text":${JsonPrimitive(text)}}],
         "stop_reason":"$stop","stop_sequence":null,
         "usage":{"input_tokens":10,"output_tokens":5}}
    """.trimIndent()

    @Test
    fun ladderRequestUsesSchemaEffortAndFallbacks() {
        val plan = """{"intro":"Hola","steps":[{"situation":"Tocar el pomo","ritual":"Lavarme","expectedDistress":140,"theme":"CONTAMINATION","prediction":"Enfermaré"}]}"""
        body = message(plan)
        val result = coach().suggestLadder("Me lavo las manos 40 veces al día")

        assertEquals("Hola", result.intro)
        assertEquals(Theme.CONTAMINATION, result.steps[0].theme)
        assertEquals(100, result.steps[0].expectedDistress)

        val (headers, req) = request()
        assertEquals("clave-de-prueba", headers["x-api-key"])
        assertTrue(headers["anthropic-beta"]!!.contains("server-side-fallback-2026-07-01"))
        assertEquals("claude-opus-5-5", req["model"]!!.jsonPrimitive.content)
        assertEquals("default", req["fallbacks"]!!.jsonPrimitive.content)
        val output = req["output_config"]!!.jsonObject
        assertEquals("medium", output["effort"]!!.jsonPrimitive.content)
        val schema = output["format"]!!.jsonObject["schema"]!!.jsonObject
        assertEquals("json_schema", output["format"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals(listOf("intro", "steps"), schema["required"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertTrue(req["system"]!!.jsonPrimitive.content.contains("EPR"))
        assertFalse(req.containsKey("thinking"))
    }

    @Test
    fun haikuSkipsServerSideFallback() {
        body = message("Si no compruebo la puerta, entrarán a robar esta noche.")
        val p = coach(AiModel.HAIKU).suggestPrediction("Salir de casa", "Comprobar la puerta")
        assertEquals("Si no compruebo la puerta, entrarán a robar esta noche.", p)
        val (headers, req) = request()
        assertFalse(req.containsKey("fallbacks"))
        assertFalse(headers["anthropic-beta"].orEmpty().contains("server-side-fallback"))
        assertEquals("low", req["output_config"]!!.jsonObject["effort"]!!.jsonPrimitive.content)
    }

    @Test
    fun chatStreamsTextAndSendsHistory() {
        contentType = "text/event-stream"
        body = listOf(
            """{"type":"message_start","message":{"id":"m","type":"message","role":"assistant","model":"claude-opus-5-5","content":[],"stop_reason":null,"stop_sequence":null,"usage":{"input_tokens":1,"output_tokens":1}}}""",
            """{"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}""",
            """{"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Puede que sí, "}}""",
            """{"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"puede que no."}}""",
            """{"type":"content_block_stop","index":0}""",
            """{"type":"message_delta","delta":{"stop_reason":"end_turn","stop_sequence":null},"usage":{"output_tokens":8}}""",
            """{"type":"message_stop"}""",
        ).joinToString("") { "event: ${Json.parseToJsonElement(it).jsonObject["type"]!!.jsonPrimitive.content}\ndata: $it\n\n" }

        val partials = mutableListOf<String>()
        val history = listOf(ChatTurn(true, "¿Seguro que cerré la puerta?"))
        val answer = coach().chat(history, "contexto de prueba") { partials += it }

        assertEquals("Puede que sí, puede que no.", answer)
        assertEquals(listOf("Puede que sí, ", "Puede que sí, puede que no."), partials)
        val req = request().second
        assertEquals(true, req["stream"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("user", req["messages"]!!.jsonArray[0].jsonObject["role"]!!.jsonPrimitive.content)
        assertTrue(req["system"]!!.jsonPrimitive.content.contains("contexto de prueba"))
    }

    @Test
    fun refusalAndAuthErrorsBecomeFriendlyMessages() {
        body = message("", stop = "refusal")
        try {
            coach().summarize("datos")
            fail()
        } catch (e: AiException) {
            assertEquals(ClaudeCoach.REFUSAL_TEXT, e.message)
        }

        status = 401
        body = """{"type":"error","error":{"type":"authentication_error","message":"invalid x-api-key"}}"""
        try {
            coach().summarize("datos")
            fail()
        } catch (e: AiException) {
            assertTrue(e.message!!.contains("clave"))
        }
    }

    @Test
    fun malformedPlanIsReported() {
        body = message("esto no es json")
        try {
            coach().planRitual("Comprobar la puerta", "Que entren a robar")
            fail()
        } catch (e: AiException) {
            assertTrue(e.message!!.contains("formato"))
        }
    }
}
