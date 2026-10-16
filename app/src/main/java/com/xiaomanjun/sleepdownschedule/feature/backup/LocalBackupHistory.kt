package com.xiaomanjun.sleepdownschedule.feature.backup

import com.xiaomanjun.sleepdownschedule.CourseScheduleApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.util.UUID

class LocalBackupHistory(private val app: CourseScheduleApp) {
    private val directory = File(app.filesDir, "backup_history")
    fun list(): List<File> = directory.listFiles().orEmpty().filter {
        it.isFile && it.name.startsWith("Gnomon-") && it.extension == "sleepdown"
    }.sortedByDescending { it.name }
    suspend fun create(): File = withContext(Dispatchers.IO) {
        check(directory.isDirectory || directory.mkdirs()) { "无法创建本地备份目录" }
        val name = "Gnomon-${Instant.now().toString().replace(':', '-')}-${UUID.randomUUID()}.sleepdown"
        val target = File(directory, name)
        val temporary = File(directory, "$name.part")
        try {
            val archive = BackupExportService(app, app.database).export()
            temporary.outputStream().use { BackupCodec.write(it, archive) }
            temporary.inputStream().use { BackupCodec.decode(it) }
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
            target
        } finally { temporary.delete() }
    }
    fun delete(file: File) {
        require(file.canonicalFile.parentFile == directory.canonicalFile && file in list()) { "备份目标已失效" }
        check(file.delete()) { "本地备份删除失败" }
    }
}
