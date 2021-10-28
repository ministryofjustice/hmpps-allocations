package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING
import com.opencsv.bean.CsvBindByPosition
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.time.LocalDateTime

data class UnallocatedCase @JsonCreator constructor(
  @CsvBindByPosition(position = 0)
  val name: String,
  @CsvBindByPosition(position = 1)
  val crn: String,
  @CsvBindByPosition(position = 2)
  val tier: String,
  @CsvBindByPosition(position = 3)
  @JsonFormat(pattern = "yyyyMMdd HH:mm:ss", shape = STRING)
  val sentence_date: LocalDateTime,
  @CsvBindByPosition(position = 4)
  @JsonFormat(pattern = "yyyyMMdd HH:mm:ss", shape = STRING)
  val initial_appointment: LocalDateTime?,
  @CsvBindByPosition(position = 5)
  val status: String
) {

  companion object {
    fun from(case: UnallocatedCaseEntity): UnallocatedCase {
      return UnallocatedCase(
        case.name,
        case.crn, case.tier, case.sentence_date, case.initial_appointment, case.status
      )
    }
  }
}
