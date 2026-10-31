package com.teachdash.accounts.api.dto;

import java.util.UUID;

public record CreateTeacherResponse(UUID id, String setPasswordLink) {
}
