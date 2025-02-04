package pl.touk.nussknacker.engine.requestresponse.http

import akka.http.scaladsl.server.{Directive1, Directives}
import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, Validated, ValidatedNel}
import io.circe.Json
import pl.touk.nussknacker.engine.api.Context
import pl.touk.nussknacker.engine.api.exception.NuExceptionInfo
import pl.touk.nussknacker.engine.lite.api.commonTypes.ErrorType
import pl.touk.nussknacker.engine.requestresponse.DefaultResponseEncoder
import pl.touk.nussknacker.engine.requestresponse.FutureBasedRequestResponseScenarioInterpreter.InterpreterType
import pl.touk.nussknacker.engine.requestresponse.RequestResponseInterpreter.RequestResponseResultType
import pl.touk.nussknacker.engine.requestresponse.api.{RequestResponseGetSource, RequestResponsePostSource}
import pl.touk.nussknacker.engine.requestresponse.metrics.InvocationMetrics

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try


//this class handles parsing, displaying and invoking interpreter. This is the only place we interact with model, hence
//only here we care about context classloaders
class RequestResponseHandler(requestResponseInterpreter: InterpreterType) extends Directives {

  private val source = requestResponseInterpreter.source

  private val encoder = source.responseEncoder.getOrElse(DefaultResponseEncoder)

  private val invocationMetrics = new InvocationMetrics(requestResponseInterpreter.context)

  private def catchParseException(parseRequest: => () => Any): ValidatedNel[ErrorType, Any] = {
    Validated.fromTry(Try(parseRequest())).leftMap(ex => NonEmptyList.one(NuExceptionInfo(None, ex, Context(""))))
  }

  private val extractInput: Directive1[ValidatedNel[ErrorType, Any]] = source match {
    case a: RequestResponsePostSource[Any] =>
      post & entity(as[Array[Byte]]).map(request => catchParseException(() => a.parse(request)))
    case a: RequestResponseGetSource[Any] =>
      get & parameterMultiMap.map(request => catchParseException(() => a.parse(request)))
  }

  val invoke: Directive1[RequestResponseResultType[Json]] =
    extractExecutionContext.flatMap { implicit ec =>
      extractInput.map {
          case Valid(value) => invokeInterpreter(value)
          case Invalid(errorMsg) => Future(Invalid(errorMsg))
        }
        .flatMap(onSuccess(_))
    }

  private def invokeInterpreter(input: Any)(implicit ec: ExecutionContext): Future[RequestResponseResultType[Json]] = invocationMetrics.measureTime {
    requestResponseInterpreter.invokeToOutput(input).map(_.andThen { data =>
      Validated.fromTry(Try(encoder.toJsonResponse(input, data))).leftMap(ex => NonEmptyList.one(NuExceptionInfo(None, ex, Context(""))))
    })
  }

}
