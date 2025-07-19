package user.dev.core.datasource.mybatis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.support.ResourceArrayPropertyEditor;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import user.dev.core.datasource.queryloader.AbstractCustomQueryLoader;
import user.dev.core.datasource.queryloader.MapperFileResolver;
import user.dev.core.datasource.queryloader.QueryEntity;
import user.dev.core.datasource.queryloader.RuntimeQueryLoader;
/**
 * 이놈은 굳이 상속 받아서 사용할 이유가 잇을까?? refresh 기능도 잘 안되는데 ㅋ
 */
@Slf4j
public class CommonDaoSqlSessionFactoryBean extends SqlSessionFactoryBean implements BeanNameAware {
	
	private DataSource dataSource;
	
	private String configLocation;
	
	private String[] mapperLocations = new String[0];
	
	private long cacheMillis = 1000;
	
	private boolean developmentMode = false;
	
	private boolean queryRefresh;
	
	//변경분 tag 된 reqList 건들만 refresh 처리,
	private boolean queryRefreshByTag;
	
	private long startupTime;
	private String startupTimeFormat;
	
	private ExecutorType executorType;
	
	//실행결과를 객체로 변환시 사용
	private ObjectFactory objectFactory;
	
	private ObjectWrapperFactory objectWrapperFactory;
	
	private String beanName;
	
	//DB 쿼리관리시  REFRESH 대상 정보 세팅.
	private List<QueryEntity> reqList;
	
	private AbstractCustomQueryLoader customQueryLoader;
	
	private Set<Resource> reloadedMappers = new HashSet<>();
	
	private MapperFileResolver mapperFileResolverForAddBatch;
	
	//내가 추가한거 primary 여부
	private boolean primary;
	
	public void setPrimary(boolean primary) {
		this.primary = primary;
	}
	public boolean getPrimary() {
		return primary;
	}
	
	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}
	
	public CommonDaoSqlSessionFactoryBean() {}
	
	public CommonDaoSqlSessionFactoryBean(boolean dbQueryuRefresh, long startupTime) {
		this.queryRefresh = dbQueryuRefresh;
		this.startupTime = startupTime;
	}
	
	public CommonDaoSqlSessionFactoryBean(boolean queryuRefresh, boolean queryuRefreshByTag, long startupTime, List<QueryEntity> reqList) {
		this.queryRefresh = queryuRefresh;
		this.queryRefreshByTag = queryuRefreshByTag;
		this.startupTime = startupTime;
		this.reqList = reqList;
	}
	
	@Override
	protected SqlSessionFactory buildSqlSessionFactory() throws Exception{
		SqlSessionFactory sqlSessionFactory = super.buildSqlSessionFactory();
		Configuration config =  sqlSessionFactory.getConfiguration();
		if(customQueryLoader != null) {
			customQueryLoader.setConfiguration(config);
			if(!queryRefresh && !queryRefreshByTag) {
				//db의 경우는 mybatis mapper 에 적재 하는 작업을 한다. 
				//file의 경우는 할작업이 없음(이미로딩)
				customQueryLoader.onQueryInitialLoad();
			}else if(!queryRefresh && queryRefreshByTag) {
				//요청 tag 된 reqList 건만 refresh처리
				customQueryLoader.onQueryRefreshByTag(reqList);
			}else {
				//서버 기동시간 이후 변경분 모두 적용.
				customQueryLoader.onQueryRefresh(new Date(startupTime));
			}
		}
		return new CommonDaoSqlSessionFactory(config, this);
	}

	@Override
	public SqlSessionFactory getObject() throws Exception{
		SqlSessionFactory factory = super.getObject();
		if(factory instanceof CommonDaoSqlSessionFactory comFactory) {
			comFactory.setBeanName(beanName);
		}
		return factory;
	}
	
	public void setExecutorType(ExecutorType executorType) {
		this.executorType = executorType;
	}
	public void setExecutorType(String executorType) {
		this.executorType = ExecutorType.valueOf(executorType);
	}
	public ExecutorType getExecutorType() {
		return executorType;
	}

	@Override
	public void setDataSource(DataSource dataSource) {
		super.setDataSource(dataSource);
		this.dataSource = dataSource;
	}
	public DataSource getDataSource() {
		return dataSource;
	}
	
	//### ConfigLocation 세팅
	public void setConfigLocation(String configLocation) {
		super.setConfigLocation(convertToResource(configLocation));
		this.configLocation = configLocation;
	}
	public String getConfigLocation() {
		return configLocation;
	}
	
	//### Mapper Mybatis쿼리 경로 set
	public void setMapperLocations(String[] mapperLocations) {
		if(customQueryLoader == null) {
			super.setMapperLocations(convertToResourceArray(mapperLocations));
			this.mapperLocations = mapperLocations;
		}
	}
	public String[] getMapperLocations() {
		//Resource[] 타입으로 절대 변경 하면 안됨.'
		return this.mapperLocations;
	}
	
	//refresher 반환
	public AbstractCustomQueryLoader getCustomQueryLoader() {
		return this.customQueryLoader;
	}
	public void setCustomQueryLoader(AbstractCustomQueryLoader customQueryLoader) {
		this.customQueryLoader = customQueryLoader;
	}
	
	//개발 운영 모드 새팅
	public void setDevelopmentMode(boolean devMode) {
		this.developmentMode = devMode;
	}
	//개발모드 자동 refresh 간격 세팅
	public void setCacheSeconds (long cacheSeconds) {
		this.cacheMillis = cacheSeconds*1000;
	}
	
	//### 오브젝트 팩토리 설정
	@Override
	public void setObjectFactory(ObjectFactory objectFactory) {
		this.objectFactory = objectFactory;
	}
	@Override
	public void setObjectWrapperFactory(ObjectWrapperFactory objectWrapperFactory) {
		this.objectWrapperFactory = objectWrapperFactory;
	}
	
	//쿼리 refresh를 제공 xml 파일로 CommonDaoSqlSessionFactory 생성
	public CommonDaoSqlSessionFactory createFactoryWithMapperChangeAfter(long startUpTime) throws Exception{
		CommonDaoSqlSessionFactoryBean factoryBean = new CommonDaoSqlSessionFactoryBean(true,startUpTime);
		factoryBean.setDataSource(this.dataSource);
		factoryBean.setConfigLocation(this.configLocation);
		factoryBean.setCustomQueryLoader(customQueryLoader);
		factoryBean.setExecutorType(this.executorType);
		
		Resource[] reloadMapper = findReloadMapper(startUpTime, this.mapperLocations);
		factoryBean.setMapperLocations(reloadMapper);
		return (CommonDaoSqlSessionFactory)factoryBean.getObject();
	}
	
	//쿼리 refresh를 제공 reqList 의 쿼리만으로 생성한다.
	public CommonDaoSqlSessionFactory createFactoryWithStatementId(long startUpTime,List<QueryEntity> reqList) throws Exception{
		CommonDaoSqlSessionFactoryBean factoryBean = new CommonDaoSqlSessionFactoryBean(false,true,startUpTime,reqList);
		factoryBean.setDataSource(this.dataSource);
		factoryBean.setConfigLocation(this.configLocation);
		factoryBean.setCustomQueryLoader(customQueryLoader);
		factoryBean.setExecutorType(this.executorType);
		return (CommonDaoSqlSessionFactory)factoryBean.getObject();
	}
	
	//문자를 resource로 변환
	public Resource convertToResource(String text) {
		ResourceEditor editor = new ResourceEditor();
		editor.setAsText(text);
		return (Resource) editor.getValue();
	}
	//경로,를 구분한 문자열을 받아 resource 배열로 반환
	public Resource[] convertToResourceArray(String text) {
		ResourceArrayPropertyEditor arrayEditor = new ResourceArrayPropertyEditor();
		arrayEditor.setAsText(text);
		return (Resource[])arrayEditor.getValue();
	}
	public Resource[] convertToResourceArray(String[] text) {
		List<Resource> result = new ArrayList<Resource>();
		for(int i=0;i<text.length;i++) {
			result.addAll( Arrays.asList(convertToResourceArray(text[i])) );
		}
		return result.toArray(new Resource[0]);
	}
	
	//was시간시간 이후로 변경된 쿼리 mapper xml 파일을 설정에 지정된 경로로 부터 찿아서 목록으로 반환한다.
	protected Resource[] findReloadMapper(long startUpTime, String[] mapperLoactions) throws IOException {
		Resource[] mappers = convertToResourceArray(mapperLoactions);
		for(int i=0;i<mappers.length; i++) {
			if(reloadedMappers.contains(mappers[i])) continue;
			try {
				if(mappers[i].getFile().lastModified()/1000 >= startUpTime/1000) {
					reloadedMappers.add(mappers[i]);
					Set<Resource> includes = findIncludeMapper(mappers[i], mappers);
					if(includes != null) reloadedMappers.addAll(includes);
				}
			}catch(IOException e) {
				continue;
			}
		}
		log.debug("REFRESH TARGET FILE:{}", reloadedMappers);
		
		return reloadedMappers.toArray(new Resource[0]);
	}
	
	//include refid로 참조된 resource 목록을 반환한다.
	private Set<Resource> findIncludeMapper(Resource mapper, Resource[] mappers) {
		var results = new HashSet<Resource>();
		var namespaceSet = new HashSet<String>();
		try(BufferedReader reader = new BufferedReader(new InputStreamReader(mapper.getInputStream(),"utf-8"))){
			String line = null;
			do {
				line = reader.readLine();
				if(line == null) break;
				int idx = line.indexOf("include");
				if(idx<0) continue;
				String substr = line.substring(idx+7);
				idx = line.indexOf("refid");
				if(idx<0) continue;
				String str = substr.substring(idx+5);
				int st = str.indexOf('"');
				int ed = str.lastIndexOf('.');
				if(st<0 || ed <0 || st> ed) continue;
				String namespace = str.substring(st+1,ed);
				namespaceSet.add(namespace);
			}while(line != null);
		}catch(IOException ie) {
			return null;
		}
		
		for(String namespace: namespaceSet) {
			Resource incMapper = findNamespaceMapper(namespace, mappers, results);
			if(incMapper!=null) {
				results.add(incMapper);
			}
		}
		return results;
	}
	
	//namespace 와 일치하는 Resource를 반환한다.
	private Resource findNamespaceMapper(String namespace, Resource[] mappers, Set<Resource> includes) {
		for(Resource mapper : mappers) {
			if(includes.contains(mapper) || reloadedMappers.contains(mapper)) continue;
			
			try(BufferedReader reader = new BufferedReader(new InputStreamReader(mapper.getInputStream(),"utf-8"))) {
				String line = null;
				do {
					line = reader.readLine();
					if(line == null) break;
					int idx = line.indexOf("namespace");
					if(idx>=0) {
						String str =  line.substring(idx);
						String mapperNamespace = str.substring(str.indexOf('"')+1, str.lastIndexOf('"'));
						if(namespace.equals(mapperNamespace)) return mapper;
					}
					
				}while(line != null);
			}catch(IOException ie) {
				return null;
			}
		}
		return null;
	}
	
	public void setStartupTimeFormat(String startupTimeFormat) {
		this.startupTimeFormat = startupTimeFormat;
	}
	
	//refresh 개발모드에서 초
	public long getCacheMillis() {
		return cacheMillis;
	}
	
	//개발모두 여부
	public boolean isDevelopmentMode() {
		return developmentMode;
	}
	
	public void loadQueryAtRuntime(List<QueryEntity> queryIds, boolean reload) throws Exception{
		((RuntimeQueryLoader)customQueryLoader).loadQueryAtRuntime(queryIds,reload);
	}
	
	public void setMapperFileResolverForAddBatch(MapperFileResolver mapperFileResolverForAddBatch) {
		this.mapperFileResolverForAddBatch = mapperFileResolverForAddBatch;
	}
	
	public MapperFileResolver getMapperFileResolverForAddBatch() {
		return this.mapperFileResolverForAddBatch;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		if(customQueryLoader!=null && mapperLocations.length>0) {
			throw new RuntimeException(" 쿼리 로더 또는 메퍼경로가 존재하지 않는다");
		}
		if(this.objectFactory == null) {
			this.objectFactory = new DefaultObjectFactory();//FastObjectFactory();
		}
		super.setObjectFactory(objectFactory);
		
		if(this.objectWrapperFactory == null) {
			this.objectWrapperFactory = new DefaultObjectWrapperFactory();//FastObjectFactory();
		}
		super.setObjectWrapperFactory(objectWrapperFactory);
		super.afterPropertiesSet();
	}
	
}
