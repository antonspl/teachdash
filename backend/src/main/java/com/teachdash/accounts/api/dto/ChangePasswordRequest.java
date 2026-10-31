package com.teachdash.accounts.api.dto;

public record ChangePasswordRequest(String currentPassword, String newPassword) {
}
