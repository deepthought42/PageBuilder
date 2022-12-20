package com.looksee.pageBuilder.models.message;


import com.looksee.pageBuilder.models.journeys.Journey;
import com.looksee.pageBuilder.models.enums.BrowserType;
import com.looksee.pageBuilder.models.enums.PathStatus;

/**
 * 
 */
public class VerifiedJourneyMessage extends Message {

	private Journey journey;
	private PathStatus status;
	private BrowserType browser;
	
	public VerifiedJourneyMessage() {
		super(-1, -1, -1);
	}
	public VerifiedJourneyMessage( Journey journey, 
								   PathStatus status, 
								   BrowserType browser,
								   long domain_id,
								   long account_id, 
								   long audit_record_id)
	{
		super(account_id, audit_record_id, domain_id);
		setJourney(journey);
		setStatus(status);
		setBrowser(browser);
		setDomainId(domain_id);
		setAccountId(account_id);
		setDomainAuditRecordId(audit_record_id);
	}
	
	public VerifiedJourneyMessage clone(){
		return new VerifiedJourneyMessage(	journey.clone(), 
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
