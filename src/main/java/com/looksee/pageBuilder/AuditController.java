package com.looksee.pageBuilder;


import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
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
import com.looksee.pageBuilder.models.journeys.DomainMap;
import com.looksee.pageBuilder.models.journeys.Journey;
import com.looksee.pageBuilder.models.journeys.LandingStep;
import com.looksee.pageBuilder.gcp.PubSubPageCreatedPublisherImpl;
import com.looksee.pageBuilder.gcp.PubSubErrorPublisherImpl;
import com.looksee.pageBuilder.gcp.PubSubJourneyVerifiedPublisherImpl;
import com.looksee.pageBuilder.mapper.Body;
import com.looksee.pageBuilder.models.Browser;
import com.looksee.pageBuilder.models.ElementState;
import com.looksee.pageBuilder.models.PageState;
import com.looksee.pageBuilder.models.enums.BrowserEnvironment;
import com.looksee.pageBuilder.models.enums.BrowserType;
import com.looksee.pageBuilder.models.enums.JourneyStatus;
import com.looksee.pageBuilder.models.enums.PathStatus;
import com.looksee.pageBuilder.models.journeys.Step;
import com.looksee.pageBuilder.models.message.PageBuiltMessage;
import com.looksee.pageBuilder.models.message.PageDataExtractionError;
import com.looksee.pageBuilder.models.message.UrlMessage;
import com.looksee.pageBuilder.models.message.VerifiedJourneyMessage;
import com.looksee.pageBuilder.services.AuditRecordService;
import com.looksee.pageBuilder.services.BrowserService;
import com.looksee.pageBuilder.services.DomainMapService;
import com.looksee.pageBuilder.services.ElementStateService;
import com.looksee.pageBuilder.services.JourneyService;
import com.looksee.pageBuilder.services.PageStateService;
import com.looksee.pageBuilder.services.StepService;
import com.looksee.utils.BrowserUtils;
import com.looksee.utils.ElementStateUtils;
import com.looksee.utils.ImageUtils;
import com.looksee.utils.TimingUtils;


/**
 * API Controller with main endpoint for running the page builder script
 * 
 */
@RestController
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
				PageDataExtractionError page_extraction_err = new PageDataExtractionError(url_msg.getDomainId(), 
																						url_msg.getAccountId(), 
																						url_msg.getDomainAuditRecordId(), 
																						url_msg.getUrl().toString(), 
																						"Received "+http_status+" status while building page state "+url_msg.getUrl());

				String error_json = mapper.writeValueAsString(page_extraction_err);
				pubSubErrorPublisherImpl.publish(error_json);
				
				return new ResponseEntity<String>("Successfully sent message to page extraction error", HttpStatus.OK);
			}
				
			//update audit record with progress
			browser = browser_service.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);
			page_state = browser_service.buildPageState(url, browser, is_secure, http_status);
			//browser.close();
		
			log.warn("Extracting element states...");
			//URL full_page_screenshot_url = new URL(page_state.getFullPageScreenshotUrlOnload());
			//BufferedImage page_screenshot = ImageUtils.readImageFromURL(full_page_screenshot_url);
			List<String> xpaths = browser_service.extractAllUniqueElementXpaths(page_state.getSrc());
			List<ElementState> element_states = browser_service.buildPageElementsWithoutNavigation(	page_state, 
																									xpaths,
																									browser);

			element_states = ElementStateUtils.enrichBackgroundColor(element_states).collect(Collectors.toList());
			
			/*
			List<ElementState> saved_elements = saveNewElements(page_state.getId(), element_states);

			List<Long> element_ids = saved_elements.parallelStream()
												   .map(element -> element.getId())
												   .collect(Collectors.toList());
			
			log.warn("element ids :: " + element_ids);
			page_state_service.addAllElements(page_state.getId(), element_ids);
*/
			page_state.setElements(element_states);
			page_state = page_state_service.save(page_state);
			//page_state.setElements(page_state_service.getElementStates(page_state.getId()));

			page_state.setElements(page_state_service.getElementStates(page_state.getId()));

			//if domain audit id is less than zero then this is a single page audit
			//send PageBuilt message to pub/sub
		    PageBuiltMessage page_built_msg = new PageBuiltMessage(url_msg.getAccountId(),
														    		url_msg.getDomainAuditRecordId(),
														    		url_msg.getDomainId(), 
														    		page_state.getId(),
														    		url_msg.getPageAuditRecordId());
		    
		    String page_built_str = mapper.writeValueAsString(page_built_msg);
		    pubSubPageCreatedPublisherImpl.publish(page_built_str);

		    if(url_msg.getDomainAuditRecordId() >= 0) {
				
				List<Step> steps = new ArrayList<>();
				Step step = new LandingStep(page_state);
				step = step_service.save(step);
				steps.add(step);
				Journey journey = new Journey(steps, JourneyStatus.VERIFIED);
				journey.setCandidateKey(journey.generateKey());
				Journey saved_journey = journey_service.save(journey);
				journey.setId(saved_journey.getId());
				
				//if domain map exists then attach journey to domain map, otherwise create new domain map and add it to the domain, then add the journey to the domain map				
				DomainMap domain_map = domain_map_service.findByDomainAuditId(url_msg.getDomainAuditRecordId());
				if(domain_map == null) {
					domain_map = domain_map_service.save(new DomainMap());
					log.warn("adding domain map to audit record = " + url_msg.getDomainAuditRecordId());

					audit_record_service.addDomainMap(url_msg.getDomainAuditRecordId(), domain_map.getId());
				}
				
				domain_map_service.addJourneyToDomainMap(journey.getId(), domain_map.getId());
				
				VerifiedJourneyMessage journey_msg = new VerifiedJourneyMessage(journey, 
																				PathStatus.READY, 
																				BrowserType.CHROME, 
																				url_msg.getDomainId(), 
																				url_msg.getAccountId(), 
																				url_msg.getDomainAuditRecordId());
				
				log.warn(page_built_str);
				String journey_msg_str = mapper.writeValueAsString(journey_msg);
				log.warn("Publishing to verified journey topic = "+journey_msg_str);

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
// [END run_pubsub_handler]
// [END cloudrun_pubsub_handler]