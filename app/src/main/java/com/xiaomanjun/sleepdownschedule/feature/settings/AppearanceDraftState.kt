package com.xiaomanjun.sleepdownschedule.feature.settings

import androidx.compose.runtime.saveable.listSaver
import cn.scvtc.campus.AppExperience
import com.xiaomanjun.sleepdownschedule.model.DefaultWallpaperStyle
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity

/** Save just this editor's fields across recreation; never serialize unrelated account state. */
internal fun appearanceConfigSaver(base: ScheduleConfigEntity) = listSaver<ScheduleConfigEntity, Any>(
    save = { listOf(it.wallpaperUri.orEmpty(), it.wallpaperBlur, it.wallpaperBrightness, it.defaultWallpaperStyle.name,
        it.wallpaperPortraitCenterX ?: -1f, it.wallpaperPortraitCenterY ?: -1f, it.wallpaperPortraitScale ?: -1f,
        it.wallpaperLandscapeCenterX ?: -1f, it.wallpaperLandscapeCenterY ?: -1f, it.wallpaperLandscapeScale ?: -1f,
        it.weekCardHeightScale, it.weekCardHeightDp ?: -1f, it.weekCardCornerProgress, it.courseCardFontScale, it.cardAlpha,
        it.wallpaperSourceWidth ?: -1, it.wallpaperSourceHeight ?: -1) },
    restore = { base.copy(wallpaperUri = (it[0] as String).takeIf(String::isNotBlank),
        wallpaperBlur = it[1] as Float, wallpaperBrightness = it[2] as Float,
        defaultWallpaperStyle = DefaultWallpaperStyle.valueOf(it[3] as String),
        wallpaperPortraitCenterX = (it[4] as Float).takeIf { value -> value >= 0f },
        wallpaperPortraitCenterY = (it[5] as Float).takeIf { value -> value >= 0f },
        wallpaperPortraitScale = (it[6] as Float).takeIf { value -> value >= 0f },
        wallpaperLandscapeCenterX = (it[7] as Float).takeIf { value -> value >= 0f },
        wallpaperLandscapeCenterY = (it[8] as Float).takeIf { value -> value >= 0f },
        wallpaperLandscapeScale = (it[9] as Float).takeIf { value -> value >= 0f },
        weekCardHeightScale = it[10] as Float, weekCardHeightDp = (it[11] as Float).takeIf { height -> height >= 0f },
        weekCardCornerProgress = it[12] as Float, courseCardFontScale = it[13] as Float, cardAlpha = it[14] as Float,
        wallpaperSourceWidth = (it[15] as Int).takeIf { value -> value > 0 },
        wallpaperSourceHeight = (it[16] as Int).takeIf { value -> value > 0 }) }
)

internal fun appearancePresentationSaver(base: AppExperience) = listSaver<AppExperience, Any>(
    save = { listOf(it.showPeriodTimes, it.cardTint, it.detailOpacity, it.showBreakDividers, it.showTeacher, it.showLocation) },
    restore = { base.copy(showPeriodTimes = it[0] as Boolean, cardTint = it[1] as Float, detailOpacity = it[2] as Float,
        showBreakDividers = it[3] as Boolean, showTeacher = it[4] as Boolean, showLocation = it[5] as Boolean) }
)
