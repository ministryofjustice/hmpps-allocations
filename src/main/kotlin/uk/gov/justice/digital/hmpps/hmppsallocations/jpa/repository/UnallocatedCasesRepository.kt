package uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.projection.CaseCountByTeam
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.projection.ConvictionIdOnly

@Repository
interface UnallocatedCasesRepository : CrudRepository<UnallocatedCaseEntity, Long> {
  fun existsByCrn(crn: String): Boolean

  fun existsByCrnAndConvictionNumber(crn: String, convictionNumber: Int): Boolean
  fun findByCrn(crn: String): List<UnallocatedCaseEntity>
  fun findFirstCaseByCrn(crn: String): UnallocatedCaseEntity?
  fun findCaseByCrnAndConvictionId(crn: String, convictionId: Long): UnallocatedCaseEntity?

  fun findCaseByCrnAndConvictionNumber(crn: String, convictionNumber: Int): UnallocatedCaseEntity?
  fun findConvictionIdsByCrn(crn: String): List<ConvictionIdOnly>

  fun findByTeamCode(teamCode: String): List<UnallocatedCaseEntity>
  @Query("select u.teamCode AS teamCode, count(*) AS caseCount from UnallocatedCaseEntity u where u.teamCode in :teamCodes GROUP BY u.teamCode")
  fun getCaseCountByTeam(teamCodes: List<String>): List<CaseCountByTeam>
}
