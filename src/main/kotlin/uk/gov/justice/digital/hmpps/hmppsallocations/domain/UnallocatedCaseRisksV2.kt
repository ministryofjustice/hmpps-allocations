package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusRisk
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.Registrations
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class UnallocatedCaseRisksV2 @JsonCreator constructor(
  @Schema(description = "Offender Name", example = "John Smith")
  val name: String,
  @Schema(description = "CRN", example = "J111111")
  val crn: String,
  @Schema(description = "Latest tier of case", example = "D2")
  val tier: String,
  val riskVersion: String,
  val activeRegistrations: List<UnallocatedCaseRegistrationV2>,
  val inactiveRegistrations: List<UnallocatedCaseRegistrationV2>,
  val roshRisk: RoshSummary?,
  val rsr: UnallocatedCaseRsrV2?,
  val ogrs: UnallocatedCaseOgrsV2?,
  val convictionNumber: Int?,
) : UnallocatedCaseRisksNew{
  companion object {
    @Suppress("LongParameterList")
    fun from(
      deliusRisk: DeliusRisk,
      case: UnallocatedCaseEntity,
      rosh: RoshSummary?,
      riskPredictor: RiskPredictorV2?,
    ): UnallocatedCaseRisksV2{
        val riskPredictorOutput = riskPredictor?.output
        return UnallocatedCaseRisksV2(
          case.name,
          case.crn,
          case.tier,
          "2",
          deliusRisk.activeRegistrations.map { UnallocatedCaseRegistrationV2.from(it) },
          deliusRisk.inactiveRegistrations.map { UnallocatedCaseRegistrationV2.from(it) },
          rosh,
          UnallocatedCaseRsrV2.from(riskPredictorOutput, riskPredictor?.completedDate),
          UnallocatedCaseOgrsV2.from(riskPredictorOutput, riskPredictor?.completedDate),
          case.convictionNumber,
          )
    }
  }
}

data class UnallocatedCaseRegistrationV2 @JsonCreator constructor(
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
  val flag: FlagV2,
) {
  companion object {
    fun from(registrations: Registrations): UnallocatedCaseRegistrationV2 = UnallocatedCaseRegistrationV2(
      registrations.description,
      registrations.startDate,
      registrations.notes,
      registrations.endDate,
      FlagV2(registrations.flag.description),
    )
  }
}

data class FlagV2(val description: String)

data class UnallocatedCaseRsrV2 @JsonCreator constructor(
  @Schema(description = "Level", example = "HIGH")
  val level: String?,
  @Schema(description = "last updated on Date", example = "2020-01-16")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
  val lastUpdatedOn: LocalDate?,
  val percentage: BigDecimal?,
) {
  companion object {
    fun from(rp: RiskPredictorOutputV2?, completedDate: LocalDateTime?): UnallocatedCaseRsrV2? = rp?.let { UnallocatedCaseRsrV2(
      it.combinedSeriousReoffendingPredictor?.band,
      completedDate?.toLocalDate(),
      it.combinedSeriousReoffendingPredictor?.score
    ) }
  }
}

data class UnallocatedCaseOgrsV2 @JsonCreator constructor(
  @Schema(description = "last updated on Date", example = "2020-01-16")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
  val lastUpdatedOn: LocalDate?,
  @Schema(description = "Score", example = "62")
  val score: BigDecimal?,
) {
  companion object {
    fun from(ogrs: RiskPredictorOutputV2?,  completedDate: LocalDateTime?): UnallocatedCaseOgrsV2? = ogrs?.let { UnallocatedCaseOgrsV2(
      completedDate?.toLocalDate(),
      it.allReoffendingPredictor?.score
    ) }
  }
}
