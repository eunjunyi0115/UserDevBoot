package user.dev.core.datasource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;

import lombok.Data;
import user.dev.core.datasource.DataSourceRecords.DataSourceInfo;

@Data
public class DataSourceStores extends HashMap<String, DataSourceInfo>{
	public static final String DEFAULT_KEY = "default";
	
	public void initTransactionManager(CompositeDataSourceTransactionManager txManager) {
		Collection<DataSourceInfo> datasourceInfos = values();
		int size = datasourceInfos.size();
		if(size==0) {
			throw new RuntimeException("There is no such dataSource bean");
		}
		txManager.setDataSources(datasourceInfos.stream().map(di->di.dataSource()).collect(Collectors.toList()));
	}
}
