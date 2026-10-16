package com.xiaomanjun.sleepdownschedule.feature.backup

import com.sun.net.httpserver.HttpServer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class WebDavBackupTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun uploadCreatesUniqueHistoryAndDownloadedArchiveIsValidated() {
        val files = ConcurrentHashMap<String, ByteArray>()
        val header = AtomicReference<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/dav/gnomon-timetable/") { request ->
            try {
                assertNotNull(request.requestHeaders.getFirst("Authorization"))
                val path = request.requestURI.path
                val (code, body) = when (request.requestMethod) {
                    "MKCOL" -> 201 to ByteArray(0)
                    "PUT" -> {
                        header.set(request.requestHeaders.getFirst("If-None-Match"))
                        val bytes = request.requestBody.readBytes()
                        if (files.putIfAbsent(path, bytes) == null) 201 to ByteArray(0) else 412 to ByteArray(0)
                    }
                    "GET" -> files[path]?.let { 200 to it } ?: (404 to ByteArray(0))
                    "PROPFIND" -> {
                        assertEquals("1", request.requestHeaders.getFirst("Depth"))
                        207 to ("<d:multistatus xmlns:d=\"DAV:\">" + files.entries.joinToString("") {
                            "<d:response><d:href>${it.key}</d:href><d:propstat><d:prop><d:getcontentlength>${it.value.size}</d:getcontentlength></d:prop></d:propstat></d:response>"
                        } + "</d:multistatus>").toByteArray()
                    }
                    else -> 405 to ByteArray(0)
                }
                request.sendResponseHeaders(code, if (body.isEmpty()) -1 else body.size.toLong())
                if (body.isNotEmpty()) request.responseBody.use { it.write(body) }
            } finally { request.close() }
        }
        server.start()
        try {
            val client = WebDavBackupClient(WebDavConfig("http://127.0.0.1:${server.address.port}/dav/", "test", "local-test"), allowLocalTestServer = true)
            val source = temporary.newFile("source.sleepdown").apply { writeBytes(BackupCodec.encode(fileTestArchive())) }
            client.prepare()
            val first = client.upload(source)
            val second = client.upload(source)
            assertNotEquals(first, second)
            assertEquals("*", header.get())
            assertEquals(2, files.size)
            assertEquals(setOf(first, second), client.list().map { it.name }.toSet())
            val destination = temporary.newFile("download.sleepdown")
            client.download(first, destination)
            assertArrayEquals(source.readBytes(), destination.readBytes())
            files["/dav/gnomon-timetable/$first"] = "broken archive".toByteArray()
            assertTrue(runCatching { client.download(first, destination) }.isFailure)
            assertFalse(destination.exists())
        } finally { server.stop(0) }
    }

    @Test fun authFailureAndCrossOriginRedirectNeverBecomeSuccess() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/dav/gnomon-timetable/") { request ->
            request.responseHeaders.set("Location", "https://example.invalid/private")
            request.sendResponseHeaders(if (request.requestMethod == "PROPFIND") 401 else 302, -1)
            request.close()
        }
        server.start()
        try {
            val client = WebDavBackupClient(WebDavConfig("http://127.0.0.1:${server.address.port}/dav/", "test", "local-test"), allowLocalTestServer = true)
            assertTrue(runCatching { client.list() }.exceptionOrNull()?.message?.contains("认证失败") == true)
            assertTrue(runCatching { client.prepare() }.exceptionOrNull()?.message?.contains("跳转") == true)
        } finally { server.stop(0) }
    }

    @Test fun historyIgnoresOutsideFilesAndRejectsEntityDeclarations() {
        val name = "Gnomon-2026-10-16-history.sleepdown"
        val xml = AtomicReference("<d:multistatus xmlns:d=\"DAV:\">" + listOf(
            "/dav/gnomon-timetable/$name", "/outside/$name", "https://example.invalid/dav/gnomon-timetable/$name",
            "/dav/gnomon-timetable/unrelated.zip"
        ).joinToString("") { "<d:response><d:href>$it</d:href><d:propstat><d:prop/></d:propstat></d:response>" } + "</d:multistatus>")
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/dav/gnomon-timetable/") { request ->
            val body = xml.get().toByteArray()
            request.sendResponseHeaders(207, body.size.toLong())
            request.responseBody.use { it.write(body) }; request.close()
        }
        server.start()
        try {
            val client = WebDavBackupClient(WebDavConfig("http://127.0.0.1:${server.address.port}/dav/", "test", "local-test"), allowLocalTestServer = true)
            assertEquals(listOf(name), client.list().map { it.name })
            xml.set("<!DOCTYPE x [<!ENTITY secret SYSTEM \"file:///private\">]><d:multistatus xmlns:d=\"DAV:\"/>")
            assertTrue(runCatching { client.list() }.exceptionOrNull()?.message?.contains("XML") == true)
        } finally { server.stop(0) }
    }
}
