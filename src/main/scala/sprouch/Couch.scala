package sprouch

import akka.actor._
import akka.dispatch.Future
import spray.can.client.HttpClient
import spray.client.HttpConduit
import HttpConduit._
import spray.http._
import HttpMethods._
import spray.httpx.encoding.{Gzip, Deflate}
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling.Unmarshaller
import spray.io._
import spray.json._
import spray.util._
import java.util.UUID
import akka.event.Logging
import java.net.URLEncoder.{encode => urlEncode}
import JsonProtocol._
import akka.dispatch.ExecutionContext

private[sprouch] trait UriBuilder {
  protected[this] def sep = "/"
  protected[this] def encode(s:String) = urlEncode(s, "UTF-8") 
  protected[this] def path(parts:String*) = sep + parts.map(encode).mkString(sep)  
  protected[this] def dbUri(dbName:String) = path(dbName)
}

/**
 * This exception is thrown whenever CouchDB returns an HTTP error code. Details about the error are in the error field.
 */
case class SprouchException(error:ErrorResponse) extends Exception

/**
 * Class that handles the connection to CouchDB. It contains methods for creating, looking up and deleting databases.
 */
class Couch(config:Config) extends UriBuilder {
  
  val pipelines = new Pipelines(config)
  private def pipeline = pipelines.pipeline[OkResponse]
  private def getDbPipeline = pipelines.pipeline[GetDbResponse]
  
  def allDbs():Future[Seq[String]] = {
    val p = pipelines.pipeline[Seq[String]]
    p(Get("/_all_dbs"))
  }
  
  /**
   * Creates a new database. Fails if the database already exists.
   */
  def createDb(dbName:String, docLogger:DocLogger=NopLogger):Future[Database] = {
    val p = pipelines.pipeline[OkResponse](docLogger=docLogger)
    p(Put(dbUri(dbName))).map(_ => new Database(dbName, pipelines))
  }
  /**
   * Deletes a database and all containing documents.
   */
  def deleteDb(dbName:String, docLogger:DocLogger=NopLogger):Future[OkResponse] = {
    val p = pipelines.pipeline[OkResponse](docLogger=docLogger)
    p(Delete(dbUri(dbName)))
  }
  /**
   * Looks up a database by its name.
   */
  def getDb(dbName:String, docLogger:DocLogger=NopLogger):Future[Database] = {
    val p = pipelines.pipeline[GetDbResponse](docLogger = docLogger)
    p(Get(dbUri(dbName))).map(_ => new Database(dbName, pipelines))
  }

}

object Couch {
  /**
   * Factory method that creates instances of Couch from a Config instance.
   */
  def apply(config:Config) = new Couch(config)
}
