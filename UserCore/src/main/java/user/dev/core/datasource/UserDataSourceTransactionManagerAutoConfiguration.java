package user.dev.core.datasource;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(value = { CompositeDataSourceTransactionManager.class })
@ConditionalOnMissingBean(CompositeDataSourceTransactionManager.class )
@EnableTransactionManagement
public class UserDataSourceTransactionManagerAutoConfiguration {
	@Primary
	@Bean("composeTransactionManager")
	//@ConditionalOnProperty(name = DataSourceStore.DATAINFO_PATH +".enabled", havingValue = "true")
	@ConditionalOnBean(value = { DataSourceStores.class })
	public CompositeDataSourceTransactionManager devonomposeTransactionManager(DataSourceStores devonDataSourceStore) {
		CompositeDataSourceTransactionManager txManager = new InterceptedDataSourceTransactionManager();
		try {
			List<DataSource> dataSourceList = new ArrayList<>();
			/** 데이터 소스 자동화처리 추가 LeeEunJun */
			devonDataSourceStore.initTransactionManager(txManager);
		} catch (Exception ex) {
			throw ex;
		}
		return txManager;
	}

	
	/**
	 * CompositeDataSourceTransactionManager 생성
	 *
	 * @param properties
	 * @param context
	 * @return
	 */
	@Primary
	@Bean
	//@ConditionalOnDefaultProperties(prefix = PREFIX, value = CompDataSourceTransactionManagerProperties.class)
	@ConditionalOnMissingBean(value = { DataSourceStores.class })
	public CompositeDataSourceTransactionManager composeTransactionManager(ApplicationContext context) {
		CompositeDataSourceTransactionManager txManager = new InterceptedDataSourceTransactionManager();

		try {
			List<DataSource> dataSourceList = new ArrayList<>();
//			MultiDataSourceStore multiDataSourceStore = context.getBean(MultiDataSourceAutoConfiguration.MULTI_DATASOURCE_STORE, MultiDataSourceStore.class);
//			//find datasources matching with datasource bean name
//			
//			/**
//			 * 데이터 소스 자동화처리 추가 LeeEunJun
//			 */
//			for (String dataSourceName : properties.getDataSourceList()) {
//				DataSource dataSource = null;
//				try {
//					dataSource = (DataSource)context.getBean(dataSourceName);
//				} catch (NoSuchBeanDefinitionException ex) {
//					log.info("Context DataSource no Bean:"+ ex.toString());
//				}
//				if(dataSource==null) {
//					dataSource = multiDataSourceStore.getDataSource(ShsCommonDaoSqlSessionFactoryAutoConfiguration.PARENT_KEY, dataSourceName);
//				}
//				if(dataSource==null) {
//					throw new DevonException("There is no such dataSource bean (name: " + dataSourceName + ")");
//				}
//				dataSourceList.add(dataSource);
////			    } catch (NoSuchBeanDefinitionException ex) {
////					throw new DevonException("There is no such dataSource bean (name: " + dataSourceName + ")", ex);
////				}
//			}
//			txManager.setDataSources(dataSourceList.toArray(new DataSource[dataSourceList.size()]));
		} catch (Exception ex) {
			throw ex;
		}

		return txManager;
	}
}
