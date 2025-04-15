package uk.gov.justice.digital.hmpps.hmppsallocations.service

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusTeams
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.Lau
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.Pdu
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.Provider
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.TeamWithLau

class RegionsServiceTest {

  @MockK
  lateinit var workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient

  @InjectMockKs
  lateinit var regionsService: GetRegionsService

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
  }

  @Test
  fun `returns correct regions`() = runTest {
    val staffId = "N25789"
    coEvery { workforceAllocationsToDeliusApiClient.getApopUsers() } returns emptyList()
    coEvery { workforceAllocationsToDeliusApiClient.getTeamsByStaffId(staffId) } returns DeliusTeams(
      listOf(
        TeamWithLau("N53", "N53 desc", Lau("lau1", "lauDesc", Pdu("fred", "flintstone", Provider("a1", "desc")))),
        TeamWithLau("N54", "Camelot", Lau("lau1", "lauDesc", Pdu("fred", "flintstone", Provider("a1", "desc")))),
        TeamWithLau("N54", "Camelot", Lau("lau3", "Lakes", Pdu("B12", "flintstone", Provider("a1", "desc")))),
        TeamWithLau("N55", "Camelot", Lau("lau3", "Lakes", Pdu("B12", "flintstone", Provider("a1", "desc")))),
      ),
    )

    val regions = regionsService.getRegionsByUser(staffId)
    assert(regions.regions.size == 3)
    assert(regions.regions.contains("N53"))
    assert(regions.regions.contains("N54"))
    assert(regions.regions.contains("N55"))
  }

  @Test
  fun `returns empty list if no regions allowed`() = runTest {
    val staffId = "67871"
    coEvery { workforceAllocationsToDeliusApiClient.getTeamsByStaffId(staffId) } returns DeliusTeams(
      emptyList(),
    )

    val regions = regionsService.getRegionsByUser(staffId)
    assert(regions.regions.isEmpty())
  }
}
