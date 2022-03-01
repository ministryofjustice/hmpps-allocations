package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

data class UnallocatedCaseRisks @JsonCreator constructor (
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
  val convictionId: Long
) {
  companion object {
    fun from(
      case: UnallocatedCaseEntity,
      activeRegistrations: List<OffenderRegistration>,
      inactiveRegistrations: List<OffenderRegistration>,
      riskSummary: RiskSummary?,
      riskPredictor: RiskPredictor?,
      offenderAssessment: OffenderAssessment?
    ): UnallocatedCaseRisks {
      return UnallocatedCaseRisks(
        case.name, case.crn, case.tier,
        activeRegistrations.map { UnallocatedCaseRegistration.from(it) },
        inactiveRegistrations.map { UnallocatedCaseRegistration.from(it) },
        riskSummary?.let { it.overallRiskLevel?.let { riskLevel -> UnallocatedCaseRosh(riskLevel, it.assessedOn!!.toLocalDate()) } },
        riskPredictor?.let { UnallocatedCaseRsr(it.rsrScoreLevel!!, it.completedDate!!.toLocalDate(), it.rsrPercentageScore!!) },
        offenderAssessment?.let { UnallocatedCaseOgrs(it.ogrsLastUpdate!!, it.ogrsScore!!) },
        case.convictionId
      )
    }
  }
}

data class UnallocatedCaseRegistration @JsonCreator constructor(
  @Schema(description = "Type", example = "Suicide/self-harm")
  val type: String,
  @Schema(description = "Registered date", example = "2020-03-21")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
  val registered: LocalDate,
  @Schema(description = "Next review Date", example = "2020-01-16")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
  val nextReviewDate: LocalDate?,
  @Schema(description = "Notes", example = "Previous suicide /self-harm attempt. Needs further investigating.")
  val notes: String?,
  @Schema(description = "End Date", example = "2020-01-16")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
  val endDate: LocalDate?,
) {
  companion object {
    fun from(offenderRegistration: OffenderRegistration): UnallocatedCaseRegistration {
      return UnallocatedCaseRegistration(
        offenderRegistration.type.description,
        offenderRegistration.startDate,
        offenderRegistration.nextReviewDate,
        offenderRegistration.notes,
        offenderRegistration.endDate
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

data class UnallocatedCaseRsr @JsonCreator constructor(
  @Schema(description = "Level", example = "HIGH")
  val level: String,
  @Schema(description = "last updated on Date", example = "2020-01-16")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
  val lastUpdatedOn: LocalDate,
  val percentage: BigDecimal
)

data class UnallocatedCaseOgrs @JsonCreator constructor(
  @Schema(description = "last updated on Date", example = "2020-01-16")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
  val lastUpdatedOn: LocalDate,
  @Schema(description = "Score", example = "62")
  val score: BigInteger
)
