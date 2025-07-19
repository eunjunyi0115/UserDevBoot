package user.dev.core.datasource.mybatis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.RuntimeErrorException;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;
import user.dev.core.datasource.queryloader.QueryEntity;
import user.dev.core.datasource.queryloader.RuntimeQueryLoader;
import user.dev.core.datasource.queryloader.XmlRuntimeQueryLoader;

//@Repository
@Slf4j
public class MyBatisCommonDao implements CommonDao , InitializingBean {

	public static final int BATCH_UPDATE_RETURN_VALUE = Integer.MIN_VALUE+1002;
	
	protected CommonDaoSqlSessionFactory sqlSessionFactory; //mybatis 설정에 <property name="sqlSessionFactory"> 설정한 경우 세팅 
	
	protected boolean useOriginalException = false;
	
	protected Class[] wrappingIgnoreExceptions;
	
	protected List<CommonDaoSqlSessionFactory> sqlSessionFactoryList;
	
	private Map<String, CommonDaoSqlSessionFactory> sqlSessionFactoryMap = new HashMap<String, CommonDaoSqlSessionFactory>();
	
	protected Map<String, SqlSession> localSqlSessionMap = new HashMap<String,SqlSession>();
	
	private String defaultSqlSessionKey = "appds";
	private static final String POSTFIX_FOR_ADDBATCH = "_ADD_BATCH";
	protected SqlSessionTemplate session;
	protected ExceptionWrapper exceptionWrapper;
	public static final int ADD_BATCH_ALREADY_EXECUTE = -9;
	
	//Query refresh 기능을 구현하는 데 필요한 SessionFactory List 객체에 대한 set
	public void setSqlSessionFactoryList(List<CommonDaoSqlSessionFactory> sqlSessionFactoryList) {
		this.sqlSessionFactoryList = sqlSessionFactoryList;
	}
	
	//Query refresh 기능을 구현하는 데 필요한 SessionFactory 객체에 대한 set
	public void setSqlSessionFactory(CommonDaoSqlSessionFactory sqlSessionFactory) {
		this.sqlSessionFactory = sqlSessionFactory;
	}
	
	//was 시작시점에 CommonDaoSqlSession 객체와 페이징Executor객체를 생성한다.
	public void afterPropertiesSet() throws Exception {
		if(sqlSessionFactory ==null && sqlSessionFactoryList == null) {
			throw new RuntimeException("sqlSessionFactory 와 sqlSessionFactoryList 가 세팅되지 않았다");
		}
		if(sqlSessionFactory !=null && sqlSessionFactoryList != null) {
			throw new RuntimeException("sqlSessionFactory 와 sqlSessionFactoryList 가 둘다 세팅될수 없다.");
		}
		
		if(this.sqlSessionFactory != null) {
			defaultSqlSessionKey = sqlSessionFactory.getBeanName();
			this.sqlSessionFactoryMap.put(defaultSqlSessionKey, sqlSessionFactory);
			localSqlSessionMap.put(defaultSqlSessionKey, new CommonDaoSqlSession(sqlSessionFactory));
		}else {
			if(this.sqlSessionFactoryList !=null && this.sqlSessionFactoryList.size()>0) {
				int count =0;
				for(int index=0;index<sqlSessionFactoryList.size() ; index++) {
					CommonDaoSqlSessionFactory sqlSessionFactory = this.sqlSessionFactoryList.get(index);
					String sqlSessionfactorybeanName = sqlSessionFactory.getBeanName();
					
					if(sqlSessionFactory.getSqlSessionFactoryBean().getPrimary()) {
						defaultSqlSessionKey = sqlSessionfactorybeanName;
					}
//					if(count ==0) {
//						defaultSqlSessionKey = sqlSessionfactorybeanName;
//					}
					this.sqlSessionFactoryMap.put(sqlSessionfactorybeanName, sqlSessionFactory);
					this.localSqlSessionMap.put(sqlSessionfactorybeanName, new CommonDaoSqlSession(sqlSessionFactory));
					count++;
				}
				
				log.info("######################### sqlSessionFactoryMap ###################\n{}",sqlSessionFactoryMap);
				log.info("######################### localSqlSessionMap ###################\n{}",localSqlSessionMap);
			}else {
				throw new RuntimeException("CommonDao property(SqlSessionFactoryList) is Nothing");
			}
		}
		if(this.exceptionWrapper == null) {
			this.exceptionWrapper = new CommonDaoExceptionWrapper();
		}
	}
	
	/**
	 * jdbc의 addBatch, execteBatch 사용을 위해서 fw 에서 내부적으로 추가하는 sqlsession
	 * spring mybatis 연동시 sqlsessionFactory의 Executor type을 중간에 변경할 경우
	 * sqlsessionUtils의 getSqlSession 수행시 Exception이 발생하기 때문에 sqlsessionFactory의 짝으로 
	 * add BATCH용 SQLSESSIONfACTORY 를 생성한후 cOMMONDAO SQLSESSION을 생성하도록 한다.
	 * 
	 * @param sqlSessionKey ExecutrorType이 simple, RESUSE용인 SQLsEssionKey명
	 * @param sqlSessionFactory ExecutrorType이 simple, RESUSE용인 sqlSessionFactory 객체
	 */
	private void makeSqlSessionForAddBatch(String sqlSessionKey, CommonDaoSqlSessionFactory sqlSessionFactory) {
		if(sqlSessionFactory.getExecutorType() == ExecutorType.BATCH) {
			//원 SQLSESSIONfACTORY 의 EXECUTORTYPE이 이미 BATCH 이면 그대로 사용.
			localSqlSessionMap.put(sqlSessionKey+POSTFIX_FOR_ADDBATCH, localSqlSessionMap.get(sqlSessionKey));
		}else {
			CommonDaoSqlSessionFactoryBean factoryBean = sqlSessionFactory.getSqlSessionFactoryBean();
			CommonDaoSqlSessionFactoryBean addBatchFactoryBean = new CommonDaoSqlSessionFactoryBean();
			
			addBatchFactoryBean.setDataSource(factoryBean.getDataSource());
			addBatchFactoryBean.setConfigLocation(addBatchFactoryBean.getConfigLocation());
			if( factoryBean.getCustomQueryLoader() == null) {
				//mybatis xml 로 서버 기동시 모두 로딩 한다는 의미
				//add batch를 위해서 다시 모든 쿼리를 xml에서 로딩하는 것은 부담이므로 runtime쿼리 로딩을 하도록 세팅
				XmlRuntimeQueryLoader xmlRuntimeQueryLoader = new XmlRuntimeQueryLoader();
				xmlRuntimeQueryLoader.setMapperLocations(factoryBean.convertToResourceArray(factoryBean.getMapperLocations()));
				xmlRuntimeQueryLoader.setMapperFileResolver(factoryBean.getMapperFileResolverForAddBatch());
				addBatchFactoryBean.setCustomQueryLoader(xmlRuntimeQueryLoader);
			}else if(factoryBean.getCustomQueryLoader() instanceof RuntimeQueryLoader) {
				addBatchFactoryBean.setCustomQueryLoader(factoryBean.getCustomQueryLoader());
			}else {
				//CustomerQueryLoader 을 설정 했으나 RuntimeQueyrLoader 이 아닌경우임.db 일괄 쿼리 로딩
				//DBQueryLoader
			}
			
			addBatchFactoryBean.setExecutorType(ExecutorType.BATCH.name()); //ADD BATCH 를 위한 것이므로  BATCH TYPE이여야함
			CommonDaoSqlSessionFactory factory = new CommonDaoSqlSessionFactory(sqlSessionFactory.getConfiguration(),addBatchFactoryBean);
			localSqlSessionMap.put(sqlSessionKey+POSTFIX_FOR_ADDBATCH, new CommonDaoSqlSession(factory));
		}
	}
	
	
	/**
	 * List의 첫번쨰 sqlSessionFactoryName 또는 sqlSessionFactory에 선언되 sqlSessioNFactory의
	 * sqlSession 객체를 가져오는 getter
	 * @return
	 */
	private SqlSession getSqlSession() {
		return getSqlSession(defaultSqlSessionKey,false);
	}
	//sqlSessionFactoryName 에 의한 sqlSession객체를 가져오는 getter method
	private SqlSession getSqlSession(String sqlSessionFactoryName) {
		return getSqlSession(sqlSessionFactoryName,false);
	}
	
	/**
	 * sqlSessionFactoryName 에 의ㅏ한 sqlSession객체를 가조오고는 get
	 * isAddBatch가 true 이면 batchSession을 리턴, 존재 하지 않으면 makeSqlSessionForAddBatch로 생성
	 * @param sqlSessionFactoryName sqlserssionfactory명
	 * @param isAddbatch batch세션 사용여부
	 * @return
	 */
	private SqlSession getSqlSession(String sqlSessionFactoryName, boolean isAddbatch) {
		if(sqlSessionFactoryName==null) {
			return getSqlSession();
		}
		if(this.localSqlSessionMap ==null || !localSqlSessionMap.containsKey(sqlSessionFactoryName)) {
			throw new RuntimeException("localSqlSessionMap 에 존재 하지 않는다["+sqlSessionFactoryName+"] ");
		}
		if(isAddbatch) {
			String addBatchFoctoryName = sqlSessionFactoryName+POSTFIX_FOR_ADDBATCH;
			if(!this.localSqlSessionMap.containsKey(addBatchFoctoryName)) {
				makeSqlSessionForAddBatch(sqlSessionFactoryName, sqlSessionFactoryMap.get(sqlSessionFactoryName));
			}
			return this.localSqlSessionMap.get(addBatchFoctoryName);
		}else {
			return this.localSqlSessionMap.get(sqlSessionFactoryName);
		}
	}
	
	/**
	 * 마이바티스 자체 익셉션을 사용자 익셉션으로 레핑 할지 여부
	 * @param useOriginalException
	 */
	public void setUseOriginalException(boolean useOriginalException) {
		this.useOriginalException = useOriginalException;
	}
	
	public void setExceptionWrapper(ExceptionWrapper exceptionWrapper) {
		this.exceptionWrapper = exceptionWrapper;
	}
	
	private void executeValidationBeforeQuery() {
		//if(!ValidDuration)
	}
	
	//사용자 익셉션 래핑 하지 않고 기본 그대로 출력될 익셉션들 정의
	public void setWrappingIgnoreExceptions(Class[] wrappingIgnoreExceptions) {
		this.wrappingIgnoreExceptions = wrappingIgnoreExceptions;
	}
	
	protected RuntimeException translateException(RuntimeException exception) {
		
		if(!useOriginalException) {
			if(wrappingIgnoreExceptions!=null) {
				for(int i=0;i<wrappingIgnoreExceptions.length;i++) {
					//익셉션을 변환하지 않고 그대로 발
					if(wrappingIgnoreExceptions[i].isAssignableFrom(exception.getClass())) {
						throw exception;
					}
				}
			}			
		}
		
		return this.exceptionWrapper.execute(exception);
		
		
	}
	
	/**
	 * 리미트 제한건수 초과 여부체크
	 * @param <E>
	 * @param result
	 * @param queryId
	 * @param limitCountRowBounds
	 */
	protected <E> void checkRowBoundsException(List<E> result, String queryId, LimitCountRowBounds limitCountRowBounds) {
		if(result.size()> limitCountRowBounds.getLimit()) {
			if(limitCountRowBounds.isDataCut()) {
				result.remove(result.size()-1);
			}else {
				throw new RuntimeException("데이터 건수가 "+limitCountRowBounds.getLimit()+"를 넘어 섰습니다.");
			}
		}
	}
	
	/**
	 * prepareRefresh() 메소드를 통해서 임시로 로딩된 refresh쿼리를 메모리에서 제거한다.
	 * @param oldBean
	 */
	public void afterRollbackRefresh(Object oldBean) {
		if(this.localSqlSessionMap!=null && this.localSqlSessionMap.size()>0) {
			for(String localSqlSessionMapKey: this.localSqlSessionMap.keySet()) {
				SqlSession sqlSession = this.localSqlSessionMap.get(localSqlSessionMapKey);
				if(sqlSession instanceof CommonDaoSqlSession csqlses) {
					csqlses.rollbackRefresh();
				}else {
					throw new RuntimeException("sqlSessionFactory 타입이 CommonDaoSqlSession 이 아닙니다.");
				}
			}
		}
	}
	
	/**
	 * prepareRefresh(), prepareRefreshOfQueryList() 메소드를 통해서 임시로 로딩된 refresh쿼리를 실제로 반영한다.
	 * 메소드 호출이 되어야만 실제 refresh반영.
	 * 단, RuntimeQueryLoading의 경우에는 이 메소드를 호출해도 아무런 영향이 없다.
	 * @param oldBean refresh되기 이전 bean객체
	 */
	public void afterConfirmRefresh(Object oldBean) {
		if(this.localSqlSessionMap!=null && this.localSqlSessionMap.size()>0) {
			for(String localSqlSessionMapKey: this.localSqlSessionMap.keySet()) {
				SqlSession sqlSession = this.localSqlSessionMap.get(localSqlSessionMapKey);
				if(sqlSession instanceof CommonDaoSqlSession csqlses) {
					csqlses.confirmRefresh();
				}else {
					throw new RuntimeException("sqlSessionFactory 타입이 CommonDaoSqlSession 이 아닙니다.");
				}
			}
		}
	}
	
	/**
	 * 사용자 요청에 따라 변경된 쿼리 내용을 찿아서 새로 로딩할 준비를 한다.,
	 * @param sqlSessionFactoryName 새로 로딩할 sqlsessionfactory명
	 * @param reqList 새로 로딩할 대상 쿼리 정보 list
	 * @throws Exception
	 */
	public void prepareRefreshOfQueryList(String sqlSessionFactoryName, List<QueryEntity> reqList) throws Exception {
		SqlSession localSqlSession = getSqlSession(sqlSessionFactoryName);
		if(localSqlSession instanceof CommonDaoSqlSession csqlsession) {
			csqlsession.prepareRefresh(reqList);
		}else {
			throw new RuntimeException("sqlSessionFactory 타입이 CommonDaoSqlSession 이 아닙니다.");
		}
	}
	
	public void prepareRefreshOfQueryList( List<QueryEntity> reqList) throws Exception {
		prepareRefreshOfQueryList(defaultSqlSessionKey,reqList);
	}
	
	
	public void afterPrepareRefresh(Object oldBean) throws Exception{
		if(this.localSqlSessionMap!=null && this.localSqlSessionMap.size()>0) {
			for(String localSqlSessionMapKey: this.localSqlSessionMap.keySet()) {
				SqlSession sqlSession = this.localSqlSessionMap.get(localSqlSessionMapKey);
				if(sqlSession instanceof CommonDaoSqlSession csqlses) {
					csqlses.prepareRefresh();
				}else {
					throw new RuntimeException("sqlSessionFactory 타입이 CommonDaoSqlSession 이 아닙니다.");
				}
			}
		}
	}

	@Override
	public <T> T select(String queryId) {
		executeValidationBeforeQuery();
		return getSqlSession().selectOne(queryId);
	}

	@Override
	public <T> T select(String queryId, Object parameter) {
		return getSqlSession().selectOne(queryId,parameter);
	}

	@Override
	public <T> T select(String queryId, Object parameter, String spec) {
		return getSqlSession(spec).selectOne(queryId,parameter);
	}

	@Override
	public <E> List<E> selectList(String queryId) {
		executeValidationBeforeQuery();
		return getSqlSession().selectList(queryId);
	}

	@Override
	public <E> List<E> selectList(String queryId, Object parameter) {
		return getSqlSession().selectList(queryId,parameter);
	}

	@Override
	public <E> List<E> selectList(String queryId, Object parameter, String spec) {
		return getSqlSession(spec).selectList(queryId,parameter);
	}

	@Override
	public <E> List<E> selectList(String queryId, Object parameter, RowBounds rowBounds) {
		return getSqlSession().selectList(queryId,parameter,rowBounds);
	}

	@Override
	public <E> List<E> selectList(String queryId, Object parameter, String spec, RowBounds rowBounds) {
		if(rowBounds instanceof LimitCountRowBounds && rowBounds.getLimit() != Integer.MAX_VALUE) {
			LimitCountRowBounds limit = (LimitCountRowBounds)rowBounds;
			RowBounds plusOneRowBounds = new RowBounds(RowBounds.NO_ROW_OFFSET,limit.getLimit()+1);
			
			List<E> result = getSqlSession(spec).selectList(queryId,parameter,plusOneRowBounds);
			checkRowBoundsException(result, queryId, limit);
			return result;
		}else {
			return getSqlSession(spec).selectList(queryId,parameter,rowBounds);
		}
	}

	@Override
	public <T> void selectList(String queryId, ResultHandler<T> handler) {
		this.selectList(queryId,null,handler);
	}

	@Override
	public <T> void selectList(String queryId, ResultHandler<T> handler, String spec) {
		this.selectList(queryId,null,handler,spec);
	}

	@Override
	public <T> void selectList(String queryId, Object parameter, ResultHandler<T> handler) {
		this.selectList(queryId,parameter,handler,defaultSqlSessionKey);
	}

	@Override
	public <T> void selectList(String queryId, Object parameter, ResultHandler<T> handler, String spec) {
		if(handler instanceof IStreamResultHandler ish) ish.preHandle();
		getSqlSession(spec).select(queryId, parameter, handler);
		if(handler instanceof IStreamResultHandler ish) ish.postHandle();
	}

	@Override
	public int insert(String queryId) {
		return getSqlSession().insert(queryId);
	}

	@Override
	public int insert(String queryId, Object parameter) {
		return getSqlSession().insert(queryId,parameter);
	}

	@Override
	public int insert(String queryId, Object parameter, String spec) {
		return getSqlSession(spec).insert(queryId,parameter);
	}

	@Override
	public int update(String queryId) {
		return getSqlSession().update(queryId);
	}

	@Override
	public int update(String queryId, Object parameter) {
		return getSqlSession().update(queryId,parameter);
	}

	@Override
	public int update(String queryId, Object parameter, String spec) {
		return getSqlSession(spec).update(queryId,parameter);
	}

	@Override
	public int delete(String queryId) {
		return getSqlSession().delete(queryId);
	}

	@Override
	public int delete(String queryId, Object parameter) {
		return getSqlSession().delete(queryId,parameter);
	}

	@Override
	public int delete(String queryId, Object parameter, String spec) {
		return getSqlSession(spec).delete(queryId,parameter);
	}

	@Override
	public int batchInsert(String queryId, List<?> parameterList) {
		return batchInsert(queryId,parameterList,defaultSqlSessionKey,false);
	}

	@Override
	public int batchInsert(String queryId, List<?> parameterList, boolean useAddBatch) {
		return batchInsert(queryId,parameterList,defaultSqlSessionKey,useAddBatch);
	}

	@Override
	public int batchInsert(String queryId, List<?> parameterList, String spec) {
		return batchInsert(queryId,parameterList,spec,false);
	}

	@Override
	public int batchInsert(String queryId, List<?> parameterList, String spec, boolean useAddBatch) {
		int count =0;
		Object parameter = null;
		if(parameterList ==null || parameterList.size()==0){
			return 0;
		}
		if(!useAddBatch) {
			for(int i=0; i<parameterList.size();i++) {
				parameter = parameterList.get(i);
				count += getSqlSession(spec).insert(queryId, parameter);
			}
		}else { //addBatch를 사용하는 경우
			SqlSession session = getSqlSession(spec, true);
			for(int i=0; i<parameterList.size();i++) {
				parameter = parameterList.get(i);
				session.insert(queryId, parameter);
			}
			count = executeBatch(spec);
		}
		return count;
	}
	
	private int executeBatch(String spec) {
		return BATCH_UPDATE_RETURN_VALUE;
	}

	@Override
	public int batchUpdate(String queryId, List<?> parameterList) {
		return batchUpdate(queryId,parameterList,defaultSqlSessionKey,false);
	}

	@Override
	public int batchUpdate(String queryId, List<?> parameterList, boolean useAddBatch) {
		return batchUpdate(queryId,parameterList,defaultSqlSessionKey,useAddBatch);
	}

	@Override
	public int batchUpdate(String queryId, List<?> parameterList, String spec) {
		return batchUpdate(queryId,parameterList,spec,false);
	}

	@Override
	public int batchUpdate(String queryId, List<?> parameterList, String spec, boolean useAddBatch) {
		int count =0;
		Object parameter = null;
		if(parameterList ==null || parameterList.size()==0){
			return 0;
		}
		if(!useAddBatch) {
			for(int i=0; i<parameterList.size();i++) {
				parameter = parameterList.get(i);
				count += getSqlSession(spec).insert(queryId, parameter);
			}
		}else { //addBatch를 사용하는 경우
			SqlSession session = getSqlSession(spec, true);
			for(int i=0; i<parameterList.size();i++) {
				parameter = parameterList.get(i);
				session.insert(queryId, parameter);
			}
			count = executeBatch(spec);
		}
		return count;
	}

	@Override
	public int batchDelete(String queryId, List<?> parameterList) {
		return batchDelete(queryId,parameterList,defaultSqlSessionKey,false);
	}

	@Override
	public int batchDelete(String queryId, List<?> parameterList, boolean useAddBatch) {
		return batchDelete(queryId,parameterList,defaultSqlSessionKey,useAddBatch);
	}

	@Override
	public int batchDelete(String queryId, List<?> parameterList, String spec) {
		return batchDelete(queryId,parameterList,spec,false);
	}

	@Override
	public int batchDelete(String queryId, List<?> parameterList, String spec, boolean useAddBatch) {
		int count =0;
		Object parameter = null;
		if(parameterList ==null || parameterList.size()==0){
			return 0;
		}
		if(!useAddBatch) {
			for(int i=0; i<parameterList.size();i++) {
				parameter = parameterList.get(i);
				count += getSqlSession(spec).delete(queryId, parameter);
			}
		}else { //addBatch를 사용하는 경우
			SqlSession session = getSqlSession(spec, true);
			for(int i=0; i<parameterList.size();i++) {
				parameter = parameterList.get(i);
				session.delete(queryId, parameter);
			}
			count = executeBatch(spec);
		}
		return count;
	}
	
	
	
}
