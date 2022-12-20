package com.looksee.pageBuilder.models.message;

import com.looksee.journeyExecutor.models.journeys.Journey;
import com.looksee.pageBuilder.models.enums.BrowserType;
import com.looksee.pageBuilder.models.enums.PathStatus;


/**
 * 
 */
public class JourneyMessage extends Message {

	//private List<Step> steps;
	private Journey journey;
	private PathStatus status;
	private BrowserType browser;
	
	public JourneyMessage(Journey journey,
					   PathStatus status, 
					   BrowserType browser_type, 
					   long domain_id, 
					   long account_id, 
					   long audit_record_id)
	{
		super(domain_id, account_id, audit_record_id);
		setJourney(journey);
		setStatus(status);
		setBrowser(browser_type);
	}
	
	public JourneyMessage clone(){
		return new JourneyMessage(journey.clone(),
								  getStatus(), 
								  getBrowser(), 
								  getDomainId(), 
								  getAccountId(),
								  getDomainAuditRecordId());
	}

	public PathStatus getStatus() {
		return status;
	}

	private void setStatus(PathStatus status) {
		this.status = status;
	}

	public BrowserType getBrowser() {
		return browser;
	}

	public void setBrowser(BrowserType browser) {
		this.browser = browser;
	}

	public Journey getJourney() {
		return journey;
	}

	public void setJourney(Journey journey) {
		this.journey = journey;
	}
}
