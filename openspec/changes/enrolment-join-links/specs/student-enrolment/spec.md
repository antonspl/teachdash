# Spec Delta

## ADDED Requirements

### Requirement: R8 Teacher manages their join link
A teacher SHALL have at most one join link and SHALL be able to view it, generate a new one
and disable it. Generating SHALL replace any current link, which then stops working.
Viewing SHALL NOT create a link. A link SHALL work until it is replaced or disabled.
Deactivating a teacher SHALL disable their link in the same transaction, and reactivating
the teacher SHALL NOT restore it. Only teachers may call these endpoints, each for their
own link.

#### Scenario: student-enrolment/R8-S1 Teacher generates and views the link
- **GIVEN** a logged-in teacher with no join link
- **WHEN** the teacher views the link, generates one, and views it again
- **THEN** the first view is 200 with no link, generating is 200 with a link that starts with the configured base URL (`APP_BASE_URL`) and carries the token in the URL fragment, and the second view returns the same link

#### Scenario: student-enrolment/R8-S2 Generating again replaces the link
- **GIVEN** a teacher with a join link L1
- **WHEN** the teacher generates a new link L2, and the preview is requested for L1 and for L2
- **THEN** L2 differs from L1, the view returns L2, the preview of L1 is 410 with code `LINK_INVALID`, and the preview of L2 is 200

#### Scenario: student-enrolment/R8-S3 Disabled link stops working
- **GIVEN** a teacher with a join link L1
- **WHEN** the teacher disables the link twice, views it, requests the preview of L1, and then generates a new link L2
- **THEN** both disable responses are 204, the view is 200 with no link, the preview of L1 is 410 with code `LINK_INVALID`, and the preview of L2 is 200

#### Scenario: student-enrolment/R8-S4 Each teacher has their own link
- **GIVEN** teachers A and B, each with a join link
- **WHEN** each teacher views their link, and a visitor joins through teacher B's link
- **THEN** the two links differ, and the new student is in teacher B's list and not in teacher A's

#### Scenario: student-enrolment/R8-S5 Deactivation disables the link for good
- **GIVEN** teacher A with a password and a join link L1
- **WHEN** the admin deactivates teacher A, the preview of L1 is requested, the admin reactivates teacher A, teacher A logs in and views the link, and the preview of L1 is requested again
- **THEN** both previews are 410 with code `LINK_INVALID`, and the view is 200 with no link

#### Scenario: student-enrolment/R8-S6 Students and admins cannot manage links
- **GIVEN** a logged-in student and a logged-in admin
- **WHEN** each views, generates and disables a join link through the teacher endpoints
- **THEN** all responses are 403, no link is created, and a logged-in teacher's view of their link is then 200

#### Scenario: student-enrolment/R8-S7 Link endpoints require a session
- **GIVEN** a client with a valid CSRF token and no session
- **WHEN** the client views, generates and disables a join link
- **THEN** all responses are 401 with code `UNAUTHENTICATED`, and a logged-in teacher's view of their link is then 200

#### Scenario: student-enrolment/R8-S8 Request without a CSRF token is rejected
- **GIVEN** a teacher with a join link L1
- **WHEN** the teacher generates a link without a CSRF token, then disables the link without a CSRF token
- **THEN** both responses are 403 with code `CSRF_INVALID`, the view still returns L1, and the preview of L1 is 200

#### Scenario: student-enrolment/R8-S9 Failed deactivation keeps the link
- **GIVEN** teacher A with a password and a join link L1, and a failure while the students are released
- **WHEN** the admin deactivates teacher A, and the preview of L1 is requested
- **THEN** the deactivation response is 500, and the preview of L1 is 200

### Requirement: R9 Visitor joins through a join link
A client without a session SHALL be able to preview an active join link (the teacher's
name) and register through it with name, email, time zone and password; the new student
is assigned to the link's teacher and logged in. Checks run in this order: a teacher or
admin session gets 403, invalid input 400, an invalid link 410 `LINK_INVALID`, an email
with an account 409 `EMAIL_TAKEN`. Rejections change nothing.

#### Scenario: student-enrolment/R9-S1 Visitor registers and is assigned
- **GIVEN** teacher A with a join link, and a client with a CSRF cookie and no session
- **WHEN** the client joins through the link with a new email and the password "new-password-1", and then sends a state-changing request with the CSRF token from the join response
- **THEN** the join response is 201 with the student's id, a session is started for the student, the response sets a new `XSRF-TOKEN` cookie different from the old one, the next request is not rejected for CSRF, the student is in teacher A's list, teacher A is the student's only teacher, the student logs in with "new-password-1" and has the role student, and the student has no unused set-password link

#### Scenario: student-enrolment/R9-S2 Preview shows the teacher's name
- **GIVEN** teacher A with the name "Мария" and a join link, and a client with no session
- **WHEN** the client requests the preview of the link
- **THEN** the response is 200 with the teacher's name "Мария" and no other details of the teacher

#### Scenario: student-enrolment/R9-S3 Invalid links get one answer
- **GIVEN** a link with an unknown token, a link replaced by a newer one, a disabled link, and the link of a teacher who was then deactivated
- **WHEN** a client with no session requests the preview of each and joins through each with a new email
- **THEN** every response is 410 with code `LINK_INVALID` and the same response body, byte for byte, and no account is created

#### Scenario: student-enrolment/R9-S4 Link is checked before the email
- **GIVEN** a student account with email "anna@example.com" and an unknown link token
- **WHEN** a client with no session joins through the unknown link with email "anna@example.com"
- **THEN** the response is 410 with code `LINK_INVALID`

#### Scenario: student-enrolment/R9-S5 Email that has an account is rejected
- **GIVEN** teacher A with a join link, a student with email "anna@example.com" and no teacher, a teacher with email "t@example.com", and an admin with email "a@example.com"
- **WHEN** a client with no session joins through the link with emails " ANNA@example.com ", "t@example.com" and "a@example.com"
- **THEN** each response is 409 with code `EMAIL_TAKEN`, no account is created, nothing is assigned to teacher A, and the student still has no teacher and keeps their name, time zone and password

#### Scenario: student-enrolment/R9-S6 Invalid input is rejected
- **GIVEN** teacher A with a join link
- **WHEN** a client with no session joins through the link with a blank name, an invalid email, an unknown time zone, no time zone, a name of 201 characters, an email of 321 characters, a password of 7 characters, a password of 73 bytes, a blank token, and a token of 65 characters
- **THEN** each response is 400 with code `VALIDATION_FAILED`, and no account is created

#### Scenario: student-enrolment/R9-S7 Submitted values are stored
- **GIVEN** teacher A with a join link
- **WHEN** a client with no session joins through the link with name " Анна ", email " Anna@Example.com " and time zone "Europe/Moscow"
- **THEN** the response is 201, and teacher A's request for the student's details returns the name "Анна", the email "anna@example.com" and the time zone "Europe/Moscow"

#### Scenario: student-enrolment/R9-S8 Request without a CSRF token is rejected
- **GIVEN** teacher A with a join link and a client with no session
- **WHEN** the client requests the preview and joins through the link with a new email, both without a CSRF token, and then joins again with the token
- **THEN** the first two responses are 403 with code `CSRF_INVALID`, the join with the token is 201, and there is exactly one account with that email

#### Scenario: student-enrolment/R9-S9 Failed join leaves nothing behind
- **GIVEN** teacher A with a join link and a failure while the assignment is stored
- **WHEN** a client with no session joins through the link with a new email
- **THEN** the response is 500, no account is created, no session is started, and teacher A's list is empty

#### Scenario: student-enrolment/R9-S10 Parallel joins create one account
- **GIVEN** teachers A and B, each with a join link
- **WHEN** clients send several parallel join requests with the same new email through both links
- **THEN** exactly one response is 201, all others are 409 with code `EMAIL_TAKEN`, and there is exactly one account with that email and one assignment

#### Scenario: student-enrolment/R9-S11 Teachers and admins cannot join
- **GIVEN** teacher A with a join link, a logged-in teacher B and a logged-in admin
- **WHEN** teacher B and the admin each request the preview and join through the link, with and without a new email
- **THEN** all responses are 403, no account is created, and teacher A's list is empty

#### Scenario: student-enrolment/R9-S12 Checks run in a fixed order
- **GIVEN** a logged-in teacher, a student account with email "anna@example.com", and an unknown link token
- **WHEN** the teacher joins through the unknown link with an invalid email, and a client with no session joins through the unknown link with an invalid email, then with email "anna@example.com"
- **THEN** the teacher's response is 403, the second response is 400 with code `VALIDATION_FAILED`, and the third is 410 with code `LINK_INVALID`

### Requirement: R10 Student with no teacher joins through a join link
A logged-in student with no teacher SHALL be assigned to the teacher of an active join
link they use. The student keeps their name, email, time zone and password; any other
fields in the request are ignored. A student who already has a teacher MUST get 409 and
change nothing: `ALREADY_ENROLLED` when it is the link's teacher, `STUDENT_EXISTS`
otherwise. An invalid link MUST get 410 with code `LINK_INVALID` and change nothing.

#### Scenario: student-enrolment/R10-S1 Student with no teacher is assigned
- **GIVEN** teacher A with a join link, and a logged-in student with no teacher, name "Анна", time zone "Europe/Moscow" and password "old-password-1"
- **WHEN** the student joins through the link, sending also the name "Anna" and the email "other@example.com"
- **THEN** the response is 200, the student is in teacher A's list with the name "Анна", the email and time zone are unchanged, teacher A is in the student's list of teachers, no account is created, and the student logs in with "old-password-1"

#### Scenario: student-enrolment/R10-S2 Student of another teacher is rejected
- **GIVEN** teacher A with a join link, and a logged-in student assigned to teacher B
- **WHEN** the student joins through teacher A's link
- **THEN** the response is 409 with code `STUDENT_EXISTS`, the student is still in teacher B's list, and teacher A's list is empty

#### Scenario: student-enrolment/R10-S3 Student of the same teacher is rejected
- **GIVEN** teacher A with a join link, and a logged-in student assigned to teacher A
- **WHEN** the student joins through the link
- **THEN** the response is 409 with code `ALREADY_ENROLLED`, and there is still exactly one assignment between them

#### Scenario: student-enrolment/R10-S4 Removed student can join again
- **GIVEN** teacher A with a join link, and a logged-in student assigned to teacher A
- **WHEN** teacher A removes the student, and the student then joins through the link
- **THEN** the join response is 200, and the student is in teacher A's list

#### Scenario: student-enrolment/R10-S5 Parallel joins assign the student once
- **GIVEN** teachers A and B, each with a join link, and a logged-in student with no teacher
- **WHEN** the student sends several parallel join requests through both links
- **THEN** exactly one response is 200, the other responses through the winning teacher's link are 409 with code `ALREADY_ENROLLED`, those through the other link are 409 with code `STUDENT_EXISTS`, and the student has exactly one teacher

#### Scenario: student-enrolment/R10-S6 Invalid link changes nothing
- **GIVEN** teacher A whose join link was disabled, teacher B with a join link who was then deactivated, and a logged-in student with no teacher
- **WHEN** the student joins through teacher A's disabled link and through teacher B's link
- **THEN** both responses are 410 with code `LINK_INVALID`, and the student still has no teacher

### Requirement: R11 Join endpoints are rate limited
The join-link preview and join endpoints SHALL be limited per client IP, each with its own
configurable number of requests per configurable window. A request over the limit MUST get
429 with code `RATE_LIMITED` and a `Retry-After` header in seconds, and change nothing.
Requests from other IPs and to other endpoints SHALL NOT be affected. Rejected requests
count towards the limit, whatever their response.

#### Scenario: student-enrolment/R11-S1 Request over the limit is rejected
- **GIVEN** a join limit of 2 requests per 10 minutes, and teacher A with a join link
- **WHEN** a client from one IP joins through the link with an unknown token twice, and then with a new email
- **THEN** the first two responses are 410, the third is 429 with code `RATE_LIMITED` and a `Retry-After` header between 1 and 600, and no account is created

#### Scenario: student-enrolment/R11-S2 Limit is per IP and per endpoint
- **GIVEN** a join limit of 2 and a preview limit of 2 requests per 10 minutes, teacher A with a join link, and a client from IP 1 that has used up the join limit
- **WHEN** a client from IP 2 joins through the link with a new email, and the client from IP 1 requests the preview and logs in as an existing teacher
- **THEN** the join from IP 2 is 201, the preview from IP 1 is 200, and the login from IP 1 is 200

#### Scenario: student-enrolment/R11-S3 Limit resets after the window
- **GIVEN** a preview limit of 2 requests per 10 minutes, teacher A with a join link, and a client that has used up the preview limit
- **WHEN** 10 minutes pass and the client requests the preview again
- **THEN** the response is 200
