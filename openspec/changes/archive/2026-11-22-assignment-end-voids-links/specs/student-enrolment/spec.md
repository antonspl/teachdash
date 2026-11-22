## MODIFIED Requirements

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

#### Scenario: student-enrolment/R7-S8 Release voids only the teacher's links for the teacher's students
- **GIVEN** teacher A with the students S1 and S2 without a password, each with an unused set-password link issued by teacher A, a student U who is not teacher A's student with an unused set-password link issued by teacher A, an unused reset link for S1 issued by an admin, and teacher B with a student S3 without a password with an unused set-password link issued by teacher B
- **WHEN** the admin deactivates teacher A, and every link is then used to set a password
- **THEN** the deactivation response is 204, the responses for the links teacher A issued for S1 and S2 are 410 with code `LINK_INVALID`, and the responses for U's link, the admin's link for S1 and S3's link are 200
