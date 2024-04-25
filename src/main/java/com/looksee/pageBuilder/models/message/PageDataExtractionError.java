package com.looksee.pageBuilder.models.message;

import lombok.Getter;
import lombok.Setter;

public class PageDataExtractionError extends Message {
	private String url;
	private String errorMessage;
	
	@Getter
	@Setter
	private long auditRecordId;

	public PageDataExtractionError(long accountId, 
								   long auditRecordId, 
								   String url, 
								   String error_message) {
		super(accountId);
		setUrl(url);
		setAuditRecordId(auditRecordId);
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

}
