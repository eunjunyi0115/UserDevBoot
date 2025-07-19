package user.dev.core.datasource;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.extern.slf4j.Slf4j;
import user.dev.core.datasource.DataSourceRecords.SingleDatasourceRecord;

@ConfigurationProperties(prefix = "user")
@Slf4j
public class DataSourceProperties {
		
	Map<String,SingleDatasourceRecord> datasourceList;
	
	
	public void setDatasourceList(Map<String,SingleDatasourceRecord> datasourceList) {
		this.datasourceList = datasourceList;
	}
	
	public Map<String,SingleDatasourceRecord> getDatasourceList() {
		return datasourceList;
	}
	
		    	
}
