/*
 * Copyright (c) 2024 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

package de.fraunhofer.iem.spha.adapter.tools.tlc.util

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.abs

internal object TimeHelper {
    fun dateToMs(dateString: String): Long {
        val formatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val dateTime: OffsetDateTime = OffsetDateTime.parse(dateString, formatter)
        return dateTime.toInstant().toEpochMilli()
    }

    fun getDifferenceInDays(currentVersion: Long, newestVersion: Long): Long {
        return getTimeDiff(currentVersion, newestVersion)
    }

    fun getDifferenceInWeeks(currentVersion: Long, newestVersion: Long): Double {
        return getTimeDiff(currentVersion, newestVersion) / 7.0
    }

    fun getDifferenceInMonths(currentVersion: Long, newestVersion: Long): Double {
        return getTimeDiff(currentVersion, newestVersion) / 30.0
    }

    private fun getTimeDiff(currentVersion: Long, newestVersion: Long): Long {
        val currentVersionTime = Date(currentVersion).toInstant()
        val newestVersionTime = Date(newestVersion).toInstant()

        val differenceInDays = ChronoUnit.DAYS.between(currentVersionTime, newestVersionTime)

        return differenceInDays
    }

    fun isWithinOneYear(date1: Long, date2: Long): Boolean {
        val oneYearInMilliseconds = TimeUnit.DAYS.toMillis(365)
        return abs(date1 - date2) <= oneYearInMilliseconds
    }
}
