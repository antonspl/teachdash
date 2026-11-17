# Spec Delta

## Purpose

Defines the languages the platform knows, as configured data: which may be a student's
native language and which may also be a course language.

## ADDED Requirements

### Requirement: R1 Anyone reads the language list
`GET /api/languages` SHALL return 200 to any caller, with or without a session, with the
known languages as a list of `{code, content}` sorted by code. `code` is a lowercase
ISO 639-1 code; `content` is true when the language may be a course language. Every
listed language may be a student's native language. The list is prefilled by a database
migration; names are not returned.

#### Scenario: languages/R1-S1 A visitor reads the list prefilled by the DB migration
- **GIVEN** a client with no session
- **WHEN** the client reads the language list
- **THEN** the response is 200 with the body exactly `[{code: "de", content: false}, {code: "en", content: true}, {code: "es", content: false}, {code: "fr", content: false}, {code: "it", content: false}, {code: "pl", content: false}, {code: "pt", content: false}, {code: "ru", content: true}, {code: "sr", content: false}, {code: "uk", content: false}]`

#### Scenario: languages/R1-S2 Every role reads the same list
- **GIVEN** a logged-in admin, a logged-in teacher and a logged-in student
- **WHEN** each of them reads the language list
- **THEN** each response is 200 with the same body as for a client with no session
