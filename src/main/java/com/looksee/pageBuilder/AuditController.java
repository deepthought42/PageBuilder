package com.looksee.pageBuilder;


import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.looksee.pageBuilder.services.AuditRecordService;
import com.looksee.pageBuilder.models.journeys.Journey;
import com.looksee.pageBuilder.gcp.PubSubPageCreatedPublisherImpl;
import com.looksee.pageBuilder.gcp.PubSubErrorPublisherImpl;
import com.looksee.pageBuilder.gcp.PubSubJourneyVerifiedPublisherImpl;
import com.looksee.pageBuilder.mapper.Body;
import com.looksee.pageBuilder.models.AuditRecord;
import com.looksee.pageBuilder.models.ElementState;
import com.looksee.pageBuilder.models.PageAuditRecord;
import com.looksee.pageBuilder.models.PageState;
import com.looksee.pageBuilder.models.dto.PageBuiltMessage;
import com.looksee.pageBuilder.models.enums.BrowserType;
import com.looksee.pageBuilder.models.enums.ExecutionStatus;
import com.looksee.pageBuilder.models.enums.PathStatus;
import com.looksee.pageBuilder.models.journeys.Step;
import com.looksee.pageBuilder.models.message.PageDataExtractionError;
import com.looksee.pageBuilder.models.message.UrlMessage;
import com.looksee.pageBuilder.models.message.VerifiedJourneyMessage;
import com.looksee.pageBuilder.services.BrowserService;
import com.looksee.pageBuilder.services.ElementStateService;
import com.looksee.pageBuilder.services.PageStateService;
import com.looksee.utils.BrowserUtils;
import com.looksee.utils.ElementStateUtils;
import com.looksee.utils.ImageUtils;


/**
 * PubsubController consumes a Pub/Sub message.
 * 
 */
@RestController
public class AuditController {
	private static Logger log = LoggerFactory.getLogger(AuditController.class);

	@Autowired
	private BrowserService browser_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private ElementStateService element_state_service;
	
	@Autowired
	private AuditRecordService audit_record_service;
	
	@Autowired
	private PubSubErrorPublisherImpl pubSubErrorPublisherImpl;
	
	@Autowired
	private PubSubJourneyVerifiedPublisherImpl pubSubJourneyVerifiedPublisherImpl;
	
	@Autowired
	private PubSubPageCreatedPublisherImpl pubSubPageCreatedPublisherImpl;
	
	@RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity<String> receiveMessage(@RequestBody Body body) 
			throws JsonMappingException, JsonProcessingException, ExecutionException, InterruptedException, MalformedURLException 
	{
		Body.Message message = body.getMessage();
		String data = message.getData();
	    String target = !data.isEmpty() ? new String(Base64.getDecoder().decode(data)) : "";
	    ObjectMapper input_mapper = new ObjectMapper();
        UrlMessage url_msg = input_mapper.readValue(target, UrlMessage.class);
        
	    AuditRecord audit_record = new PageAuditRecord(ExecutionStatus.BUILDING_PAGE, new HashSet<>(), true);
		
		audit_record = audit_record_service.save(audit_record);
		audit_record_service.addPageAuditToDomainAudit(url_msg.getDomainAuditRecordId(), 
														audit_record.getId());
		
		//URL url = new URL(url_msg.getUrl());
		URL url = new URL(BrowserUtils.sanitizeUserUrl(url_msg.getUrl()));
		
	    JsonMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
	    PageState page_state = null;
	    
	    try {						
			int http_status = BrowserUtils.getHttpStatus(url);

			//usually code 301 is returned which is a redirect, which is usually transferring to https
			if(http_status == 404 || http_status == 408) {
				log.warn("Recieved " + http_status + "status for link :: "+url_msg.getUrl());
				//send message to audit manager letting it know that an error occurred
				PageDataExtractionError page_extraction_err = new PageDataExtractionError(url_msg.getDomainId(), 
																						url_msg.getAccountId(), 
																						url_msg.getDomainAuditRecordId(), 
																						url_msg.getUrl().toString(), 
																						"Received "+http_status+" status while building page state "+url_msg.getUrl());

				String audit_record_json = mapper.writeValueAsString(page_extraction_err);
				pubSubErrorPublisherImpl.publish(audit_record_json);
				
				return new ResponseEntity<String>("Successfully sent message to page extraction error", HttpStatus.OK);
			}
			else {
				//update audit record with progress
				page_state = browser_service.buildPageState(url); 
				page_state = page_state_service.save(page_state);
				log.warn("saved page state :: "+page_state.getId());
				audit_record_service.addPageToAuditRecord(audit_record.getId(), page_state.getId());				
			}
		
			log.warn("Extracting element states...");
			URL full_page_screenshot_url = new URL(page_state.getFullPageScreenshotUrlOnload());
			BufferedImage page_screenshot = ImageUtils.readImageFromURL(full_page_screenshot_url);
			URL page_url = new URL(BrowserUtils.sanitizeUrl(page_state.getUrl(),
															page_state.isSecured()));
			
			List<String> xpaths = browser_service.extractAllUniqueElementXpaths(page_state.getSrc());
			List<ElementState> element_states = browser_service.buildPageElements(	page_state, 
																					xpaths,
																					page_url,
																					page_screenshot.getHeight());

			element_states = ElementStateUtils.enrichBackgroundColor(element_states).collect(Collectors.toList());
			page_state.setElements(element_states);
			
			List<Long> element_ids = saveNewElements(page_state.getId(), element_states);
			page_state_service.addAllElements(page_state.getId(), element_ids);
			/*
			for(long element_id : element_ids) {
				page_state_service.addElement(page_state.getId(), element_id);
			}
			*/
			
			//if domain audit id is less than zero then this is a single page audit
			//send PageBuilt message to pub/sub
			log.warn("page state id :: " + page_state.getId());
	   		PageBuiltMessage page_built_msg = new PageBuiltMessage(url_msg.getAccountId(),
																	 		 url_msg.getDomainAuditRecordId(),
				   															 url_msg.getDomainId(), 
				   															 page_state.getId(),
				   															 audit_record.getId());
			
			String page_built_str = mapper.writeValueAsString(page_built_msg);
			//TODO: SEND PUB SUB MESSAGE THAT AUDIT RECORD NOT FOUND WITH PAGE DATA EXTRACTION MESSAGE
		    pubSubPageCreatedPublisherImpl.publish(page_built_str);

			if(url_msg.getDomainAuditRecordId() >= 0) {
				List<Step> steps = new ArrayList<>();
				steps.add(new Step(page_state, null));
				log.warn("adding steps to journey");
				Journey journey = new Journey(steps);
				log.warn("building journey Candidate message");
				
				VerifiedJourneyMessage journey_msg = new VerifiedJourneyMessage(journey, 
																				PathStatus.READY, 
																				BrowserType.CHROME, 
																				url_msg.getDomainId(), 
																				url_msg.getAccountId(), 
																				url_msg.getDomainAuditRecordId());
				log.warn("sending verified journey");
				String journey_msg_str = mapper.writeValueAsString(journey_msg);

				pubSubJourneyVerifiedPublisherImpl.publish(journey_msg_str);
			}
			
			return new ResponseEntity<String>("Successfully sent message to verifed journey topic", HttpStatus.OK);
		}
		catch(Exception e) {
			PageDataExtractionError page_extracton_err = new PageDataExtractionError(url_msg.getDomainId(), 
																						url_msg.getAccountId(), 
																						url_msg.getDomainAuditRecordId(), 
																						url_msg.getUrl().toString(), 
																						"An exception occurred while building page state "+url_msg.getUrl()+".\n"+e.getMessage());

			String element_extraction_str = mapper.writeValueAsString(page_extracton_err);
			pubSubErrorPublisherImpl.publish(element_extraction_str);
		    
			log.error("An exception occurred that bubbled up to the page state builder");
			e.printStackTrace();
			
			return new ResponseEntity<String>("Error building page state for url "+url_msg.getUrl(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
	}
	
	/**
	 * 
	 * @param page_state_id
	 * @param element_states
	 * @return
	 */
	private List<Long> saveNewElements(long page_state_id, List<ElementState> element_states) {		
		Set<String> existing_keys = new HashSet<>();
		existing_keys.addAll(element_state_service.getAllExistingKeys(page_state_id));
		return element_states.parallelStream()
									   .filter(f -> !existing_keys.contains(f.getKey()))
									   .map(element -> element_state_service.save(element).getId())
									   .collect(Collectors.toList());
	}	

}
// [END run_pubsub_handler]
// [END cloudrun_pubsub_handler]