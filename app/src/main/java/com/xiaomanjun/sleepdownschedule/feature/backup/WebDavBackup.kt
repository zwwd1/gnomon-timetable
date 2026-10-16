package com.xiaomanjun.sleepdownschedule.feature.backup

import android.content.Context
import cn.scvtc.campus.LocalCipher
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

@Serializable
data class WebDavConfig(val url: String, val username: String, val password: String)
data class WebDavEntry(val name: String, val bytes: Long, val modified: String)

class WebDavConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences("gnomon-webdav-private", Context.MODE_PRIVATE)
    private val cipher = LocalCipher(context, "gnomon.webdav.v1")
    fun load(): WebDavConfig? = prefs.getString("encrypted", null)?.let {
        try { Json.decodeFromString<WebDavConfig>(cipher.open(it, "webdav-config")) }
        catch (_: Exception) { throw IOException("本机 WebDAV 配置无法读取，请重新填写连接信息") }
    }
    fun save(value: WebDavConfig) {
        WebDavBackupClient.validateUrl(value.url)
        require(value.username.isNotBlank() && value.password.isNotBlank()) { "请填写账号和应用密码" }
        check(prefs.edit().putString("encrypted", cipher.seal(Json.encodeToString(value), "webdav-config")).commit())
    }
    fun clear() { check(prefs.edit().clear().commit()) }
}

/** Manual immutable backup objects; no polling, credential logging or cross-origin redirects. */
class WebDavBackupClient(config: WebDavConfig, client: OkHttpClient? = null,
    allowLocalTestServer: Boolean = false) {
    private val base = validateUrl(config.url, allowLocalTestServer).newBuilder()
        .addPathSegment("gnomon-timetable").addPathSegment("").build()
    private val authorization = Credentials.basic(config.username, config.password, Charsets.UTF_8)
    private val http = (client ?: OkHttpClient()).newBuilder().followRedirects(false).followSslRedirects(false)
        .connectTimeout(12, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS).build()
    private fun request(url: HttpUrl = base) = Request.Builder().url(url).header("Authorization", authorization)
    private fun requireCode(code: Int, allowed: Set<Int>) {
        if (code !in allowed) throw IOException(when (code) {
            401, 403 -> "WebDAV 认证失败，请检查账号、应用密码与目录权限"
            404 -> "WebDAV 目录不存在，请检查地址"
            301, 302, 307, 308 -> "WebDAV 地址发生跳转，请填写最终的 HTTPS 目录地址"
            412 -> "同名备份已存在，本次未覆盖，请重试生成新的备份"
            else -> "WebDAV 请求失败（HTTP $code）"
        })
    }
    fun prepare() {
        http.newCall(request().method("MKCOL", ByteArray(0).toRequestBody(null)).build()).execute().use {
            requireCode(it.code, setOf(201, 405))
        }
    }
    fun list(): List<WebDavEntry> {
        val body = """<?xml version="1.0" encoding="utf-8"?><d:propfind xmlns:d="DAV:"><d:prop><d:getlastmodified/><d:getcontentlength/><d:resourcetype/></d:prop></d:propfind>"""
        return http.newCall(request().header("Depth", "1")
            .method("PROPFIND", body.toRequestBody("application/xml; charset=utf-8".toMediaType())).build())
            .execute().use { response ->
                requireCode(response.code, setOf(207))
                val bytes = response.body?.byteStream()?.use { input ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (output.size() <= 4 * 1024 * 1024) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
                    ?: throw IOException("WebDAV 返回了空目录响应")
                require(bytes.size <= 4 * 1024 * 1024) { "WebDAV 目录响应过大" }
                val xml = Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString()
                require('\u0000' !in xml && !xml.contains("<!DOCTYPE", true) && !xml.contains("<!ENTITY", true)) { "WebDAV 目录包含不支持的 XML 声明" }
                val factory = DocumentBuilderFactory.newInstance().apply {
                    isNamespaceAware = true
                    isExpandEntityReferences = false
                }
                val doc = factory.newDocumentBuilder().parse(bytes.inputStream())
                require(doc.documentElement.localName == "multistatus") { "WebDAV 目录格式无法识别" }
                val rows = doc.getElementsByTagNameNS("DAV:", "response")
                buildList {
                    for (i in 0 until rows.length) {
                        val element = rows.item(i) as org.w3c.dom.Element
                        fun value(name: String) = element.getElementsByTagNameNS("DAV:", name).item(0)?.textContent.orEmpty()
                        val url = base.resolve(value("href")) ?: continue
                        if (url.scheme != base.scheme || url.host != base.host || url.port != base.port ||
                            url.encodedPath.substringBeforeLast('/') != base.encodedPath.trimEnd('/')) continue
                        val name = url.pathSegments.lastOrNull() ?: continue
                        if (!validName(name)) continue
                        add(WebDavEntry(name, value("getcontentlength").toLongOrNull() ?: 0L, value("getlastmodified")))
                    }
                }.distinctBy { it.name }.sortedByDescending { it.name }
            }
    }
    fun upload(file: File): String {
        require(file.length() in 1..BackupCodecLimits.MAX_ARCHIVE_BYTES) { "备份大小不在允许范围内" }
        val name = "Gnomon-${Instant.now().toString().replace(':', '-')}-${UUID.randomUUID()}.sleepdown"
        http.newCall(request(fileUrl(name)).header("If-None-Match", "*")
            .put(file.asRequestBody("application/zip".toMediaType())).build()).execute().use {
                requireCode(it.code, setOf(201, 204))
        }
        return name
    }
    fun download(name: String, destination: File) {
        require(validName(name)) { "备份文件名无效" }
        try {
            http.newCall(request(fileUrl(name)).get().build()).execute().use { response ->
                requireCode(response.code, setOf(200))
                val body = response.body ?: throw IOException("备份响应为空")
                require(body.contentLength() <= BackupCodecLimits.MAX_ARCHIVE_BYTES) { "备份文件过大" }
                body.byteStream().use { input -> destination.outputStream().use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= BackupCodecLimits.MAX_ARCHIVE_BYTES) { "备份文件过大" }
                        output.write(buffer, 0, count)
                    }
                } }
            }
            destination.inputStream().use { BackupCodec.decode(it) }
        } catch (error: Exception) { destination.delete(); throw error }
    }
    private fun fileUrl(name: String) = base.newBuilder().addPathSegment(name).build()
    companion object {
        private fun validName(value: String) = value.matches(Regex("Gnomon-[A-Za-z0-9T.Z_-]+\\.sleepdown"))
        fun validateUrl(value: String, allowLocalTestServer: Boolean = false): HttpUrl {
            val parsed = value.trim().trimEnd('/').plus('/').toHttpUrl()
            require(parsed.isHttps || (allowLocalTestServer && parsed.host in setOf("localhost", "127.0.0.1"))) { "请使用 HTTPS WebDAV 目录地址" }
            require(parsed.username.isEmpty() && parsed.password.isEmpty() && parsed.query == null && parsed.fragment == null) { "请在独立输入框填写账号和应用密码" }
            return parsed
        }
    }
}
