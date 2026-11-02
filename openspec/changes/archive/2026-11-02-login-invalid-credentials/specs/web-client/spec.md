# Spec Delta

## MODIFIED Requirements

### Requirement: R5 Translated API errors

When a request fails, the API client SHALL give the caller a translated message chosen
by the problem `code`. A code without a translation, or a response that is not a problem
response, SHALL give a generic translated message. The message for `INVALID_CREDENTIALS`
SHALL say that the email or password is incorrect, and that a user who has not set a
password yet uses the link they were given.

#### Scenario: web-client/R5-S1 Known problem code gives its translated message
- **GIVEN** the backend answers with a problem response with code `WRONG_PASSWORD`
- **WHEN** the API client handles the response
- **THEN** the caller gets the translated message for `WRONG_PASSWORD`

#### Scenario: web-client/R5-S2 Unknown code or non-problem response gives the generic message
- **GIVEN** the response has a code without a translation, or is not a problem response
- **WHEN** the API client handles the response
- **THEN** the caller gets the generic translated error message

#### Scenario: web-client/R5-S3 Real backend error is recognised as a problem response
- **GIVEN** the local stack is running
- **WHEN** a client sends a login with an empty body and the headers the API client uses
- **THEN** the response has content type `application/problem+json` and code `VALIDATION_FAILED`

#### Scenario: web-client/R5-S4 Failed login message includes the set-password hint
- **GIVEN** the backend answers with a problem response with code `INVALID_CREDENTIALS`
- **WHEN** the API client handles the response
- **THEN** the caller gets a message saying the email or password is incorrect and pointing to the set-password link
