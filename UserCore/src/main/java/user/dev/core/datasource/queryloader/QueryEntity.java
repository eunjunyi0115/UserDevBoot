package user.dev.core.datasource.queryloader;

import lombok.Data;

/**
 * 쿼리를 db로 관리 하기 위한 모델
 */

@Data
public class QueryEntity {
	private String namespace;
	private String stmtId;
	private String stmtCnt;
	
	public String getQueryId() {
		return namespace+"."+stmtId;
	}
}
