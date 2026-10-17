---
name: User Story
about: Create a user story
title: ''
labels: ''
assignees: ''
---

# Smart Clinic Management System — User Stories

These stories describe planned functionality, not completed implementation. Administrator and doctor dashboards use React and communicate with the Spring Boot REST API.

## ADM-01 — Access the administrator dashboard

**As a** administrator

**I need** to sign in with my credentials

**So that** I can manage the clinic through a protected dashboard

### Details and Assumptions

- The React dashboard communicates with the Spring Boot REST API.
- Administrative permissions must be enforced on the server.

### Acceptance Criteria

```gherkin
Feature: Access the administrator dashboard

  Scenario: Successful sign-in
    Given I have an active administrator account
    When I submit valid credentials
    Then the administrator dashboard opens

  Scenario: Invalid credentials
    Given I am on the sign-in page
    When I submit invalid credentials
    Then an error is displayed and access is denied

  Scenario: Role enforcement
    Given I am signed in without the administrator role
    When I request an administrative operation through the API
    Then the server denies the operation
```

## ADM-02 — Manage doctors

**As a** administrator

**I need** to add doctors and edit their profiles, specialties, and contact details

**So that** I can keep the clinic directory up to date

### Details and Assumptions

- Doctor accounts require a unique sign-in identifier.
- Required profile fields must be validated before saving.

### Acceptance Criteria

```gherkin
Feature: Manage doctors

  Scenario: Save a doctor profile
    Given I am signed in as an administrator
    When I save a valid new or updated doctor profile
    Then the saved details appear in the doctor list

  Scenario: Reject invalid profiles
    Given I am editing a doctor profile
    When I omit a required field
    Then validation errors are displayed and the profile is not saved

  Scenario: Reject duplicate accounts
    Given a doctor account already uses a sign-in identifier
    When I create another account with that identifier
    Then the duplicate account is rejected
```

## ADM-03 — Manage user access

**As a** administrator

**I need** to block and restore user access

**So that** I can control who uses the system

### Details and Assumptions

- Blocking an account preserves appointment history and medical records.
- Access checks also apply to previously issued sessions.

### Acceptance Criteria

```gherkin
Feature: Manage user access

  Scenario: Block access
    Given a user has an active account and session
    When I block the account
    Then new sign-ins and protected requests using the existing session are denied
    And the appointment history and medical records are preserved

  Scenario: Restore access
    Given a user account is blocked
    When I restore access
    Then the user can sign in again with valid credentials
```

## ADM-04 — Review clinic appointments

**As a** administrator

**I need** to filter appointments by doctor, date, and status

**So that** I can coordinate clinic operations

### Details and Assumptions

- Each appointment displays its patient, doctor, time, and status.
- Filters can be combined and reset.

### Acceptance Criteria

```gherkin
Feature: Review clinic appointments

  Scenario: Apply filters
    Given clinic appointments exist
    When I apply doctor, date, and status filters
    Then only matching appointments are displayed with their patient, doctor, time, and status

  Scenario: Reset filters
    Given appointment filters are active
    When I reset the filters
    Then the unfiltered appointment list is displayed

  Scenario: No matching appointments
    Given no appointments match my filters
    When I view the results
    Then a clear empty-state message is displayed
```

## ADM-05 — Stop new bookings for a doctor

**As a** administrator

**I need** to disable new bookings for a doctor who no longer accepts patients

**So that** patients can book only available practitioners

### Details and Assumptions

- Making a doctor inactive does not delete existing appointments or history.
- Future appointments require review for rescheduling or cancellation.

### Acceptance Criteria

```gherkin
Feature: Stop new bookings for a doctor

  Scenario: Deactivate a doctor
    Given a doctor has future appointments
    When I disable new bookings for that doctor
    Then patients cannot create new appointments with the doctor
    And existing appointments and history remain available
    And future appointments requiring review are displayed
```

## PAT-01 — Register and sign in

**As a** patient

**I need** to create an account and sign in to my dashboard

**So that** I can manage my appointments independently

### Details and Assumptions

- Registration requires valid mandatory fields and a unique sign-in identifier.
- Patients can access only their own appointment records.

### Acceptance Criteria

```gherkin
Feature: Register and sign in

  Scenario: Register and sign in
    Given I provide valid registration details with an unused sign-in identifier
    When I register and sign in with my credentials
    Then my patient dashboard opens

  Scenario: Reject invalid registration
    Given I am on the registration page
    When I omit required details or use an existing sign-in identifier
    Then registration is rejected with a validation message

  Scenario: Protect other patients
    Given I am signed in as a patient
    When I request another patient's appointments through the interface or API
    Then access is denied
```

## PAT-02 — Find a doctor and available time

**As a** patient

**I need** to search by doctor name and specialty and view available slots for a date

**So that** I can choose a convenient appointment

### Details and Assumptions

- Only active doctors are available for booking.
- Past and occupied slots cannot be selected.

### Acceptance Criteria

```gherkin
Feature: Find a doctor and available time

  Scenario: Search availability
    Given active doctors have available slots
    When I search by name and specialty and select a date
    Then matching active doctors and their available future slots are displayed
    And occupied and past slots cannot be selected

  Scenario: No available slots
    Given no slots are available for my selection
    When I view the search results
    Then I am prompted to choose another date or doctor
```

## PAT-03 — Book an appointment

**As a** patient

**I need** to reserve an available slot with a selected doctor

**So that** I can receive a consultation

### Details and Assumptions

- The doctor, date, and time are shown before confirmation.
- The server checks availability again when saving and prevents double booking.

### Acceptance Criteria

```gherkin
Feature: Book an appointment

  Scenario: Confirm a booking
    Given I am signed in and have selected a free slot
    When I review the doctor, date, and time and confirm the booking
    Then the appointment is saved and appears in my dashboard

  Scenario: Handle concurrent booking
    Given another patient reserves my selected slot before my booking is saved
    When I confirm the booking
    Then no duplicate booking is created
    And I am asked to select another slot
```

## PAT-04 — View my appointments

**As a** patient

**I need** to view upcoming and past appointments

**So that** I can track my schedule and visit history

### Details and Assumptions

- Appointment details include doctor, date, time, and status.
- Upcoming visits are separated from completed and cancelled visits.

### Acceptance Criteria

```gherkin
Feature: View my appointments

  Scenario: Review appointments
    Given I am signed in and have appointments with different statuses
    When I open my appointment list
    Then only my appointments are displayed with their doctor, date, time, and status
    And upcoming appointments are separated from completed and cancelled appointments
```

## PAT-05 — Reschedule or cancel an appointment

**As a** patient

**I need** to reschedule or cancel my future appointments

**So that** I can change my plans without contacting the clinic

### Details and Assumptions

- Only available future slots can be used for rescheduling.
- Failed rescheduling preserves the original appointment.
- Completed and cancelled appointments cannot be rescheduled or cancelled again.

### Acceptance Criteria

```gherkin
Feature: Reschedule or cancel an appointment

  Scenario: Reschedule successfully
    Given I have a future appointment and another slot is available
    When I move the appointment to that slot
    Then the new time is saved and the original slot is released

  Scenario: Preserve a booking on failure
    Given I have a future appointment
    When a rescheduling attempt fails
    Then the original appointment remains unchanged and an error is displayed

  Scenario: Confirm cancellation
    Given I have a future appointment
    When I request cancellation and confirm it
    Then the status changes to cancelled and the slot becomes available

  Scenario: Reject invalid changes
    Given my appointment is completed or cancelled
    When I attempt to reschedule or cancel it
    Then the operation is rejected
```

## PAT-06 — View my prescriptions

**As a** patient

**I need** to read prescriptions issued after my appointments

**So that** I can follow my doctor's treatment instructions

### Details and Assumptions

- Prescriptions show the doctor, issue date, medication, dosage, and instructions.
- Patients have read-only access to their own prescriptions.

### Acceptance Criteria

```gherkin
Feature: View my prescriptions

  Scenario: Read prescriptions
    Given a prescription has been issued to me
    When I open it in my dashboard
    Then the doctor, issue date, medication, dosage, and instructions are displayed

  Scenario: Enforce prescription access
    Given I am signed in as a patient
    When I attempt to edit a prescription or read another patient's prescription
    Then the server denies the request

  Scenario: No prescriptions
    Given I have no prescriptions
    When I open my prescription list
    Then a clear empty-state message is displayed
```

## DOC-01 — Access the doctor dashboard

**As a** doctor

**I need** to sign in to my personal dashboard

**So that** I can work with my schedule and patients

### Details and Assumptions

- Protected operations require an active account with the doctor role.
- Signing out ends access through the current session.

### Acceptance Criteria

```gherkin
Feature: Access the doctor dashboard

  Scenario: Sign in
    Given I have an active doctor account
    When I submit valid credentials
    Then my doctor dashboard opens

  Scenario: Enforce access checks
    Given my account is inactive or lacks the doctor role
    When I request a protected doctor operation
    Then the server denies access

  Scenario: Sign out
    Given I am signed in as a doctor
    When I sign out and attempt to reuse the session
    Then protected access is denied
```

## DOC-02 — Manage availability

**As a** doctor

**I need** to set working hours and periods of absence

**So that** patients can book only when I am available

### Details and Assumptions

- Working intervals must not overlap and must end after they start.
- Availability changes must not silently cancel existing appointments.

### Acceptance Criteria

```gherkin
Feature: Manage availability

  Scenario: Update availability
    Given I am signed in as a doctor and my changes do not conflict with appointments
    When I save valid working hours and periods of absence
    Then the booking interface reflects the updated availability

  Scenario: Reject invalid intervals
    Given I am editing my availability
    When I enter overlapping intervals or an end time that is not after the start
    Then the changes are rejected with a validation message

  Scenario: Handle existing appointments
    Given my proposed availability change conflicts with an existing appointment
    When I try to save it
    Then the conflict is reported and the appointment is not automatically cancelled
```

## DOC-03 — View my schedule

**As a** doctor

**I need** to view my appointments for a selected date

**So that** I can prepare for consultations

### Details and Assumptions

- Appointments are ordered by time and include the patient and status.
- The server enforces access to schedules independently of API parameters.

### Acceptance Criteria

```gherkin
Feature: View my schedule

  Scenario: Select a date
    Given I am signed in as a doctor
    When I select a date in my schedule
    Then my appointments for that date are displayed in time order with their patient and status

  Scenario: Protect other schedules
    Given I do not have permission to view another doctor's schedule
    When I modify API parameters to request it
    Then access is denied
```

## DOC-04 — View patient medical history

**As a** doctor

**I need** to review permitted medical records for a patient booked with me

**So that** I can consider previous visits and prescriptions during treatment

### Details and Assumptions

- Medical history is opened from an appointment.
- The server checks access to the selected patient.

### Acceptance Criteria

```gherkin
Feature: View patient medical history

  Scenario: Review history
    Given I am authorized to access a patient booked with me
    When I open the medical history from the appointment
    Then previous visit dates and associated prescriptions are displayed

  Scenario: No history
    Given an authorized patient has no medical history
    When I open the history
    Then a clear empty-state message is displayed

  Scenario: Reject unauthorized access
    Given I am not authorized to access a patient's records
    When I request that patient's medical history
    Then the server denies access
```

## DOC-05 — Complete an appointment

**As a** doctor

**I need** to save consultation results and mark an appointment as completed

**So that** I can maintain a treatment history

### Details and Assumptions

- Doctors may update only appointments they are authorized to manage.
- Cancelled appointments cannot be completed.

### Acceptance Criteria

```gherkin
Feature: Complete an appointment

  Scenario: Save consultation results
    Given I am authorized to manage an active appointment
    When I save consultation results and complete the appointment
    Then the results are available for later review and the status is completed

  Scenario: Handle saving errors
    Given I am completing an appointment
    When saving the consultation results fails
    Then an error is displayed and the interface does not report successful completion

  Scenario: Reject cancelled appointments
    Given an appointment is cancelled
    When I try to mark it completed
    Then the operation is rejected

  Scenario: Reject unauthorized updates
    Given I am not authorized to manage an appointment
    When I attempt to update it
    Then the server denies the operation
```

## DOC-06 — Issue a prescription

**As a** doctor

**I need** to issue a prescription after a consultation

**So that** the patient receives clear treatment instructions

### Details and Assumptions

- Each prescription is linked to a patient, doctor, and appointment.
- Medication, dosage, and instructions are required.

### Acceptance Criteria

```gherkin
Feature: Issue a prescription

  Scenario: Save a prescription
    Given I am authorized to manage the patient's appointment
    When I submit a prescription with medication, dosage, and instructions
    Then the prescription is saved with its patient, doctor, and appointment links
    And it is available to the authorized doctor and the patient

  Scenario: Validate required fields
    Given I am writing a prescription
    When I omit medication, dosage, or instructions
    Then validation errors are displayed and the prescription is not saved

  Scenario: Handle saving errors
    Given I submit a valid prescription
    When the save operation fails
    Then an error is displayed and the interface does not report successful issuance
```
