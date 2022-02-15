package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity

data class PotentialAllocateCase @JsonCreator constructor(
  @Schema(description = "Offender Name", example = "John Smith")
  val name: String,
  @Schema(description = "CRN", example = "J111111")
  val crn: String,
  @Schema(description = "Latest tier of case", example = "D2")
  val tier: String,
) {
  companion object {
    fun from(unallocatedCaseEntity: UnallocatedCaseEntity): PotentialAllocateCase {
      return PotentialAllocateCase(
        unallocatedCaseEntity.name,
        unallocatedCaseEntity.crn,
        unallocatedCaseEntity.tier
      )
    }
  }
}
