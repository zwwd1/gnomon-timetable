package com.xiaomanjun.sleepdownschedule.feature.backup

import com.xiaomanjun.sleepdownschedule.feature.importing.*

import com.xiaomanjun.sleepdownschedule.*
import com.xiaomanjun.sleepdownschedule.feature.agent.*

import com.xiaomanjun.sleepdownschedule.core.identity.AppIconManager

import android.content.Context
import java.io.File

/** Applies only the protocol's non-secret preference whitelist after the Room commit. */
object BackupPreferencesApplier {
    fun apply(
        context: Context,
        preferences: BackupPreferences,
        scheduleRoomIdsByStableId: Map<String, Int>,
        finalAssetFilesById: Map<String, File>
    ) {
        preferences.presentation?.let { p ->
            val store = cn.scvtc.campus.AppExperiencePreferences.get(context)
            store.entrance(p.entrance)
            store.dock(p.dock)
            store.presentation(store.state.value.copy(showPeriodTimes = p.showPeriodTimes, cardTint = p.cardTint,
                detailOpacity = p.detailOpacity, haptics = p.haptics, predictiveBack = p.predictiveBack,
                materialQuality = p.materialQuality, showBreakDividers = p.showBreakDividers,
                showTeacher = p.showTeacher, showLocation = p.showLocation))
            store.saveGlass(com.xiaomanjun.sleepdownschedule.glass.GlassAppearance.decode(p.glass))
        }
        preferences.appIcon?.let { AppIconManager.applyBackupPreferences(context, it) }
        preferences.dayAgent?.let {
            DayAgentPreferences.applyBackupPreferences(context, it, scheduleRoomIdsByStableId)
        }
        preferences.aiImport?.let { AiImportSettingsStore.applyBackupPreferences(context, it) }
        AiImportHistoryStore.applyBackup(
            context = context,
            entries = preferences.aiImportHistory,
            contextFilesByAssetId = finalAssetFilesById,
            retentionDays = preferences.aiImportHistoryRetentionDays
        )
    }
}
