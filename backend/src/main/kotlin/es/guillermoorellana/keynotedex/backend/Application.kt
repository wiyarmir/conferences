package es.guillermoorellana.keynotedex.backend

import es.guillermoorellana.keynotedex.backend.api.api
import es.guillermoorellana.keynotedex.backend.auth.JwtConfig
import es.guillermoorellana.keynotedex.backend.auth.JwtTokenProvider
import es.guillermoorellana.keynotedex.backend.auth.createJwtConfig
import es.guillermoorellana.keynotedex.backend.auth.createJwtTokenProvider
import es.guillermoorellana.keynotedex.backend.data.KeynotedexDatabase
import es.guillermoorellana.keynotedex.backend.data.KeynotedexStorage
import es.guillermoorellana.keynotedex.backend.data.conferences.ConferencesTable
import es.guillermoorellana.keynotedex.backend.external.GithubConferenceScrapper
import es.guillermoorellana.keynotedex.backend.external.toDao
import es.guillermoorellana.keynotedex.datasource.responses.ErrorResponse
import freemarker.cache.ClassTemplateLoader
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.jwt.jwt
import io.ktor.features.CallLogging
import io.ktor.features.Compression
import io.ktor.features.ConditionalHeaders
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.PartialContent
import io.ktor.features.StatusPages
import io.ktor.freemarker.FreeMarker
import io.ktor.freemarker.FreeMarkerContent
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.resource
import io.ktor.http.content.static
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Locations
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.Routing
import io.ktor.routing.accept
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.error
import org.jetbrains.squash.connection.transaction
import org.jetbrains.squash.statements.insertInto
import org.jetbrains.squash.statements.values
import java.io.File

class Keynotedex

@UseExperimental(KtorExperimentalLocationsAPI::class)
fun Application.keynotedex(
    storage: KeynotedexStorage = createStorage().also { populateConferences(it) },
    jwtConfig: JwtConfig = createJwtConfig(environment),
    jwtTokenProvider: JwtTokenProvider = createJwtTokenProvider(jwtConfig)
) {

    install(DefaultHeaders)
    install(CallLogging)
    install(ConditionalHeaders)
    install(PartialContent)
    install(Compression)
    install(Locations)
    install(StatusPages) {
        exception<Throwable> { cause ->
            environment.log.error(cause)
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    install(Authentication) {
        jwt { jwtConfig.applyJwtConfig(this) }
    }

    install(ContentNegotiation) {
        serializable {
            register(ErrorResponse.serializer())
        }
    }

    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(Keynotedex::class.java.classLoader, "")
    }

    install(Routing) {
        api(storage, jwtTokenProvider)
        index()
    }
}

@UseExperimental(KtorExperimentalLocationsAPI::class)
private fun Route.index() {
    static("frontend") {
        resource("web.bundle.js")
    }
    accept(ContentType.Text.Html) {
        get<IndexPage> {
            val model = mapOf(
                "jsbundle" to "/frontend/web.bundle.js"
            )
            call.respond(FreeMarkerContent(template = "index.ftl", model = model))
        }
    }
}

private fun Application.createStorage(): KeynotedexDatabase = when {
    isDevelopment() -> KeynotedexDatabase(File("build/db"))
    else -> KeynotedexDatabase()
}

@UseExperimental(KtorExperimentalAPI::class)
fun Application.populateConferences(database: KeynotedexDatabase) = database.apply {
    if (conferences().isNotEmpty()) return@apply
    environment.config.configList("keynotedex.conferences.frontload")
        .map { it.property("repo").getString() to it.property("path").getString() }
        .flatMap { (repo, path) ->
            environment.log.debug("Loading conferences at repo $repo from path $path")
            GithubConferenceScrapper(oauthToken = environment.config.propertyOrNull("keynotedex.oauth.github.token")?.getString())
                .fetch(repo, path)
        }
        .map { it.toDao() }
        .toList()
        .also { environment.log.debug("Loading ${it.size} conferences into the database") }
        .let {
            db.transaction {
                it.forEach { conference ->
                    insertInto(ConferencesTable)
                        .values { values ->
                            values[name] = conference.name
                            values[dateStart] = conference.dateStart
                            values[dateEnd] = conference.dateEnd
                            values[location] = conference.location
                            values[website] = conference.website
                            values[twitter] = conference.twitter
                            values[cfpStart] = conference.cfpStart
                            values[cfpEnd] = conference.cfpEnd
                            values[cfpSite] = conference.cfpSite
                        }
                        .execute()
                }
            }
            environment.log.debug("Done loading conferences")
        }
}

@UseExperimental(KtorExperimentalAPI::class)
fun Application.isDevelopment() =
    environment.config.propertyOrNull("ktor.deployment.environment")?.getString() == "development"
