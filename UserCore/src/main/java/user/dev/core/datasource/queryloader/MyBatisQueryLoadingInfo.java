package user.dev.core.datasource.queryloader;

import lombok.Data;

@Data
public class MyBatisQueryLoadingInfo {

	private String namespace;
	private String stmtId;
	private String xmlQuery;
	private String resultMapId;

	public MyBatisQueryLoadingInfo(String namespace, String stmtId) {
		this.namespace = namespace;
		this.stmtId = stmtId;
	}
}
