package com.looksee.pageBuilder.models.message;

/**
 * Message used to indicate that a domain page has been built and data extracted
 */
public class PageBuiltMessage extends Message{
	private long pageId;
	private long pageAuditRecordId;
	
	public PageBuiltMessage() {
		super(-1);
	}
	
	public PageBuiltMessage(long account_id,
							long page_id, 
							long page_audit_record_id) 
	{
		super(account_id);
		setPageAuditRecordId(page_audit_record_id);
		setPageId(page_id);
	}
	
	public long getPageId() {
		return pageId;
	}
	public void setPageId(long page_id) {
		this.pageId = page_id;
	}

	public long getPageAuditRecordId() {
		return pageAuditRecordId;
	}

	public void setPageAuditRecordId(long pageAuditRecordId) {
		this.pageAuditRecordId = pageAuditRecordId;
	}

}
