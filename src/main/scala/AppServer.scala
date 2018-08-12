import akka.actor.ActorSystem
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContextExecutor

object AppServer extends HttpApp with App {
  implicit val system: ActorSystem = ActorSystem("elama-test-service")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  override def routes: Route =
    path("stats") {
      get {
        parameters('id.as[Int].*) { ids =>
          ids.toList match {
            case Nil => complete("{}")
            case _ => complete(StatsService.getStats(ids.toList))
          }
        }
      }
    }

  AppServer.startServer(AppConfig.host, AppConfig.port)
}