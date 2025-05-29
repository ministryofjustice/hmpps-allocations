package uk.gov.justice.digital.hmpps.hmppsallocations.client.dto

data class DeliusTeams constructor(
  val datasets: List<Dataset>,
  val teams: List<TeamWithLau>,
)

data class Dataset(
  val code: String,
  val description: String,
)
