package com.looksee.pageBuilder.models.message;

import com.looksee.pageBuilder.models.enums.BrowserType;

/**
 * Message for different audit actions to perform and which audit types to perform them for.
 * 
 */
public class UrlMessage extends Message{
	private String url;
	private BrowserType browser;
	private long pageAuditId;

	public UrlMessage() {}
	
	public UrlMessage(String url, 
					  BrowserType browser,
					  long page_audit_id,
					  long domain_id, 
					  long account_id, 
					  long audit_record_id)
	{
		setUrl(url);
		setBrowser(browser);
		setPageAuditRecordId(page_audit_id);
		setDomainId(domain_id);
		setAccountId(account_id);
		setDomainAuditRecordId(audit_record_id);
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public BrowserType getBrowser() {
		return browser;
	}

	private void setBrowser(BrowserType browser) {
		this.browser = browser;
	}

	public long getPageAuditRecordId() {
		return pageAuditId;
	}

	public void setPageAuditRecordId(long pageAuditId) {
		this.pageAuditId = pageAuditId;
	}
}
