package com.sulck.backend.domain.usecase

object AuthUseCases {
    lateinit var createUserUseCase: CreateUserUseCase
    lateinit var getUserUseCase: GetUserUseCase
    lateinit var authenticateUserUseCase: AuthenticateUserUseCase
    lateinit var refreshTokenUseCase: RefreshTokenUseCase
}