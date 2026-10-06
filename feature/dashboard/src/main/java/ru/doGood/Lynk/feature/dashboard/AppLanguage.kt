package ru.doGood.Lynk.feature.dashboard

enum class AppLanguage(val code: String) {
    RU("ru"),
    EN("en");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: RU
        }
    }
}
