package uk.gov.justice.digital.hmpps.hmppsallocations.service

import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

fun getWmtPeriod(now: LocalDateTime): String {
  var startOfPeriod = now.minusDays(1).atSixThirty()
  var endOfPeriod = now.atSixThirty()

  if (now.isAfterSixThirty()) {
    startOfPeriod = now.atSixThirty()
    endOfPeriod = now.plusDays(1).atSixThirty()
    if (now.dayOfWeek == TUESDAY) {
      endOfPeriod = endOfPeriod.atSevenThirty()
    }
  }

  if (now.dayOfWeek == WEDNESDAY) {
    if (now.isAfterSevenThirty()) {
      startOfPeriod = now.atSevenThirty()
    } else {
      startOfPeriod = now.minusDays(1).atSixThirty()
      endOfPeriod = now.atSevenThirty()
    }
  }

  return "${startOfPeriod.format(formatter)} to ${endOfPeriod.format(formatter)}"
}

fun LocalDateTime.atSixThirty(): LocalDateTime = this.withHour(18).withMinute(30)

fun LocalDateTime.atSevenThirty(): LocalDateTime = this.withHour(19).withMinute(30)

fun LocalDateTime.isAfterSixThirty(): Boolean = this.isAfter(LocalDateTime.of(this.year, this.month, this.dayOfMonth, 18, 30))

fun LocalDateTime.isAfterSevenThirty(): Boolean = this.isAfter(LocalDateTime.of(this.year, this.month, this.dayOfMonth, 19, 30))
