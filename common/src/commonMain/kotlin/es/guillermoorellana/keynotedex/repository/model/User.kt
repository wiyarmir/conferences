package es.guillermoorellana.keynotedex.repository.model

import es.guillermoorellana.keynotedex.datasource.dto.User as DtoUser

data class User(
    val userId: String,
    val displayName: String,
    val bio: String?
)

fun DtoUser.toModel() = User(
    userId = userId,
    displayName = displayName ?: userId,
    bio = bio
)

fun User.toDto() = DtoUser(
    userId = userId,
    displayName = displayName,
    bio = bio
)
