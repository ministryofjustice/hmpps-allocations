package uk.gov.justice.digital.hmpps.hmppsallocations.client

import ch.qos.logback.core.net.server.Client
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitExchangeOrNull
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusRisk

class WorkforceAllocationsToDeliusApiClientTest {
  private var webClient: WebClient = mockk()
  private var cut: WorkforceAllocationsToDeliusApiClient = WorkforceAllocationsToDeliusApiClient(webClient)

  @Test
  fun testDeliusRiskRetries() {
    val crn = "00000000001"
    val response: ClientResponse = mockk()
    val webClientRequestHeadersUriSpec: WebClient.RequestHeadersUriSpec<*> = mockk()
    val webClientResponseSpec: WebClient.RequestHeadersSpec<*> = mockk()
    val deliusRisk: DeliusRisk = mockk()
    coEvery { response.statusCode() } returns HttpStatus.INTERNAL_SERVER_ERROR
    coEvery { response.bodyToMono(DeliusRisk::class.java) } returns Mono.just(deliusRisk)
    coEvery { webClient.get() } returns webClientRequestHeadersUriSpec
    coEvery { webClientRequestHeadersUriSpec.uri( "/allocation-demand/$crn/risk" ) } returns webClientResponseSpec

//
//    coEvery { webClientResponseSpec.awaitExchangeOrNull(ofType<(ClientResponse) -> DeliusRisk>()) } answers {
//      val handler = it.invocation.args[0] as (ClientResponse) -> DeliusRisk
//      handler(response)
//    }

//    val actual = runBlocking {cut.getDeliusRisk(crn)}
//    assert(deliusRisk == actual)
  }
}
