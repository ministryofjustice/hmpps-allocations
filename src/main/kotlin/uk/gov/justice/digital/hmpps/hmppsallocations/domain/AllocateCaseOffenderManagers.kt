package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity

data class AllocateCaseOffenderManagers @JsonCreator constructor(
  @Schema(description = "Offender Name", example = "John Smith")
  val name: String,
  @Schema(description = "CRN", example = "J111111")
  val crn: String,
  @Schema(description = "Latest tier of case", example = "D2")
  val tier: String,
  @Schema(description = "Probation Status", example = "Currently managed")
  val status: String,
  val offenderManager: OffenderManagerDetails,
  val offenderManagersToAllocate: List<AllocateCaseOffenderManagerWorkload>
) {
  companion object {
    fun from(unallocatedCaseEntity: UnallocatedCaseEntity, offenderManagers: List<AllocateCaseOffenderManagerWorkload>): AllocateCaseOffenderManagers {
      return AllocateCaseOffenderManagers(
        unallocatedCaseEntity.name,
        unallocatedCaseEntity.crn,
        unallocatedCaseEntity.tier,
        unallocatedCaseEntity.status,
        OffenderManagerDetails(
          unallocatedCaseEntity.offenderManagerForename,
          unallocatedCaseEntity.offenderManagerSurname,
          unallocatedCaseEntity.offenderManagerGrade
        ),
        offenderManagers
      )
    }
  }
}
