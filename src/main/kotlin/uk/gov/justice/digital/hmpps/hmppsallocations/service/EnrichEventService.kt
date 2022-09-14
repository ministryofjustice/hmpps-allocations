package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.config.CacheConfiguration
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseTypes
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.OffenderManagerDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import uk.gov.justice.digital.hmpps.hmppsallocations.service.ProbationStatusType.CURRENTLY_MANAGED
import uk.gov.justice.digital.hmpps.hmppsallocations.service.ProbationStatusType.NEW_TO_PROBATION
import uk.gov.justice.digital.hmpps.hmppsallocations.service.ProbationStatusType.PREVIOUSLY_MANAGED
import java.time.LocalDate

@Service
class EnrichEventService(
  @Qualifier("communityApiClient") private val communityApiClient: CommunityApiClient,
  @Qualifier("hmppsTierApiClient") private val hmppsTierApiClient: HmppsTierApiClient,
  private val unallocatedCasesRepository: UnallocatedCasesRepository
) {

  fun getInitialAppointmentDate(crn: String, contactsFromDate: LocalDate): LocalDate? {
    return communityApiClient.getInductionContacts(crn, contactsFromDate)
      .mapNotNull { contacts ->
        contacts.maxByOrNull { c -> c.contactStart }?.contactStart?.toLocalDate()
      }
      .block()
  }

  fun getOffenderName(crn: String): String {
    return communityApiClient.getOffenderDetails(crn)
      .map { "${it.firstName} ${it.surname}" }
      .block()!!
  }

  fun getTier(crn: String): String? {
    return hmppsTierApiClient.getTierByCrn(crn)
  }

  fun getActiveSentencedConvictions(crn: String): List<Conviction> {
    return communityApiClient.getActiveConvictions(crn).filter { conviction -> conviction.sentence != null }
      .collectList()
      .block() ?: emptyList()
  }

  fun getProbationStatus(crn: String, activeConvictions: List<Conviction>): ProbationStatus {
    val offenderManager = communityApiClient.getOffenderManagerName(crn)
      .block()!!
    return when {
      activeConvictions.size > 1 && !offenderManager.isUnallocated -> {
        ProbationStatus(CURRENTLY_MANAGED, offenderManagerDetails = OffenderManagerDetails.from(offenderManager))
      }
      else -> {
        val inactiveConvictions = communityApiClient.getInactiveConvictions(crn).block() ?: emptyList()
        return when {
          inactiveConvictions.isNotEmpty() -> {
            val mostRecentInactiveConvictionEndDate =
              inactiveConvictions.filter { c -> c.sentence?.terminationDate != null }
                .map { c -> c.sentence!!.terminationDate!! }
                .maxByOrNull { it }
            ProbationStatus(
              PREVIOUSLY_MANAGED,
              mostRecentInactiveConvictionEndDate,
              OffenderManagerDetails.from(offenderManager)
            )
          }
          else -> ProbationStatus(
            NEW_TO_PROBATION,
            offenderManagerDetails = OffenderManagerDetails.from(offenderManager)
          )
        }
      }
    }
  }

  fun getAllConvictionIdsAssociatedToCrn(crn: String): Set<Long> =
    unallocatedCasesRepository.findConvictionIdsByCrn(crn).let { storedConvictions ->
      val storedConvictionIds = storedConvictions.map { it.getConvictionId() }
      val convictionIds = communityApiClient.getActiveConvictions(crn)
        .map { conviction -> conviction.convictionId }.collectList().block()
      return listOf(storedConvictionIds, convictionIds).flatten().toSet()
    }

  @Cacheable(CacheConfiguration.INDUCTION_APPOINTMENT_CACHE_NAME)
  fun enrichInductionAppointment(unallocatedCaseEntity: UnallocatedCaseEntity): Mono<UnallocatedCaseEntity> {
    if (inductionCaseTypes.contains(unallocatedCaseEntity.caseType)) {
      return communityApiClient.getInductionContacts(unallocatedCaseEntity.crn, unallocatedCaseEntity.sentenceDate)
        .filter { unallocatedCasesRepository.existsById(unallocatedCaseEntity.id!!) }
        .map { contacts ->
          unallocatedCaseEntity.initialAppointment = contacts.map { it.contactStart }.maxByOrNull { it }?.toLocalDate()
          unallocatedCasesRepository.save(unallocatedCaseEntity)
        }
    }
    return Mono.just(unallocatedCaseEntity)
  }

  companion object {
    private val inductionCaseTypes = setOf(CaseTypes.COMMUNITY, CaseTypes.LICENSE)
  }
}

data class ProbationStatus(
  val status: ProbationStatusType,
  val previousConvictionDate: LocalDate? = null,
  val offenderManagerDetails: OffenderManagerDetails? = null
)

enum class ProbationStatusType(
  val status: String
) {
  CURRENTLY_MANAGED("Currently managed"),
  PREVIOUSLY_MANAGED("Previously managed"),
  NEW_TO_PROBATION("New to probation")
}
