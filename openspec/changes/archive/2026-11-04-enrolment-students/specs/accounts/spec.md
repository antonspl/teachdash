# Spec Delta

## ADDED Requirements

### Requirement: R11 Account fields have one rule
Name, email and time zone SHALL follow the same rules wherever an account is created or
changed. Whitespace around a name or email SHALL be ignored before they are checked and
stored, including invisible characters around a name. A name MUST contain a letter or
digit and MUST NOT contain line breaks, control or text-direction characters.

#### Scenario: accounts/R11-S1 Limits apply to teacher creation
- **GIVEN** a logged-in admin
- **WHEN** the admin creates a teacher with a name of 201 characters, an email of 321 characters, a blank email, a blank time zone, no time zone, and a name of only "\u00A0"
- **THEN** each response is 400 with code `VALIDATION_FAILED`, and no account is created

#### Scenario: accounts/R11-S2 Limits apply to the profile
- **GIVEN** a logged-in teacher
- **WHEN** the teacher updates the profile with a name of 201 characters, a blank time zone, no time zone, and a name of only "\u00A0"
- **THEN** each response is 400 with code `VALIDATION_FAILED`, and the profile is unchanged

#### Scenario: accounts/R11-S3 Whitespace around values is ignored
- **GIVEN** a logged-in admin and a logged-in teacher
- **WHEN** the admin creates a teacher with a 199-character name followed by two spaces and email " anna@example.com ", and the teacher updates the profile with a 199-character name followed by two spaces
- **THEN** both responses are successful, and the stored names and email have no surrounding whitespace

#### Scenario: accounts/R11-S4 Invisible characters around a name are removed
- **GIVEN** a logged-in admin
- **WHEN** the admin creates a teacher with the name "\u00A0Анна\u200B\u202F"
- **THEN** the response is 201 and the stored name is "Анна"

#### Scenario: accounts/R11-S5 Names that only look filled or hide characters are rejected
- **GIVEN** a logged-in admin
- **WHEN** the admin creates a teacher with each of the names "\u3164", "\u2800", "\u034F", "Anna\u202Exyz" and "A\nB"
- **THEN** each response is 400 with code `VALIDATION_FAILED`, and no account is created
