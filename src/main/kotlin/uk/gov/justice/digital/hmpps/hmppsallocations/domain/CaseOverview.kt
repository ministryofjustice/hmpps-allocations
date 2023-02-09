package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity

data class CaseOverview @JsonCreator constructor(
  @Schema(description = "Offender Name", example = "John Smith")
  val name: String,
  @Schema(description = "CRN", example = "J111111")
  val crn: String,
  @Schema(description = "Latest tier of case", example = "D2")
  val tier: String,
  @Schema(description = "Conviction Id")
  val convictionId: Long,
  @Schema(description = "Conviction Number")
  val convictionNumber: Int
) {
  companion object {
    fun from(case: UnallocatedCaseEntity): CaseOverview {
      return CaseOverview(
        case.name,
        case.crn, case.tier,
        case.convictionId,
        case.convictionNumber
      )
    }
  }
}
