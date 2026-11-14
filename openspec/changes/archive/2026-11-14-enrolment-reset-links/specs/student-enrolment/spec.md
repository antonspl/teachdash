## ADDED Requirements

### Requirement: R12 Teacher issues password reset links for own students
A teacher SHALL be able to issue a password reset link for a student assigned to them,
with or without a password. The link follows the rules and effects of an admin's reset
link (accounts R7 and R8): it voids the older unused link this teacher issued for the
student, an admin's unused link stays usable until one of them is used, and using it
replaces the password and ends the student's other sessions. An id that is not a student
assigned to the teacher MUST return 404, as in R2, and change nothing.

#### Scenario: student-enrolment/R12-S1 Teacher issues a reset link for own student
- **GIVEN** a student with the password "old-password-1", assigned to teacher A, and logged in on another client
- **WHEN** teacher A requests a reset link for the student and the link is used to set the password "new-password-1"
- **THEN** the response to the request is 201 with a link that starts with the configured base URL (`APP_BASE_URL`) and carries the token in the URL fragment, the link response is 200, logging in with "old-password-1" returns 401, the student's earlier session gets 401, and the student logs in with "new-password-1"

#### Scenario: student-enrolment/R12-S2 Another teacher's student is not found
- **GIVEN** a student without a password, assigned to teacher B, with an unused set-password link issued by teacher B
- **WHEN** teacher A requests a reset link for that student
- **THEN** the response is 404 with code `NOT_FOUND`, and the set-password link still sets the password

#### Scenario: student-enrolment/R12-S3 Ids that are not own students are not found
- **GIVEN** a logged-in teacher A, teacher B with an unused link, a student with no teacher and an unused link, and an id with no account
- **WHEN** teacher A requests a reset link for teacher B's id, the student's id, the id with no account and the id "not-a-uuid"
- **THEN** each of the four responses is 404 with code `NOT_FOUND`, the existing links of teacher B and the student are still unused, and no other link is stored for either

#### Scenario: student-enrolment/R12-S4 Student without a password gets a working link
- **GIVEN** a student without a password, assigned to teacher A, with an unused set-password link issued by teacher A
- **WHEN** teacher A requests a reset link for the student, the set-password link is then used to set the password "old-link-1", and the reset link to set the password "new-password-1"
- **THEN** the response to the request is 201 with a link, the set-password link response is 410 with code `LINK_INVALID`, and the student logs in with "new-password-1"

#### Scenario: student-enrolment/R12-S5 Teacher's link keeps an admin's unused link until one is used
- **GIVEN** a student with a password, assigned to teacher A, with an unused reset link issued by an admin
- **WHEN** teacher A requests a reset link for the student, the admin's link is then used to set the password "admin-password-1", and the teacher's link to set the password "new-password-1"
- **THEN** the admin's link response is 200, the teacher's link response is 410 with code `LINK_INVALID`, and the student logs in with "admin-password-1"

#### Scenario: student-enrolment/R12-S6 Students and admins cannot issue through the teacher endpoint
- **GIVEN** a student assigned to teacher A, a logged-in student and a logged-in admin
- **WHEN** the logged-in student and the admin each request a reset link for that student through the teacher endpoint
- **THEN** both responses are 403

#### Scenario: student-enrolment/R12-S7 Issuing requires a session
- **GIVEN** a student assigned to teacher A and a client with a valid CSRF token and no session
- **WHEN** the client requests a reset link for that student
- **THEN** the response is 401 with code `UNAUTHENTICATED`

#### Scenario: student-enrolment/R12-S8 Request without a CSRF token is rejected
- **GIVEN** a student with the password "old-password-1", assigned to teacher A
- **WHEN** teacher A requests a reset link for the student without a CSRF token
- **THEN** the response is 403 with code `CSRF_INVALID`, and the student still logs in with "old-password-1"

#### Scenario: student-enrolment/R12-S9 Only the newest link works
- **GIVEN** a student with a password, assigned to teacher A
- **WHEN** teacher A requests a reset link twice, the first link is then used to set the password "first-password-1", and the second to set the password "new-password-1"
- **THEN** both requests return 201, the first link response is 410 with code `LINK_INVALID`, and the student logs in with "new-password-1"

## MODIFIED Requirements

### Requirement: R1 Teacher registers a new student
A teacher SHALL be able to register a student by name, email and time zone; emails are
compared case-insensitively. A new email creates a passwordless student account, assigned
to the teacher, with a single-use set-password link in the response. The email of a student
with no teacher and no password assigns that account and keeps its details; the response
carries a new set-password link, which voids the older unused links this teacher issued
for it; an admin's unused link stays usable until one of them is used (accounts R7). Any other existing
email MUST return 409 and change nothing, with the first matching code in this order:
`EMAIL_TAKEN` for a teacher or admin account; `ALREADY_ENROLLED` for a student assigned to
this teacher; `STUDENT_EXISTS` for a student assigned to another teacher, or for a student
with no teacher and a password. Such a student joins a teacher through a join link (R10).

#### Scenario: student-enrolment/R1-S1 New email creates and assigns a student
- **GIVEN** a logged-in teacher
- **WHEN** the teacher registers a student with a new email and the returned set-password link is used to set the password "new-password-1"
- **THEN** the response to registering is 201 with the student's id and a set-password link that starts with the configured base URL (`APP_BASE_URL`) and carries the token in the URL fragment, the student appears in the teacher's student list, and the student logs in with "new-password-1" and has the role student

#### Scenario: student-enrolment/R1-S2 Existing student is not assigned
- **GIVEN** a student without a password, assigned to teacher B, with an unused set-password link
- **WHEN** teacher A registers a student with that email and a different name and time zone
- **THEN** the response is 409 with code `STUDENT_EXISTS`, the student is not in teacher A's list, the student is still in teacher B's list, the student's name and time zone are unchanged, and the unused link still sets the password

#### Scenario: student-enrolment/R1-S3 Email of a teacher or admin is rejected
- **GIVEN** a teacher account with email "t@example.com" and an admin account with email "a@example.com"
- **WHEN** another teacher registers a student with each of these emails
- **THEN** both responses are 409 with code `EMAIL_TAKEN`, and nothing is assigned

#### Scenario: student-enrolment/R1-S4 Already assigned student is rejected
- **GIVEN** a student assigned to teacher A
- **WHEN** teacher A registers a student with that email again
- **THEN** the response is 409 with code `ALREADY_ENROLLED` and there is still exactly one assignment between them

#### Scenario: student-enrolment/R1-S5 Email case is ignored
- **GIVEN** a student with email "student@example.com", assigned to teacher B
- **WHEN** teacher A registers a student with email "STUDENT@example.com"
- **THEN** the response is 409 with code `STUDENT_EXISTS`, the student is not in teacher A's list, and no account is created

#### Scenario: student-enrolment/R1-S6 Invalid input is rejected
- **GIVEN** a logged-in teacher
- **WHEN** the teacher registers a student with a blank name, then with an invalid email, then with an unknown time zone
- **THEN** each response is 400 with code `VALIDATION_FAILED`, and no account is created

#### Scenario: student-enrolment/R1-S7 Email with invisible characters is rejected
- **GIVEN** a logged-in teacher and a student account with email "nina@example.com" and no teacher
- **WHEN** the teacher registers a student with email "nina @example.com", then with email " @example.com"
- **THEN** each response is 400 with code `VALIDATION_FAILED`, no account is created, and the student is not in the teacher's list

#### Scenario: student-enrolment/R1-S8 Field limits apply
- **GIVEN** a logged-in teacher
- **WHEN** the teacher registers a student with a name of 201 characters, an email of 321 characters, a blank email, a blank time zone, no time zone, and a name of only " "
- **THEN** each response is 400 with code `VALIDATION_FAILED`, and no account is created

#### Scenario: student-enrolment/R1-S9 Surrounding whitespace is ignored
- **GIVEN** a logged-in teacher A and a student with email "nina@example.com", assigned to teacher B
- **WHEN** teacher A registers a student with a 199-character name followed by two spaces and email " anna@example.com ", then with email " nina@example.com "
- **THEN** the first response is 201 and the account stores the name and email without the whitespace, and the second response is 409 with code `STUDENT_EXISTS`

#### Scenario: student-enrolment/R1-S10 Submitted values are stored
- **GIVEN** a logged-in teacher
- **WHEN** the teacher registers a student with name "Анна", email "anna@example.com" and time zone "Europe/Moscow"
- **THEN** the student's details return that name, email and time zone

#### Scenario: student-enrolment/R1-S11 Request without a CSRF token is rejected
- **GIVEN** a logged-in teacher
- **WHEN** the teacher registers a student without a CSRF token
- **THEN** the response is 403 with code `CSRF_INVALID`, and no account is created

#### Scenario: student-enrolment/R1-S12 Failed registration leaves nothing behind
- **GIVEN** a logged-in teacher and a failure while the set-password link is issued
- **WHEN** the teacher registers a student with a new email
- **THEN** the response is 500, and neither an account nor an assignment is created

#### Scenario: student-enrolment/R1-S13 Parallel requests create one account
- **GIVEN** two logged-in teachers
- **WHEN** both send several parallel requests to register the same new email
- **THEN** exactly one response is 201, all others are 409, and there is exactly one account, one assignment and one unused link

#### Scenario: student-enrolment/R1-S14 Student with no teacher and a password is not assigned
- **GIVEN** a student with no teacher, with email "anna@example.com", name "Анна", time zone "Europe/Moscow", the password "old-password-1" and an unused reset link issued by an admin
- **WHEN** teacher A registers a student with email " ANNA@example.com ", name "Anna" and time zone "Europe/Berlin"
- **THEN** the response is 409 with code `STUDENT_EXISTS` and no link, there is still exactly one account with that email, the student is not in teacher A's list, teacher A's request for the student's details returns 404, the student has no teacher, name "Анна" and time zone "Europe/Moscow", the student logs in with "old-password-1", and the admin's link then sets the password "admin-password-1"

#### Scenario: student-enrolment/R1-S15 Registering a student with no teacher and no password assigns them with a new link
- **GIVEN** a student without a password and with email "nina@example.com", assigned to teacher B, with an unused set-password link
- **WHEN** teacher B removes the student, teacher A then registers a student with email "Nina@Example.com", and the link returned to teacher A is used to set the password "new-password-1"
- **THEN** the response to registering is 201 with the student's id and a set-password link, there is still exactly one account with that email, the student is in teacher A's list, and the student logs in with "new-password-1"

#### Scenario: student-enrolment/R1-S16 Parallel requests assign a student with no teacher once
- **GIVEN** a student without a password and with no teacher, and two logged-in teachers
- **WHEN** both teachers send several parallel requests to register that student's email
- **THEN** exactly one response is 201, the other responses to the winning teacher are 409 with code `ALREADY_ENROLLED`, the responses to the other teacher are 409 with code `STUDENT_EXISTS`, and the student has exactly one assignment and one unused link

#### Scenario: student-enrolment/R1-S17 Failed assignment leaves nothing behind
- **GIVEN** a logged-in teacher, a student without a password and with no teacher, and a failure while the set-password link is issued
- **WHEN** the teacher registers a student with that email
- **THEN** the response is 500, and the student is not in the teacher's list

#### Scenario: student-enrolment/R1-S18 New link keeps an admin's unused link until one is used
- **GIVEN** a student without a password and with no teacher, with an unused reset link issued by an admin
- **WHEN** teacher A registers a student with that email, the admin's link is then used to set the password "admin-password-1", and the link returned to teacher A to set the password "new-password-1"
- **THEN** the response to registering is 201 with a set-password link, the admin's link response is 200, the teacher's link response is 410 with code `LINK_INVALID`, and the student logs in with "admin-password-1"

### Requirement: R4 Teacher removes their student
A teacher SHALL be able to remove a student assigned to them. Removal deletes the
assignment and keeps the student's account, which then has no teacher. The student can join
a teacher again through a join link (R10), or be registered again as in R1 if it has no
password. Removal MUST also invalidate the student's unused link if this teacher
issued it, whether a set-password link (R1) or a reset link (R12); a link an admin issued
stays usable. An id that is not a student assigned to the teacher MUST return 404, as in R2.

#### Scenario: student-enrolment/R4-S1 Removed student disappears from the lists
- **GIVEN** a student with a password, assigned to teacher A
- **WHEN** teacher A removes the student
- **THEN** the response is 204, the student is not in teacher A's list, teacher A is not in the student's list of teachers, and the student can still log in

#### Scenario: student-enrolment/R4-S3 Removal keeps the account and invalidates the unused link
- **GIVEN** a student without a password, assigned to teacher A, with an unused set-password link issued by teacher A, and a logged-in admin
- **WHEN** teacher A removes the student, the link is then used to set the password "new-password-1", and the admin looks up the student's email
- **THEN** the removal response is 204, the link response is 410 with code `LINK_INVALID`, logging in with "new-password-1" returns 401, and the lookup response is 200

#### Scenario: student-enrolment/R4-S4 Another teacher's student is not found
- **GIVEN** a student without a password, assigned to teacher B, with an unused set-password link issued by teacher B
- **WHEN** teacher A removes that student, and then an id with no account
- **THEN** both responses are 404 with code `NOT_FOUND`, the student is still in teacher B's list, and the link still sets the password

#### Scenario: student-enrolment/R4-S5 Removing twice is not found
- **GIVEN** a student assigned to teacher A
- **WHEN** teacher A removes the student twice
- **THEN** the first response is 204 and the second is 404 with code `NOT_FOUND`

#### Scenario: student-enrolment/R4-S6 Malformed id is not found
- **GIVEN** a logged-in teacher
- **WHEN** the teacher removes the id "not-a-uuid"
- **THEN** the response is 404 with code `NOT_FOUND`

#### Scenario: student-enrolment/R4-S7 Removed student can join another teacher
- **GIVEN** a student with a password and a session, assigned to teacher A, and teacher B with a session and a join link
- **WHEN** teacher A removes the student, teacher B registers a student with that email, the student then joins through teacher B's link, and teacher A then registers a student with that email
- **THEN** the removal response is 204, teacher B's registration response is 409 with code `STUDENT_EXISTS`, the join response is 200, teacher A's response is 409 with code `STUDENT_EXISTS`, the student is in teacher B's list and not in teacher A's, teacher B's request for the student's details returns 200 and teacher A's returns 404, and teacher B is the only entry in the student's list of teachers

#### Scenario: student-enrolment/R4-S8 Students and admins cannot remove
- **GIVEN** a student assigned to teacher A, a logged-in student and a logged-in admin
- **WHEN** the logged-in student and the admin each remove that student through the teacher endpoint
- **THEN** both responses are 403, and the student is still in teacher A's list

#### Scenario: student-enrolment/R4-S9 Removing requires a session
- **GIVEN** a student assigned to teacher A and a client with a valid CSRF token and no session
- **WHEN** the client removes that student
- **THEN** the response is 401 with code `UNAUTHENTICATED`, and the student is still in teacher A's list

#### Scenario: student-enrolment/R4-S10 Request without a CSRF token is rejected
- **GIVEN** a student assigned to teacher A
- **WHEN** teacher A removes the student without a CSRF token
- **THEN** the response is 403 with code `CSRF_INVALID`, and the student is still in teacher A's list

#### Scenario: student-enrolment/R4-S11 Teacher's other students stay
- **GIVEN** teacher A with students S1 and S2
- **WHEN** teacher A removes S1
- **THEN** the response is 204, and teacher A's list contains S2 only

#### Scenario: student-enrolment/R4-S13 Removal keeps an admin's reset link
- **GIVEN** a student with a password, assigned to teacher A, with an unused reset link issued by an admin
- **WHEN** teacher A removes the student and the link is then used to set the password "new-password-1"
- **THEN** the removal response is 204, the link response is 200, and the student logs in with "new-password-1"

#### Scenario: student-enrolment/R4-S14 Removed student can join the same teacher again
- **GIVEN** a student with a password and a session, assigned to teacher A, and teacher A's join link
- **WHEN** teacher A removes the student and then registers a student with that email, and the student then joins through teacher A's link
- **THEN** the removal response is 204, the registration response is 409 with code `STUDENT_EXISTS`, the join response is 200, and the student is in teacher A's list

#### Scenario: student-enrolment/R4-S15 Removal invalidates the teacher's reset link
- **GIVEN** a student with the password "old-password-1", assigned to teacher A, with an unused reset link issued by teacher A
- **WHEN** teacher A removes the student and the link is then used to set the password "new-password-1"
- **THEN** the removal response is 204, the link response is 410 with code `LINK_INVALID`, logging in with "new-password-1" returns 401, and the student logs in with "old-password-1"

#### Scenario: student-enrolment/R4-S16 Removal keeps an admin's link that the teacher's newer link did not void
- **GIVEN** a student with a password, assigned to teacher A, with an unused reset link issued by an admin
- **WHEN** teacher A requests a reset link for the student and then removes the student, the teacher's link is then used to set the password "teacher-password-1", and the admin's link to set the password "admin-password-1"
- **THEN** the removal response is 204, the teacher's link response is 410 with code `LINK_INVALID`, the admin's link response is 200, and the student logs in with "admin-password-1"

### Requirement: R7 Deactivating a teacher releases their students
Deactivating a teacher SHALL remove all of that teacher's assignments in the same
transaction, as if the teacher removed each student (R4): each account stays, has no
teacher, can join or be registered again as in R4, and its unused link is invalidated if
that teacher issued it; a link an admin issued stays usable.
Reactivating the teacher SHALL NOT restore the assignments.

#### Scenario: student-enrolment/R7-S1 Deactivation releases every student
- **GIVEN** teacher A with a student S1 who has a password and a student S2 without a password with an unused set-password link issued by teacher A, and teacher B with a student S3 without a password with an unused set-password link issued by teacher B
- **WHEN** the admin deactivates teacher A, the link of S2 is then used to set the password "new-password-1", and the link of S3 to set the password "new-password-3"
- **THEN** the deactivation response is 204, the admin's list of teacher A's students is empty, S1's list of teachers is empty, S1 can still log in, the response for S2's link is 410 with code `LINK_INVALID`, S3 is still in teacher B's list, and S3 logs in with "new-password-3"

#### Scenario: student-enrolment/R7-S2 Released student can join another teacher
- **GIVEN** teacher A with a student who has a password and a session, and teacher B with a session and a join link
- **WHEN** the admin deactivates teacher A, teacher B then registers a student with that email, and the student then joins through teacher B's link
- **THEN** the registration response is 409 with code `STUDENT_EXISTS`, the join response is 200, the student is in teacher B's list, and teacher B is the only entry in the student's list of teachers

#### Scenario: student-enrolment/R7-S3 Reactivation does not restore students
- **GIVEN** teacher A with a password and a student
- **WHEN** the admin deactivates and then reactivates teacher A
- **THEN** the admin's list of teacher A's students is empty, and the student's list of teachers is empty

#### Scenario: student-enrolment/R7-S4 Failed release keeps the teacher active
- **GIVEN** teacher A with a password and a student, and a failure while the students are released
- **WHEN** the admin deactivates teacher A
- **THEN** the response is 500, teacher A can log in, and the student is still in teacher A's list

#### Scenario: student-enrolment/R7-S5 Release keeps an admin's reset link
- **GIVEN** teacher A with a student who has a password and an unused reset link issued by an admin
- **WHEN** the admin deactivates teacher A and the link is then used to set the password "new-password-1"
- **THEN** the deactivation response is 204, the link response is 200, and the student logs in with "new-password-1"

#### Scenario: student-enrolment/R7-S6 Teacher without students and repeated deactivation
- **GIVEN** teacher A with no students, and teacher B with a student
- **WHEN** the admin deactivates teacher A twice
- **THEN** both responses are 204, and the student is still in teacher B's list

#### Scenario: student-enrolment/R7-S7 Release invalidates the teacher's reset link
- **GIVEN** teacher A with a student who has the password "old-password-1" and an unused reset link issued by teacher A
- **WHEN** the admin deactivates teacher A and the link is then used to set the password "new-password-1"
- **THEN** the deactivation response is 204, the link response is 410 with code `LINK_INVALID`, logging in with "new-password-1" returns 401, and the student logs in with "old-password-1"
