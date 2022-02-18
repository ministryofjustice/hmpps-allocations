package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkloadApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.AllocateCaseOffenderManagerWorkload
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.AllocateCaseOffenderManagers
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.PotentialAllocateCase
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import uk.gov.justice.digital.hmpps.hmppsallocations.mapper.GradeMapper

@Service
class GetAllocateCaseService(
  private val unallocatedCasesRepository: UnallocatedCasesRepository,
  private val workloadApiClient: WorkloadApiClient,
  private val gradeMapper: GradeMapper,
  private val caseTypeEngine: CaseTypeEngine
) {
  fun getOffenderManagers(crn: String): AllocateCaseOffenderManagers? =
    unallocatedCasesRepository.findCaseByCrn(crn)?.let {

      val offenderManagers = workloadApiClient.getOffenderManagersForTeam()
        .map { offenderManagerWorkloads ->
          offenderManagerWorkloads.offenderManagers
            .map { offenderManagerWorkload ->
              offenderManagerWorkload.grade = gradeMapper.workloadToStaffGrade(offenderManagerWorkload.grade)
              AllocateCaseOffenderManagerWorkload.from(offenderManagerWorkload)
            }
        }.block()!!
      return AllocateCaseOffenderManagers.from(it, offenderManagers)
    }

  fun getImpactOfAllocation(crn: String, offenderManagerCode: String): PotentialAllocateCase? =
    unallocatedCasesRepository.findCaseByCrn(crn)?.let {
      val caseType = caseTypeEngine.getCaseType(crn)
      return workloadApiClient.getPotentialCaseLoad(it.tier, caseType, offenderManagerCode)
        .map { offenderManagerWorkload ->
          PotentialAllocateCase.from(it, offenderManagerWorkload, gradeMapper.workloadToStaffGrade(offenderManagerWorkload.grade))
        }
        .block()
    }
}
