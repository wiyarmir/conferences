package es.guillermoorellana.keynotedex.backend

import io.ktor.application.Application
import io.ktor.application.ApplicationStopping
import io.ktor.application.call
import io.ktor.auth.Authentication
import io.ktor.auth.OAuthAccessTokenResponse
import io.ktor.auth.OAuthServerSettings
import io.ktor.auth.authenticate
import io.ktor.auth.oauth
import io.ktor.auth.principal
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.locations.location
import io.ktor.routing.Routing
import io.ktor.routing.param
import io.ktor.sessions.sessions
import io.ktor.sessions.set

fun Application.configureOAuth(authConf: Authentication.Configuration) = authConf.oauth("oauth") {
    val config = environment.config.config("keynotedex.oauth.github")
    client = HttpClient(Apache).also {
        environment.monitor.subscribe(ApplicationStopping) { client.close() }
    }
    providerLookup = {
        OAuthServerSettings.OAuth2ServerSettings(
            name = "github",
            authorizeUrl = "https://github.com/login/oauth/authorize",
            accessTokenUrl = "https://github.com/login/oauth/access_token",
            clientId = config.propertyOrNull("clientId")?.getString() ?: "",
            clientSecret = config.propertyOrNull("clientSecret")?.getString() ?: ""
        )
    }
    urlProvider = { settings: OAuthServerSettings -> redirectString(OAuthLoginEndpoint(settings.name)) }
}

fun Routing.oauth() {
    authenticate("oauth") {
        location<OAuthLoginEndpoint> {
            param("error") {
                handle {
                    call.redirect(OauthFailedPage(call.parameters.getAll("error").orEmpty()))
                }
            }
            handle {
                val principal = call.principal<OAuthAccessTokenResponse>()
                when (principal) {
                    is OAuthAccessTokenResponse.OAuth2 -> call.sessions.set(Session(principal.accessToken))
                }
                call.redirect(LoginPage())
            }
        }
    }
}
