package org.f7.motoactions.util

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {
    /** Empty / "system" → follow device. Tags: en, pt, es, zh-CN, zh-TW, hi, ru */
    fun wrap(context: Context, languageTag: String): Context {
        val tag = languageTag.trim()
        if (tag.isEmpty() || tag.equals("system", ignoreCase = true)) {
            return context
        }
        val locale = Locale.forLanguageTag(tag.replace('_', '-'))
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(locale))
        return context.createConfigurationContext(config)
    }

    data class Option(val tag: String, val label: String)

    val OPTIONS =
        listOf(
            Option("system", "System"),
            Option("en", "English"),
            Option("pt", "Português"),
            Option("es", "Español"),
            Option("zh-CN", "简体中文"),
            Option("zh-TW", "繁體中文"),
            Option("hi", "हिन्दी"),
            Option("ru", "Русский"),
        )
}
