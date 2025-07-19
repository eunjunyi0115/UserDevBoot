package user.dev.core.datasource.mybatis;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CommonDaoMultiSqlSessionFactory {

	private Map<String,CommonDaoSqlSessionFactoryBean> commonDaoSqlSessionFactoryBeanMap = new HashMap<String,CommonDaoSqlSessionFactoryBean>();
	
	public Map<String,CommonDaoSqlSessionFactoryBean> getCommonDaoSqlSessionFactoryBeanMap(){
		return commonDaoSqlSessionFactoryBeanMap;
	}
	
	public void setCommonDaoSqlSessionFactoryBeanMap(Map<String,CommonDaoSqlSessionFactoryBean> commonDaoSqlSessionFactoryBeanMap) {
		this.commonDaoSqlSessionFactoryBeanMap = commonDaoSqlSessionFactoryBeanMap;
	}
	
	public CommonDaoSqlSessionFactoryBean getSqlSessionFactory(String key) {
		return commonDaoSqlSessionFactoryBeanMap.get(key);
	}
	
	public void setCommonDaoSqlSessionFactoryBean(String key,CommonDaoSqlSessionFactoryBean factoryBean) {
		this.commonDaoSqlSessionFactoryBeanMap.put(key, factoryBean);
	}
	
	public Set<String> keySet(){
		return commonDaoSqlSessionFactoryBeanMap.keySet();
	}
	
}
