package uk.gov.justice.digital.hmpps.hmppsallocations.client

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

class WorkforceAllocationsToDeliusApiClientTest {
  @Test
  fun `test 500 retries on delius client`() = runBlocking {
    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build())
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val exception = assertThrows<RuntimeException> {
      WorkforceAllocationsToDeliusApiClient(webClient).getUnallocatedEvents("999999")
    }
    assert(exception.message == "Retries exhausted: 3/3")
  }

  @Test
  fun `test 404 on delius client`() = runBlocking {
    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build())
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val exception = assertThrows<RuntimeException> {
      WorkforceAllocationsToDeliusApiClient(webClient).getUnallocatedEvents("999999")
    }
    assert(exception.message == "Unallocated events not found for 999999")
  }

  @Test
  fun `test 403 on delius client`() = runBlocking {
    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(ClientResponse.create(HttpStatus.FORBIDDEN).build())
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val exception = assertThrows<RuntimeException> {
      WorkforceAllocationsToDeliusApiClient(webClient).getUnallocatedEvents("999999")
    }
    assert(exception.message == "Unable to access offender details for 999999")
  }

  @Test
  fun `test object constructs when complete response from delius`() = runBlocking {
    val incompleteResponse = """{
      "datasets": [
    {
      "code": "N01",
      "description": "Region 1"
    }
  ],
  "teams": [
    {
      "code": "N01T1",
      "description": "Team1",
      "localAdminUnit": {
        "code": "N01LAU1",
        "description": "LAU1",
        "probationDeliveryUnit": {
          "code": "N01PDU1",
          "description": "PDU 1",
          "provider": {
            "code": "N01",
            "description": "Region 1"
          }
        }
      }
    }
  ]
  }  
    """.trimIndent()

    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(
        ClientResponse.create(HttpStatus.OK)
          .header("Content-Type", "application/json")
          .body(incompleteResponse)
          .build(),
      )
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val result = WorkforceAllocationsToDeliusApiClient(webClient).getTeamsByUsername("JoeBloggs")
  }

  @Test
  fun `test object constructs when incomplete response from delius`() = runBlocking {
    val incompleteResponse = """{
      "datasets": [
    {
      "code": "N01",
      "description": ""
    }
  ],
  "teams": [
    {
      "code": "N01T1",
      "description": "Team1",
      "localAdminUnit": {
        "code": "N01LAU1",
        "description": "LAU1",
        "probationDeliveryUnit": {
          "code": "",
          "description": "PDU 1",
          "provider": {
            "code": "N01",
            "description": "Region 1"
          }
        }
      }
    }
  ]
  }  
    """.trimIndent()

    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(
        ClientResponse.create(HttpStatus.OK)
          .header("Content-Type", "application/json")
          .body(incompleteResponse)
          .build(),
      )
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val result = WorkforceAllocationsToDeliusApiClient(webClient).getTeamsByUsername("JoeBloggs")
  }

  fun `test 504 on delius client`() = runBlocking {
    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(ClientResponse.create(HttpStatus.GATEWAY_TIMEOUT).build())
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val exception = assertThrows<RuntimeException> {
      WorkforceAllocationsToDeliusApiClient(webClient).getUnallocatedEvents("999999")
    }
    assert(exception.message!!.contains("Retries exhausted: 3/3"))
  }

  @Test
  fun `test 424 on delius client`() = runBlocking {
    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build())
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val exception = assertThrows<AllocationsFailedDependencyException> {
      WorkforceAllocationsToDeliusApiClient(webClient).getUserAccess(listOf("X999999", "E912831"))
    }
    assert(exception.message == "/users/limited-access failed")
  }
}
