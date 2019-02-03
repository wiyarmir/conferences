package es.guillermoorellana.keynotedex.backend.api.submission

import es.guillermoorellana.keynotedex.backend.JsonSerializableConverter
import es.guillermoorellana.keynotedex.backend.api.getCurrentLoggedUser
import es.guillermoorellana.keynotedex.backend.data.submissions.SubmissionStorage
import es.guillermoorellana.keynotedex.backend.data.submissions.toDao
import es.guillermoorellana.keynotedex.backend.data.users.UserStorage
import es.guillermoorellana.keynotedex.requests.SubmissionCreateRequest
import es.guillermoorellana.keynotedex.responses.ErrorResponse
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.accept
import java.sql.SQLException

fun Route.postSubmission(submissionStorage: SubmissionStorage, userStorage: UserStorage) {

    JsonSerializableConverter.register(SubmissionCreateRequest.serializer())

    accept(ContentType.Application.Json) {
        authenticate {
            post<SubmissionsEndpoint> {
                val user = getCurrentLoggedUser(userStorage)
                if (user == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@post
                }

                val incompleteSubmission = call.receive<SubmissionCreateRequest>()
                val submission = incompleteSubmission.toDao(userId = user.userId)
                try {
                    submissionStorage.create(submission)
                } catch (e: SQLException) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(message = "Failed to create submission: ${e.message}")
                    )
                }
                call.respond(HttpStatusCode.Created)
            }
        }
    }
}
