# Spec Delta

## ADDED Requirements

### Requirement: R13 A new student has a native language
Registering a new student (R1) and joining through a join link without a session (R9)
SHALL require `nativeLanguage`, a code from the language list (languages R1), in
addition to the fields those requirements name. It is checked with the other fields: a
missing value is `REQUIRED`, any other value not in the list is `INVALID_FORMAT`. An
existing account that is assigned (R1, R10) keeps its stored language.

#### Scenario: student-enrolment/R13-S1 Teacher registers a student with a native language
- **GIVEN** a logged-in teacher A
- **WHEN** teacher A registers a student with the name "Marko", a new email, the time zone "Europe/Belgrade" and the native language "sr", and another with the name "Оксана", a new email, the time zone "Europe/Kyiv" and the native language "uk"
- **THEN** both responses are 201, and teacher A's student details show the native language "sr" for Marko and "uk" for Оксана

#### Scenario: student-enrolment/R13-S2 Registration needs a listed native language
- **GIVEN** a logged-in teacher A
- **WHEN** teacher A registers students with the name "Anna", a new email, the time zone "Europe/Berlin" and, in turn: no native language; `null`; ""; "xx"; "SR"; "srp"
- **THEN** the responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "nativeLanguage", code: "REQUIRED"}]` for the first two and `[{field: "nativeLanguage", code: "INVALID_FORMAT"}]` for the others, no body contains "xx" or "srp", and no account is created

#### Scenario: student-enrolment/R13-S3 All failing fields are reported together
- **GIVEN** a logged-in teacher A
- **WHEN** teacher A registers a student with a blank name, a new email, the time zone "Europe/Berlin" and the native language "xx"
- **THEN** the response is 400 with `errors` exactly `[{field: "name", code: "REQUIRED"}, {field: "nativeLanguage", code: "INVALID_FORMAT"}]`

#### Scenario: student-enrolment/R13-S4 An existing student keeps their language
- **GIVEN** a logged-in teacher A, and a student with no teacher and no password, email "anna@example.com" and native language "de"
- **WHEN** teacher A registers a student with the email "anna@example.com", the name "Anna", the time zone "Europe/Berlin" and the native language "fr"
- **THEN** the response is 201, the student is assigned to teacher A, and teacher A's student details show the native language "de"

#### Scenario: student-enrolment/R13-S5 A visitor joins with a native language
- **GIVEN** teacher A with a join link, and a client with a CSRF cookie and no session
- **WHEN** the client joins through the link with the name "Kasia", a new email, the time zone "Europe/Warsaw", the password "password-1" and the native language "pl"
- **THEN** the response is 201, and teacher A's student details show the native language "pl"

#### Scenario: student-enrolment/R13-S6 A visitor must give a listed native language
- **GIVEN** teacher A with an active join link, an expired join link, and a client with a CSRF cookie and no session
- **WHEN** the client joins through the active link with the name "Anna", a new email, the time zone "Europe/Berlin", the password "password-1" and no native language; then with the native language "xx"; then through the expired link with the same details and no native language
- **THEN** the first and third responses are 400 with `errors` exactly `[{field: "nativeLanguage", code: "REQUIRED"}]`, the second is 400 with `errors` exactly `[{field: "nativeLanguage", code: "INVALID_FORMAT"}]`, and no account is created

#### Scenario: student-enrolment/R13-S7 A logged-in student keeps their language when joining
- **GIVEN** teacher A with a join link, and a logged-in student with no teacher and native language "it"
- **WHEN** the student joins through the link, sending the native language "es"
- **THEN** the response is 200, and teacher A's student details show the native language "it"

### Requirement: R14 A teacher changes their student's native language
`PATCH /api/teacher/students/{id}` SHALL set the native language of the teacher's own
student from `{nativeLanguage}`, with the R13 rules, and return 200 with the student's
details. Other fields in the body are ignored. A student who is not the caller's, an
unknown id or a malformed id gets 404 `NOT_FOUND` and changes nothing. Only teachers
may call it.

#### Scenario: student-enrolment/R14-S1 Teacher changes the native language
- **GIVEN** teacher A with a student "Анна" whose native language is "ru"
- **WHEN** teacher A sets the student's native language to "uk", sending also the name "Anna"
- **THEN** the response is 200 with the student's details and `nativeLanguage` "uk", teacher A's student details show "uk", and the name is still "Анна"

#### Scenario: student-enrolment/R14-S2 Invalid values change nothing
- **GIVEN** teacher A with a student whose native language is "ru"
- **WHEN** teacher A sets the student's native language to "xx", to `null`, and with an empty body
- **THEN** the first response is 400 with `errors` exactly `[{field: "nativeLanguage", code: "INVALID_FORMAT"}]`, the other two are 400 with `errors` exactly `[{field: "nativeLanguage", code: "REQUIRED"}]`, and the native language is still "ru"

#### Scenario: student-enrolment/R14-S3 Only the teacher's own students
- **GIVEN** teacher A, teacher B with a student S whose native language is "ru", a student with no teacher, and teacher A logged in
- **WHEN** teacher A sets the native language of S, of the student with no teacher, of an unknown id and of the id "not-a-uuid" to "de"
- **THEN** each response is 404 with code `NOT_FOUND` and no `errors` property, and S's native language is still "ru"

#### Scenario: student-enrolment/R14-S4 Only teachers change it
- **GIVEN** teacher A with a student S, a logged-in admin, S logged in, and a client with a CSRF cookie and no session
- **WHEN** the admin, S and the client each set S's native language to "de"
- **THEN** the admin's and S's responses are 403 with code `FORBIDDEN`, the client's is 401 with code `UNAUTHENTICATED`, and S's native language is unchanged

### Requirement: R15 Student data shows the native language
Every response that returns a student's details to their teacher (R2), to an admin
(R3, R6) or to the student (`GET /api/me`) SHALL include `nativeLanguage`. Responses about
a teacher or an admin MUST NOT include it. The student MUST NOT be able to change it
through the profile (accounts R9).

#### Scenario: student-enrolment/R15-S1 The teacher sees it
- **GIVEN** teacher A with students "Анна" (native language "ru") and "Ben" (native language "en")
- **WHEN** teacher A lists students and reads Анна's details
- **THEN** the list shows `nativeLanguage` "ru" for Анна and "en" for Ben, and the details show "ru"

#### Scenario: student-enrolment/R15-S2 The admin sees it on students only
- **GIVEN** a logged-in admin, and teacher A with student S, email "s@example.com", native language "pt"
- **WHEN** the admin lists teacher A's students, looks up "s@example.com" and lists S's teachers
- **THEN** the first two responses show `nativeLanguage` "pt" for S, and the entry for teacher A in the third has no `nativeLanguage` property

#### Scenario: student-enrolment/R15-S3 The student sees it and cannot change it
- **GIVEN** a logged-in student with the name "Анна" and native language "ru", and a logged-in teacher
- **WHEN** the student reads the current user, sets the name "Anna" with the native language "de" in the same request, and reads the current user again; and the teacher reads the current user
- **THEN** the student's responses show `nativeLanguage` "ru", the second read shows the name "Anna", and the teacher's response has no `nativeLanguage` property
