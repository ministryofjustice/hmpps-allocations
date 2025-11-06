package uk.gov.justice.digital.hmpps.hmppsallocations.service

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.Name
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.AllocatedActiveEvent
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.AllocatedEventOffences
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.AllocatedEventRequirement
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.AllocatedEventSentence
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusAllocatedCaseView
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusCrnRestrictionStatus
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.MainAddressDto
import java.time.LocalDate

class GetAllocatedCaseServiceTest {

  private val workforceAllocationsToDeliusApiClient = mockk<WorkforceAllocationsToDeliusApiClient>()
  private val laoService = mockk<LaoService>()
  private val tierApiClient = mockk<HmppsTierApiClient>()

  private lateinit var service: GetAllocatedCaseService

  @BeforeEach
  fun setUp() {
    service = GetAllocatedCaseService(
      workforceAllocationsToDeliusApiClient,
      laoService,
      tierApiClient,
    )
  }

  @Test
  fun `should return allocated case details`() = runBlocking {
    val crn = "X123456"
    val deliusCaseView = DeliusAllocatedCaseView(
      Name(forename = "John", middleName = null, surname = "Doe"),
      LocalDate.of(1980, 1, 1),
      "Male",
      "pnc123456",
      MainAddressDto(
        "The Manor", "1", "High Street", "London", "Greater London", "AB1 2CD", false, true, "home",
        LocalDate.of(2020, 1, 1),
      ),
      nextAppointmentDate = LocalDate.of(2023, 12, 1),
      activeEvents = listOf(
        AllocatedActiveEvent(
          1,
          0,
          LocalDate.of(2022, 1, 27),
          AllocatedEventSentence(
            "Custodial",
            LocalDate.of(2022, 1, 15),
            LocalDate.of(2023, 1, 15),
            "12 months",
          ),
          listOf(AllocatedEventOffences("Burglary", "Theft", true)),
          listOf(AllocatedEventRequirement("Curfew", "Must comply with curfew", "6 months")),
        ),
      ),
    )

    val deliusCrnRestrictionStatus = DeliusCrnRestrictionStatus(
      crn = crn,
      isRestricted = false,
      isRedacted = false,
    )

    val tier = "C2"

    // Arrange
    coEvery { workforceAllocationsToDeliusApiClient.getAllocatedDeliusCaseView(any()) } returns Mono.just(deliusCaseView)
    coEvery { laoService.getCrnRestrictionStatus(any(), any()) } returns deliusCrnRestrictionStatus
    coEvery { tierApiClient.getTierByCrn(any()) } returns tier

    // Act
    val result = service.getCase("userName", crn)

    // Assert
    assert(result!!.tier == tier)
    assert(result.crn == crn)
    assert(result.activeEvents.size == 1)
    assert(result.activeEvents.first().sentence!!.length == "12 months")
    assert(result.activeEvents.first().offences.first().mainOffence == true)
  }

  @Test
  fun `should return allocated case details with null address`() = runBlocking {
    val crn = "X123456"
    val deliusCaseView = DeliusAllocatedCaseView(
      Name(forename = "John", middleName = null, surname = "Doe"),
      LocalDate.of(1980, 1, 1),
      "Male",
      "pnc123456",
      null,
      nextAppointmentDate = LocalDate.of(2023, 12, 1),
      activeEvents = listOf(
        AllocatedActiveEvent(
          1,
          0,
          LocalDate.of(2022, 1, 27),
          AllocatedEventSentence(
            "Custodial",
            LocalDate.of(2022, 1, 15),
            LocalDate.of(2023, 1, 15),
            "12 months",
          ),
          listOf(AllocatedEventOffences("Burglary", "Theft", true)),
          listOf(AllocatedEventRequirement("Curfew", "Must comply with curfew", "6 months")),
        ),
      ),
    )

    val deliusCrnRestrictionStatus = DeliusCrnRestrictionStatus(
      crn = crn,
      isRestricted = false,
      isRedacted = false,
    )

    val tier = "C2"

    // Arrange
    coEvery { workforceAllocationsToDeliusApiClient.getAllocatedDeliusCaseView(any()) } returns Mono.just(deliusCaseView)
    coEvery { laoService.getCrnRestrictionStatus(any(), any()) } returns deliusCrnRestrictionStatus
    coEvery { tierApiClient.getTierByCrn(any()) } returns tier

    // Act
    val result = service.getCase("userName", crn)

    // Assert
    assert(result!!.tier == tier)
    assert(result.crn == crn)
    assert(result.activeEvents.size == 1)
    assert(result.activeEvents.first().sentence!!.length == "12 months")
    assert(result.activeEvents.first().offences.first().mainOffence == true)
  }
}
