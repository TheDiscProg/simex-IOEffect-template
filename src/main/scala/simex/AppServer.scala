package simex

import cats.data.NonEmptyList
import cats.effect.{Async, Resource}
import cats.{Monad, MonadError, Parallel}
import com.comcast.ip4s._
import io.circe.config.parser
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.Logger
import org.typelevel.log4cats.{Logger => Log4CatsLogger}
import shareprice.config.ServerConfiguration
import simex.guardrail.healthcheck.HealthcheckResource
import simex.server.domain.healthcheck.{
  HealthCheckService,
  HealthChecker,
  HealthcheckAPIHandler,
  SelfHealthCheck
}
import simex.server.entities.AppService

object AppServer {

  def createServer[F[
      _
  ]: Monad: Async: Log4CatsLogger: Parallel: MonadError[*[_], Throwable]]()
      : Resource[F, AppService] =
    for {
      conf <- Resource.eval(parser.decodePathF[F, ServerConfiguration](path = "server"))

      // Health checkers
      checkers = NonEmptyList.of[HealthChecker[F]](SelfHealthCheck[F])
      healthCheckers = HealthCheckService(checkers)
      healthRoutes = new HealthcheckResource().routes(
        new HealthcheckAPIHandler[F](healthCheckers)
      )

      // Routes and HTTP App
      allRoutes = healthRoutes.orNotFound
      httpApp = Logger.httpApp(logHeaders = true, logBody = true)(allRoutes)

      // Build server
      httpPort = Port.fromInt(conf.http.port)
      httpHost = Ipv4Address.fromString(conf.http.hostAddress)
      server <- EmberServerBuilder.default
        .withPort(httpPort.getOrElse(port"8000"))
        .withHost(httpHost.getOrElse(ipv4"0.0.0.0"))
        .withHttpApp(httpApp)
        .build
    } yield AppService(server)
}
