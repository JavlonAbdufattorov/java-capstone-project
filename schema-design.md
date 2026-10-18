# Smart Clinic — Database Schema Design

This document describes the target schema, not an already deployed database. It complements [the application architecture](schema-architecture.md) and [user stories](user_stories.md). The current Java models are scaffolds; implementing this design will require entity mappings, validation, and database migrations.

## MySQL Database Design

MySQL owns structured operational data: accounts, doctor profiles, availability, and appointments. Relationships and constraints keep these records consistent. The proposed baseline is MySQL 8.4 with InnoDB and `utf8mb4`. All primary and foreign identifiers use signed `BIGINT`, matching Java `Long`. All timestamps are stored in UTC; the application converts clinic-local times at the boundary and displays the clinic time zone.

### Table: patients

- `id`: BIGINT, Primary Key, NOT NULL, AUTO_INCREMENT.
- `name`: VARCHAR(100), NOT NULL.
- `email`: VARCHAR(254), NOT NULL, UNIQUE.
- `password_hash`: VARCHAR(255), NOT NULL; stores a password hash, never plaintext.
- `phone`: VARCHAR(20), NOT NULL.
- `address`: VARCHAR(255), NOT NULL.
- `is_active`: BOOLEAN, NOT NULL, DEFAULT TRUE.
- `created_at`: DATETIME(6), NOT NULL, DEFAULT CURRENT_TIMESTAMP(6).
- `updated_at`: DATETIME(6), NOT NULL, DEFAULT CURRENT_TIMESTAMP(6), ON UPDATE CURRENT_TIMESTAMP(6).

A patient has many appointments. Registration must collect the required profile fields in addition to email and password. Deactivation preserves references to previous visits.

### Table: doctors

- `id`: BIGINT, Primary Key, NOT NULL, AUTO_INCREMENT.
- `name`: VARCHAR(100), NOT NULL.
- `specialty`: VARCHAR(50), NOT NULL.
- `email`: VARCHAR(254), NOT NULL, UNIQUE.
- `password_hash`: VARCHAR(255), NOT NULL.
- `phone`: VARCHAR(20), NOT NULL; public clinic contact information.
- `is_active`: BOOLEAN, NOT NULL, DEFAULT TRUE.
- `created_at`: DATETIME(6), NOT NULL, DEFAULT CURRENT_TIMESTAMP(6).
- `updated_at`: DATETIME(6), NOT NULL, DEFAULT CURRENT_TIMESTAMP(6), ON UPDATE CURRENT_TIMESTAMP(6).

A doctor has many appointments and availability intervals. An inactive doctor is hidden from the public directory and cannot receive new bookings. Index `specialty` for directory filtering. Availability is stored in dated rows rather than a comma-separated string or an undated list of times.

### Table: appointments

- `id`: BIGINT, Primary Key, NOT NULL, AUTO_INCREMENT.
- `doctor_id`: BIGINT, NOT NULL, Foreign Key → `doctors(id)`, ON DELETE RESTRICT, ON UPDATE RESTRICT.
- `patient_id`: BIGINT, NOT NULL, Foreign Key → `patients(id)`, ON DELETE RESTRICT, ON UPDATE RESTRICT.
- `appointment_time`: DATETIME(6), NOT NULL; scheduled start in UTC.
- `status`: TINYINT, NOT NULL, DEFAULT 0; CHECK (`status IN (0, 1, 2)`), where 0 = Scheduled, 1 = Completed, 2 = Cancelled.
- `created_at`: DATETIME(6), NOT NULL, DEFAULT CURRENT_TIMESTAMP(6).
- `updated_at`: DATETIME(6), NOT NULL, DEFAULT CURRENT_TIMESTAMP(6), ON UPDATE CURRENT_TIMESTAMP(6).

Each appointment belongs to exactly one patient and one doctor. Duration is fixed at 60 minutes, so the end is derived as `appointment_time + INTERVAL 1 HOUR`, consistent with the Java model scaffold. No stored end-time column can drift from that rule.

Indexes: `(doctor_id, appointment_time)`, `(patient_id, appointment_time)`, and `(appointment_time)` for calendars and monthly reporting. These are non-unique indexes: cancelled appointments remain in history while their time becomes bookable again. A unique start-time index alone would not prevent partial overlap.

### Table: admin

- `id`: BIGINT, Primary Key, NOT NULL, AUTO_INCREMENT.
- `username`: VARCHAR(50), NOT NULL, UNIQUE.
- `password_hash`: VARCHAR(255), NOT NULL.
- `is_active`: BOOLEAN, NOT NULL, DEFAULT TRUE.
- `created_at`: DATETIME(6), NOT NULL, DEFAULT CURRENT_TIMESTAMP(6).

Administrators sign in using a username. No foreign key is required for this account table in the initial design. Patient and doctor email uniqueness is enforced within each table; role-specific sign-in determines which account table is queried.

### Table: doctor_availability

- `id`: BIGINT, Primary Key, NOT NULL, AUTO_INCREMENT.
- `doctor_id`: BIGINT, NOT NULL, Foreign Key → `doctors(id)`, ON DELETE RESTRICT, ON UPDATE RESTRICT.
- `starts_at`: DATETIME(6), NOT NULL.
- `ends_at`: DATETIME(6), NOT NULL.
- Constraint: CHECK (`ends_at > starts_at`).
- Constraint: UNIQUE (`doctor_id`, `starts_at`, `ends_at`) to reject identical intervals.
- Index: `(doctor_id, starts_at)` for availability queries.

Rows describe dated working intervals, potentially covering several one-hour appointments. The service rejects overlapping intervals for the same doctor; uniqueness alone does not perform that check. A booking must fit entirely within one interval. Recurring weekly schedules are outside the initial scope; the service can generate dated intervals later.

### Table: doctor_unavailability

- `id`: BIGINT, Primary Key, NOT NULL, AUTO_INCREMENT.
- `doctor_id`: BIGINT, NOT NULL, Foreign Key → `doctors(id)`, ON DELETE RESTRICT, ON UPDATE RESTRICT.
- `starts_at`: DATETIME(6), NOT NULL.
- `ends_at`: DATETIME(6), NOT NULL.
- `reason`: VARCHAR(255), NULL; internal optional note, not shown publicly.
- Constraint: CHECK (`ends_at > starts_at`).
- Index: `(doctor_id, starts_at)`.

Absences override working intervals. The service rejects duplicate or overlapping absence intervals and blocks changes that conflict with scheduled appointments until those appointments are explicitly resolved. Removing working hours follows the same conflict rule.

### Validation and booking integrity

- Spring Boot validates nonblank names, field lengths, email syntax, and phone formats. Phone numbers are strings to preserve `+` and leading zeros; the target design accepts normalized international numbers, replacing the scaffold's ten-digit-only assumption. The database enforces required fields and uniqueness, not whether an email or phone actually belongs to a user.
- Normalize email and username input consistently before persistence and lookup. Use a documented case-insensitive uniqueness policy. Password hashes must never appear in response DTOs.
- Validate active accounts, future start times, and complete one-hour availability when creating or rescheduling an appointment. Do not apply a future-only rule when reading or completing historical appointments.
- Intervals are half-open: `[start, end)`. Two intervals overlap when `existing_start < requested_end AND existing_end > requested_start`. Back-to-back appointments are allowed. Cancelled appointments do not block time; scheduled and completed appointments do.
- To prevent races, every booking, rescheduling, cancellation, availability change, and doctor deactivation runs in a transaction and first locks the affected doctor row with `SELECT ... FOR UPDATE`. If multiple doctors are involved, lock their rows in increasing ID order. Then perform current locking reads for conflicting appointments and schedule intervals, validate, write, and commit. A failed rescheduling rolls back and preserves the original booking. All application write paths must follow this protocol; a preliminary browser check is insufficient.
- The first version prevents overlaps for a doctor. Preventing a patient from booking different doctors simultaneously is a separate rule that would also require coordinated patient locking.

### Deletion, history, and reporting

Patient and doctor removal means deactivation, not cascading deletion of appointments. Foreign keys use `RESTRICT` to reject deletion of referenced parents, preserving visit history. This follows the documented [MySQL foreign-key behavior](https://dev.mysql.com/doc/refman/8.4/en/create-table-foreign-keys.html). Account deactivation must also block protected operations by existing sessions.

Historical records are retained through normal portal operations, but indefinite retention is not assumed. The clinic must define a retention and archival policy before implementing a coordinated purge or anonymization process across both databases. Future appointments for a deactivated account require explicit review.

The planned monthly statistics procedure accepts a year and month, validates them, and counts appointments by scheduled start, including all statuses. It uses the clinic-local month boundaries converted to UTC and the predicate `appointment_time >= month_start_utc AND appointment_time < next_month_start_utc`. It returns zero for an empty month. The procedure and database privileges are future implementation work; portal administrator access does not automatically grant MySQL CLI access.

`clinic_locations` and `payments` are omitted because multiple locations and billing are not required by the current stories.

## MongoDB Collection Design

MongoDB stores prescriptions with a variable number of medications, optional notes, and extensible metadata. MySQL remains the source of truth for patient and doctor identities and appointments. The initial required collection is `prescriptions`; messaging is an optional future extension.

### Collection: prescriptions

Example in MongoDB Extended JSON. Identifiers and timestamps use explicit BSON representations; names and medication content below are fictional placeholders, not clinical instructions.

```json
{
  "_id": { "$oid": "680000000000000000000001" },
  "schemaVersion": 1,
  "appointmentId": { "$numberLong": "51" },
  "patientId": { "$numberLong": "12" },
  "doctorId": { "$numberLong": "7" },
  "patientNameAtIssue": "Example Patient",
  "doctorNameAtIssue": "Example Doctor",
  "medications": [
    {
      "name": "Example medication",
      "dosage": "Clinician-entered dosage",
      "frequency": "Clinician-entered frequency",
      "duration": "Clinician-entered duration"
    }
  ],
  "doctorNotes": "Example consultation instructions",
  "tags": ["follow-up"],
  "pharmacy": {
    "name": "Example Pharmacy",
    "location": "Example address"
  },
  "issuedAt": { "$date": "2026-10-18T10:00:00Z" },
  "metadata": {
    "createdAt": { "$date": "2026-10-18T10:00:00Z" },
    "updatedAt": { "$date": "2026-10-18T10:00:00Z" },
    "status": "active"
  }
}
```

Field definitions and constraints:

- `_id`: BSON ObjectId, required, unique primary identifier generated for each document.
- `schemaVersion`: BSON int, required; initially 1.
- `appointmentId`, `patientId`, `doctorId`: BSON long, required; logical references to MySQL records.
- `patientNameAtIssue`, `doctorNameAtIssue`: required nonblank strings, maximum 100 characters; historical display snapshots, not authoritative identity keys.
- `medications`: required nonempty array of embedded objects; each contains required nonblank string fields `name`, `dosage`, `frequency`, and `duration`. Medication names are limited to 100 characters; the other strings to 255 characters each.
- `doctorNotes`: optional string, maximum 2,000 characters.
- `tags`: optional array of strings for categorization.
- `pharmacy`: optional embedded object; if supplied, `name` and `location` are required nonblank strings.
- `issuedAt`: required BSON date in UTC.
- `metadata.createdAt`, `metadata.updatedAt`: required BSON dates in UTC, managed by the service.
- `metadata.status`: required string, one of `active` or `voided`. Voiding preserves the document rather than deleting it.

Configure collection validation for required fields, BSON types, array shape, and allowed status values. Apply matching service validation for lengths and nonblank values. Flexible storage still needs validation.

Indexes: unique `(appointmentId)` for the initial rule of one prescription document per appointment; `(patientId, issuedAt)` for patient history; `(doctorId, issuedAt)` for doctor lookup. Multiple medications are embedded in that one prescription, rather than separate documents. Appointments may have zero or one prescription.

### References, consistency, and schema evolution

Every prescription must belong to an existing, non-cancelled appointment. Independent prescriptions are outside the initial scope. Before issuing one, the service verifies the appointment in MySQL, verifies the authenticated doctor is assigned to it, and derives patient and doctor IDs from that appointment rather than trusting client-provided IDs. The embedded names capture their values at issuance; full patient profiles, passwords, phone numbers, and addresses are not copied into prescriptions.

MongoDB cannot enforce a foreign key to MySQL. For the initial synchronous workflow, hold the appointment row lock in a MySQL transaction while checking eligibility and writing the MongoDB document; cancellation must obtain that same appointment lock. The unique appointment index makes retries detectable: on a duplicate, read and compare the existing prescription instead of inserting another or silently overwriting it. A later cancellation must reject an appointment with an active prescription until the prescription is explicitly voided. Database outages must return a failure or an explicit uncertain outcome, not a false success.

This protocol does not create an atomic transaction across both databases. A MongoDB write may succeed even if the response is lost or the MySQL transaction later fails. Retry reconciliation must inspect the existing document and its MySQL appointment. A reconciliation job should report missing or mismatched references; a transactional outbox can support a future asynchronous workflow. Do not automatically delete medical documents during reconciliation.

The document structure intentionally extends the Java scaffold's single `medication` and `dosage` fields to a `medications` array. Implementing it requires updating the model and DTOs together. `schemaVersion` allows readers to distinguish versions; add optional fields compatibly, deploy readers that understand a new version before writing it, and migrate older documents explicitly. Do not silently reinterpret existing fields.

### Optional future collection: chat_messages

Messaging is not part of the current implementation scope. A possible message document would reference an appointment and identify the sender's role, because patient and doctor IDs come from separate tables:

```json
{
  "_id": { "$oid": "680000000000000000000002" },
  "schemaVersion": 1,
  "appointmentId": { "$numberLong": "51" },
  "sender": { "role": "patient", "id": { "$numberLong": "12" } },
  "body": "Could you confirm the consultation time?",
  "sentAt": { "$date": "2026-10-18T09:00:00Z" }
}
```

If implemented, require the appointment reference, an allowed sender role, sender ID, nonblank bounded message body, and BSON date. Index `(appointmentId, sentAt, _id)` for ordered pagination, and authorize both reading and sending through the appointment's participants. Store messages separately so a conversation does not create an indefinitely growing embedded array. Attachments would use protected object-storage references rather than embedding file bytes in each message.
