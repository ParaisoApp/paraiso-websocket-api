package com.paraiso.domain.util

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

fun convertStringToInstant(date: String?) =
    date?.let { dateString ->
        LocalDate.parse(dateString)
            .atStartOfDayIn(TimeZone.UTC)
    }

fun convertStringZToInstant(date: String?) =
    date?.let { dateString ->
        val normalized = if (Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}Z$""").matches(dateString)) {
            dateString.replace("Z", ":00Z") // add missing seconds
        } else {
            dateString
        }
        Instant.parse(normalized)
    }
