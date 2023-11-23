package com.looksee.pageBuilder.models.message;

/**
 * Error message to be sent whenever an exception occurs performing 
 *   page data extraction. Mostly used for observability and analytics
 */
public class PageDataExtractionError extends Message {
	private String url;
	private String errorMessage;
	private long audit_record_id;
	
	public PageDataExtractionError(long accountId, 
								   long auditRecordId, 
								   String url, 
								   String error_message) {
		super(accountId);
		setUrl(url);
		setErrorMessage(error_message);
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String error_message) {
		this.errorMessage = error_message;
	}

	public long getAuditRecordId() {
		return audit_record_id;
	}

	public void setAuditRecordId(long audit_record_id) {
		this.audit_record_id = audit_record_id;
	}

}
