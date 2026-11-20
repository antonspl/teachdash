# student-enrolment Specification

## Purpose

Links students to teachers. A student has at most one teacher. A teacher registers new
students or students with no teacher and no password, sees only their own, can issue them
password reset links and can remove them; a teacher also has a join link, through which a
new visitor registers or a student with no teacher joins; a student sees their teacher;
deactivating a teacher releases their students; and admins see all assignments and can
find a student by email. Every new student has a native language from the language list,
which their teacher can change.

## Requirements

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

### Requirement: R2 Teacher sees only own students
A teacher's student list SHALL contain exactly the students assigned to that teacher,
in name order, each with id, name, email, time zone, active flag and native language (R15). Name order ignores case and follows each alphabet. Any teacher request about an
id that is not a student assigned to them, including a malformed id, MUST return 404. Teacher endpoints SHALL be
allowed only for teachers.

#### Scenario: student-enrolment/R2-S1 List shows only own students
- **GIVEN** teacher A with students S1 and S2, and teacher B with student S3
- **WHEN** teacher A lists students
- **THEN** the list contains S1 and S2 only, ordered by name

#### Scenario: student-enrolment/R2-S2 Another teacher's student is not found
- **GIVEN** a student assigned only to teacher B
- **WHEN** teacher A requests that student's details, and then the details of an id with no account
- **THEN** both responses are 404 with code `NOT_FOUND`

#### Scenario: student-enrolment/R2-S3 Student cannot call teacher endpoints
- **GIVEN** a logged-in student
- **WHEN** the student calls the teacher endpoint that lists students
- **THEN** the response is 403

#### Scenario: student-enrolment/R2-S4 Teacher reads own student's details
- **GIVEN** a student assigned to teacher A
- **WHEN** teacher A requests that student's details
- **THEN** the response is 200 with the student's id, name, email and time zone

#### Scenario: student-enrolment/R2-S5 Admin cannot call teacher endpoints
- **GIVEN** a logged-in admin
- **WHEN** the admin calls the teacher endpoint that lists students
- **THEN** the response is 403

#### Scenario: student-enrolment/R2-S6 Malformed id is not found
- **GIVEN** a logged-in teacher
- **WHEN** the teacher requests the details of the id "not-a-uuid"
- **THEN** the response is 404 with code `NOT_FOUND`

#### Scenario: student-enrolment/R2-S7 Students and admins cannot register or read details
- **GIVEN** a logged-in student and a logged-in admin
- **WHEN** each registers a student and requests a student's details
- **THEN** all responses are 403, and no account is created

#### Scenario: student-enrolment/R2-S8 Teacher endpoints require a session
- **GIVEN** no session
- **WHEN** a client calls the teacher endpoints that list students and read details
- **THEN** both responses are 401 with code `UNAUTHENTICATED`

#### Scenario: student-enrolment/R2-S9 Name order ignores case and follows the alphabet
- **GIVEN** a teacher with students "Boris", "anna", "Ёлка", "Анна" and "яна"
- **WHEN** the teacher lists students
- **THEN** the order is "anna", "Boris", "Анна", "Ёлка", "яна"

### Requirement: R3 Admin sees a teacher's students and a student's teacher
An admin SHALL be able to list the students assigned to any teacher, in name order as in R2,
and the teachers of any student, a list with at most one entry. Each entry has id, name,
email, time zone and active flag, and a student's or a teacher's entry also the native language (R15). An id that is not a teacher or a student, respectively,
including a malformed id, MUST return 404.

#### Scenario: student-enrolment/R3-S1 Admin lists a teacher's students
- **GIVEN** teacher A with students S1 and S2, and teacher B with student S3
- **WHEN** the admin lists teacher A's students
- **THEN** the response is 200 and the list contains S1 and S2 only, ordered by name

#### Scenario: student-enrolment/R3-S2 Id that is not a teacher is not found
- **GIVEN** a logged-in admin and a student account
- **WHEN** the admin lists the students of the student's id, and then of an id with no account
- **THEN** both responses are 404 with code `NOT_FOUND`

#### Scenario: student-enrolment/R3-S3 Admin lists a student's teacher
- **GIVEN** a student assigned to teacher A, and teacher C with another student
- **WHEN** the admin lists the student's teachers
- **THEN** the response is 200 and the list contains A only, with id, name, email, time zone, active flag and `nativeLanguage`

#### Scenario: student-enrolment/R3-S4 Id that is not a student is not found
- **GIVEN** a logged-in admin and a teacher account
- **WHEN** the admin lists the teachers of the teacher's id, and then of an id with no account
- **THEN** both responses are 404 with code `NOT_FOUND`

#### Scenario: student-enrolment/R3-S5 Malformed id is not found
- **GIVEN** a logged-in admin
- **WHEN** the admin lists the students of the id "not-a-uuid", and then the teachers of the id "not-a-uuid"
- **THEN** both responses are 404 with code `NOT_FOUND`

#### Scenario: student-enrolment/R3-S6 Teachers and students cannot call admin endpoints
- **GIVEN** teacher A, teacher B with a student, and a logged-in student
- **WHEN** teacher A and the student list teacher B's students and the student's teachers through the admin endpoints
- **THEN** all responses are 403

#### Scenario: student-enrolment/R3-S8 Admin list of students uses the same name order
- **GIVEN** a teacher with students "Boris", "anna", "Ёлка", "Анна" and "яна"
- **WHEN** the admin lists the teacher's students
- **THEN** the students are in the order "anna", "Boris", "Анна", "Ёлка", "яна"

#### Scenario: student-enrolment/R3-S9 Student with no teacher has an empty list
- **GIVEN** a logged-in admin and a student with no teacher
- **WHEN** the admin lists the student's teachers
- **THEN** the response is 200 with an empty list

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

### Requirement: R5 Student sees their teacher
A student SHALL be able to list their teachers: a list with at most one entry, which holds
their teacher with id and name only. The student endpoint SHALL be allowed only for
students.

#### Scenario: student-enrolment/R5-S1 Student sees only their own teacher
- **GIVEN** a student assigned to teacher A, and teacher C with another student
- **WHEN** the student lists their teachers
- **THEN** the response is 200, the list contains A only, and the entry has only the fields id and name

#### Scenario: student-enrolment/R5-S2 Student without teachers gets an empty list
- **GIVEN** a logged-in student with no teachers
- **WHEN** the student lists their teachers
- **THEN** the response is 200 with an empty list

#### Scenario: student-enrolment/R5-S6 Teachers and admins cannot call the student endpoint
- **GIVEN** a logged-in teacher and a logged-in admin
- **WHEN** each calls the student endpoint that lists teachers
- **THEN** both responses are 403

#### Scenario: student-enrolment/R5-S7 Student endpoint requires a session
- **GIVEN** no session
- **WHEN** a client calls the student endpoint that lists teachers
- **THEN** the response is 401 with code `UNAUTHENTICATED`

### Requirement: R6 Admin finds a student by email
An admin SHALL be able to find a student account by email, using the same email rule as
R1, and get the student's id, name, email, time zone, active flag and native language (R15). An email with no
student account MUST return 404.

#### Scenario: student-enrolment/R6-S1 Admin finds a student
- **GIVEN** a logged-in admin and a student account with email "student@example.com"
- **WHEN** the admin looks up "student@example.com"
- **THEN** the response is 200 with the student's id, name, email, time zone and active flag

#### Scenario: student-enrolment/R6-S2 Removed student without a password can be recovered
- **GIVEN** a logged-in admin and a student without a password, assigned only to teacher A, with an unused set-password link
- **WHEN** teacher A removes the student, the admin looks up the student's email and issues a reset link for the returned id, and that link is used to set the password "new-password-1"
- **THEN** the removal response is 204, the lookup response is 200, and the student logs in with "new-password-1"

#### Scenario: student-enrolment/R6-S3 Email case and surrounding whitespace are ignored
- **GIVEN** a logged-in admin and a student account with email "student@example.com"
- **WHEN** the admin looks up " STUDENT@example.com "
- **THEN** the response is 200 with that student's id

#### Scenario: student-enrolment/R6-S4 Email without a student account is not found
- **GIVEN** a logged-in admin and a teacher account
- **WHEN** the admin looks up an email with no account, the teacher's email, and the admin's own email
- **THEN** each response is 404 with code `NOT_FOUND`

#### Scenario: student-enrolment/R6-S5 Invalid email is rejected
- **GIVEN** a logged-in admin
- **WHEN** the admin looks up a blank email, then a malformed email, then sends no email
- **THEN** each response is 400 with code `VALIDATION_FAILED`

#### Scenario: student-enrolment/R6-S6 Teachers and students cannot look up
- **GIVEN** a student account, a logged-in teacher and a logged-in student
- **WHEN** the teacher and the student each look up that student's email
- **THEN** both responses are 403

#### Scenario: student-enrolment/R6-S7 Lookup requires a session
- **GIVEN** a client with a valid CSRF token and no session
- **WHEN** the client looks up an email
- **THEN** the response is 401 with code `UNAUTHENTICATED`

#### Scenario: student-enrolment/R6-S8 Request without a CSRF token is rejected
- **GIVEN** a logged-in admin
- **WHEN** the admin looks up an email without a CSRF token
- **THEN** the response is 403 with code `CSRF_INVALID`

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

### Requirement: R8 Teacher manages their join link
A teacher SHALL have at most one join link and SHALL be able to view it (with its expiry),
generate a new one, which replaces the old, and disable it. Viewing SHALL NOT create a link.
A link SHALL work for 7 days unless replaced or disabled first; an expired link is shown as
no link. Deactivating a teacher SHALL disable their link for good. Only teachers may call
these endpoints, each for their own link.

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
- **GIVEN** teacher A with a password, a student and a join link L1, and a failure while the students are released
- **WHEN** the admin deactivates teacher A, and the preview of L1 is requested
- **THEN** the deactivation response is 500, the preview of L1 is 200, and the student is still in teacher A's list

#### Scenario: student-enrolment/R8-S10 Link expires after 7 days
- **GIVEN** a logged-in teacher A, a logged-in student with no teacher, and a client with no session
- **WHEN** teacher A generates a link L1, 7 days less one minute pass, the client requests the preview of L1, one more minute passes, the client requests the preview of L1 and joins through it with a new email, the student joins through L1, teacher A views the link, and teacher A then generates a link L2
- **THEN** the generate response carries an expiry 7 days after it was generated, the first preview is 200, the second preview and both joins are 410 with code `LINK_INVALID`, no account is created, the student still has no teacher, the view is 200 with no link, and the preview of L2 is 200 with an expiry 7 days after L2 was generated in the generate response

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
- **THEN** every response is 410 with code `LINK_INVALID`, all preview responses have the same body and all join responses have the same body, and no account is created

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
- **THEN** the response is 201, and teacher A's request for the student's details returns the name "Анна", the email "Anna@Example.com" and the time zone "Europe/Moscow"

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
link they use. The student keeps their name, email, time zone, native language and password; any other
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

#### Scenario: student-enrolment/R11-S4 Encoded path is limited too
- **GIVEN** a join limit of 2 requests per 10 minutes, teacher A with a join link, and a client from one IP that has used up the join limit
- **WHEN** the client joins through the link with a new email, sending the request to the join path with one letter percent-encoded (`/api/join-links/joi%6E`)
- **THEN** the response is 429 with code `RATE_LIMITED`, and no account is created

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

### Requirement: R13 A new student has a native language
Registering a new student (R1) and joining through a join link without a session (R9)
SHALL require `nativeLanguage`, a code from the language list (languages R1), in
addition to the fields those requirements name. Surrounding whitespace is ignored. It
is checked with the other fields: missing or blank is `REQUIRED`, an array or object
`INVALID_TYPE`, any other value not in the list `INVALID_FORMAT`. An assigned existing account (R1, R10)
keeps its stored language.

#### Scenario: student-enrolment/R13-S1 Teacher registers a student with a native language
- **GIVEN** a logged-in teacher A
- **WHEN** teacher A registers a student with the name "Marko", a new email, the time zone "Europe/Belgrade" and the native language "sr", and another with the name "Оксана", a new email, the time zone "Europe/Kyiv" and the native language " uk "
- **THEN** both responses are 201, and teacher A's student details show the native language "sr" for Marko and "uk" for Оксана

#### Scenario: student-enrolment/R13-S2 Registration needs a listed native language
- **GIVEN** a logged-in teacher A
- **WHEN** teacher A registers students with the name "Anna", a new email, the time zone "Europe/Berlin" and, in turn: no native language; `null`; ""; "  "; "xx"; "SR"; "srp"
- **THEN** the responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "nativeLanguage", code: "REQUIRED"}]` for the first four and `[{field: "nativeLanguage", code: "INVALID_FORMAT"}]` for the others, no body contains "xx" or "srp", and no account is created

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
- **GIVEN** teacher A with an active join link, teacher B with an expired join link, and a client with a CSRF cookie and no session
- **WHEN** the client joins through the active link with the name "Anna", a new email, the time zone "Europe/Berlin", the password "password-1" and no native language; then with the native language "xx"; then through teacher B's expired link with the same details and no native language
- **THEN** the first and third responses are 400 with `errors` exactly `[{field: "nativeLanguage", code: "REQUIRED"}]`, the second is 400 with `errors` exactly `[{field: "nativeLanguage", code: "INVALID_FORMAT"}]`, and no account is created

#### Scenario: student-enrolment/R13-S7 A logged-in student keeps their language when joining
- **GIVEN** teacher A with a join link, and a logged-in student with no teacher and native language "it"
- **WHEN** the student joins through the link, sending the native language "es"
- **THEN** the response is 200, and teacher A's student details show the native language "it"

#### Scenario: student-enrolment/R13-S8 A wrong JSON type is reported
- **GIVEN** a logged-in teacher A, teacher A's join link, and a client with a CSRF cookie and no session
- **WHEN** teacher A registers a student with the name "Anna", a new email, the time zone "Europe/Berlin" and the native language `["sr"]`, and the client joins through the link with the same details, the password "password-1" and the native language `{"code": "sr"}`
- **THEN** both responses are 400 with code `VALIDATION_FAILED` and `errors` exactly `[{field: "nativeLanguage", code: "INVALID_TYPE"}]`, and no account is created

### Requirement: R14 A teacher changes their student's native language
`PATCH /api/teacher/students/{id}` SHALL set the native language of the teacher's own
student from `{nativeLanguage}` with the R13 rules and return 200 with the student's
details. Other body fields are ignored. Checks run in order: CSRF and role (403),
deactivated teacher (401), malformed id (404), input (400), then a student who is not
the caller's or an unknown id (404). Rejections change nothing.

#### Scenario: student-enrolment/R14-S1 Teacher changes the native language
- **GIVEN** teacher A with a student "Анна" whose native language is "ru"
- **WHEN** teacher A sets the student's native language to "uk", sending the name "Anna" in the same request
- **THEN** the response is 200 with the student's details, the name "Анна" and `nativeLanguage` "uk", and teacher A's student details show "uk" and the name "Анна"

#### Scenario: student-enrolment/R14-S2 Invalid values change nothing
- **GIVEN** teacher A with a student whose native language is "ru"
- **WHEN** teacher A sets the student's native language to "xx", to `null`, to `["uk"]`, and sends the body `{}`
- **THEN** the responses are 400 with `errors` exactly `[{field: "nativeLanguage", code: "INVALID_FORMAT"}]`, `[{field: "nativeLanguage", code: "REQUIRED"}]`, `[{field: "nativeLanguage", code: "INVALID_TYPE"}]` and `[{field: "nativeLanguage", code: "REQUIRED"}]`, and the native language is still "ru"

#### Scenario: student-enrolment/R14-S3 Only the teacher's own students
- **GIVEN** teacher A, teacher B with a student S whose native language is "ru", a student with no teacher, and teacher A logged in
- **WHEN** teacher A sets the native language of S, of the student with no teacher, of an unknown id and of the id "not-a-uuid" to "de", the native language of the id "not-a-uuid" to "xx", and the native language of S to "xx"
- **THEN** the first five responses are 404 with code `NOT_FOUND` and no `errors` property, the last is 400 with code `VALIDATION_FAILED`, and S's native language is still "ru"

#### Scenario: student-enrolment/R14-S4 Only teachers change it
- **GIVEN** teacher A with a student S, a logged-in admin, S logged in, a client with a CSRF cookie and no session, and a logged-in teacher B who is then deactivated
- **WHEN** the admin, S, the client and teacher B each set S's native language to "de", and teacher A does so without a CSRF token
- **THEN** the admin's and S's responses are 403 with code `FORBIDDEN`, the client's and teacher B's are 401 with code `UNAUTHENTICATED`, teacher A's is 403 with code `CSRF_INVALID`, and S's native language is unchanged

### Requirement: R15 Student data shows the native language
Every response that returns a student's details to their teacher (R2), to an admin
(R3, R6) or to the student (their current-user data, as from `GET /api/me`, login and setting a password)
SHALL include `nativeLanguage`. So SHALL a teacher's current-user data (the same
endpoints), the admin's teacher list (accounts R12) and every entry for a teacher in the
admin's lists (R3), which extends R3's entry fields. Responses about an admin, and the
response to creating a teacher (accounts R5), MUST NOT include it. The student MUST NOT be
able to change it through the profile (accounts R9: a valid code is ignored); a teacher
changes their own through it.

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
