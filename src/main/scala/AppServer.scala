import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{HttpApp, Route}

import scala.util.{Failure, Success}

object AppServer extends HttpApp with App {
  override def routes: Route =
    path("stats") {
      get {
        parameters('id.as[Int].*) { ids =>
          ids.toList match {
            case Nil => complete("{}")
            case idList => onComplete(StatsService.getStats(idList)) {
              case Success(result) => complete(result)
              case Failure(exception) => complete(StatusCodes.InternalServerError -> exception.getMessage)
            }
          }
        }
      }
    }

  AppServer.startServer(AppConfig.host, AppConfig.port)
}