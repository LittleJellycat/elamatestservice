import akka.http.scaladsl.server.{HttpApp, Route}

object AppServer extends HttpApp with App {
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