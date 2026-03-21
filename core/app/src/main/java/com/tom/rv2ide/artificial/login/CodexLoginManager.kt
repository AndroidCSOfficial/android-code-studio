package com.tom.rv2ide.artificial.login

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.BindException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.TimeUnit

class CodexLoginManager {
  companion object {
    private const val CLIENT_ID = "app_EMoamEEZ73f0CkXaXp7hrann"
    private const val AUTHORIZATION_URL = "https://auth.openai.com/oauth/authorize"
    private const val TOKEN_URL = "https://auth.openai.com/oauth/token"
    private const val CALLBACK_PATH = "/auth/callback"
    private const val LOOPBACK_HOST = "127.0.0.1"
    private const val DEFAULT_PORT = 1455
    private const val CALLBACK_TIMEOUT_MS = 180_000L
    private const val RESPONSE_BODY_CHARSET = "utf-8"
    private const val ORIGINATOR_VALUE = "codex_cli_rs"
    private val SCOPE_STRING = listOf(
      "openid",
      "profile",
      "email",
      "offline_access",
      "api.connectors.read",
      "api.connectors.invoke"
    ).joinToString(" ")
  }

  private val httpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()

  @Throws(IOException::class)
  suspend fun loginWithChatGPT(context: Context): String {
    val pkce = createPkceCodes()
    val state = generateState()
    val (serverSocket, actualPort) = createServerSocket()
    val redirectUri = "http://$LOOPBACK_HOST:$actualPort$CALLBACK_PATH"
    val authUrl = buildAuthorizationUrl(redirectUri, pkce, state)

    try {
      withContext(Dispatchers.Main) {
        openBrowser(context, authUrl)
      }

      val callback = withTimeout(CALLBACK_TIMEOUT_MS) {
        waitForCallback(serverSocket, state)
      }

      callback.error?.let {
        throw IllegalStateException(callback.errorDescription ?: it)
      }

      val code = callback.code ?: throw IllegalStateException("Missing authorization code from ChatGPT login.")
      val tokens = exchangeCodeForTokens(code, redirectUri, pkce)
      return exchangeIdTokenForApiKey(tokens.idToken)
    } finally {
      try {
        serverSocket.close()
      } catch (_: IOException) {
      }
    }
  }

  private fun createServerSocket(): Pair<ServerSocket, Int> {
    val server = ServerSocket()
    server.reuseAddress = true
    try {
      server.bind(InetSocketAddress(InetAddress.getByName(LOOPBACK_HOST), DEFAULT_PORT))
    } catch (bindException: BindException) {
      server.bind(InetSocketAddress(InetAddress.getByName(LOOPBACK_HOST), 0))
    }
    return server to server.localPort
  }

  private fun buildAuthorizationUrl(redirectUri: String, pkce: PkceCodes, state: String): String {
    val params = listOf(
      "response_type" to "code",
      "client_id" to CLIENT_ID,
      "redirect_uri" to redirectUri,
      "scope" to SCOPE_STRING,
      "code_challenge" to pkce.codeChallenge,
      "code_challenge_method" to "S256",
      "id_token_add_organizations" to "true",
      "codex_cli_simplified_flow" to "true",
      "state" to state,
      "originator" to ORIGINATOR_VALUE
    )

    val encoded = params.joinToString("&") { "${it.first}=${urlEncode(it.second)}" }
    return "$AUTHORIZATION_URL?$encoded"
  }

  private fun urlEncode(value: String): String {
    return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
  }

  private fun openBrowser(context: Context, authUrl: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
    if (context !is Activity) {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
      context.startActivity(intent)
    } catch (error: ActivityNotFoundException) {
      throw IllegalStateException("No browser found to complete the ChatGPT login.", error)
    }
  }

  private suspend fun waitForCallback(server: ServerSocket, expectedState: String): CallbackResult {
    return withContext(Dispatchers.IO) {
      server.soTimeout = 1_000
      while (true) {
        coroutineContext.ensureActive()
        try {
          val socket = server.accept()
          val callback = handleSocket(socket, expectedState)
          if (callback != null) {
            return@withContext callback
          }
        } catch (socketTimeout: SocketTimeoutException) {
          continue
        }
      }
    }
  }

  private fun handleSocket(socket: Socket, expectedState: String): CallbackResult? {
    socket.use {
      val reader = BufferedReader(InputStreamReader(it.getInputStream()))
      val requestLine = reader.readLine() ?: return null
      val segments = requestLine.split(" ")
      if (segments.size < 2) {
        drainRequest(reader)
        sendHttpResponse(it.getOutputStream(), 400, "Invalid request")
        return null
      }

      val target = segments[1]
      val uri = Uri.parse("http://$LOOPBACK_HOST$target")
      val path = uri.path ?: ""
      drainRequest(reader)

      if (path != CALLBACK_PATH) {
        sendHttpResponse(it.getOutputStream(), 404, "<html><body><h1>Not found</h1></body></html>")
        return null
      }

      val code = uri.getQueryParameter("code")
      val state = uri.getQueryParameter("state")
      val error = uri.getQueryParameter("error")
      val errorDescription = uri.getQueryParameter("error_description")
      val body = when {
        error != null -> buildErrorPage(errorDescription ?: "An error occurred")
        code != null -> buildSuccessPage()
        else -> buildErrorPage("Missing authorization code")
      }
      sendHttpResponse(it.getOutputStream(), 200, body)

      if (error != null) {
        return CallbackResult(null, state, error, errorDescription)
      }

      if (state != expectedState) {
        return CallbackResult(null, state, "State mismatch", "The login response does not match the request state.")
      }

      return CallbackResult(code, state, null, null)
    }
  }

  private fun drainRequest(reader: BufferedReader) {
    while (true) {
      val line = reader.readLine() ?: break
      if (line.isEmpty()) {
        break
      }
    }
  }

  private fun sendHttpResponse(output: OutputStream, statusCode: Int, body: String) {
    val payload = body.toByteArray(StandardCharsets.UTF_8)
    OutputStreamWriter(output, StandardCharsets.UTF_8).use { writer ->
      writer.write("HTTP/1.1 $statusCode ${statusText(statusCode)}\r\n")
      writer.write("Content-Type: text/html; charset=$RESPONSE_BODY_CHARSET\r\n")
      writer.write("Content-Length: ${payload.size}\r\n")
      writer.write("Connection: close\r\n")
      writer.write("\r\n")
      writer.flush()
      output.write(payload)
      output.flush()
    }
  }

  private fun statusText(code: Int): String = when (code) {
    200 -> "OK"
    400 -> "Bad Request"
    404 -> "Not Found"
    else -> "OK"
  }


  private fun buildErrorPage(message: String): String {
    return """
      |<html>
      |<head><title>Login error</title></head>
      |<body>
      |  <h1>Unable to complete login</h1>
      |  <p>${message}</p>
      |</body>
      |</html>
      |""".trimMargin()
  }

  private suspend fun exchangeCodeForTokens(code: String, redirectUri: String, pkce: PkceCodes): ExchangedTokens {
    return withContext(Dispatchers.IO) {
      val body = "grant_type=authorization_code&code=${urlEncode(code)}&redirect_uri=${urlEncode(redirectUri)}&client_id=${urlEncode(CLIENT_ID)}&code_verifier=${urlEncode(pkce.codeVerifier)}"
      val request = Request.Builder()
        .url(TOKEN_URL)
        .header("Content-Type", "application/x-www-form-urlencoded")
        .post(body.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
        .build()

      val response = httpClient.newCall(request).execute()
      response.use { resp ->
        val responseBody = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
          throw IOException("Token exchange failed: ${resp.code} - $responseBody")
        }
        val json = JSONObject(responseBody)
        val idToken = json.optString("id_token")
        val accessToken = json.optString("access_token")
        val refreshToken = json.optString("refresh_token")
        if (idToken.isBlank()) {
          throw IllegalStateException("Token exchange did not return an id_token")
        }
        return@withContext ExchangedTokens(idToken, accessToken, refreshToken)
      }
    }
  }

  private suspend fun exchangeIdTokenForApiKey(idToken: String): String {
    return withContext(Dispatchers.IO) {
      val form = "grant_type=${urlEncode("urn:ietf:params:oauth:grant-type:token-exchange")}&client_id=${urlEncode(CLIENT_ID)}&requested_token=${urlEncode("openai-api-key")}&subject_token=${urlEncode(idToken)}&subject_token_type=${urlEncode("urn:ietf:params:oauth:token-type:id_token")}" 
      val request = Request.Builder()
        .url(TOKEN_URL)
        .header("Content-Type", "application/x-www-form-urlencoded")
        .post(form.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
        .build()

      val response = httpClient.newCall(request).execute()
      response.use { resp ->
        val body = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
          throw IOException("API key exchange failed: ${resp.code} - $body")
        }
        val json = JSONObject(body)
        return@withContext json.getString("access_token")
      }
    }
  }

  private fun generateState(): String {
    val bytes = ByteArray(32)
    SecureRandom().nextBytes(bytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
  }
  private fun createPkceCodes(): PkceCodes {
    val random = SecureRandom()
    val verifierBytes = ByteArray(32)
    random.nextBytes(verifierBytes)
    val verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(verifierBytes)
    val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(StandardCharsets.US_ASCII))
    val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    return PkceCodes(verifier, challenge)
  }

  private data class CallbackResult(
    val code: String?,
    val state: String?,
    val error: String?,
    val errorDescription: String?
  )

  private data class ExchangedTokens(
    val idToken: String,
    val accessToken: String,
    val refreshToken: String
  )

  private data class PkceCodes(
    val codeVerifier: String,
    val codeChallenge: String
  )

  private fun buildSuccessPage(): String {
    return """
      |<html>
      |<head><meta charset=\"utf-8\"/><title>Login complete</title></head>
      |<body>
      |  <h1>Login completed</h1>
      |  <p>The chat login completed successfully. Close this tab to return to the IDE.</p>
      |</body>
      |</html>
      |""".trimMargin()
  }
}
