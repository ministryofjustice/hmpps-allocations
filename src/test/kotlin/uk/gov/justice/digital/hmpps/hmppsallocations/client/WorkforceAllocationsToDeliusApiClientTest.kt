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
  fun `test 500 retries on delius client` () = runBlocking {
    val exchangeFunction = ExchangeFunction{
      request -> Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build())
  }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val exception = assertThrows<RuntimeException> {
      WorkforceAllocationsToDeliusApiClient(webClient).getUnallocatedEvents("999999")
    }
    assert(exception.message == "Retries exhausted: 3/3")
  }

  @Test
  fun `test 404 on delius client` () = runBlocking {
    val exchangeFunction = ExchangeFunction{
        request -> Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build())
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val result = WorkforceAllocationsToDeliusApiClient(webClient).getUnallocatedEvents("999999")
    assert(result == null)
  }

  @Test
  fun `test 403 on delius client` () = runBlocking {
    val exchangeFunction = ExchangeFunction{
        request -> Mono.just(ClientResponse.create(HttpStatus.FORBIDDEN).build())
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()

    val exception = assertThrows<RuntimeException> {
      WorkforceAllocationsToDeliusApiClient(webClient).getUnallocatedEvents("999999")
    }
    assert(exception.message == "Unable to access offender details for 999999")
  }
}