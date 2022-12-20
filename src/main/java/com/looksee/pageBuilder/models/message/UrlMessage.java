package com.looksee.pageBuilder.models.message;


/**
 * Message for different audit actions to perform and which audit types to perform them for.
 * 
 */
public class UrlMessage extends Message {
	
	private long pageAuditId;
	private String url;
	
	public UrlMessage() {}
	
	public UrlMessage( long domain_id, 
				  	   long account_id,
				  	   long domain_audit_id,
				  	   long page_audit_id, 
				  	   String url)
	{
		super(account_id, domain_id, domain_audit_id);
		setUrl(url);
		setPageAuditId(page_audit_id);
	}

	public long getPageAuditId() {
		return pageAuditId;
	}

	public void setPageAuditId(long page_audit_record_id) {
		this.pageAuditId = page_audit_record_id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
