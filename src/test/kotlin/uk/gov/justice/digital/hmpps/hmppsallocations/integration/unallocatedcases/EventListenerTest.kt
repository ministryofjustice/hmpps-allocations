package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import com.amazonaws.services.sns.model.MessageAttributeValue
import com.amazonaws.services.sns.model.PublishRequest
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class EventListenerTest : IntegrationTestBase() {

  @Test
  fun `event is written to database`() {
    val firstSentenceDate = LocalDateTime.now().minusDays(4).truncatedTo(ChronoUnit.SECONDS)
    val firstInitialAppointment = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.SECONDS)
    val event = unallocatedCaseEvent(
      "Dylan Adam Armstrong", "J678910", "C1",
      firstSentenceDate, firstInitialAppointment, "Currently managed"
    )
    hmppsDomainSnsClient.publish(
      PublishRequest(hmppsDomainTopicArn, jsonString(event))
        .withMessageAttributes(
          mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue(event.eventType))
        )
    )

    await untilCallTo { hmppsDomainSqsClient.countMessagesOnQueue(hmppsDomainQueueUrl) } matches { it == 0 }

    val unallocatedCasesCount = repository.count()

    assertThat(unallocatedCasesCount).isEqualTo(1)
  }
}
