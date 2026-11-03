package com.scms.backend.qa;

import static org.assertj.core.api.Assertions.assertThat;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.sql.DriverManager;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import com.scms.backend.account.AccountRole;
import tools.jackson.databind.ObjectMapper;

@Testcontainers
@org.junit.jupiter.api.Timeout(180)
class CourseCrashRecoveryIntegrationTests {
 static {TimeZone.setDefault(TimeZone.getTimeZone("UTC"));}
 @Container static final PostgreSQLContainer postgres=new PostgreSQLContainer("postgres:17");
 Path temporary=Path.of("target","course-qa",UUID.randomUUID().toString());
 final ObjectMapper json=new ObjectMapper();
 final HttpClient http=HttpClient.newHttpClient();
 JdbcTemplate db(){return new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword()));}
 @Test void processCrashBeforeCommitRollsBackAndSameKeyCanComplete() throws Exception { crash(false); }
 @Test void processCrashAfterCommitBeforeResponseReplaysWithoutDuplicates() throws Exception { crash(true); }
 void crash(boolean committed) throws Exception {
  try(var app=new CourseQaProcess(temporary,postgres)) {
   app.start();
   var db=db();var fixture=new CourseQaFixtures(db);
   var payment=fixture.payment(fixture.account("member",AccountRole.MEMBER),Instant.now().plusSeconds(600),true,3);
   String token=login(app,"manager");
   String body=json.writeValueAsString(Map.of("status","PAID","receivedAmount",500000,"transferContent",payment.reference(),"providerTransactionId","qa-"+payment.paymentId(),"evidence","Synthetic receipt","reason","QA reconcile"));
   try(var blocker=DriverManager.getConnection(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword())) {
    if(!committed) {
     blocker.createStatement().execute("select pg_advisory_lock(879230)");
     db.execute("create or replace function qa_pause_audit() returns trigger language plpgsql as $$ begin if NEW.action='COURSE_PAYMENT_RECONCILED' then perform pg_advisory_xact_lock(879230); end if; return NEW; end $$");
     db.execute("create trigger qa_pause_audit before insert on audit_events for each row execute function qa_pause_audit()");
    }
    var request=request(app,"/course-payments/"+payment.paymentId()+"/reconcile",token).header("Idempotency-Key","crash-retry");
    if(committed) request.header("X-QA-Barrier","after-commit");
    var pending=http.sendAsync(request.POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    if(committed) {
     CourseQaProcess.await(()->Files.exists(temporary.resolve("after-commit")),Duration.ofSeconds(20));
     assertPaid(db,payment);
    } else {
     CourseQaProcess.await(()->db.queryForObject("select count(*) from pg_locks where locktype='advisory' and objid=879230 and not granted",Long.class)>0,Duration.ofSeconds(20));
     assertPending(db,payment);
    }
    assertThat(pending.isDone()).isFalse();
    app.kill();
    pending.handle((value,error)->null).get(10,java.util.concurrent.TimeUnit.SECONDS);
    if(!committed) {blocker.createStatement().execute("select pg_advisory_unlock(879230)");db.execute("drop trigger qa_pause_audit on audit_events");db.execute("drop function qa_pause_audit()");}
   }
   app.start(); token=login(app,"manager");
   var result=http.send(request(app,"/course-payments/"+payment.paymentId()+"/result",token).GET().build(),HttpResponse.BodyHandlers.ofString());
   assertThat(result.statusCode()).isEqualTo(200);
   assertThat(json.readTree(result.body()).get("status").asText()).isEqualTo(committed?"PAID":"PENDING");
   if(!committed) assertPending(db,payment);
   String response=null;
   for(int retry=0;retry<2;retry++) {
    var replay=http.send(request(app,"/course-payments/"+payment.paymentId()+"/reconcile",token).header("Idempotency-Key","crash-retry").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    assertThat(replay.statusCode()).withFailMessage(replay.body()).isEqualTo(200);
    if(response!=null) assertThat(replay.body()).isEqualTo(response);response=replay.body();
    assertPaid(db,payment);
   }
   var conflict=http.send(request(app,"/course-payments/"+payment.paymentId()+"/reconcile",token).header("Idempotency-Key","crash-retry").POST(HttpRequest.BodyPublishers.ofString(body.replace("QA reconcile","changed reason"))).build(),HttpResponse.BodyHandlers.ofString());
   assertThat(conflict.statusCode()).isEqualTo(409);assertPaid(db,payment);
  }
 }
 @Test void realScheduledInvocationCompletesRunOnceWithSystemClock() throws Exception {
  try(var app=new CourseQaProcess(temporary,postgres)) {
   app.start();var db=db();var fixture=new CourseQaFixtures(db);
   var p=fixture.payment(fixture.account("member2",AccountRole.MEMBER),Instant.now().plusSeconds(600),false,1);
   // Arrange a future short session before testing; never seed the expected COMPLETED result.
   Instant start=Instant.now().plusSeconds(2),end=start.plusSeconds(3);
   db.update("update course_runs set planned_start_at=?,planned_end_at=?,registration_closes_at=?,registration_opens_at=?,status='CONFIRMED' where id=?",java.sql.Timestamp.from(start),java.sql.Timestamp.from(end),java.sql.Timestamp.from(start.minusSeconds(1)),java.sql.Timestamp.from(start.minusSeconds(3600)),p.runId());
   db.update("update course_sessions set start_at=?,end_at=?,original_start_at=?,original_end_at=? where course_run_id=?",java.sql.Timestamp.from(start),java.sql.Timestamp.from(end),java.sql.Timestamp.from(start),java.sql.Timestamp.from(end),p.runId());
   assertThat(db.queryForObject("select status from course_runs where id=?",String.class,p.runId())).isEqualTo("CONFIRMED");
   CourseQaProcess.await(()->"COMPLETED".equals(db.queryForObject("select status from course_runs where id=?",String.class,p.runId())),Duration.ofSeconds(20));
   long version=db.queryForObject("select version from course_runs where id=?",Long.class,p.runId());
   // Observe additional scheduled cycles, not a second direct service invocation.
   CourseQaProcess.await(()->Instant.now().isAfter(end.plusSeconds(2)),Duration.ofSeconds(10));
   assertThat(db.queryForObject("select version from course_runs where id=?",Long.class,p.runId())).isEqualTo(version);
   assertThat(db.queryForObject("select count(*) from audit_events where target_id=? and action='COURSE_RUN_COMPLETED'",Long.class,p.runId())).isOne();
  }
 }
 String login(CourseQaProcess app,String name) throws Exception {
  var r=http.send(request(app,"/auth/login",null).POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("identifier",name+"@course-qa.invalid","password",CourseQaFixtures.PASSWORD)))).build(),HttpResponse.BodyHandlers.ofString());
  assertThat(r.statusCode()).withFailMessage(r.body()).isEqualTo(200);return json.readTree(r.body()).get("accessToken").asText();
 }
 HttpRequest.Builder request(CourseQaProcess app,String path,String token) {
  var r=HttpRequest.newBuilder(URI.create(app.base()+path)).timeout(Duration.ofSeconds(30)).header("Content-Type","application/json");if(token!=null)r.header("Authorization","Bearer "+token);return r;
 }
 void assertPending(JdbcTemplate db,CourseQaFixtures.Payment p) {
  assertThat(db.queryForObject("select status from course_payments where id=?",String.class,p.paymentId())).isEqualTo("PENDING");
  assertThat(db.queryForObject("select status from course_orders where id=?",String.class,p.orderId())).isEqualTo("PENDING_PAYMENT");
  assertThat(db.queryForObject("select status from course_reservations where course_order_id=?",String.class,p.orderId())).isEqualTo("HELD");
  assertArtifacts(db,p,0);
  assertThat(db.queryForObject("select provider_transaction_id from course_payments where id=?",String.class,p.paymentId())).isNull();
 }
 void assertPaid(JdbcTemplate db,CourseQaFixtures.Payment p) {
  assertThat(db.queryForObject("select status from course_payments where id=?",String.class,p.paymentId())).isEqualTo("PAID");
  assertThat(db.queryForObject("select status from course_orders where id=?",String.class,p.orderId())).isEqualTo("PAID");
  assertThat(db.queryForObject("select status from course_reservations where course_order_id=?",String.class,p.orderId())).isEqualTo("ENROLLED");
  assertArtifacts(db,p,1);
 }
 void assertArtifacts(JdbcTemplate db,CourseQaFixtures.Payment p,int count) {
  assertThat(db.queryForObject("select count(*) from course_enrollments where course_order_id=?",Long.class,p.orderId())).isEqualTo(count);
  assertThat(db.queryForObject("select count(*) from course_enrollment_sessions where course_run_id=?",Long.class,p.runId())).isEqualTo(count*3);
  assertThat(db.queryForObject("select count(*) from course_payment_reconciliations where course_payment_id=?",Long.class,p.paymentId())).isEqualTo(count);
  assertThat(db.queryForObject("select count(*) from audit_events where target_id=? and action='COURSE_PAYMENT_RECONCILED'",Long.class,p.paymentId())).isEqualTo(count);
  assertThat(db.queryForObject("select count(*) from notification_outbox where payload->>'orderId'=? and notification_type='COURSE_ENROLLMENT_CREATED'",Long.class,p.orderId().toString())).isEqualTo(count);
 }
}
