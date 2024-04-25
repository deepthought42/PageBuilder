package com.looksee.pageBuilder.models.message;

import com.looksee.pageBuilder.models.enums.BrowserType;
import com.looksee.pageBuilder.models.enums.PathStatus;
import com.looksee.pageBuilder.models.journeys.Journey;

import lombok.Getter;
import lombok.Setter;


/**
 * 
 */
public class JourneyMessage extends Message {

	private Journey journey;
	private PathStatus status;
	private BrowserType browser;

	@Getter
	@Setter
	private long auditRecordId;

	public JourneyMessage(Journey journey,
					   PathStatus status, 
					   BrowserType browser_type, 
					   long account_id, 
					   long audit_record_id)
	{
		super(account_id);
		setJourney(journey);
		setStatus(status);
		setBrowser(browser_type);
		setAuditRecordId(audit_record_id);
	}
	
	public JourneyMessage clone(){
		return new JourneyMessage(journey.clone(),
								  getStatus(), 
								  getBrowser(),
								  getAccountId(),
								  getAuditRecordId());
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
