package com.ums.org.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;

public record UserResponse(@JsonAlias("userId") UUID id, String email, String firstName, String lastName) {
}
