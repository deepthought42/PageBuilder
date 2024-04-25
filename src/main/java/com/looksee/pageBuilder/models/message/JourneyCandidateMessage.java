package com.looksee.pageBuilder.models.message;


import com.looksee.pageBuilder.models.enums.BrowserType;
import com.looksee.pageBuilder.models.journeys.Journey;

import lombok.Getter;
import lombok.Setter;


/**
 * 
 */
public class JourneyCandidateMessage extends Message {

	private Journey journey;
	private BrowserType browser;

	@Getter
	@Setter
	private long auditRecordId;
	
	public JourneyCandidateMessage() {}
	
	public JourneyCandidateMessage(Journey journey, 
								   BrowserType browser_type, 
								   long account_id, 
								   long audit_record_id)
	{
		super(account_id);
		setJourney(journey);
		setBrowser(browser_type);
		setAuditRecordId(audit_record_id);
	}

	public JourneyCandidateMessage clone(){
		return new JourneyCandidateMessage(null, 
								  getBrowser(), 
								  getAccountId(), 
								  getAuditRecordId());
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
