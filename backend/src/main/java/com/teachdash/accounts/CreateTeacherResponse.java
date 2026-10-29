package com.teachdash.accounts;

import java.util.UUID;

public record CreateTeacherResponse(UUID id, String setPasswordLink) {
}
