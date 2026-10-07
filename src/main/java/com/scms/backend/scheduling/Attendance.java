package com.scms.backend.scheduling;
import java.time.Instant; import java.util.UUID; import jakarta.persistence.*;
@Entity @Table(name="attendance") class Attendance {
 @Id UUID id; @Column(name="booking_id") UUID bookingId;
 @Enumerated(EnumType.STRING) AttendanceStatus status; @Column(name="recorded_at") Instant recordedAt; @Column(name="updated_at") Instant updatedAt;
 @Column(name="center_visit_id") UUID centerVisitId; @Column(name="recorded_by_account_id") UUID recordedBy; @Column(name="recording_source") String recordingSource; @Version Long version;
 protected Attendance(){} Attendance(UUID id,UUID bookingId,AttendanceStatus status,Instant at,UUID visit,UUID by,String source){this.id=id;this.bookingId=bookingId;this.status=status;this.recordedAt=at;this.updatedAt=at;this.centerVisitId=visit;this.recordedBy=by;this.recordingSource=source;}
 UUID getId(){return id;} UUID getBookingId(){return bookingId;} AttendanceStatus getStatus(){return status;} Instant getRecordedAt(){return recordedAt;} UUID getCenterVisitId(){return centerVisitId;}
 void mark(AttendanceStatus s,UUID v,UUID by,Instant at){status=s;centerVisitId=v;recordedBy=by;recordingSource="COACH";updatedAt=at;}
}
