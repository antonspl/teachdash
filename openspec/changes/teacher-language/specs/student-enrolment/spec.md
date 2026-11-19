# Spec Delta

## MODIFIED Requirements

### Requirement: R15 Student data shows the native language
Every response that returns a student's details to their teacher (R2), to an admin
(R3, R6) or to the student (their current-user data, as from `GET /api/me`, login and setting a password)
SHALL include `nativeLanguage`. So SHALL a teacher's current-user data (the same
endpoints), the admin's teacher list (accounts R12) and every entry for a teacher in the
admin's lists (R3). Responses about an admin MUST NOT include it. The student MUST NOT be
able to change it through the profile (accounts R9); a teacher changes their own (accounts R13).

#### Scenario: student-enrolment/R15-S1 The teacher sees it
- **GIVEN** teacher A with students "Анна" (native language "ru") and "Ben" (native language "en")
- **WHEN** teacher A lists students and reads Анна's details
- **THEN** the list shows `nativeLanguage` "ru" for Анна and "en" for Ben, and the details show "ru"

#### Scenario: student-enrolment/R15-S2 The admin sees it on students only
- **GIVEN** a logged-in admin, and teacher A with native language "fr" and student S, email "s@example.com", native language "pt"
- **WHEN** the admin lists teacher A's students, looks up "s@example.com" and lists S's teachers
- **THEN** the first two responses show `nativeLanguage` "pt" for S, and the entry for teacher A in the third shows `nativeLanguage` "fr"

#### Scenario: student-enrolment/R15-S3 The student sees it and cannot change it
- **GIVEN** a logged-in student with the name "Анна" and native language "ru", and a logged-in teacher with native language "fr"
- **WHEN** the student logs in, reads the current user, sets the name "Anna" with the native language "de" in the same request, and reads the current user again; and the teacher reads the current user
- **THEN** the student's responses show `nativeLanguage` "ru", the second read shows the name "Anna", and the teacher's response shows `nativeLanguage` "fr"

#### Scenario: student-enrolment/R15-S4 Setting a password returns it
- **GIVEN** teacher A with a student with no password, native language "sr" and a valid set-password link
- **WHEN** the student sets the password "password-1" through the link
- **THEN** the response shows `nativeLanguage` "sr"

#### Scenario: student-enrolment/R15-S5 A teacher's login and password link return it, an admin's data does not
- **GIVEN** a teacher with native language "es", no password and a valid set-password link, and a logged-in admin
- **WHEN** the teacher sets the password "password-1" through the link and then logs in with it, and the admin logs in and reads the current user
- **THEN** both of the teacher's responses show `nativeLanguage` "es", and the admin's responses have no `nativeLanguage` property
