package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import java.time.LocalDate
import javax.transaction.Transactional

@Service
class JpaBasedUpsertUnallocatedCaseService(
  private val repository: UnallocatedCasesRepository,
  @Qualifier("communityApiClient") private val communityApiClient: CommunityApiClient,
  private val enrichEventService: EnrichEventService,
  @Value("\${unallocates.cases.officer.includes}") private val includedOfficerCodes: List<String>
) : UpsertUnallocatedCaseService {
  @Transactional
  override fun upsertUnallocatedCase(crn: String, convictionId: Long) {
    repository.findCaseByCrnAndConvictionId(crn, convictionId)?.let {
      updateExistingCase(it)
    } ?: run {
      insertNewCase(crn, convictionId)
    }
  }

  private fun updateExistingCase(unallocatedCaseEntity: UnallocatedCaseEntity) {
    communityApiClient.getConviction(unallocatedCaseEntity.crn, unallocatedCaseEntity.convictionId)
      .block()!!.orElse(null)?.let { conviction ->
      if (isUnallocatedIncluded(conviction) && conviction.active) {
        conviction.sentence?.let { sentence ->
          enrichEventService.getTier(unallocatedCaseEntity.crn)?.let { tier ->
            val initialAppointment = enrichEventService.getInitialAppointmentDate(unallocatedCaseEntity.crn, sentence.startDate)
            val name = enrichEventService.getOffenderName(unallocatedCaseEntity.crn)
            val (status, previousConvictionDate, offenderManagerDetails) = enrichEventService.getProbationStatus(unallocatedCaseEntity.crn)

            unallocatedCaseEntity.sentenceDate = sentence.startDate
            unallocatedCaseEntity.initialAppointment = initialAppointment
            unallocatedCaseEntity.tier = tier
            unallocatedCaseEntity.name = name
            unallocatedCaseEntity.status = status.status
            unallocatedCaseEntity.previousConvictionDate = previousConvictionDate
            unallocatedCaseEntity.offenderManagerSurname = offenderManagerDetails?.surname
            unallocatedCaseEntity.offenderManagerForename = offenderManagerDetails?.forenames
            unallocatedCaseEntity.offenderManagerGrade = offenderManagerDetails?.grade
          }
        } ?: run {
          repository.deleteById(unallocatedCaseEntity.id!!)
        }
      } else {
        repository.deleteById(unallocatedCaseEntity.id!!)
      }
    } ?: run {
      repository.deleteById(unallocatedCaseEntity.id!!)
    }
  }

  private fun insertNewCase(crn: String, convictionId: Long) {
    communityApiClient.getConviction(crn, convictionId)
      .block()!!.ifPresent { conviction ->
      if (isUnallocatedIncluded(conviction) && conviction.active) {
        conviction.sentence?.let { sentence ->
          enrichEventService.getTier(crn)?.let { tier ->
            val initialAppointment = enrichEventService.getInitialAppointmentDate(crn, sentence.startDate)
            val name = enrichEventService.getOffenderName(crn)
            val (status, previousConvictionDate, offenderManagerDetails) = enrichEventService.getProbationStatus(crn)

            val unallocatedCase = UnallocatedCaseEntity(
              null, name,
              crn, tier, sentence.startDate, initialAppointment, status.status, previousConvictionDate,
              offenderManagerSurname = offenderManagerDetails?.surname,
              offenderManagerForename = offenderManagerDetails?.forenames,
              offenderManagerGrade = offenderManagerDetails?.grade,
              convictionId = convictionId
            )

            repository.save(unallocatedCase)
          } ?: run {
            log.info("no tier for crn $crn so unable to allocate")
          }
        } ?: run {
          log.info("No sentence for crn $crn so unable to allocate")
        }
      } else {
        log.info("crn $crn already allocated")
      }
    }
  }

  private fun isUnallocatedIncluded(conviction: Conviction): Boolean {
    return includedOfficerCodes.contains(conviction.orderManagers.maxByOrNull { it.dateStartOfAllocation ?: LocalDate.MIN }?.staffCode)
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
