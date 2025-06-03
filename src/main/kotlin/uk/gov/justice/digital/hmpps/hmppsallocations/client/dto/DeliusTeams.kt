package uk.gov.justice.digital.hmpps.hmppsallocations.client.dto

data class DeliusTeams constructor(
  val datasets: List<Dataset> = emptyList(),
  val teams: List<TeamWithLau> = emptyList(),
)

data class Dataset(
  val code: String = "",
  val description: String = "",
)
