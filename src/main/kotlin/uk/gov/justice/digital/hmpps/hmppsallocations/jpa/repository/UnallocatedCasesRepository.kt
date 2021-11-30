package uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity

@Repository
interface UnallocatedCasesRepository : CrudRepository<UnallocatedCaseEntity, Long> {

  fun findByCrn(crn: String): UnallocatedCaseEntity
}
