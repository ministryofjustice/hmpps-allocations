package uk.gov.justice.digital.hmpps.hmppsallocations.client

import com.fasterxml.jackson.annotation.JsonCreator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Contact
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.ConvictionRequirements
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CourtReport
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.InactiveConviction
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.OffenderSummary
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class CommunityApiClient(@Qualifier("communityWebClientAppScope") private val webClient: WebClient) {

  fun getActiveConvictions(crn: String): List<Conviction> {
    val responseType = object : ParameterizedTypeReference<List<Conviction>>() {}

    return webClient
      .get()
      .uri("/offenders/crn/$crn/convictions?activeOnly=true")
      .retrieve()
      .bodyToMono(responseType)
      .block() ?: listOf()
  }
  fun getActiveRequirements(crn: String, convictionId: Long): ConvictionRequirements {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/convictions/$convictionId/requirements?activeOnly=true")
      .retrieve()
      .bodyToMono(ConvictionRequirements::class.java)
      .block()!!
  }

  fun getInactiveConvictions(crn: String): List<InactiveConviction> {
    val responseType = object : ParameterizedTypeReference<List<InactiveConviction>>() {}
    return webClient
      .get()
      .uri("/offenders/crn/$crn/convictions")
      .retrieve()
      .bodyToMono(responseType)
      .block()
      ?.filter { c -> !c.active }
      ?: listOf()
  }

  fun getInductionContacts(crn: String, contactDateFrom: LocalDate): List<Contact> {
    val responseType = object : ParameterizedTypeReference<List<Contact>>() {}
    val contactDateFromQuery = contactDateFrom.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    return webClient
      .get()
      .uri("/offenders/crn/$crn/contact-summary/inductions?contactDateFrom=$contactDateFromQuery")
      .retrieve()
      .bodyToMono(responseType)
      .block() ?: listOf()
  }

  fun getOffenderSummary(crn: String): OffenderSummary {
    return webClient
      .get()
      .uri("/offenders/crn/$crn")
      .retrieve()
      .bodyToMono(OffenderSummary::class.java)
      .block()!!
  }

  fun getOffenderManagerName(crn: String): OffenderManager {
    val responseType = object : ParameterizedTypeReference<List<OffenderManager>>() {}

    return webClient
      .get()
      .uri("/offenders/crn/$crn/allOffenderManagers")
      .retrieve()
      .bodyToMono(responseType)
      .block().first() // Not sure if will always be just one. Needs to be checked.
  }

  fun getCourtReports(crn: String, convictionId: Long): List<CourtReport> {
    val responseType = object : ParameterizedTypeReference<List<CourtReport>>() {}

    return webClient
      .get()
      .uri("/offenders/crn/$crn/convictions/$convictionId/courtReports")
      .retrieve()
      .bodyToMono(responseType)
      .block()
  }

  data class Staff @JsonCreator constructor(
    val forenames: String,
    val surname: String
  )

  data class Grade @JsonCreator constructor(
    val code: String
  )

  data class OffenderManager @JsonCreator constructor(val staff: Staff, val grade: Grade?)
}
