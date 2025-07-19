package user.dev.core.datasource.config;

import java.sql.SQLException;
import java.util.ArrayList;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;
import user.dev.core.datasource.mybatis.CommonDao;
import user.dev.core.datasource.mybatis.CommonDaoMultiSqlSessionFactory;
import user.dev.core.datasource.mybatis.CommonDaoSqlSessionFactory;
import user.dev.core.datasource.mybatis.MyBatisCommonDao;

@Slf4j
@Configuration
//@ConditionalOnWebApplication
@AutoConfigureAfter({DataSourceAutoconfigure.class})
public class MyBatisCommonDaoAutoConfigure {

	@Bean("commonDao")
	//@ConditionalOnMissingBean
	public CommonDao commonDao(CommonDaoMultiSqlSessionFactory multiSqlSessionFactoryBean) {
		MyBatisCommonDao commonDao = new MyBatisCommonDao();
		
		//사용자익셉션 사용시 익셉션 그대로 발행할 클래스들 정의 프로퍼티에서 획득 하도록 변경 필요.
		Class[] wInnoreExceptions =  new Class[] {SQLException.class};
		commonDao.setWrappingIgnoreExceptions(wInnoreExceptions);
		commonDao.setUseOriginalException(false); //사용자익셉션 사용여부.
		
		//SQLSession 리스트 세팅. ?
		var sqlSessionFactoryList = new ArrayList<CommonDaoSqlSessionFactory>();
		
		//원래는 yml 파일에서 정의도 정보로 생성해야 하지만, 난 datasourcestroes의 키를 그대로 사용한다.
		try {
			for(String key1: multiSqlSessionFactoryBean.keySet()) {
				
				sqlSessionFactoryList.add((CommonDaoSqlSessionFactory)multiSqlSessionFactoryBean.getSqlSessionFactory(key1).getObject());
			}
			commonDao.setSqlSessionFactoryList(sqlSessionFactoryList);
			
			log.info("######################### sqlSessionFactoryList ###################\n{}",sqlSessionFactoryList);
			
			//commonDao.afterPropertiesSet();
		}catch(Exception ex) {
			throw new RuntimeException("SQLSESSIONfACTORY LIST 정의되지 않았다.");
		}
		
		return commonDao;
	}
}
