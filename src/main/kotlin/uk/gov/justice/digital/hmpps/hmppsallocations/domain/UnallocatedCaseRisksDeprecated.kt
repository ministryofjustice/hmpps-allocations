package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.time.LocalDate

data class UnallocatedCaseRisksDeprecated @JsonCreator constructor(
  @Schema(description = "Offender Name", example = "John Smith")
  val name: String,
  @Schema(description = "CRN", example = "J111111")
  val crn: String,
  @Schema(description = "Latest tier of case", example = "D2")
  val tier: String,
  val activeRegistrations: List<UnallocatedCaseRegistration>,
  val inactiveRegistrations: List<UnallocatedCaseRegistration>,
  val rosh: UnallocatedCaseRosh?,
  val rsr: UnallocatedCaseRsr?,
  val ogrs: UnallocatedCaseOgrs?,
  val convictionId: Long,
  @Schema(description = "Case Type")
  val caseType: CaseTypes
) {
  companion object {
    fun from(
      case: UnallocatedCaseEntity,
      activeRegistrations: List<OffenderRegistration>,
      inactiveRegistrations: List<OffenderRegistration>,
      riskSummary: RiskSummary?,
      riskPredictor: RiskPredictor?,
      offenderAssessment: OffenderAssessment?
    ): UnallocatedCaseRisksDeprecated {
      return UnallocatedCaseRisksDeprecated(
        case.name, case.crn, case.tier,
        activeRegistrations.map { UnallocatedCaseRegistration.from(it) },
        inactiveRegistrations.map { UnallocatedCaseRegistration.from(it) },
        riskSummary?.let {
          it.overallRiskLevel?.let { riskLevel ->
            UnallocatedCaseRosh(
              riskLevel,
              it.assessedOn!!.toLocalDate()
            )
          }
        },
        riskPredictor?.let {
          UnallocatedCaseRsr(
            it.rsrScoreLevel!!,
            it.completedDate!!.toLocalDate(),
            it.rsrPercentageScore!!
          )
        },
        offenderAssessment?.let { assessment ->
          assessment.ogrsScore?.let { score ->
            UnallocatedCaseOgrs(
              assessment.ogrsLastUpdate,
              score
            )
          }
        },
        case.convictionId,
        case.caseType
      )
    }
  }
}


data class UnallocatedCaseRosh @JsonCreator constructor(
  @Schema(description = "Level", example = "HIGH")
  val level: String,
  @Schema(description = "last updated on Date", example = "2020-01-16")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
  val lastUpdatedOn: LocalDate
)

