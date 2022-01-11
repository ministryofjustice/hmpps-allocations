package uk.gov.justice.digital.hmpps.hmppsallocations.controller.advice

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageConversionException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.service.exception.EntityNotFoundException

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class ControllerAdvice {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @ExceptionHandler(EntityNotFoundException::class)
  @ResponseStatus(NOT_FOUND)
  fun handle(e: EntityNotFoundException): ResponseEntity<ErrorResponse?> {
    log.debug("Not found (404) returned with message {}", e.message)
    return ResponseEntity(ErrorResponse(status = 404, developerMessage = e.message), NOT_FOUND)
  }

  @ExceptionHandler(HttpMessageConversionException::class)
  @ResponseStatus(BAD_REQUEST)
  fun handle(e: HttpMessageConversionException): ResponseEntity<ErrorResponse?> {
    log.error("HttpMessageConversionException: {}", e.message)
    return ResponseEntity(ErrorResponse(status = 400, developerMessage = e.message), BAD_REQUEST)
  }

  @ExceptionHandler(IllegalArgumentException::class)
  @ResponseStatus(BAD_REQUEST)
  fun handle(e: IllegalArgumentException): ResponseEntity<ErrorResponse?> {
    log.error("IllegalArgumentException: {}", e.message)
    return ResponseEntity(ErrorResponse(status = 400, developerMessage = e.message), BAD_REQUEST)
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException::class)
  @ResponseStatus(BAD_REQUEST)
  fun handle(e: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse?> {
    log.error("MethodArgumentTypeMismatchException: {}", e.message)
    return ResponseEntity(ErrorResponse(status = 400, developerMessage = e.message), BAD_REQUEST)
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
  @ResponseStatus(METHOD_NOT_ALLOWED)
  fun handle(e: HttpRequestMethodNotSupportedException): ResponseEntity<ErrorResponse?> {
    log.error("HttpRequestMethodNotSupportedException: {}", e.message)
    return ResponseEntity(ErrorResponse(status = 405, developerMessage = e.message), METHOD_NOT_ALLOWED)
  }

  @ExceptionHandler(Exception::class)
  @ResponseStatus(INTERNAL_SERVER_ERROR)
  fun handle(e: Exception): ResponseEntity<ErrorResponse?> {
    log.error("Exception: {}", e.message)
    return ResponseEntity(
      ErrorResponse(
        status = 500,
        developerMessage = "Internal Server Error. Check Logs",
        userMessage = "An unexpected error has occurred"
      ),
      INTERNAL_SERVER_ERROR
    )
  }
}
