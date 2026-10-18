# User Story Template

**Title:**
_As a [user role], I want [feature/goal], so that [reason]._

**Acceptance Criteria:**
1. [Criteria 1]
2. [Criteria 2]
3. [Criteria 3]

**Priority:** [High/Medium/Low]

**Story Points:** [Estimated Effort in Points]

**Notes:**
- [Additional information or edge cases]

---

# Smart Clinic Management System — User Stories

These stories describe planned functionality, not completed implementation. Priorities and story points are initial estimates for team review; points represent relative effort, not days.

## Administrator Stories

### ADM-01

**Title:** Sign in as an administrator

_As an administrator, I want to sign in with my username and password, so that I can securely manage the platform._

**Acceptance Criteria:**

1. Valid credentials for an active administrator account open the administrator dashboard.
2. Invalid credentials display an error and do not grant access.
3. The server denies administrative operations to unauthenticated users and users without the administrator role.

**Priority:** High

**Story Points:** 3

**Notes:**

- The dashboard uses React and the Spring Boot REST API; hiding interface controls does not replace server authorization.

### ADM-02

**Title:** Sign out as an administrator

_As an administrator, I want to sign out of the portal, so that I can protect access to the system._

**Acceptance Criteria:**

1. A sign-out action is available from the administrator dashboard.
2. Signing out ends the current authenticated session and returns the user to the sign-in page.
3. Reusing the ended session or navigating back does not grant access to protected operations.

**Priority:** High

**Story Points:** 2

**Notes:**

- The backend must reject credentials associated with the ended session according to the chosen session or token invalidation mechanism.

### ADM-03

**Title:** Add a doctor

_As an administrator, I want to add doctors to the portal, so that patients can find and book appointments with clinic practitioners._

**Acceptance Criteria:**

1. An authenticated administrator can submit a doctor profile with the required account details, specialty, and contact information.
2. Missing or invalid required fields and duplicate account identifiers are rejected with clear validation messages.
3. A successfully saved doctor appears in the doctor directory.

**Priority:** High

**Story Points:** 3

**Notes:**

- Doctor profiles are stored in PostgreSQL. A new profile does not imply availability until working slots are configured.

### ADM-04

**Title:** Remove a doctor profile

_As an administrator, I want to remove a doctor profile from the portal, so that patients no longer book with a doctor who has left the clinic._

**Acceptance Criteria:**

1. The administrator must confirm removal of the selected doctor profile.
2. After removal, the profile is absent from the public directory and the server rejects new bookings for that doctor.
3. Existing medical history is preserved, and future appointments are flagged for review rather than silently deleted.

**Priority:** High

**Story Points:** 3

**Notes:**

- Assumption: removal from the portal uses archival or soft deletion to preserve references to past visits. Future appointments need an explicit cancellation or rescheduling workflow.

### ADM-05

**Title:** Count monthly appointments using psql

_As an administrator, I want to execute a stored procedure in psql that counts appointments for a selected month, so that I can track platform usage._

**Acceptance Criteria:**

1. An authorized database operator can call the documented stored procedure from psql with a year and month.
2. The result contains the number of appointments whose scheduled start is within that calendar month, including zero when none exist.
3. Invalid month or year inputs are rejected; appointments outside the selected month are excluded.

**Priority:** Medium

**Story Points:** 3

**Notes:**

- Assumptions: count all appointment statuses by scheduled start time using the clinic time zone and a start-inclusive, next-month-exclusive interval. Database access is separate from portal authentication; the PostgreSQL procedure will return the count via an OUT BIGINT parameter using CALL in psql; its name and supported year range will be documented during implementation.

## Patient Stories

### PAT-01

**Title:** Browse doctors without signing in

_As a patient, I want to browse the doctor directory without signing in, so that I can explore my options before registering._

**Acceptance Criteria:**

1. An unauthenticated visitor can open the doctor directory.
2. The directory shows active doctors and their public names, specialties, and clinic contact information.
3. An empty directory displays a helpful message; private account and patient data are never included in the public response.

**Priority:** High

**Story Points:** 2

**Notes:**

- Viewing the directory is public; booking an appointment requires authentication.

### PAT-02

**Title:** Register a patient account

_As a patient, I want to register with my email address and password, so that I can book appointments._

**Acceptance Criteria:**

1. A valid unused email address and a password meeting the stated requirements create a patient account.
2. Invalid email addresses, missing fields, and duplicate email addresses produce validation messages without creating an account.
3. Passwords are stored as secure password hashes and are not included in API responses.

**Priority:** High

**Story Points:** 3

**Notes:**

- Password requirements must be displayed on the registration form. Registration assigns the patient role and cannot be used to obtain administrator or doctor privileges.

### PAT-03

**Title:** Sign in as a patient

_As a patient, I want to sign in to the portal, so that I can manage my appointments._

**Acceptance Criteria:**

1. Valid credentials for an active patient account open the patient dashboard.
2. Invalid credentials do not grant access and display an error.
3. Authenticated patients can access only their own appointments, including through direct API requests.

**Priority:** High

**Story Points:** 3

**Notes:**

- Patients sign in using the email address and password supplied during registration.

### PAT-04

**Title:** Sign out as a patient

_As a patient, I want to sign out of the portal, so that I can protect my account._

**Acceptance Criteria:**

1. A sign-out action is available from the patient dashboard.
2. Signing out ends the current session and returns the patient to the sign-in page.
3. Protected account and appointment requests using the ended session are rejected.

**Priority:** High

**Story Points:** 2

**Notes:**

- The interface clears patient-specific state on sign-out so it is not shown to the next user of the same browser.

### PAT-05

**Title:** Book a one-hour appointment

_As a patient, I want to sign in and book a one-hour appointment with a doctor, so that I can receive a medical consultation._

**Acceptance Criteria:**

1. Unauthenticated users are asked to sign in before confirming an appointment.
2. An authenticated patient can select and confirm a future available slot lasting exactly 60 minutes; the doctor, date, start time, and end time are shown before confirmation.
3. The server rechecks availability when saving, prevents overlapping bookings for the doctor, and displays the saved appointment or a conflict message.

**Priority:** High

**Story Points:** 5

**Notes:**

- The entire hour must fit within the doctor’s availability and must not overlap an absence or existing booking. Concurrent requests must not reserve the same slot.

### PAT-06

**Title:** View upcoming appointments

_As a patient, I want to view my upcoming appointments, so that I can prepare for my visits._

**Acceptance Criteria:**

1. The authenticated patient sees only their own future, non-cancelled appointments, ordered by start time.
2. Each appointment displays the doctor, specialty, date, start time, end time, and status.
3. When no upcoming appointments exist, a clear empty-state message is displayed.

**Priority:** High

**Story Points:** 2

**Notes:**

- Display the applicable time zone consistently. Completed and cancelled appointments are excluded from the upcoming list.

## Doctor Stories

### DOC-01

**Title:** Sign in as a doctor

_As a doctor, I want to sign in to the portal, so that I can manage my appointments._

**Acceptance Criteria:**

1. Valid credentials for an active doctor account open the doctor dashboard.
2. Invalid credentials or an inactive account do not grant access.
3. The server restricts doctor operations to the authenticated doctor’s permitted records.

**Priority:** High

**Story Points:** 3

**Notes:**

- Doctor accounts are provisioned by an administrator; public patient registration does not grant doctor access.

### DOC-02

**Title:** Sign out as a doctor

_As a doctor, I want to sign out of the portal, so that I can protect my data._

**Acceptance Criteria:**

1. A sign-out action is available from the doctor dashboard.
2. Signing out ends the current session and returns the doctor to the sign-in page.
3. Requests for protected schedules and patient details using the ended session are rejected.

**Priority:** High

**Story Points:** 2

**Notes:**

- Clear patient details and schedule data from interface state on sign-out.

### DOC-03

**Title:** View my appointment calendar

_As a doctor, I want to view my appointment calendar, so that I can stay organized._

**Acceptance Criteria:**

1. An authenticated doctor can select a date or date range in the calendar.
2. Appointments for the selected period show the patient, start and end times, and status, in chronological order.
3. The calendar displays an empty state when no appointments exist, and API requests cannot expose another doctor’s calendar without authorization.

**Priority:** High

**Story Points:** 3

**Notes:**

- Use the same clinic time zone as the booking interface. Cancelled appointments must be clearly identified if displayed.

### DOC-04

**Title:** Mark periods of unavailability

_As a doctor, I want to mark periods when I am unavailable, so that patients see only time slots when I can accept appointments._

**Acceptance Criteria:**

1. The doctor can save an absence with a start and end time, and the end must be later than the start.
2. Slots overlapping a saved absence are excluded from patient booking options and rejected by the booking API.
3. If an absence conflicts with an existing appointment, the system reports the conflict and does not silently cancel or overwrite the appointment.

**Priority:** High

**Story Points:** 5

**Notes:**

- Assumption: conflicting absence changes are blocked until existing appointments are resolved. The server must also handle concurrent absence and booking requests.

### DOC-05

**Title:** Update my doctor profile

_As a doctor, I want to update my specialty and contact information, so that patients have accurate information about me._

**Acceptance Criteria:**

1. An authenticated doctor can edit their own specialty and public contact information.
2. Missing required fields and invalid contact details are rejected with validation messages.
3. Successfully saved changes appear in the public directory; attempts to edit another doctor’s profile without permission are rejected.

**Priority:** Medium

**Story Points:** 3

**Notes:**

- Only intended public clinic contact information is published. Profile editing does not permit changing account roles or privileges.

### DOC-06

**Title:** View patient details for upcoming appointments

_As a doctor, I want to view patient details for my upcoming appointments, so that I can prepare for consultations._

**Acceptance Criteria:**

1. The doctor can open relevant patient details from an upcoming appointment assigned to them.
2. The displayed details identify the patient and include available information needed to prepare; missing optional information is clearly indicated.
3. The server denies access to patient details when the doctor does not have permission, including when API identifiers are changed manually.

**Priority:** High

**Story Points:** 3

**Notes:**

- Patient details are protected and never included in the public doctor directory. The exact fields and scope of medical-history access must be agreed before implementation.
