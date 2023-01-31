package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import com.amazonaws.services.sns.model.MessageAttributeValue
import com.amazonaws.services.sns.model.PublishRequest
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseTypes
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.CommunityApiExtension.Companion.communityApi
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.TierApiExtension.Companion.hmppsTier
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.service.getWmtPeriod
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

class UpdateUnallocatedCaseOffenderEventListenerTests : IntegrationTestBase() {

  @Test
  fun `only contain one record in db if case remains the same after event emitted about it`() {
    val crn = "J678910"
    val convictionId = 123456789L
    repository.save(
      UnallocatedCaseEntity(
        crn = crn,
        initialAppointment = LocalDate.parse("2021-11-30"),
        name = "Tester TestSurname",
        tier = "B3",
        convictionId = convictionId,
        caseType = CaseTypes.CUSTODY,
        providerCode = "",
        convictionNumber = 1
      )
    )
    communityApi.singleActiveConvictionResponseForAllConvictions(crn)
    communityApi.unallocatedConvictionResponse(crn, convictionId)
    communityApi.singleActiveInductionResponse(crn)
    hmppsTier.tierCalculationResponse(crn)
    communityApi.offenderDetailsResponse(crn)
    communityApi.singleActiveConvictionResponse(crn)

    hmppsOffenderSnsClient.publish(
      PublishRequest(hmppsOffenderTopicArn, jsonString(offenderEvent(crn))).withMessageAttributes(
        mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue("CONVICTION_CHANGED"))
      )
    )

    await untilCallTo { countMessagesOnOffenderEventQueue() } matches { it == 0 }

    assertThat(repository.count()).isEqualTo(1)
    val case = repository.findAll().first()

    assertThat(case.initialAppointment).isEqualTo(LocalDate.parse("2021-11-30"))
    assertThat(case.name).isEqualTo("Tester TestSurname")
    assertThat(case.tier).isEqualTo("B3")
  }

  @Test
  fun `delete when conviction allocated to actual officer`() {
    val crn = "J678910"
    val convictionId = 123456789L
    val savedEntity = repository.save(
      UnallocatedCaseEntity(
        crn = crn,
        initialAppointment = LocalDate.parse("2021-11-30"),
        name = "Tester TestSurname",
        tier = "B3",
        convictionId = convictionId,
        caseType = CaseTypes.CUSTODY,
        providerCode = "PC1",
        teamCode = "TC1",
        convictionNumber = 1
      )
    )
    communityApi.singleActiveConvictionResponseForAllConvictions(crn)
    communityApi.allocatedConvictionResponse(crn, convictionId)
    communityApi.singleActiveInductionResponse(crn)
    hmppsTier.tierCalculationResponse(crn)
    communityApi.offenderDetailsResponse(crn)
    communityApi.singleActiveConvictionResponse(crn)

    hmppsOffenderSnsClient.publish(
      PublishRequest(hmppsOffenderTopicArn, jsonString(offenderEvent(crn))).withMessageAttributes(
        mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue("CONVICTION_CHANGED"))
      )
    )

    await untilCallTo { countMessagesOnOffenderEventQueue() } matches { it == 0 }

    assertThat(countMessagesOnOffenderEventDeadLetterQueue()).isEqualTo(0)
    assertThat(repository.count()).isEqualTo(0)
    val parameters = slot<MutableMap<String, String>>()
    verify(exactly = 1) {
      telemetryClient.trackEvent(
        "EventAllocated",
        capture(parameters),
        null
      )
    }
    val startTime = ZonedDateTime.parse(parameters.captured["startTime"])
    Assertions.assertAll(
      { Assertions.assertEquals(savedEntity.crn, parameters.captured["crn"]) },
      { Assertions.assertEquals(savedEntity.teamCode, parameters.captured["teamCode"]) },
      { Assertions.assertEquals(savedEntity.providerCode, parameters.captured["providerCode"]) },
      { Assertions.assertEquals(getWmtPeriod(LocalDateTime.now()), parameters.captured["wmtPeriod"]) },
      { Assertions.assertTrue(startTime.isEqual(savedEntity.createdDate)) },
      { Assertions.assertNotNull(parameters.captured["endTime"]) }
    )
  }

  @Test
  fun `delete when conviction's sentence is removed`() {
    val crn = "J678910"
    val convictionId = 123456789L
    repository.save(
      UnallocatedCaseEntity(
        crn = crn,
        initialAppointment = LocalDate.parse("2021-11-30"),
        name = "Tester TestSurname",
        tier = "B3",
        convictionId = convictionId,
        caseType = CaseTypes.CUSTODY, providerCode = "",
        convictionNumber = 1
      )
    )
    communityApi.singleActiveConvictionResponseForAllConvictions(crn)
    communityApi.convictionWithNoSentenceResponse(crn, convictionId)
    communityApi.singleActiveInductionResponse(crn)
    hmppsTier.tierCalculationResponse(crn)
    communityApi.offenderDetailsResponse(crn)
    communityApi.singleActiveConvictionResponse(crn)

    hmppsOffenderSnsClient.publish(
      PublishRequest(hmppsOffenderTopicArn, jsonString(offenderEvent(crn))).withMessageAttributes(
        mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue("CONVICTION_CHANGED"))
      )
    )

    await untilCallTo { countMessagesOnOffenderEventQueue() } matches { it == 0 }

    assertThat(countMessagesOnOffenderEventDeadLetterQueue()).isEqualTo(0)
    assertThat(repository.count()).isEqualTo(0)
  }

  @Test
  fun `delete when conviction comes inactive`() {
    val crn = "J678910"
    val convictionId = 123456789L
    repository.save(
      UnallocatedCaseEntity(
        crn = crn,
        initialAppointment = LocalDate.parse("2021-11-30"),
        name = "Tester TestSurname",
        tier = "B3",
        convictionId = convictionId,
        caseType = CaseTypes.CUSTODY, providerCode = "",
        convictionNumber = 1
      )
    )
    communityApi.singleActiveConvictionResponseForAllConvictions(crn)
    communityApi.inactiveConvictionResponse(crn, convictionId)
    communityApi.singleActiveInductionResponse(crn)
    hmppsTier.tierCalculationResponse(crn)
    communityApi.offenderDetailsResponse(crn)
    communityApi.singleActiveConvictionResponse(crn)

    hmppsOffenderSnsClient.publish(
      PublishRequest(hmppsOffenderTopicArn, jsonString(offenderEvent(crn))).withMessageAttributes(
        mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue("CONVICTION_CHANGED"))
      )
    )

    await untilCallTo { countMessagesOnOffenderEventQueue() } matches { it == 0 }

    assertThat(countMessagesOnOffenderEventDeadLetterQueue()).isEqualTo(0)
    assertThat(repository.count()).isEqualTo(0)
  }

  @Test
  fun `delete when conviction is not found`() {
    val crn = "J678910"
    val convictionId = 123456789L
    repository.save(
      UnallocatedCaseEntity(
        crn = crn,
        initialAppointment = LocalDate.parse("2021-11-30"),
        name = "Tester TestSurname",
        tier = "B3",
        convictionId = convictionId,
        caseType = CaseTypes.CUSTODY, providerCode = "",
        convictionNumber = 1
      )
    )
    communityApi.singleActiveConvictionResponseForAllConvictions(crn)
    communityApi.notFoundConvictionResponse(crn, convictionId)
    communityApi.singleActiveInductionResponse(crn)
    hmppsTier.tierCalculationResponse(crn)
    communityApi.offenderDetailsResponse(crn)
    communityApi.singleActiveConvictionResponse(crn)

    hmppsOffenderSnsClient.publish(
      PublishRequest(hmppsOffenderTopicArn, jsonString(offenderEvent(crn))).withMessageAttributes(
        mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue("CONVICTION_CHANGED"))
      )
    )

    await untilCallTo { countMessagesOnOffenderEventQueue() } matches { it == 0 }

    assertThat(countMessagesOnOffenderEventDeadLetterQueue()).isEqualTo(0)
    assertThat(repository.count()).isEqualTo(0)
  }

  @Test
  fun `delete when crn is not found`() {
    val crn = "J678910"
    val convictionId = 123456789L
    repository.save(
      UnallocatedCaseEntity(
        crn = crn,
        initialAppointment = LocalDate.parse("2021-11-30"),
        name = "Tester TestSurname",
        tier = "B3",
        convictionId = convictionId,
        caseType = CaseTypes.CUSTODY, providerCode = "",
        convictionNumber = 1
      )
    )
    communityApi.notFoundAllConvictionResponse(crn)

    hmppsOffenderSnsClient.publish(
      PublishRequest(hmppsOffenderTopicArn, jsonString(offenderEvent(crn))).withMessageAttributes(
        mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue("CONVICTION_CHANGED"))
      )
    )

    await untilCallTo { countMessagesOnOffenderEventQueue() } matches { it == 0 }

    assertThat(countMessagesOnOffenderEventDeadLetterQueue()).isEqualTo(0)
    assertThat(repository.count()).isEqualTo(0)
  }

  @Test
  fun `should not be able to insert more than one row of crn conviction id combination`() {
    val crn = "J678910"
    val convictionId = 123456789L
    repository.save(
      UnallocatedCaseEntity(
        crn = crn,
        initialAppointment = LocalDate.parse("2021-11-30"),
        name = "Tester TestSurname",
        tier = "B3",
        convictionId = convictionId,
        caseType = CaseTypes.CUSTODY, providerCode = "",
        convictionNumber = 1
      )
    )

    Assertions.assertThrows(DataIntegrityViolationException::class.java) {
      repository.save(
        UnallocatedCaseEntity(
          crn = crn,
          initialAppointment = LocalDate.parse("2021-11-30"),
          name = "Tester TestSurname",
          tier = "B3",
          convictionId = convictionId,
          caseType = CaseTypes.CUSTODY,
          providerCode = "",
          convictionNumber = 1
        )
      )
    }
  }

  @Test
  fun `initial appointment should be empty when there is no active conviction`() {
    val crn = "J678910"
    val convictionId = 123456789L
    repository.save(
      UnallocatedCaseEntity(
        crn = crn,
        initialAppointment = LocalDate.parse("2021-11-30"),
        name = "Tester TestSurname",
        tier = "B3",
        convictionId = convictionId,
        caseType = CaseTypes.CUSTODY,
        providerCode = "",
        convictionNumber = 1
      )
    )
    communityApi.singleActiveConvictionResponseForAllConvictions(crn)
    communityApi.unallocatedConvictionResponse(crn, convictionId)
    communityApi.noActiveInductionResponse(crn)
    hmppsTier.tierCalculationResponse(crn)
    communityApi.offenderDetailsResponse(crn)
    communityApi.singleActiveConvictionResponse(crn)

    hmppsOffenderSnsClient.publish(
      PublishRequest(hmppsOffenderTopicArn, jsonString(offenderEvent(crn))).withMessageAttributes(
        mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue("CONVICTION_CHANGED"))
      )
    )

    await untilCallTo { countMessagesOnOffenderEventQueue() } matches { it == 0 }

    assertThat(repository.count()).isEqualTo(1)
    val case = repository.findAll().first()

    assertThat(case.initialAppointment).isNull()
  }
}
