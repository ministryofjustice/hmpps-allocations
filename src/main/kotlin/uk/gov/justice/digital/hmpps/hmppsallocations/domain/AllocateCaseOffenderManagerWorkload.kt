package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.math.BigInteger

data class AllocateCaseOffenderManagerWorkload @JsonCreator constructor(
  @Schema(description = "Offender Manager forename", example = "John")
  val forename: String,
  @Schema(description = "Offender Manager surname", example = "Smith")
  val surname: String,
  @Schema(description = "Offender Manager Grade", example = "PO")
  val grade: String,
  @Schema(description = "Offender Manager Code", example = "OM1")
  val code: String,
  @Schema(description = "Count of total Community cases (includes licence cases too)", example = "15")
  val totalCommunityCases: BigInteger,
  @Schema(description = "Count of total Custody cases", example = "25")
  val totalCustodyCases: BigInteger,
  @Schema(description = "Offender Manager Capacity in decimal", example = "0.5")
  val capacity: BigDecimal,

) {
  companion object {
    fun from(offenderManagerWorkload: OffenderManagerWorkload): AllocateCaseOffenderManagerWorkload {
      return AllocateCaseOffenderManagerWorkload(
        offenderManagerWorkload.forename,
        offenderManagerWorkload.surname,
        offenderManagerWorkload.grade,
        offenderManagerWorkload.code,
        offenderManagerWorkload.totalCommunityCases,
        offenderManagerWorkload.totalCustodyCases,
        offenderManagerWorkload.capacity
      )
    }
  }
}
