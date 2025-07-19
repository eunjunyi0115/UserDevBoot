package user.dev.core.datasource.mybatis;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;

public class CommonDaoSqlSessionFactory extends DefaultSqlSessionFactory {

	private final CommonDaoSqlSessionFactoryBean sqlSessionFactoryBean;
	private String beanName;
	private ExecutorType executorType;
	
	public CommonDaoSqlSessionFactory(Configuration configuration,CommonDaoSqlSessionFactoryBean sqlSessionFactoryBean) {
		super(configuration);
		this.sqlSessionFactoryBean = sqlSessionFactoryBean;
		if(sqlSessionFactoryBean.getExecutorType()==null) {
			sqlSessionFactoryBean.setExecutorType(configuration.getDefaultExecutorType().name());
		}
	}
	
	public CommonDaoSqlSessionFactoryBean getSqlSessionFactoryBean() {
		return sqlSessionFactoryBean;
	}
	
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}
	
	public String getBeanName() {
		return beanName;
	}
	
	public ExecutorType getExecutorType() {
		if(this.executorType == null) {
			this.executorType = sqlSessionFactoryBean.getExecutorType();
		}
		if(this.executorType != null) {
			return executorType;
		}else {
			return getConfiguration().getDefaultExecutorType(); //이부분 수행 안됨.
		}
	}
}
