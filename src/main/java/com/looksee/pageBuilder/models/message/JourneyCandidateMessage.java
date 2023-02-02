package com.looksee.pageBuilder.models.message;


import com.looksee.pageBuilder.models.enums.BrowserType;
import com.looksee.pageBuilder.models.journeys.Journey;


/**
 * 
 */
public class JourneyCandidateMessage extends Message {

	private Journey journey;
	//private List<Step> steps;
	private BrowserType browser;
	
	public JourneyCandidateMessage() {}
	
	public JourneyCandidateMessage(Journey journey, 
								   BrowserType browser_type, 
								   long domain_id, 
								   long account_id, 
								   long audit_record_id)
	{
		super(account_id, audit_record_id, domain_id);
		setJourney(journey);
		//setSteps(steps);
		setBrowser(browser_type);
	}

	public JourneyCandidateMessage clone(){
		return new JourneyCandidateMessage(null, 
								  getBrowser(), 
								  getDomainId(),
								  getAccountId(), 
								  getDomainAuditRecordId());
	}

	public BrowserType getBrowser() {
		return browser;
	}

	public void setBrowser(BrowserType browser) {
		this.browser = browser;
	}

	/*
	public void setSteps(List<Step> steps) {
		this.steps = steps;
	}
	
	public List<Step> getSteps() {
		return this.steps;
	}
*/
	public Journey getJourney() {
		return journey;
	}

	public void setJourney(Journey journey) {
		this.journey = journey;
	}
	
}
