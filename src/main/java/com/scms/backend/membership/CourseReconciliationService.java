package com.scms.backend.membership;

import static com.scms.backend.membership.PaymentFulfillmentService.*;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.scms.backend.account.*;
import com.scms.backend.audit.*;
import com.scms.backend.course.CourseReservationExpiryService;
import com.scms.backend.membership.CourseReconciliationModels.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
class CourseReconciliationService {
 private final JdbcTemplate db;
 private final AccountRepository accounts;
 private final PaymentFulfillmentService fulfillment;
 private final CourseReservationExpiryService expiry;
 private final AuditEventRepository audits;
 private final ObjectMapper json;
 private final Clock clock;
 CourseReconciliationService(JdbcTemplate db, AccountRepository accounts, PaymentFulfillmentService fulfillment,
   CourseReservationExpiryService expiry, AuditEventRepository audits, ObjectMapper json, Clock clock) {
  this.db=db; this.accounts=accounts; this.fulfillment=fulfillment; this.expiry=expiry;
  this.audits=audits; this.json=json; this.clock=clock;
 }

 @Transactional(readOnly=true)
 Page queue(UUID actor, ReconciliationQueueStatus status, String query, int page, int size) {
  staff(actor);
  if (page<0 || size<1 || size>100 || (query!=null && query.length()>200))
   throw PaymentException.validation("page >= 0, size 1..100 and query <= 200 characters required");
  String from = " from course_payments p join course_orders o on o.id=p.course_order_id "
    + "join accounts a on a.id=p.member_account_id left join member_profiles m on m.account_id=a.id ";
  String where = " where p.status='PENDING' and p.method='SEPAY' and o.status='PENDING_PAYMENT'";
  List<Object> args=new ArrayList<>();
  if (status==ReconciliationQueueStatus.PENDING || status==ReconciliationQueueStatus.EXPIRED_WINDOW) {
   where += status==ReconciliationQueueStatus.PENDING ? " and p.deadline_at>?" : " and p.deadline_at<=?";
   args.add(Timestamp.from(clock.instant()));
  }
  if (query!=null && !query.isBlank()) {
   where += " and (p.id::text ilike ? or o.order_reference ilike ? or p.payment_reference ilike ? or a.full_name ilike ? or m.member_code ilike ?)";
   for (int i=0;i<5;i++) args.add("%"+query.trim()+"%");
  }
  long count=db.queryForObject("select count(*)"+from+where,Long.class,args.toArray());
  args.add(size); args.add((long)page*size);
  List<QueueItem> items=db.query("select p.*,o.course_run_id,o.order_reference,m.member_code,a.full_name"+from+where
    + " order by p.created_at desc,p.id desc limit ? offset ?", (rs,n)->new QueueItem(
    rs.getObject("id",UUID.class),rs.getObject("course_order_id",UUID.class),rs.getObject("course_reservation_id",UUID.class),
    rs.getObject("course_run_id",UUID.class),rs.getObject("member_account_id",UUID.class),rs.getString("member_code"),
    rs.getString("full_name"),rs.getString("order_reference"),rs.getString("payment_reference"),
    rs.getBigDecimal("amount").toBigIntegerExact(),rs.getString("currency_code").trim(),rs.getString("status"),
    rs.getTimestamp("deadline_at").toInstant(),rs.getTimestamp("created_at").toInstant()),args.toArray());
  return new Page(items,page,size,count,(int)Math.min(Integer.MAX_VALUE,Math.ceilDiv(count,(long)size)));
 }

 @Transactional(readOnly=true)
 Detail detail(UUID actor, UUID paymentId) {
  staff(actor);
  Map<String,Object> row=fulfillment.coursePayment(paymentId);
  String blocked=paidBlockedReason(row,clock.instant());
  boolean failed=canFail(row,clock.instant());
  return new Detail(view(row),string(row,"bank_code_snapshot"),string(row,"bank_account_number_snapshot"),
    string(row,"bank_account_name_snapshot"),string(row,"bank_transfer_content"),blocked==null,failed,blocked);
 }

 @Transactional(readOnly=true)
 Result result(UUID actor, UUID paymentId) {
  Account account=active(actor);
  if (account.getRole()!=AccountRole.MANAGER && account.getRole()!=AccountRole.RECEPTIONIST
    && account.getRole()!=AccountRole.MEMBER) throw PaymentException.forbidden();
  Map<String,Object> row=fulfillment.coursePayment(paymentId);
  if (account.getRole()==AccountRole.MEMBER && !actor.equals(uuid(row,"member_account_id"))) throw PaymentException.forbidden();
  return view(row);
 }

 @Transactional
 Result reconcile(UUID actor, UUID paymentId, String key, PaymentActionRequest request) {
  staff(actor);
  validate(key,request);
  Map<String,Object> row=fulfillment.coursePaymentForUpdate(paymentId);
  String payload=json.writeValueAsString(request);
  var saved=db.queryForList("select request_json::text,response_json::text from course_payment_reconciliations "
    + "where actor_account_id=? and course_payment_id=? and idempotency_key=?",actor,paymentId,key);
  if (!saved.isEmpty()) {
   if (!json.readTree(payload).equals(json.readTree((String)saved.getFirst().get("request_json"))))
    throw PaymentException.conflict("Idempotency-Key was used with another payload");
   return json.readValue((String)saved.getFirst().get("response_json"),Result.class);
  }
  boolean changed=false;
  Instant now=clock.instant();
  if (request.status()==PaymentActionRequest.ReconciliationStatus.PAID) {
   if (!amount(row,"payment_amount").equals(request.receivedAmount())
     || !string(row,"bank_transfer_content").equals(request.transferContent().trim()))
    throw PaymentException.validation("receivedAmount or transferContent does not match the payment snapshot");
   String tx=request.providerTransactionId().trim();
   fulfillment.lockProviderTransaction("SEPAY",tx);
   now=clock.instant();
   if (fulfillment.providerTransactionClaimed("SEPAY",tx,null,paymentId))
    throw PaymentException.conflict("Provider transaction was already used");
   if ("PAID".equals(string(row,"payment_status"))) {
    if (!"SEPAY".equals(string(row,"payment_method")) || !tx.equals(string(row,"provider_transaction_id")))
     throw PaymentException.conflict("Payment was already confirmed differently");
   } else {
    String blocked=paidBlockedReason(row,now);
    if (blocked!=null) throw PaymentException.conflict(blocked);
    fulfillment.fulfillCourseReconciliation(row,actor,tx,now);
    changed=true;
   }
  } else {
   if (!canFail(row,now)) throw PaymentException.conflict("Only an expired pending Course payment can be failed");
   if (!expiry.expireReservation(uuid(row,"reservation_id"),now)) throw PaymentException.conflict("Reservation changed");
   changed=true;
  }
  Result response=view(fulfillment.coursePayment(paymentId));
  db.update("""
    insert into course_payment_reconciliations(id,actor_account_id,course_payment_id,idempotency_key,
      request_json,response_json,changed_payment,created_at) values(?,?,?,?,?::jsonb,?::jsonb,?,?)
    """,UUID.randomUUID(),actor,paymentId,key,payload,json.writeValueAsString(response),changed,Timestamp.from(now));
  if (changed) audits.save(new AuditEvent(UUID.randomUUID(),actor,"COURSE_PAYMENT_RECONCILED","COURSE_PAYMENT",paymentId,
    request.reason().trim(),Map.of("status",string(row,"payment_status")),
    Map.of("status",response.status(),"evidence",request.evidence().trim(),"orderId",response.orderId().toString())));
  return response;
 }

 private Result view(Map<String,Object> row) {
  return new Result(uuid(row,"payment_id"),uuid(row,"order_id"),uuid(row,"reservation_id"),uuid(row,"course_run_id"),
    uuid(row,"member_account_id"),string(row,"order_reference"),amount(row,"payment_amount"),string(row,"payment_currency").trim(),
    string(row,"payment_status"),string(row,"order_status"),string(row,"reservation_status"),string(row,"payment_method"),
    instant(row,"expires_at"),"PAID".equals(string(row,"payment_status"))
      ? fulfillment.courseEnrollment(uuid(row,"order_id"),uuid(row,"member_account_id")) : null);
 }
 private String paidBlockedReason(Map<String,Object> row,Instant now) {
  if (!"PENDING".equals(string(row,"payment_status")) || !"PENDING_PAYMENT".equals(string(row,"order_status"))
    || !"HELD".equals(string(row,"reservation_status"))) return "PAYMENT_NOT_PENDING";
  if (!"SEPAY".equals(string(row,"payment_method"))) return "PAYMENT_METHOD_NOT_TRANSFER";
  if (!"VND".equals(string(row,"payment_currency").trim())) return "PAYMENT_CURRENCY_MISMATCH";
  if (!now.isBefore(instant(row,"expires_at")) || !now.isBefore(instant(row,"reservation_expires_at"))) return "PAYMENT_WINDOW_EXPIRED";
  if (!courseRunPayable(string(row,"run_status"))) return "COURSE_RUN_NOT_PAYABLE";
  for (String field:List.of("bank_code_snapshot","bank_account_number_snapshot","bank_account_name_snapshot"))
   if (string(row,field)==null || string(row,field).isBlank()) return "DESTINATION_SNAPSHOT_MISSING";
  try { fulfillment.courseUnits(row,now); }
  catch (PaymentException ex) { return "COURSE_ENROLLMENT_NOT_AVAILABLE"; }
  return null;
 }
 private boolean canFail(Map<String,Object> row, Instant now) {
  return "PENDING".equals(string(row,"payment_status")) && "SEPAY".equals(string(row,"payment_method"))
    && "PENDING_PAYMENT".equals(string(row,"order_status")) && "HELD".equals(string(row,"reservation_status"))
    && !now.isBefore(instant(row,"expires_at")) && !now.isBefore(instant(row,"reservation_expires_at"));
 }
 static void validate(String key,PaymentActionRequest request) {
  if (key==null || key.isBlank() || key.length()>128) throw PaymentException.validation("Idempotency-Key must contain 1..128 characters");
  if (request==null || request.status()==null || blank(request.evidence()) || blank(request.reason())
    || request.evidence().length()>4000 || request.reason().length()>2000)
   throw PaymentException.validation("status, evidence (1..4000), reason (1..2000) required");
  if (request.status()==PaymentActionRequest.ReconciliationStatus.PAID && (request.receivedAmount()==null
    || request.receivedAmount().signum()<=0 || blank(request.transferContent()) || request.transferContent().length()>200
    || blank(request.providerTransactionId()) || request.providerTransactionId().length()>100))
   throw PaymentException.validation("Positive receivedAmount, transferContent and providerTransactionId required for PAID");
 }
 private static boolean blank(String value) { return value==null || value.isBlank(); }
 private Account active(UUID actor) {
  return accounts.findById(actor).filter(a->a.getStatus()==AccountStatus.ACTIVE).orElseThrow(PaymentException::forbidden);
 }
 private void staff(UUID actor) {
  Account a=active(actor);
  if (a.getRole()!=AccountRole.MANAGER && a.getRole()!=AccountRole.RECEPTIONIST) throw PaymentException.forbidden();
 }
}
