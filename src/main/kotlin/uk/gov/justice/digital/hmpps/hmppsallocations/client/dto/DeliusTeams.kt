package uk.gov.justice.digital.hmpps.hmppsallocations.client.dto

data class DeliusTeams constructor(
  val datasets: List<Provider>,
  val teams: List<TeamWithLau>,
)
