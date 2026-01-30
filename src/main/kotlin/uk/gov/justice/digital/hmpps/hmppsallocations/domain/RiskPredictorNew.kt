package uk.gov.justice.digital.hmpps.hmppsallocations.domain
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import java.time.LocalDateTime

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "outputVersion"
)
@JsonSubTypes(
  JsonSubTypes.Type(value = RiskPredictorV1::class, name = "1"),
  JsonSubTypes.Type(value = RiskPredictorV2::class, name = "2"),
)
data class RiskPredictorNew @JsonCreator constructor(
  val completedDate: LocalDateTime?,
  val source: String?,
  val status: String?,
  val outputVersion: String,
  val output: Any?,
)

@JsonTypeName("1")
data class RiskPredictorV1 @JsonCreator constructor(
  val completedDate: LocalDateTime?,
  val source: String?,
  val status: String?,
  val outputVersion: String,
  val output: RiskPredictorOutputV1?,
)

@JsonTypeName("2")
data class RiskPredictorV2 @JsonCreator constructor(
  val completedDate: LocalDateTime?,
  val source: String?,
  val status: String?,
  val outputVersion: String,
  val output: RiskPredictorOutputV2?,
)
