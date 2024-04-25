package com.looksee.pageBuilder.models.message;

/**
 * 
 * 
 */
public class PageAuditMessage extends Message {
	private long page_audit_id;
	private long page_id;
	
	public PageAuditMessage() {
		super(-1);
	}
	
	public PageAuditMessage(long account_id,
							long page_id,
							long page_audit_id
	) {
		super(account_id);
		setPageAuditId(page_audit_id);
		setPageId(page_id);
	}

	public long getPageAuditId() {
		return page_audit_id;
	}

	public void setPageAuditId(long page_audit_id) {
		this.page_audit_id = page_audit_id;
	}

	public long getPageId() {
		return page_id;
	}

	public void setPageId(long page_id) {
		this.page_id = page_id;
	}
	
	
}
