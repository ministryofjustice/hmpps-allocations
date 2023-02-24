package uk.gov.justice.digital.hmpps.hmppsallocations.listener

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsallocations.service.TierCalculationService

@Component
class CalculationEventListener(
  private val calculationTierService: TierCalculationService,
  private val objectMapper: ObjectMapper
) {

  @JmsListener(destination = "tiercalculationqueue", containerFactory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(rawMessage: String?) {
    val calculationEventData = readMessage(rawMessage)
    CoroutineScope(Dispatchers.Default).future {
      calculationTierService.updateTier(crnFrom(calculationEventData))
    }.get()
  }

  private fun readMessage(wrapper: String?): CalculationEventData {
    val (message) = objectMapper.readValue(wrapper, Message::class.java)
    return objectMapper.readValue(message, CalculationEventData::class.java)
  }

  private fun crnFrom(calculationEventData: CalculationEventData) =
    calculationEventData.personReference.identifiers.first { it.type == "CRN" }.value

  data class CalculationEventData(val personReference: PersonReference)

  data class PersonReference(val identifiers: List<PersonReferenceType>)

  data class PersonReferenceType(
    val type: String,
    val value: String
  )

  data class Message(@JsonProperty("Message") val message: String)
}
