package user.dev.core.datasource.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import user.dev.core.datasource.CompositeDataSourceTransactionManager;
import user.dev.core.datasource.DataSourceProperties;
import user.dev.core.datasource.DataSourceRecords.DataSourceInfo;
import user.dev.core.datasource.DataSourceRecords.SingleDatasourceRecord;
import user.dev.core.datasource.DataSourceStores;
import user.dev.core.datasource.InterceptedDataSourceTransactionManager;
import user.dev.core.datasource.mybatis.CommonDaoMultiSqlSessionFactory;
import user.dev.core.datasource.mybatis.CommonDaoSqlSessionFactoryBean;
import user.dev.core.datasource.queryloader.AbstractCustomQueryLoader;
import user.dev.core.datasource.queryloader.XmlRuntimeQueryLoader;

@Configuration
@Slf4j
@EnableConfigurationProperties(DataSourceProperties.class)
@EnableTransactionManagement //@Transactional 을 사용하도록 하는 스위치
//@ConditionalOnWebApplication
public class DataSourceAutoconfigure {

	public static String FIRST_DATASOURCE_NAME = "appds";
	
	@Autowired(required = false)
	private AbstractCustomQueryLoader customQueryLoader;
//	@Bean
//	public DataSource getDataSource(DataSource datasource) {
//		log.info("datasource:"+datasource);
//		return datasource;
//	}
	
	@Bean
	public DataSourceStores appCustomerDataSource(DataSourceProperties dataSourceProperties) {
		log.info("dataSourceProperties:"+dataSourceProperties);
		
		DataSourceStores dataSourceStores = new DataSourceStores();
		Map<String,SingleDatasourceRecord> singleDatasourceRecordMap = dataSourceProperties.getDatasourceList(); 
		for(String dataKey: singleDatasourceRecordMap.keySet() ) {
			SingleDatasourceRecord datasourceRecord = singleDatasourceRecordMap.get(dataKey);
			JdbcDataSource datasource = new JdbcDataSource();
			datasource.setURL(datasourceRecord.url());
			datasource.setUser(datasourceRecord.username());
			datasource.setPassword(datasourceRecord.password());
			
			DataSourceInfo drecord = new DataSourceInfo(datasource,
					datasourceRecord.mybatis().mapperLocations(),  //여기 ,로 구분해서 여러개 로케이션에서 가져올수 있게 하면 좋을듯.
					datasourceRecord.mybatis().configLocation(),
					dataKey, 
					datasourceRecord.alias(), //업무에서 데이터 소스 획득시 사용할 alias 
					datasourceRecord.primary());
			dataSourceStores.put(dataKey, drecord);
		}
		return dataSourceStores;
	}
	
	@Bean
	@Primary
	public DataSource dataSource(DataSourceStores dataSourceStores) {
		for(String key: dataSourceStores.keySet()) {
			if( dataSourceStores.get(key).primary()) {
				return dataSourceStores.get(key).dataSource();
			}
		}
		return dataSourceStores.get(FIRST_DATASOURCE_NAME).dataSource();
	}
	
	@Bean("transactionManager")
	@Primary
	@ConditionalOnWebApplication
	public CompositeDataSourceTransactionManager transactionManager(DataSourceStores dataSourceStores) {
		CompositeDataSourceTransactionManager txManahger = 	new InterceptedDataSourceTransactionManager();
		try {
			List<DataSource> datasourceList = new ArrayList<DataSource>();
			for(String key: dataSourceStores.keySet()) {
				datasourceList.add(dataSourceStores.get(key).dataSource());
			}
			txManahger.setDataSources(datasourceList);
		}catch(Exception ex) {
			throw new RuntimeException("복합 트랜잭션 메니저 생성중 오류",ex);
		}
		return txManahger;
	}
	
	//배치는 JPATRANSACTIONMANAGHER 이 기본 트랜잭션 메니저이다.
	@Bean("transactionManager")
	@ConditionalOnNotWebApplication
    public PlatformTransactionManager jpaTransactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
	
	
	//멀티 세션 기반으로 하는거야 무조건
	//원래 CommonDaoSqlSessionFactoryAutoConfiguration 에서 하는데 이것을 여기서 datasource명으로 바로 되도록 했다.
	@Bean
	public CommonDaoMultiSqlSessionFactory commonDaoMultiSqlSessionFactory(
			DataSourceStores dataSourceStores, ApplicationContext ac){ //MultiCommonDaoSqlSessionFactoryuProperties)
		
		CommonDaoMultiSqlSessionFactory commonDaoMultiSqlSessionFactory = new CommonDaoMultiSqlSessionFactory();
		
		for(String key: dataSourceStores.keySet()) {
			String mapperLocation = dataSourceStores.get(key).mapperLocation();
			String configLocations = dataSourceStores.get(key).configLocation();
			DataSource datasource = dataSourceStores.get(key).dataSource();
			
			CommonDaoSqlSessionFactoryBean sqlSessionFactoryBean = new CommonDaoSqlSessionFactoryBean();
			sqlSessionFactoryBean.setBeanName(key);
			sqlSessionFactoryBean.setDataSource(datasource);
			sqlSessionFactoryBean.setDevelopmentMode(true); //개발모드로 하자 
			sqlSessionFactoryBean.setCacheSeconds(60); //60초 마다 갱신.
			sqlSessionFactoryBean.setPrimary(dataSourceStores.get(key).primary()); //primary 여부
			try {
				PathMatchingResourcePatternResolver resolve = new PathMatchingResourcePatternResolver();
				sqlSessionFactoryBean.setConfigLocation(resolve.getResource(configLocations));
				//쿼리 로더 세팅.
				if(this.customQueryLoader != null) {
					sqlSessionFactoryBean.setCustomQueryLoader(customQueryLoader);
				}else {
					if(mapperLocation==null || mapperLocation.equals("")) {
						throw new RuntimeException("매퍼 위치가 정의 되지 않았다");
					}
					//, 구분으로 여러 매퍼 경로가 설정됨.
					String[] locations = StringUtils.commaDelimitedListToStringArray(StringUtils.trimAllWhitespace(mapperLocation)); 
					sqlSessionFactoryBean.setMapperLocations(locations);
				}
				
			}catch(Throwable e) {
				throw new RuntimeException("세션 생성중 오류 발생 했다.",e);
			}
			
			commonDaoMultiSqlSessionFactory.setCommonDaoSqlSessionFactoryBean(key, sqlSessionFactoryBean);
			//commonDaoMultiSqlSessionFactory.setCommonDaoSqlSessionFactoryBean(dataSourceStores.get(key).alias(), sqlSessionFactoryBean); alise로 업무에서 사용하게 할지???
		}
		
		log.info("######################### CommonDaoSqlSessionFactoryBean #########################\n{}",commonDaoMultiSqlSessionFactory.getCommonDaoSqlSessionFactoryBeanMap());
		
		return commonDaoMultiSqlSessionFactory;
	}
	
	
	
	@Bean
	public CommonDaoSqlSessionFactoryBean CommonDaoSqlSessionFactory(CommonDaoMultiSqlSessionFactory commonDaoMultiSqlSessionFactory, ApplicationContext ac) {
		return commonDaoMultiSqlSessionFactory.getSqlSessionFactory(FIRST_DATASOURCE_NAME);
	}
	
	
	
}
