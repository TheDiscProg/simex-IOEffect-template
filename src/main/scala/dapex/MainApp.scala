package dapex

import cats.effect._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object MainApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    Resource
      .eval(Slf4jLogger.create[IO])
      .use { implicit logger: Logger[IO] =>
        AppServer
          .createServer[IO]()
          .use(appService => IO.never) //For simple HTTP server, not used
          .as(ExitCode.Success)
      }
}
