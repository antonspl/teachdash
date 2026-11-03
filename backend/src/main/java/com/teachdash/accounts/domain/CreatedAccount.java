package com.teachdash.accounts.domain;

import java.util.UUID;

public record CreatedAccount(
		UUID id,
		String setPasswordLink) {
}
