package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusRisk
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.Ogrs
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.Registrations
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

data class UnallocatedCaseRisks @JsonCreator constructor(
  @Schema(description = "Offender Name", example = "John Smith")
  val name: String,
  @Schema(description = "CRN", example = "J111111")
  val crn: String,
  @Schema(description = "Latest tier of case", example = "D2")
  val tier: String,
  val activeRegistrations: List<UnallocatedCaseRegistration>,
  val inactiveRegistrations: List<UnallocatedCaseRegistration>,
  val roshRisk: RoshSummary?,
  val rsr: UnallocatedCaseRsr?,
  val ogrs: UnallocatedCaseOgrs?,
  val convictionNumber: Int,
) {
  companion object {
    @Suppress("LongParameterList")
    fun from(
      deliusRisk: DeliusRisk,
      case: UnallocatedCaseEntity,
      rosh: RoshSummary?,
      riskPredictor: RiskPredictor?,
    ): UnallocatedCaseRisks {
      return UnallocatedCaseRisks(
        case.name,
        case.crn,
        case.tier,
        deliusRisk.activeRegistrations.map { UnallocatedCaseRegistration.from(it) },
        deliusRisk.inactiveRegistrations.map { UnallocatedCaseRegistration.from(it) },
        rosh,
        UnallocatedCaseRsr.from(riskPredictor),
        UnallocatedCaseOgrs.from(deliusRisk.ogrs),
        case.convictionNumber,
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
  @Schema(description = "Notes", example = "Previous suicide /self-harm attempt. Needs further investigating.")
  val notes: String?,
  @Schema(description = "End Date", example = "2020-01-16")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
  val endDate: LocalDate?,
  @Schema(description = "Flag", example = "RoSH")
  @JsonFormat(shape = JsonFormat.Shape.OBJECT)
  val flag: Flag,
) {
  companion object {
    fun from(registrations: Registrations): UnallocatedCaseRegistration {
      return UnallocatedCaseRegistration(
        registrations.description,
        registrations.startDate,
        registrations.notes,
        registrations.endDate,
        Flag(registrations.flag.description),
      )
    }
  }
}

data class Flag(val description: String)

data class UnallocatedCaseRsr @JsonCreator constructor(
  @Schema(description = "Level", example = "HIGH")
  val level: String?,
  @Schema(description = "last updated on Date", example = "2020-01-16")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
  val lastUpdatedOn: LocalDate?,
  val percentage: BigDecimal?,
) {
  companion object {
    fun from(rp: RiskPredictor?): UnallocatedCaseRsr? {
      return rp?.let { UnallocatedCaseRsr(it.rsrScoreLevel, it.completedDate?.toLocalDate(), it.rsrPercentageScore) }
    }
  }
}

data class UnallocatedCaseOgrs @JsonCreator constructor(
  @Schema(description = "last updated on Date", example = "2020-01-16")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
  val lastUpdatedOn: LocalDate,
  @Schema(description = "Score", example = "62")
  val score: BigInteger,
) {
  companion object {
    fun from(ogrs: Ogrs?): UnallocatedCaseOgrs? {
      return ogrs?.let { UnallocatedCaseOgrs(it.lastUpdatedDate, it.score) }
    }
  }
}
