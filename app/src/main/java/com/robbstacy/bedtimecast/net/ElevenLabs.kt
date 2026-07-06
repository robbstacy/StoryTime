package com.robbstacy.bedtimecast.net

import java.io.DataOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Minimal ElevenLabs REST client (instant voice cloning + text-to-speech). */
object ElevenLabs {

    private const val BASE = "https://api.elevenlabs.io/v1"
    private const val TTS_MODEL = "eleven_multilingual_v2"

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class VoiceAddResponse(val voice_id: String)

    /** Creates a cloned voice from sample audio files; returns the voice id. */
    fun createVoice(apiKey: String, name: String, samples: List<File>): Result<String> = runCatching {
        val boundary = "----BedtimeCast${System.currentTimeMillis()}"
        val conn = (URL("$BASE/voices/add").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 30_000
            readTimeout = 300_000
            setRequestProperty("xi-api-key", apiKey)
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }
        DataOutputStream(conn.outputStream.buffered()).use { out ->
            out.writeBytes("--$boundary\r\n")
            out.writeBytes("Content-Disposition: form-data; name=\"name\"\r\n\r\n")
            out.write(name.toByteArray())
            out.writeBytes("\r\n")
            samples.forEach { file ->
                out.writeBytes("--$boundary\r\n")
                out.writeBytes(
                    "Content-Disposition: form-data; name=\"files\"; filename=\"${file.name}\"\r\n",
                )
                out.writeBytes("Content-Type: audio/mp4\r\n\r\n")
                file.inputStream().use { it.copyTo(out) }
                out.writeBytes("\r\n")
            }
            out.writeBytes("--$boundary--\r\n")
        }
        val body = readBody(conn)
        json.decodeFromString<VoiceAddResponse>(body).voice_id
    }

    /** Reads [text] in the cloned voice; returns MP3 bytes. */
    fun textToSpeech(apiKey: String, voiceId: String, text: String): Result<ByteArray> = runCatching {
        val conn = (URL("$BASE/text-to-speech/$voiceId").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 30_000
            readTimeout = 300_000
            setRequestProperty("xi-api-key", apiKey)
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "audio/mpeg")
        }
        val payload = buildJsonObject {
            put("text", text)
            put("model_id", TTS_MODEL)
        }.toString()
        conn.outputStream.use { it.write(payload.toByteArray()) }
        val code = conn.responseCode
        if (code !in 200..299) {
            val err = conn.errorStream?.bufferedReader()?.readText().orEmpty()
            error(friendlyError(code, err))
        }
        conn.inputStream.use { it.readBytes() }
    }

    fun deleteVoice(apiKey: String, voiceId: String): Result<Unit> = runCatching {
        val conn = (URL("$BASE/voices/$voiceId").openConnection() as HttpURLConnection).apply {
            requestMethod = "DELETE"
            connectTimeout = 30_000
            readTimeout = 60_000
            setRequestProperty("xi-api-key", apiKey)
        }
        readBody(conn)
        Unit
    }

    private fun readBody(conn: HttpURLConnection): String {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val body = stream?.bufferedReader()?.readText().orEmpty()
        if (code !in 200..299) error(friendlyError(code, body))
        return body
    }

    private fun friendlyError(code: Int, body: String): String = when (code) {
        401 -> "ElevenLabs rejected the API key — check it on the Voices screen."
        402, 429 -> "ElevenLabs plan limit reached: ${body.take(160)}"
        422 -> "ElevenLabs could not use the request: ${body.take(160)}"
        else -> "ElevenLabs error $code: ${body.take(160)}"
    }
}
