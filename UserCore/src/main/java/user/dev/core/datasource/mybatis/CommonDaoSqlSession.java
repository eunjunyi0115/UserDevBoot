package user.dev.core.datasource.mybatis;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionTemplate;

import lombok.extern.slf4j.Slf4j;
import user.dev.core.datasource.queryloader.QueryEntity;
import user.dev.core.datasource.queryloader.RuntimeQueryLoader;

//refresh 기능이 추가된 sqlSession
/*
 * SqlFragment 는 공통SQL조각 을 정의 하고 재사용하기 위한 기능. 중복되는 SQL일부를 모듈화 관리
 * XML기반의 MYBATIS Mapper 파일에서 선언되며 refID로 참조되어 다른 SQL태그에 삽입됨.
 */
@Slf4j
public class CommonDaoSqlSession implements SqlSession{

	CommonDaoSqlSessionFactoryBean sqlSessionFactoryBean;
	SqlSessionTemplate initialSession;
	SqlSessionTemplate reloadeSession;
	SqlSessionTemplate tempsqlSession;
	long startupTimestamp;
	long refreshTimestamp;
	
	public CommonDaoSqlSession(CommonDaoSqlSessionFactory sqlSessionFactory) {
		this.sqlSessionFactoryBean = sqlSessionFactory.getSqlSessionFactoryBean();
		this.initialSession = new SqlSessionTemplate(sqlSessionFactory,sqlSessionFactory.getExecutorType());
		
		this.startupTimestamp = System.currentTimeMillis();
	}
	
	//사용자 요청에 따라 변경된 쿼리 내용을 찿아서 새로 로딩할 준비를 한다.
	public void prepareRefresh() throws Exception{
		log.debug("Query refresh requested..");
		if(isRuntimeQueryLoading()) {
			//RuntimeQueryLoading 이면 tempsqlsession을 새로 생성하지 않고 원래 initialSession에 리로딩 하도록 한다.
			refreshQueryWithRuntimeQueryLoading();
		} else {
			CommonDaoSqlSessionFactory tempFactory = sqlSessionFactoryBean.createFactoryWithMapperChangeAfter(startupTimestamp);
			this.tempsqlSession = new SqlSessionTemplate(tempFactory, tempFactory.getExecutorType());
		}
	}
	//사용자 요청에 따라 변경된 쿼리 내용을 찿아서 새로 로딩할 준비를 한다.
	public void prepareRefresh(List<QueryEntity> reqList) throws Exception{
		log.debug("Query refresh requested..");
		if(isRuntimeQueryLoading()) {
			//RuntimeQueryLoading 이면 tempsqlsession을 새로 생성하지 않고 원래 initialSession에 리로딩 하도록 한다.
			refreshQueryWithRuntimeQueryLoading(reqList);
		} else {
			CommonDaoSqlSessionFactory tempFactory = sqlSessionFactoryBean.createFactoryWithStatementId(startupTimestamp,reqList);
			this.tempsqlSession = new SqlSessionTemplate(tempFactory, tempFactory.getExecutorType());
		}
	}
	
	//prepareRefresh() 메소드를 통해서 임시로 로딩된 refresh쿼리를 실제로 반영한다.
	public void confirmRefresh() {
		this.reloadeSession = this.tempsqlSession;
		this.refreshTimestamp = System.currentTimeMillis();
		
		log.debug("Query refresh completed");
	}
	
	//RuntimeQueryLoading 일 경우 parameter 로 받은 List<QueryEntity> 안의 sql문을 reloading한다.
	//RuntimeQueryLoading이 아닐 경우 이 메소드를 호출하면 CommonException이 발생한다.
	private void refreshQueryWithRuntimeQueryLoading(List<QueryEntity> queryInfos) {
		if(isRuntimeQueryLoading()) {
			try {
				sqlSessionFactoryBean.getCustomQueryLoader().onQueryRefreshByTag(queryInfos);
			}catch(Exception e) {
				log.error("fail to reload query:{}",queryInfos, e);
				throw new RuntimeException("fail to reload query:"+queryInfos,e);
			}
		}else {
			throw new RuntimeException("refreshQueryWithRuntimeQueryLoading is only for RuntimeQueryLoading..");
		}
	}
	
	//RuntimeQueryLoading 일 경우 현재 까지 로딩된 쿼리들을 다시 reloading한다.
	//리로딩 되는 시점은 실제로 쿼리가 다시 호출될때이다.
	private void refreshQueryWithRuntimeQueryLoading() {
		if(isRuntimeQueryLoading()) {
			try {
				sqlSessionFactoryBean.getCustomQueryLoader().onQueryRefresh(new Date(startupTimestamp));
			}catch(Exception e) {
				log.error("fail to reload query", e);
				throw new RuntimeException("fail to reload query",e);
			}
		}else {
			throw new RuntimeException("refreshQueryWithRuntimeQueryLoading is only for RuntimeQueryLoading..");
		}
	}
	
	//prepareRefresh 메소드를 통해서 임시로 로딩된 refresh쿼리를 메모리에서 제거한다.
	public void rollbackRefresh() {
		this.tempsqlSession = null;
	}
	
	private boolean isRuntimeQueryLoading() {
		return sqlSessionFactoryBean.getCustomQueryLoader() instanceof RuntimeQueryLoader;
	}
	
	//런타임 쿼리 마이바티스에 로딩.
	private void loadQueryAtRuntime(List<QueryEntity> queryInfos, boolean reload) throws Exception{
		sqlSessionFactoryBean.loadQueryAtRuntime(queryInfos, reload);
	}
	
	//특정쿼리가 reload가 필요한지 여부 정보
	private boolean needToRoadQueryNow(String queryId) {
		return (!this.initialSession.getConfiguration().hasStatement(queryId) ||
				((RuntimeQueryLoader)sqlSessionFactoryBean.getCustomQueryLoader()).needToReload(queryId));
	}
	
	//reloaded 세션 사용여부
	private boolean isQueryNotInReloadedSession(String queryId) {
		return (this.reloadeSession ==null ||
				 (this.reloadeSession !=null && !this.reloadeSession.getConfiguration().hasStatement(queryId)));
	}
	
	//런타임에 쿼리를 db 또는 file로 부터 로딩하는 경우에 대한 SqlSessionTemplate 를 구하는 메소드
	//@param queryId쿼리 ID문자열, DB로부터 로딩하는 경우 이 문자열은 tdo_stmt_info테이블의 buz_cd.stmt_id형식
	//ex edon.Q0001 으로 사용되어야만 한다. BUZ_CD(업무코드)가 내부적으로 Mybatis Mapper namespace로 동작하게 될것이다.
	//return 런타임에 쿼리를 db 또는 file로 부터 로딩하는 경우에 대한 sqlSessionTemplate 객체.
	private SqlSessionTemplate resolveRuntimeQueryLoadingTargetSession(String queryId) {
		//뒤져보고 없으면 runtime에 로딩, 이미쿼리가 있으면 그냥 sqlSessionTemplate만 넘겨 주면된다.
		if(isQueryNotInReloadedSession(queryId) && needToRoadQueryNow(queryId)) {
			try {
				//SYC는 이곳에서 수행하지 ㅇ낳고 실제  PARSER 로딩하는 곳에 처리
				int dotIndex = queryId.lastIndexOf(".");
				if(dotIndex <0) {
					throw new RuntimeException("Query id["+queryId+"] is not valid, must be namespace.id format..");
				}
				QueryEntity queryVo = new QueryEntity();
				queryVo.setNamespace(queryId.substring(0,dotIndex));
				queryVo.setStmtId(queryId.substring(dotIndex+1));
				List<QueryEntity> queryInfos = new ArrayList<QueryEntity>(1);
				queryInfos.add(queryVo);
				
				log.info("queryInfos{}",queryInfos);
				loadQueryAtRuntime(queryInfos, false);
			}catch(RuntimeException re) {
				throw re;
			}catch(Throwable e) {
				throw new RuntimeException ("Fail to load query["+queryId+"]" , e);
			}
		}
		//CommonDaoSqlSessionFactoryBean.buildSqlSessionFactory() 에서 customerQueryLoader에서 iniailSession 에 대한 
		//configuration 객체를 넣어주므로 runtime loadiung항상 initialSession에 적용.
		return initialSession;
	}
	
	//reloadedSession이 refreshable 하도록 sql parsing되었는지 확인
	protected boolean IsReloadedSessionRefreshable(String queryId) throws Exception{
		//reloadedSession에 queryId가 없고, sqlFragment(공통sql조각)가 refresh되는 경우
		//같은 파일 내에서 참조하지 않는 sqlFragment를 refresh한것이르모 오류발행.
		if(this.reloadeSession.getConfiguration().hasStatement(queryId)) {
			return true;
		}else {
			if(!this.reloadeSession.getConfiguration().getSqlFragments().isEmpty()) {
				this.reloadeSession.getConfiguration().getSqlFragments().clear();
				throw new RuntimeException("XML FILES INCLUDING SQL ELEMENT CANNOT BE REFRESHED");
			}else {
				return false;
			}
		}
	}
	
	private SqlSessionTemplate resolveEarlyQueryLoadingTargetSession(String queryId) {
		if(this.sqlSessionFactoryBean.isDevelopmentMode() && System.currentTimeMillis() - this.refreshTimestamp > this.sqlSessionFactoryBean.getCacheMillis()) {
			try {
				prepareRefresh();
				confirmRefresh();
			}catch(Exception e) {
				log.error("QUERY REFRESH FAILED", e);
				throw new RuntimeException("QUERY prepareRefresh() FAILED", e);
			}
		}
		
		try {
			if(this.reloadeSession!=null && IsReloadedSessionRefreshable(queryId)) {
				this.reloadeSession.getConfiguration().getSqlFragments().clear();
				return reloadeSession;
			}
		}catch(Exception ex) {
			//getIncompleteStatements 아직 완전히 처리 되지 않은 sql 구분들 수집 추적.
			this.reloadeSession.getConfiguration().getIncompleteStatements().clear();
			throw new RuntimeException("SQL statement to incluide with sql statement cannot be refreshed", ex);
		}
		return initialSession;
	}
	
	protected SqlSessionTemplate resolveTargetSession() {
		if(this.sqlSessionFactoryBean.isDevelopmentMode() && System.currentTimeMillis() - this.refreshTimestamp > this.sqlSessionFactoryBean.getCacheMillis()) {
			try {
				prepareRefresh();
				confirmRefresh();
			}catch(Exception e) {
				log.error("QUERY REFRESH FAILED", e);
				throw new RuntimeException("QUERY prepareRefresh() FAILED", e);
			}
		}
		if(this.reloadeSession!=null) { return reloadeSession;}
		return initialSession;
	}
	
	protected SqlSessionTemplate resolveTargetSession(String queryId) {
		if(isRuntimeQueryLoading()) {
			return resolveRuntimeQueryLoadingTargetSession(queryId);
		}else{
			return resolveEarlyQueryLoadingTargetSession(queryId);
		}
	}

	@Override
	public <T> T selectOne(String statement) {
		return resolveTargetSession(statement).selectOne(statement);
	}

	@Override
	public <T> T selectOne(String statement, Object parameter) {
		return resolveTargetSession(statement).selectOne(statement,parameter);
	}

	@Override
	public <E> List<E> selectList(String statement) {
		return (List<E>)resolveTargetSession(statement).selectList(statement);
	}

	@Override
	public <E> List<E> selectList(String statement, Object parameter) {
		return (List<E>)resolveTargetSession(statement).selectList(statement,parameter);
	}

	@Override
	public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
		return (List<E>)resolveTargetSession(statement).selectList(statement,parameter, rowBounds);
	}

	@Override
	public <K, V> Map<K, V> selectMap(String statement, String mapKey) {
		return (Map<K, V>)resolveTargetSession(statement).selectMap(statement,mapKey);
	}

	@Override
	public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey) {
		return (Map<K, V>)resolveTargetSession(statement).selectMap(statement,parameter,mapKey);
	}

	@Override
	public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
		return (Map<K, V>)resolveTargetSession(statement).selectMap(statement,parameter,mapKey, rowBounds);
	}

	@Override
	public <T> Cursor<T> selectCursor(String statement) {
		return resolveTargetSession(statement).selectCursor(statement);
	}

	@Override
	public <T> Cursor<T> selectCursor(String statement, Object parameter) {
		return resolveTargetSession(statement).selectCursor(statement,parameter);
	}

	@Override
	public <T> Cursor<T> selectCursor(String statement, Object parameter, RowBounds rowBounds) {
		return resolveTargetSession(statement).selectCursor(statement,parameter,rowBounds);
	}

	@Override
	public void select(String statement, Object parameter, ResultHandler handler) {
		resolveTargetSession(statement).select(statement,parameter,handler);
	}

	@Override
	public void select(String statement, ResultHandler handler) {
		resolveTargetSession(statement).select(statement,handler);
	}

	@Override
	public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
		resolveTargetSession(statement).select(statement,parameter,rowBounds,handler);
	}

	@Override
	public int insert(String statement) {
		return resolveTargetSession(statement).insert(statement);
	}

	@Override
	public int insert(String statement, Object parameter) {
		return resolveTargetSession(statement).insert(statement,parameter);
	}

	@Override
	public int update(String statement) {
		return resolveTargetSession(statement).update(statement);
	}

	@Override
	public int update(String statement, Object parameter) {
		return resolveTargetSession(statement).update(statement,parameter);
	}

	@Override
	public int delete(String statement) {
		return resolveTargetSession(statement).delete(statement);
	}

	@Override
	public int delete(String statement, Object parameter) {
		return resolveTargetSession(statement).delete(statement,parameter);
	}

	@Override
	public void commit() {
		throw new RuntimeException("지원하지 않는다.");
	}

	@Override
	public void commit(boolean force) {
		throw new RuntimeException("지원하지 않는다.");
	}

	@Override
	public void rollback() {
		throw new RuntimeException("지원하지 않는다.");
	}

	@Override
	public void rollback(boolean force) {
		throw new RuntimeException("지원하지 않는다.");
	}

	@Override
	public List<BatchResult> flushStatements() {
		return resolveTargetSession().flushStatements();
	}

	@Override
	public void close() {
		throw new RuntimeException("지원하지 않는다.");
	}

	@Override
	public void clearCache() {
		throw new RuntimeException("지원하지 않는다.");
	}

	@Override
	public Configuration getConfiguration() {
		return resolveTargetSession().getConfiguration();
	}

	/**
	 * 데브온 query refresh 기능에 의해서 내부적으로 사용되는 initialSession, reloadedSession 에 따라 configuration 객체가 구분되어야만
	 * DefaultPersistneceTracer쪽에서 sql 을 찿아서 tracing 할수 있다. 
	 * 이 Method 는 queryId로 mybatis 내부의 해당 Configuration을 리턴한다.
	 * @param queryId mybatis 내부의 해당 Configuration을 찿을 queryId
	 * @return
	 */
	public Configuration getConfiguration(String queryId) {
		return resolveTargetSession(queryId).getConfiguration();
	}
	
	@Override
	public <T> T getMapper(Class<T> type) {
		throw new RuntimeException("지원하지 않는다.");
	}

	@Override
	public Connection getConnection() {
		throw new RuntimeException("지원하지 않는다.");
	}
}

