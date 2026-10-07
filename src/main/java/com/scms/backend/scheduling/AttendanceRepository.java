package com.scms.backend.scheduling;
import java.time.Instant; import java.util.Optional; import java.util.UUID; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
interface AttendanceRepository extends JpaRepository<Attendance,UUID>{ Optional<Attendance> findByBookingId(UUID bookingId);
 @Query(value="select id from center_visits where member_account_id=:memberId and checked_in_at<=:now and checked_in_at>=date_trunc('day',:sessionStart at time zone 'Asia/Ho_Chi_Minh') at time zone 'Asia/Ho_Chi_Minh' and checked_in_at<(date_trunc('day',:sessionStart at time zone 'Asia/Ho_Chi_Minh')+interval '1 day') at time zone 'Asia/Ho_Chi_Minh' order by checked_in_at desc limit 1",nativeQuery=true)
 Optional<UUID> findCurrentCenterVisit(@Param("memberId") UUID memberId,@Param("sessionStart") Instant sessionStart,@Param("now") Instant now); }
