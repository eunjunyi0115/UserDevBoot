package user.dev.core.datasource;

import javax.sql.DataSource;

public class DataSourceRecords {
	public static record DataSourceInfo(DataSource dataSource, String mapperLocation, String configLocation, String key, String alias, boolean primary) {
		public DataSourceInfo(DataSource dataSource, String mapperLocation, String configLocation, String key, String alias) {
			this( dataSource,  mapperLocation,  configLocation,  key, alias , false );
		}
	};

	public static record SingleDatasourceRecord(String url,String driverClassName,String username,String password, String alias, MyBatisRecord mybatis, boolean primary) {};
	
	public static record MyBatisRecord(String configLocation, String mapperLocations) {};

}
