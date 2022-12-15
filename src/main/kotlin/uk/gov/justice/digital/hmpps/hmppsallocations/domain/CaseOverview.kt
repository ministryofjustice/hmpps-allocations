package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.time.LocalDate

data class CaseOverview @JsonCreator constructor(
  @Schema(description = "Offender Name", example = "John Smith")
  val name: String,
  @Schema(description = "CRN", example = "J111111")
  val crn: String,
  @Schema(description = "Latest tier of case", example = "D2")
  val tier: String,
  @Schema(description = "Initial Appointment Date", example = "2020-03-21")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
  val initialAppointment: LocalDate?,
  @Schema(description = "Conviction Id")
  val convictionId: Long,
  @Schema(description = "Case Type")
  val caseType: CaseTypes,
  @Schema(description = "Conviction Number")
  val convictionNumber: Int
) {
  companion object {
    fun from(case: UnallocatedCaseEntity): CaseOverview {
      return CaseOverview(
        case.name,
        case.crn, case.tier, case.initialAppointment,
        case.convictionId,
        case.caseType,
        case.convictionNumber
      )
    }
  }
}
