package com.flame.astronaut.ui

import java.util.Locale

/** English-only app: always format numbers with 0-9 and "." even on an Arabic-locale phone. */
fun String.fmtUS(vararg args: Any?): String = String.format(Locale.US, this, *args)
