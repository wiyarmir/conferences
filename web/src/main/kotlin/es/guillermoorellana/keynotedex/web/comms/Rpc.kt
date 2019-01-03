package es.guillermoorellana.keynotedex.web.comms


import es.guillermoorellana.keynotedex.api.*
import es.guillermoorellana.keynotedex.dto.Submission
import es.guillermoorellana.keynotedex.dto.User
import es.guillermoorellana.keynotedex.responses.*
import es.guillermoorellana.keynotedex.web.model.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import org.w3c.dom.url.*
import org.w3c.fetch.*
import kotlin.browser.*
import kotlin.js.*
import kotlinx.serialization.json.JSON as KJSON

suspend fun register(
    userId: String,
    password: String,
    displayName: String,
    email: String
): User = postAndParseResult(
    ApiPaths.register,
    URLSearchParams().apply {
        append("userId", userId)
        append("password", password)
        append("displayName", displayName)
        append("email", email)
    },
    { parseUserProfileResponse(it).user }
)

suspend fun user(userId: String) =
    getAndParseResult(ApiPaths.user.replace("{userId}", userId), null, { parseUserResponse(it) })
        .toModel()


suspend fun userProfile(userId: String) =
    getAndParseResult(ApiPaths.user.replace("{userId}", userId), null, { parseUserProfileResponse(it) })
        .toModel()

@UseExperimental(ImplicitReflectionSerializer::class)
suspend fun updateUserProfile(userProfile: UserProfile): UserProfile {
    val userId = userProfile.user.userId
    val body = KJSON.stringify(userProfile.toUpdateRequest())
    return putAndParseResult(ApiPaths.user.replace("{userId}", userId), body, { parseUserProfileResponse(it) })
        .toModel()
}

suspend fun checkSession() =
    getAndParseResult(ApiPaths.login, null, { parseUserResponse(it) })
        .toModel()

suspend fun login(userId: String, password: String) =
    postAndParseResult(
        ApiPaths.login,
        URLSearchParams().apply {
            append("userId", userId)
            append("password", password)
        },
        { parseUserResponse(it) }
    ).toModel()

suspend fun logoutUser() {
    window.fetch(
        ApiPaths.logout,
        object : RequestInit {
            override var method: String? = "POST"
            override var credentials: RequestCredentials? = RequestCredentials.SAME_ORIGIN
        })
        .await()
}

suspend fun getSubmission(submissionId: String) =
    getAndParseResult(
        ApiPaths.submissions.replace("{submissionId}", submissionId),
        null,
        { parseSubmissionResponse(it) })
        .toModel()

@UseExperimental(ImplicitReflectionSerializer::class)
private suspend fun parseUserResponse(response: Response): User {
    val responseText = response.text().await()
    when {
        response.ok -> {
            val userResponse: UserResponse = KJSON.parse(responseText)
            return userResponse.user
        }
        else -> {
            val errorResponse: ErrorResponse = KJSON.parse(responseText)
            throw LoginOrRegisterFailedException(errorResponse)
        }
    }
}

@UseExperimental(ImplicitReflectionSerializer::class)
private suspend fun parseUserProfileResponse(response: Response): UserProfileResponse {
    val responseText = response.text().await()
    when {
        response.ok -> {
            return KJSON.parse(responseText)
        }
        else -> {
            val errorResponse: ErrorResponse = KJSON.parse(responseText)
            throw LoginOrRegisterFailedException(errorResponse)
        }
    }
}

@UseExperimental(ImplicitReflectionSerializer::class)
private suspend fun parseSubmissionResponse(response: Response): Submission {
    val responseText = response.text().await()
    when {
        response.ok -> {
            val submissionResponse: SubmissionResponse = KJSON.parse(responseText)
            return submissionResponse.submission
        }
        else -> {
            val errorResponse: ErrorResponse = KJSON.parse(responseText)
            throw LoginOrRegisterFailedException(errorResponse)
        }
    }
}

class LoginOrRegisterFailedException(message: ErrorResponse) : Throwable(message.message)

suspend fun <T> postAndParseResult(url: String, body: dynamic, parse: suspend (Response) -> T): T =
    requestAndParseResult("POST", url, body, parse)

suspend fun <T> putAndParseResult(url: String, body: dynamic, parse: suspend (Response) -> T): T =
    requestAndParseResult("PUT", url, body, parse)

suspend fun <T> getAndParseResult(url: String, body: dynamic, parse: suspend (Response) -> T): T =
    requestAndParseResult("GET", url, body, parse)

suspend fun <T> requestAndParseResult(method: String, url: String, body: dynamic, parse: suspend (Response) -> T): T {
    val headers =
        mutableListOf("Accept" to "application/json")
            // TODO rewrite, so ugly
            .apply { if (body != null && body !is URLSearchParams) add("Content-Type" to "application/json") }
            .toTypedArray()
    val request: RequestInit = object : RequestInit {
        override var method: String? = method
        override var body: dynamic = body
        override var credentials: RequestCredentials? = RequestCredentials.SAME_ORIGIN
        override var headers: dynamic = json(*headers)
    }
    val response = window.fetch(url, request).await()
    return parse(response)
}
