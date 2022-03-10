package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDateTime

data class OfficerOverviewAllocateCase @JsonCreator constructor(
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
  @Schema(description = "Offender Manager Total Cases", example = "35")
  val offenderManagerTotalCases: BigDecimal,
  val convictionId: Long,
  @Schema(description = "Team Name", example = "Test Team")
  val teamName: String,
  @Schema(description = "Offender Manager Weekly Contracted Hours", example = "35")
  val offenderManagerWeeklyHours: BigDecimal,
  @Schema(description = "Offender Manager Total Reduction Hours", example = "35")
  val offenderManagerTotalReductionHours: BigDecimal,
  @Schema(description = "Offender Manager Points Available", example = "35")
  val offenderManagerPointsAvailable: BigInteger,
  @Schema(description = "Offender Manager Points Used", example = "35")
  val offenderManagerPointsUsed: BigInteger,
  @Schema(description = "Offender Manager Points Remaining", example = "35")
  val offenderManagerPointsRemaining: BigInteger,
  @Schema(description = "Last time the Capacity was updated", example = "2013-11-03T09:00:00")
  val lastUpdatedOn: LocalDateTime?
) {
  companion object {
    fun from(unallocatedCaseEntity: UnallocatedCaseEntity, offenderManagerOverview: OffenderManagerOverview, grade: String): OfficerOverviewAllocateCase {
      return OfficerOverviewAllocateCase(
        unallocatedCaseEntity.name,
        unallocatedCaseEntity.crn,
        unallocatedCaseEntity.tier,
        offenderManagerOverview.forename,
        offenderManagerOverview.surname,
        grade,
        offenderManagerOverview.capacity,
        offenderManagerOverview.code,
        offenderManagerOverview.totalCases,
        unallocatedCaseEntity.convictionId,
        offenderManagerOverview.teamName,
        offenderManagerOverview.weeklyHours,
        offenderManagerOverview.totalReductionHours,
        offenderManagerOverview.pointsAvailable,
        offenderManagerOverview.pointsUsed,
        offenderManagerOverview.pointsRemaining,
        offenderManagerOverview.lastUpdatedOn
      )
    }
  }
}
