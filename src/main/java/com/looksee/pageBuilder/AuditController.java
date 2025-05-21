package com.looksee.pageBuilder;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.validation.Valid;

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
import com.looksee.pageBuilder.gcp.GoogleCloudStorage;
import com.looksee.pageBuilder.gcp.PubSubErrorPublisherImpl;
import com.looksee.pageBuilder.gcp.PubSubJourneyVerifiedPublisherImpl;
import com.looksee.pageBuilder.gcp.PubSubPageAuditPublisherImpl;
import com.looksee.pageBuilder.gcp.PubSubPageCreatedPublisherImpl;
import com.looksee.pageBuilder.models.Browser;
import com.looksee.pageBuilder.models.ElementState;
import com.looksee.pageBuilder.models.PageState;
import com.looksee.pageBuilder.models.enums.AuditLevel;
import com.looksee.pageBuilder.models.enums.BrowserEnvironment;
import com.looksee.pageBuilder.models.enums.BrowserType;
import com.looksee.pageBuilder.models.enums.JourneyStatus;
import com.looksee.pageBuilder.models.journeys.DomainMap;
import com.looksee.pageBuilder.models.journeys.Journey;
import com.looksee.pageBuilder.models.journeys.LandingStep;
import com.looksee.pageBuilder.models.journeys.Step;
import com.looksee.pageBuilder.models.message.AuditStartMessage;
import com.looksee.pageBuilder.models.message.PageAuditMessage;
import com.looksee.pageBuilder.models.message.PageBuiltMessage;
import com.looksee.pageBuilder.models.message.PageDataExtractionError;
import com.looksee.pageBuilder.models.message.VerifiedJourneyMessage;
import com.looksee.pageBuilder.schemas.BodySchema;
import com.looksee.pageBuilder.services.AuditRecordService;
import com.looksee.pageBuilder.services.BrowserService;
import com.looksee.pageBuilder.services.DomainMapService;
import com.looksee.pageBuilder.services.ElementStateService;
import com.looksee.pageBuilder.services.JourneyService;
import com.looksee.pageBuilder.services.PageStateService;
import com.looksee.pageBuilder.services.StepService;
import com.looksee.utils.BrowserUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;


/**
 * API Controller with main endpoint for running the page builder script
 * 
 */
@RestController
@Tag(name = "Audit", description = "API endpoints for building page objects")
public class AuditController {
	private static Logger log = LoggerFactory.getLogger(AuditController.class);

	@Autowired
	private AuditRecordService audit_record_service;
	
	@Autowired
	private BrowserService browser_service;
	
	@Autowired
	private JourneyService journey_service;
	
	@Autowired
	private StepService step_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private ElementStateService element_state_service;
	
	@Autowired
	private DomainMapService domain_map_service;
	
	@Autowired
	private GoogleCloudStorage googleCloudStorage;
	
	@Autowired
	private PubSubErrorPublisherImpl pubSubErrorPublisherImpl;
	
	@Autowired
	private PubSubJourneyVerifiedPublisherImpl pubSubJourneyVerifiedPublisherImpl;
	
	@Autowired
	private PubSubPageCreatedPublisherImpl pubSubPageCreatedPublisherImpl;

	@Autowired
	private PubSubPageAuditPublisherImpl audit_record_topic;
	
	@Operation(
		summary = "Extract page data from a given URL",
		description = "Receives an audit message, processes the page, and publishes appropriate messages to Pub/Sub topics"
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "Successfully processed audit message",
			content = @Content(schema = @Schema(implementation = String.class))
		),
		@ApiResponse(
			responseCode = "500",
			description = "Internal server error while processing audit message",
			content = @Content(schema = @Schema(implementation = String.class))
		)
	})
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
		description = "Audit message containing URL and audit details",
		required = true,
		content = @Content(schema = @Schema(implementation = BodySchema.class))
	)
	@RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity<String> receiveMessage(
		@Valid @RequestBody BodySchema body
	) throws JsonMappingException, JsonProcessingException, ExecutionException, InterruptedException, MalformedURLException {
		String data = body.getMessage().getData();
	    String target = !data.isEmpty() ? new String(Base64.getMimeDecoder().decode(data)) : "";
	    ObjectMapper input_mapper = new ObjectMapper();
        AuditStartMessage url_msg = input_mapper.readValue(target, AuditStartMessage.class);
        
		URL url = new URL(BrowserUtils.sanitizeUserUrl(url_msg.getUrl()));
		
	    JsonMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
	    PageState page_state = null;
		Browser browser = null;

	    try {
	    	boolean is_secure = BrowserUtils.checkIfSecure(url);
			int http_status = BrowserUtils.getHttpStatus(url);

			//usually code 301 is returned which is a redirect, which is usually transferring to https
			if(http_status == 404 || http_status == 408) {
				log.warn("Recieved " + http_status + "status for link :: "+url_msg.getUrl());
				//send message to audit manager letting it know that an error occurred
				PageDataExtractionError page_extraction_err = new PageDataExtractionError(url_msg.getAccountId(),
																						  url_msg.getAuditId(),
																						  url_msg.getUrl().toString(),
																						  "Received "+http_status+" status while building page state "+url_msg.getUrl());

				String error_json = mapper.writeValueAsString(page_extraction_err);
				pubSubErrorPublisherImpl.publish(error_json);
				
				return new ResponseEntity<String>("Successfully sent message to page extraction error", HttpStatus.OK);
			}
			
			DomainMap domain_map = null;
			if(AuditLevel.DOMAIN.equals(url_msg.getType())) {
				domain_map = domain_map_service.findByDomainAuditId(url_msg.getAuditId());
				if(domain_map == null) {
					domain_map = domain_map_service.save(new DomainMap());
					log.warn("adding domain map to audit record = " + url_msg.getAuditId());
					audit_record_service.addDomainMap(url_msg.getAuditId(), domain_map.getId());
				}
			}

			//update audit record with progress
			browser = browser_service.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);
			page_state = browser_service.buildPageState(url, browser, is_secure, http_status, url_msg.getAuditId());
		
			//CHECK IF PAGE STATE EXISTS IN DOMAIN AUDIT ALREADY. IF IT DOESN'T, OR IT DOES AND
			// THERE AREN'T ANY ELEMENTS ASSOCIATED IN DB THEN BUILD PAGE ELEMENTS,
			PageState page_state_record = audit_record_service.findPageWithKey(url_msg.getAuditId(),
																				page_state.getKey());
			
			if(page_state_record == null 
					|| element_state_service.getAllExistingKeys(page_state_record.getId()).isEmpty())
			{
				log.warn("Extracting element states...");
				List<String> xpaths = browser_service.extractAllUniqueElementXpaths(page_state.getSrc());
				List<ElementState> element_states = browser_service.getDomElementStates(	page_state,
																							xpaths,
																							browser,
																							url_msg.getAuditId());
				//String host = url.getHost();
				//element_states = browser_service.enrichElementStates(element_states, page_state, browser, host);
				//element_states = ElementStateUtils.enrichBackgroundColor(element_states).collect(Collectors.toList());
				page_state.setElements(element_states);
				page_state = page_state_service.save(page_state);
			}
			else {
				page_state = page_state_record;
			}

			List<ElementState> elements = page_state_service.getElementStates(page_state.getId());
			page_state.setElements(elements);
			
		    if(AuditLevel.DOMAIN.equals(url_msg.getType())) {
				domain_map_service.addPageToDomainMap(domain_map.getId(), page_state.getId());
				PageBuiltMessage page_built_msg = new PageBuiltMessage(url_msg.getAccountId(),
																		page_state.getId(),
																		url_msg.getAuditId());
	
				String page_built_str = mapper.writeValueAsString(page_built_msg);
				log.warn("sending page built message to PageCreated topic : "+page_built_str);
				pubSubPageCreatedPublisherImpl.publish(page_built_str);
				List<Step> steps = new ArrayList<>();
				Step step = new LandingStep(page_state, JourneyStatus.VERIFIED);
				step = step_service.save(step);
				steps.add(step);
				Journey journey = new Journey(steps, JourneyStatus.VERIFIED);
				journey.setCandidateKey(journey.generateKey());
				Journey saved_journey = journey_service.save(journey);
				journey.setId(saved_journey.getId());
				
				domain_map_service.addJourneyToDomainMap(journey.getId(), domain_map.getId());
				
				VerifiedJourneyMessage journey_msg = new VerifiedJourneyMessage(journey, 
																				BrowserType.CHROME, 
																				url_msg.getAccountId(), 
																				url_msg.getAuditId());
				log.warn("journey steps = "+ journey.getSteps());
				String journey_msg_str = mapper.writeValueAsString(journey_msg);
				log.warn("Publishing to verified journey topic = "+journey_msg_str);

				pubSubJourneyVerifiedPublisherImpl.publish(journey_msg_str);
			}
			else if(AuditLevel.PAGE.equals(url_msg.getType())){
				audit_record_service.addPageToAuditRecord(url_msg.getAuditId(), page_state.getId());
				//send message to page audit message topic
				PageAuditMessage audit_msg = new PageAuditMessage(	url_msg.getAccountId(),
																	url_msg.getAuditId());
				log.warn("sending page audit message = "+audit_msg.getPageAuditId());
				String audit_record_json = mapper.writeValueAsString(audit_msg);
				log.warn("(PageAudit) Sending PageAuditMessage to Pub/Sub = "+audit_record_json);
				audit_record_topic.publish(audit_record_json);
			}
			
			return new ResponseEntity<String>("Successfully sent message to verifed journey topic", HttpStatus.OK);
		}
		catch(Exception e) {
			PageDataExtractionError page_extracton_err = new PageDataExtractionError(url_msg.getAccountId(),
																						url_msg.getAuditId(),
																						url_msg.getUrl().toString(),
																						"An exception occurred while building page state "+url_msg.getUrl()+".\n"+e.getMessage());

			String element_extraction_str = mapper.writeValueAsString(page_extracton_err);
			pubSubErrorPublisherImpl.publish(element_extraction_str);
			log.error("An exception occurred that bubbled up to the page state builder : "+e.getMessage());
			e.printStackTrace();
			
			return new ResponseEntity<String>("Error building page state for url "+url_msg.getUrl(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		finally {
			if(browser != null) {
				browser.close();
			}
		}
	}
	
	/**
	 * Retrieves keys for all existing element states that are connected the the page with the given page state id
	 * 
	 * NOTE: This is best for a database with significant memory as the size of data can be difficult to process all at once
	 * on smaller machines
	 * 
	 * @param page_state_id
	 * @param element_states
	 * @return {@link List} of {@link ElementState} ids 
	 */
	private List<ElementState> saveNewElements(long page_state_id, List<ElementState> element_states) {
		return element_states.stream()
							   .map(element -> element_state_service.save(element))
							   .collect(Collectors.toList());
	}	

}