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

data class UnallocatedCaseRisksV1 @JsonCreator constructor(
  @Schema(description = "Offender Name", example = "John Smith")
  val name: String,
  @Schema(description = "CRN", example = "J111111")
  val crn: String,
  @Schema(description = "Latest tier of case", example = "D2")
  val tier: String,
  val riskVersion: String,
  val activeRegistrations: List<UnallocatedCaseRegistrationV1>,
  val inactiveRegistrations: List<UnallocatedCaseRegistrationV1>,
  val roshRisk: RoshSummary?,
  val rsr: UnallocatedCaseRsrV1?,
  val ogrs: UnallocatedCaseOgrsV1?,
  val convictionNumber: Int?,
) : UnallocatedCaseRisksNew(){
  companion object {
    @Suppress("LongParameterList")
    fun from(
      deliusRisk: DeliusRisk,
      case: UnallocatedCaseEntity,
      rosh: RoshSummary?,
      riskPredictor: RiskPredictorNew?,
    ): UnallocatedCaseRisksV1{
        val riskPredictorOutput = riskPredictor?.output?.getRiskPredictorOutputV1()
        return UnallocatedCaseRisksV1(
          case.name,
          case.crn,
          case.tier,
          "1",
          deliusRisk.activeRegistrations.map { UnallocatedCaseRegistrationV1.from(it) },
          deliusRisk.inactiveRegistrations.map { UnallocatedCaseRegistrationV1.from(it) },
          rosh,
          UnallocatedCaseRsrV1.from(riskPredictorOutput, riskPredictor?.completedDate),
          UnallocatedCaseOgrsV1.from(riskPredictorOutput, riskPredictor?.completedDate),
          case.convictionNumber,
          )
    }
  }
}

data class UnallocatedCaseRegistrationV1 @JsonCreator constructor(
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
  val flag: FlagV1,
) {
  companion object {
    fun from(registrations: Registrations): UnallocatedCaseRegistrationV1 = UnallocatedCaseRegistrationV1(
      registrations.description,
      registrations.startDate,
      registrations.notes,
      registrations.endDate,
      FlagV1(registrations.flag.description),
    )
  }
}

data class FlagV1(val description: String)

data class UnallocatedCaseRsrV1 @JsonCreator constructor(
  @Schema(description = "Level", example = "HIGH")
  val level: String?,
  @Schema(description = "last updated on Date", example = "2020-01-16")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
  val lastUpdatedOn: LocalDate?,
  val percentage: BigDecimal?,
) {
  companion object {
    fun from(rp: RiskPredictorOutputV1?, completedDate: LocalDateTime?): UnallocatedCaseRsrV1? = rp?.let { UnallocatedCaseRsrV1(
      it.riskOfSeriousRecidivismScore?.scoreLevel,
      completedDate?.toLocalDate(),
      it.riskOfSeriousRecidivismScore?.percentageScore
    ) }
  }
}


data class UnallocatedCaseOgrsV1 @JsonCreator constructor(
  @Schema(description = "last updated on Date", example = "2020-01-16")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
  val lastUpdatedOn: LocalDate?,
  @Schema(description = "Score", example = "62")
  val score: BigDecimal?,
) {
  companion object {
    fun from(ogrs: RiskPredictorOutputV1?,  completedDate: LocalDateTime?): UnallocatedCaseOgrsV1? = ogrs?.let { UnallocatedCaseOgrsV1(
      completedDate?.toLocalDate(),
      it.groupReconvictionScore?.twoYears
    ) }
  }
}
