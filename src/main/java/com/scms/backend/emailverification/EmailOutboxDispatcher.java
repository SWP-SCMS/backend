package com.scms.backend.emailverification;

import java.lang.management.ManagementFactory;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class EmailOutboxDispatcher {
	private static final Logger log=LoggerFactory.getLogger(EmailOutboxDispatcher.class);
	private final EmailOutboxDeliveryService delivery;private final Clock clock;
	private final String worker=ManagementFactory.getRuntimeMXBean().getName();
	EmailOutboxDispatcher(EmailOutboxDeliveryService delivery,Clock clock){this.delivery=delivery;this.clock=clock;}
	@Scheduled(fixedDelayString="${scms.email-verification.dispatch-delay:10000}",
		initialDelayString="${scms.email-verification.dispatch-initial-delay:10000}")
	void dispatch(){Instant now=clock.instant();int expired=delivery.expire(now);if(expired>0)log.warn("event=email_outbox_expired count={}",expired);
		for(int i=0;i<50;i++){var claimed=delivery.claim(clock.instant(),worker);if(claimed.isEmpty())return;var job=claimed.orElseThrow();
			try{var result=delivery.deliver(job,clock.instant());delivery.sent(job.id(),result.providerMessageId(),clock.instant());}
			catch(EmailDeliveryException ex){
				if(ex.retryable())delivery.failed(job,clock.instant(),ex.errorName(),ex.retryAfter());
				else delivery.terminal(job.id(),clock.instant(),ex.errorName());
				log.warn("event=email_delivery_failed outboxId={} accountId={} provider={} error={} retryable={}",
					job.id(),job.accountId(),job.deliveryProvider(),ex.errorName(),ex.retryable());}
			catch(RuntimeException ex){delivery.failed(job,clock.instant(),"UNEXPECTED_EMAIL_PROVIDER_ERROR",null);
				log.warn("event=email_delivery_failed outboxId={} accountId={} provider={} error=unexpected",
					job.id(),job.accountId(),job.deliveryProvider());}}
	}
}
