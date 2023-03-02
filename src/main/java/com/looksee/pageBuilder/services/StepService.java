package com.looksee.pageBuilder.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.pageBuilder.models.ElementState;
import com.looksee.pageBuilder.models.journeys.LandingStep;
import com.looksee.pageBuilder.models.journeys.LoginStep;
import com.looksee.pageBuilder.models.journeys.SimpleStep;
import com.looksee.pageBuilder.models.journeys.Step;
import com.looksee.pageBuilder.models.repository.LandingStepRepository;
import com.looksee.pageBuilder.models.repository.LoginStepRepository;
import com.looksee.pageBuilder.models.repository.SimpleStepRepository;
import com.looksee.pageBuilder.models.repository.StepRepository;

import io.github.resilience4j.retry.annotation.Retry;

/**
 * Enables interacting with database for {@link SimpleStep Steps}
 */
@Service
@Retry(name = "neoforj")
public class StepService {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(StepService.class);

	@Autowired
	private StepRepository step_repo;
	
	@Autowired
	private SimpleStepRepository simple_step_repo;

	@Autowired
	private LoginStepRepository login_step_repo;
	
	@Autowired
	private LandingStepRepository landing_step_repo;
	
	public Step findByKey(String step_key) {
		return step_repo.findByKey(step_key);
	}

	public Step save(Step step) {
		assert step != null;
		
		if(step instanceof SimpleStep) {
			log.warn("saving simple step");
			SimpleStep step_record = simple_step_repo.findByKey(step.getKey());
			SimpleStep simple_step = (SimpleStep)step;
			
			if(step_record != null) {
				step_record.setElementState(simple_step.getElementState());
				step_record.setStartPage(simple_step.getStartPage());
				step_record.setEndPage(simple_step.getEndPage());
				return step_record;
			}
			
			
			SimpleStep new_simple_step = new SimpleStep();
			new_simple_step.setAction(simple_step.getAction());
			new_simple_step.setActionInput(simple_step.getActionInput());
			new_simple_step.setKey(simple_step.generateKey());
			new_simple_step = simple_step_repo.save(new_simple_step);
			//new_simple_step.setStartPage(simple_step_repo.addStartPage(new_simple_step.getId(), simple_step.getStartPage().getId()));
			new_simple_step.setStartPage(simple_step.getStartPage());
			//new_simple_step.setEndPage(simple_step_repo.addEndPage(new_simple_step.getId(), simple_step.getEndPage().getId()));
			new_simple_step.setEndPage(simple_step.getEndPage());
			//new_simple_step.setElementState(simple_step_repo.addElementState(new_simple_step.getId(), simple_step.getElementState().getId()));
			new_simple_step.setElementState(simple_step.getElementState());
			
			return new_simple_step;
		}
		else if(step instanceof LoginStep) {
			log.warn("looking up LOGIN step with key :: "+step.getKey());
			LoginStep step_record = login_step_repo.findByKey(step.getKey());
			LoginStep login_step = (LoginStep)step;
			if(step_record != null) {
				log.warn("found login step with key :: "+step_record.getKey());
				log.warn("loading LOGIN STEP connections...");
				step_record.setTestUser(login_step.getTestUser());
				step_record.setUsernameElement(login_step.getUsernameElement());
				step_record.setPasswordElement(login_step.getPasswordElement());
				step_record.setSubmitElement(login_step.getSubmitElement());
				step_record.setStartPage(login_step.getStartPage());
				step_record.setEndPage(login_step.getEndPage());

				return step_record;
			}
			
			
			LoginStep new_login_step = new LoginStep();
			new_login_step.setKey(login_step.generateKey());
			log.warn("saving login step");
			new_login_step = login_step_repo.save(new_login_step);
			log.warn("adding start page to login step");
			new_login_step.setStartPage(login_step.getStartPage());
			
			log.warn("setting end page");
			new_login_step.setEndPage(login_step.getEndPage());
			
			log.warn("adding username element to login step");
			new_login_step.setUsernameElement(login_step.getUsernameElement());
			
			log.warn("adding password element to login step");
			new_login_step.setPasswordElement(login_step.getPasswordElement());

			log.warn("adding submit element to login step");
			new_login_step.setSubmitElement(login_step.getSubmitElement());

			log.warn("login step test user id :: "+login_step.getTestUser().getId());
			new_login_step.setTestUser(login_step.getTestUser());

			return new_login_step;
		}
		else if(step instanceof LandingStep) {
			LandingStep landing_step_record = landing_step_repo.findByKey(step.getKey());
			
			if(landing_step_record != null) {
				landing_step_record.setStartPage(step.getStartPage());
				
				return landing_step_record;
			}
			else {
				LandingStep landing_step = (LandingStep)step;
				
				Step saved_step = landing_step_repo.save(landing_step);
				//page_state_repo.addStartPage(saved_step.getId(), landing_step.getStartPage().getId());
				saved_step.setStartPage(landing_step.getStartPage());
				
				return saved_step;
			}
		}
		else {
			Step step_record = step_repo.findByKey(step.getKey());
			
			if(step_record != null) {
				step_record.setStartPage(step.getStartPage());
				step_record.setEndPage(step.getEndPage());
				
				return step_record;
			}
			else {
				Step saved_step = step_repo.save(step);
				//step_repo.addStartPage(saved_step.getId(), saved_step.getStartPage().getId());
				//step_repo.addEndPage(saved_step.getId(), saved_step.getEndPage().getId());
				saved_step.setStartPage(saved_step.getStartPage());
				saved_step.setEndPage(saved_step.getEndPage());
				
				return saved_step;
			}
		}
	}

	public ElementState getElementState(String step_key) {
		return step_repo.getElementState(step_key);
	}
}
