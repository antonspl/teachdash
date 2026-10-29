package com.teachdash.accounts;

public record ChangePasswordRequest(String currentPassword, String newPassword) {
}
