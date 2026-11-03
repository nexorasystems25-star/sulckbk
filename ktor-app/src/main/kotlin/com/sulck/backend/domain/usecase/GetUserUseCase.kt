package com.sulck.backend.domain.usecase

import com.sulck.backend.domain.repository.UserRepository
import com.sulck.backend.domain.entity.User
import javax.inject.Inject
import java.util.UUID

class GetUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(userId: UUID): User? {
        return userRepository.findById(userId)
    }
}