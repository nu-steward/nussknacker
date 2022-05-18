package pl.touk.nussknacker.ui.security.oauth2

import akka.http.scaladsl.server.directives.Credentials.Provided
import akka.http.scaladsl.server.directives.{Credentials, SecurityDirectives}
import cats.data.NonEmptyList
import com.typesafe.scalalogging.LazyLogging
import io.circe.Json
import pdi.jwt.{JwtClaim, JwtHeader}
import pl.touk.nussknacker.ui.security.api.AuthenticatedUser
import sttp.client.{NothingT, SttpBackend}

import java.security.PublicKey
import scala.concurrent.{ExecutionContext, Future}

class OAuth2Authenticator(configuration: OAuth2Configuration, service: OAuth2Service[AuthenticatedUser, _])
                         (implicit ec: ExecutionContext, sttpBackend: SttpBackend[Future, Nothing, NothingT])
  extends SecurityDirectives.AsyncAuthenticator[AuthenticatedUser] with LazyLogging {
  def apply(credentials: Credentials): Future[Option[AuthenticatedUser]] =
    authenticate(credentials)

  private[security] def authenticate(credentials: Credentials): Future[Option[AuthenticatedUser]] = {
    credentials match {
      case Provided(token) => authenticate(token)
      case _ => Future.successful(Option.empty)
    }
  }

  private[oauth2] def authenticate(token: String): Future[Option[AuthenticatedUser]] =
    service.checkAuthorizationAndObtainUserinfo(token).map(prf => Option(prf._1)).recover {
      case OAuth2ErrorHandler(ex) => {
        logger.debug("Access token rejected:", ex)
        Option.empty // Expired or non-exists token - user not authenticated
      }
    }
}

object OAuth2Authenticator extends LazyLogging {
  def apply(configuration: OAuth2Configuration, service: OAuth2Service[AuthenticatedUser, _])(implicit ec: ExecutionContext, sttpBackend: SttpBackend[Future, Nothing, NothingT]): OAuth2Authenticator =
    new OAuth2Authenticator(configuration, service)
}

object OAuth2ErrorHandler {

  case class ParsedToken(header: JwtHeader, claim: JwtClaim, signature: String)

  def unapply(t: Throwable): Option[Throwable] = Some(t).filter(apply)

  def apply(t: Throwable): Boolean = t match {
    case OAuth2CompoundException(errors) => errors.toList.collectFirst { case e@OAuth2ServerError(_) => e }.isEmpty
    case _ => false
  }

  trait OAuth2Error {
    def msg: String
  }

  case class OAuth2CompoundException(errors: NonEmptyList[OAuth2Error]) extends Exception {
    override def getMessage: String = errors.toList.mkString("OAuth2 exception with the following errors:\n - ", "\n - ", "")
  }

  trait OAuth2JwtError extends OAuth2Error

  case class OAuth2JwtDecodeRawError(rawToken: String, cause: Throwable) extends OAuth2JwtError {
    override def msg: String = s"Failure in jwt decode: ${cause.getLocalizedMessage}. Token: $rawToken"
  }

  case class OAuth2JwtKeyDetermineError(token: ParsedToken, cause: Throwable) extends OAuth2JwtError {
    override def msg: String = s"Failure in key determining: ${cause.getLocalizedMessage}. Token: $token"
  }

  case class OAuth2JwtDecodeClaimsError(token: ParsedToken, publicKey: PublicKey, cause: Throwable) extends OAuth2JwtError {
    override def msg: String = s"Failure in decoding json using public key: ${cause.getLocalizedMessage}. Public key: $publicKey, token: $token"
  }

  case class OAuth2JwtDecodeClaimsJsonError(token: ParsedToken, publicKey: PublicKey, tokenClaimsJson: Json, cause: Throwable) extends OAuth2JwtError {
    override def msg: String = s"Failure in decoding token claims: ${cause.getLocalizedMessage}. Public key: $publicKey, token: $token, token claims: ${tokenClaimsJson.noSpaces}"
  }

  case class OAuth2AuthenticationRejection(msg: String) extends OAuth2Error

  case class OAuth2AccessTokenRejection(msg: String) extends OAuth2Error

  case class OAuth2ServerError(msg: String) extends OAuth2Error
}
