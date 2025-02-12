package uk.gov.justice.digital.hmpps.hmppsallocations.config

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageConversionException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.resource.NoResourceFoundException
import org.springframework.web.server.MethodNotAllowedException
import uk.gov.justice.digital.hmpps.hmppsallocations.service.exception.EntityNotFoundException
import uk.gov.justice.digital.hmpps.hmppsallocations.service.exception.NotAllowedForLAOException

@Suppress("TooManyFunctions")
@RestControllerAdvice
class HmppsAllocationsExceptionHandler {
  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: Exception): ResponseEntity<ErrorResponse> {
    log.error("Validation exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Validation failure: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(NotAllowedForLAOException::class)
  fun handleNotAllowedForLAOException(e: NotAllowedForLAOException): ResponseEntity<ErrorResponse> {
    log.error("NotAllowedForLAOException: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.FORBIDDEN)
      .body(
        ErrorResponse(
          status = FORBIDDEN,
          userMessage = "Access Denied (LAO): ${e.message}",
          developerMessage = e.message,
          moreInfo = e.crn,
        ),
      )
  }

  @ExceptionHandler(MethodNotAllowedException::class)
  fun handleMethodNotAllowed(e: Exception): ResponseEntity<ErrorResponse> {
    log.error("Method not allowed exception: {}", e.message)
    return ResponseEntity
      .status(METHOD_NOT_ALLOWED)
      .body(
        ErrorResponse(
          status = METHOD_NOT_ALLOWED,
          userMessage = "Method not allowed: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse?>? = ResponseEntity
    .status(FORBIDDEN)
    .body(
      ErrorResponse(
        status = FORBIDDEN,
        userMessage = "Access is denied",
        developerMessage = e.message,
      ),
    )

  @ExceptionHandler(EntityNotFoundException::class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  fun handle(e: EntityNotFoundException): ResponseEntity<uk.gov.justice.digital.hmpps.hmppsallocations.domain.ErrorResponse?> {
    log.error("Not found (404) returned with message {}", e.message)
    return ResponseEntity(
      uk.gov.justice.digital.hmpps.hmppsallocations.domain.ErrorResponse(
        status = 404,
        developerMessage = e.message,
      ),
      HttpStatus.NOT_FOUND,
    )
  }

  @ExceptionHandler(HttpMessageConversionException::class)
  @ResponseStatus(BAD_REQUEST)
  fun handle(e: HttpMessageConversionException): ResponseEntity<uk.gov.justice.digital.hmpps.hmppsallocations.domain.ErrorResponse?> {
    log.error("HttpMessageConversionException: {}", e.message)
    return ResponseEntity(
      uk.gov.justice.digital.hmpps.hmppsallocations.domain.ErrorResponse(
        status = 400,
        developerMessage = e.message,
      ),
      BAD_REQUEST,
    )
  }

  @ExceptionHandler(IllegalArgumentException::class)
  @ResponseStatus(BAD_REQUEST)
  fun handle(e: IllegalArgumentException): ResponseEntity<uk.gov.justice.digital.hmpps.hmppsallocations.domain.ErrorResponse?> {
    log.error("IllegalArgumentException: {}", e.message)
    return ResponseEntity(
      uk.gov.justice.digital.hmpps.hmppsallocations.domain.ErrorResponse(
        status = 400,
        developerMessage = e.message,
      ),
      BAD_REQUEST,
    )
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException::class)
  @ResponseStatus(BAD_REQUEST)
  fun handle(e: MethodArgumentTypeMismatchException): ResponseEntity<uk.gov.justice.digital.hmpps.hmppsallocations.domain.ErrorResponse?> {
    log.error("MethodArgumentTypeMismatchException: {}", e.message)
    return ResponseEntity(
      uk.gov.justice.digital.hmpps.hmppsallocations.domain.ErrorResponse(
        status = 400,
        developerMessage = e.message,
      ),
      BAD_REQUEST,
    )
  }

  @ExceptionHandler(WebClientResponseException.NotFound::class)
  fun handle(e: WebClientResponseException.NotFound): ResponseEntity<uk.gov.justice.digital.hmpps.hmppsallocations.domain.ErrorResponse> = ResponseEntity(
    uk.gov.justice.digital.hmpps.hmppsallocations.domain.ErrorResponse(
      status = 404,
      developerMessage = e.message,
    ),
    NOT_FOUND,
  )

  @ExceptionHandler(java.lang.Exception::class)
  fun handleException(e: java.lang.Exception): ResponseEntity<ErrorResponse?>? {
    log.error("Unexpected exception", e)
    return ResponseEntity
      .status(INTERNAL_SERVER_ERROR)
      .body(
        ErrorResponse(
          status = INTERNAL_SERVER_ERROR,
          userMessage = "Unexpected error: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(NoResourceFoundException::class)
  fun handleEntityNotFoundException(e: NoResourceFoundException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(NOT_FOUND)
    .body(
      ErrorResponse(
        status = NOT_FOUND.value(),
        developerMessage = e.message,
      ),
    )

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class ErrorResponse(
  val status: Int,
  val errorCode: Int? = null,
  val userMessage: String? = null,
  val developerMessage: String? = null,
  val moreInfo: String? = null,
) {
  constructor(
    status: HttpStatus,
    errorCode: Int? = null,
    userMessage: String? = null,
    developerMessage: String? = null,
    moreInfo: String? = null,
  ) :
    this(status.value(), errorCode, userMessage, developerMessage, moreInfo)
}
