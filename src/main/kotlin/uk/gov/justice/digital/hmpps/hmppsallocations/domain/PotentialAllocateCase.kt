package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.math.BigDecimal

data class PotentialAllocateCase @JsonCreator constructor(
  @Schema(description = "Offender Name", example = "John Smith")
  val name: String,
  @Schema(description = "CRN", example = "J111111")
  val crn: String,
  @Schema(description = "Latest tier of case", example = "D2")
  val tier: String,
  @Schema(description = "Offender Manager Forename", example = "John")
  val offenderManagerForename: String,
  @Schema(description = "Offender Manager Surname", example = "Doe")
  val offenderManagerSurname: String,
  @Schema(description = "Offender Manager Grade", example = "PO")
  var offenderManagerGrade: String,
  @Schema(description = "Offender Manager Current Capacity", example = "50.4")
  val offenderManagerCurrentCapacity: BigDecimal,
  @Schema(description = "Offender Manager Code", example = "OM1")
  val offenderManagerCode: String,
  @Schema(description = "Offender Manager Potential Capacity", example = "64.8")
  val offenderManagerPotentialCapacity: BigDecimal,
  val convictionId: Long,
  @Schema(description = "Case Type")
  val caseType: CaseTypes
) {
  companion object {
    fun from(unallocatedCaseEntity: UnallocatedCaseEntity, potentialOffenderManagerWorkload: PotentialOffenderManagerWorkload, grade: String): PotentialAllocateCase {
      return PotentialAllocateCase(
        unallocatedCaseEntity.name,
        unallocatedCaseEntity.crn,
        unallocatedCaseEntity.tier,
        potentialOffenderManagerWorkload.forename,
        potentialOffenderManagerWorkload.surname,
        grade,
        potentialOffenderManagerWorkload.capacity,
        potentialOffenderManagerWorkload.code,
        potentialOffenderManagerWorkload.potentialCapacity,
        unallocatedCaseEntity.convictionId,
        unallocatedCaseEntity.caseType
      )
    }
  }
}
