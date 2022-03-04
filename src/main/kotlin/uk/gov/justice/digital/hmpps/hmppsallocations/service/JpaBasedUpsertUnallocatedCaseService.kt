package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.config.CaseOfficerConfigProperties
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseTypes
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import java.time.LocalDate
import java.time.LocalDateTime
import javax.transaction.Transactional

@Service
class JpaBasedUpsertUnallocatedCaseService(
  private val repository: UnallocatedCasesRepository,
  @Qualifier("communityApiClient") private val communityApiClient: CommunityApiClient,
  private val enrichEventService: EnrichEventService,
  private val caseOfficerConfigProperties: CaseOfficerConfigProperties,
  private val caseTypeEngine: CaseTypeEngine
) : UpsertUnallocatedCaseService {
  @Transactional
  override fun upsertUnallocatedCase(crn: String, convictionId: Long) {
    repository.findCaseByCrnAndConvictionId(crn, convictionId)?.let {
      updateExistingCase(it)
    } ?: run {
      updateExistingCase(UnallocatedCaseEntity(null, "", crn, "", LocalDate.now(), null, "", null, null, null, null, convictionId, CaseTypes.COMMUNITY))?.let {
        repository.save(it)
      }
    }
  }

  private fun updateExistingCase(unallocatedCaseEntity: UnallocatedCaseEntity): UnallocatedCaseEntity? {
    communityApiClient.getConviction(unallocatedCaseEntity.crn, unallocatedCaseEntity.convictionId)
      .block()!!.orElse(null)?.let { conviction ->
      if (isUnallocatedIncluded(conviction) && conviction.active) {
        conviction.sentence?.let { sentence ->
          enrichEventService.getTier(unallocatedCaseEntity.crn)?.let { tier ->
            val initialAppointment = enrichEventService.getInitialAppointmentDate(unallocatedCaseEntity.crn, sentence.startDate)
            val name = enrichEventService.getOffenderName(unallocatedCaseEntity.crn)
            val activeConvictions = enrichEventService.getActiveConvictions(unallocatedCaseEntity.crn)
            val (status, previousConvictionDate, offenderManagerDetails) = enrichEventService.getProbationStatus(unallocatedCaseEntity.crn, activeConvictions)
            val caseType = caseTypeEngine.getCaseType(activeConvictions, unallocatedCaseEntity.convictionId)
            unallocatedCaseEntity.sentenceDate = sentence.startDate
            unallocatedCaseEntity.initialAppointment = initialAppointment
            unallocatedCaseEntity.tier = tier
            unallocatedCaseEntity.name = name
            unallocatedCaseEntity.status = status.status
            unallocatedCaseEntity.previousConvictionDate = previousConvictionDate
            unallocatedCaseEntity.offenderManagerSurname = offenderManagerDetails?.surname
            unallocatedCaseEntity.offenderManagerForename = offenderManagerDetails?.forenames
            unallocatedCaseEntity.offenderManagerGrade = offenderManagerDetails?.grade
            unallocatedCaseEntity.caseType = caseType
            return unallocatedCaseEntity
          } ?: run {
            log.info("no tier for crn ${unallocatedCaseEntity.crn} so unable to allocate")
            unallocatedCaseEntity.id?.let {
              repository.deleteById(it)
            }
            return null
          }
        } ?: run {
          log.info("no sentence for crn ${unallocatedCaseEntity.crn} so unable to allocate")
          unallocatedCaseEntity.id?.let {
            repository.deleteById(it)
          }
          return null
        }
      } else {
        log.info("Case for crn ${unallocatedCaseEntity.crn} already allocated")
        unallocatedCaseEntity.id?.let {
          repository.deleteById(it)
        }
        return null
      }
    } ?: run {
      log.info("Unable to retrieve case for crn ${unallocatedCaseEntity.crn}")
      unallocatedCaseEntity.id?.let {
        repository.deleteById(it)
      }
      return null
    }
  }

  private fun isUnallocatedIncluded(conviction: Conviction): Boolean {
    return caseOfficerConfigProperties.includes.contains(conviction.orderManagers.maxByOrNull { it.dateStartOfAllocation ?: LocalDateTime.MIN }?.staffCode)
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
