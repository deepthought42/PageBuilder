package com.looksee.pageBuilder.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.looksee.pageBuilder.models.PageAuditRecord;
import com.looksee.pageBuilder.models.dto.DomainDto;
import com.pusher.rest.Pusher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

/**
 * Defines methods for emitting data to subscribed clients
 */
public class MessageBroadcaster {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(MessageBroadcaster.class);
	
	private Pusher pusher;
	
	public MessageBroadcaster(@Value( "${pusher.appId}" ) String app_id,
			@Value("${pusher.key}") String key,
			@Value("${pusher.secret}") String secret,
			@Value("${pusher.cluster}") String cluster) {
		pusher = new Pusher(app_id, key, secret);
		pusher.setCluster(cluster);
		pusher.setEncrypted(true);
	}
	
	/**
	 * send {@link AuditRecord} to the users pusher channel
	 * @param account_id
	 * @param audit
	 */
	public static void sendAuditRecord(String user_id, DomainDto domain_dto) throws JsonProcessingException {
		log.warn("Sending audit record to Pusher");
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

		String domain_dto_json = mapper.writeValueAsString(domain_dto);
		pusher.trigger(user_id, "audit-record", domain_dto_json);
	}
	
	/**
	 * send {@link AuditRecord} to the users pusher channel
	 * @param account_id
	 * @param audit
	 */
	public static void sendPageAuditUpdate(String user_id, PageAuditRecord page_audit) throws JsonProcessingException {
		log.warn("Sending audit record to Pusher");
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

		String domain_dto_json = mapper.writeValueAsString(page_audit);
		pusher.trigger(user_id, "audit-record", domain_dto_json);
	}
}
