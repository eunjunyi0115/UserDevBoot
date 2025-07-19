package user.dev.core.datasource.queryloader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.session.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import user.dev.core.datasource.config.DataSourceAutoconfigure;
import user.dev.core.datasource.mybatis.CommonDaoMultiSqlSessionFactory;

/**
 * XMLMapperBuilder 이 아주 중요하다. Mybatis 에 쿼리를 파싱 및 등록 시키는 것이다.
 */
@Slf4j
public abstract class AbstractCustomQueryLoader {

	private Configuration configuration;
	
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}
	
	public Configuration getConfiguration() {
		return this.configuration;
	}
	
	//파라미터 전달된 쿼리 xml 문자열 파싱
	public final void parseQueryXml(String contents, String charset) {
		try {
			log.info("세팅쿼리:{}",contents );
			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(contents.getBytes(charset));
			parseQueryXml(byteArrayInputStream, contents);
		}catch(UnsupportedEncodingException ue) {
			ue.printStackTrace();
			throw new RuntimeException("쿼리 파싱 중 오류 발생",ue);
		}
	}
	public final void parseQueryXml(String contents) {
		parseQueryXml(contents,Charset.defaultCharset().name());
	}
	
	private final void parseQueryXml(InputStream inputStream, String contents) {
		parseQueryXml(inputStream,contents,"database:SqlRepository");
	}
	
	private final void parseQueryXml(InputStream inputStream, String contents, String namespace) {
		try {
			//Mybatis 에 실제 쿼리 파싱 및 등록 한다. namespace는 실제 사용하기 보다 중복체크등에 사용.
			XMLMapperBuilder builder = new XMLMapperBuilder(inputStream,configuration,namespace, configuration.getSqlFragments());
			builder.parse();
		}catch(Exception ue) {
			throw new RuntimeException("쿼리 파싱 중 오류 발생",ue);
		}finally {
			ErrorContext.instance().reset();
		}
	}
	
	/**
	 * 파라미터로 전달된 쿼리 xml  문자열을 parse 하여  mybatis 에 등록한다.
	 * @param loadingInfo 쿼리 문자열
	 * @param charset
	 * @param reload
	 */
	public final void parseQueryXmlWithMyBatisQueryLoadingInfo(MyBatisQueryLoadingInfo loadingInfo,String charset, boolean reload) {
		String fakeNamespace = loadingInfo.getNamespace()+"."+loadingInfo.getStmtId();
		Map<String,ResultMap> resultMaps = null;
		if(reload && loadingInfo.getResultMapId()!=null) {
			resultMaps = getResultMaps();
			//resultMap(오라클 프로시저실행히는 OUT PARAM 이 COURSE 이면 필수로 필요함)
			// resultMap reoload 의 경우는 mappedStatements 와 다르게 nbamespace.resultMapId 와 resultMapId 두개를 삭제해야만처리됨.
			resultMaps.remove(loadingInfo.getNamespace()+"."+loadingInfo.getResultMapId());
			resultMaps.remove(loadingInfo.getResultMapId());
		}
		//mapper 쿼리 등록처리
		parseQueryXmlWithNamespace(loadingInfo.getXmlQuery(), fakeNamespace, Charset.defaultCharset().name(), reload);
	}
	
	
	/**
	 * 파라미터로 전단달된 쿼리 xml 문자열을 parse 하여 Mybatis 에 등록한다.
	 * @param contents 쿼리 xml문자열
	 * @param namespace 쿼리 namespace, mapper 의 namespace 또는 namespace.statid 일수 있음.
	 * @param charset
	 * @param reload
	 */
	public final void parseQueryXmlWithNamespace(String contents ,String namespace, String charset) {
		parseQueryXmlWithNamespace(contents,namespace,charset, false);
	}
	
	public final void parseQueryXmlWithNamespace(String contents ,String namespace, String charset,boolean reload) {
		ByteArrayInputStream byteArrayInputStream = null;
		Set<String> loadedResouirce;
		Map<String, MappedStatement> mappedStatements;
		try {
			//configuration 에서 기존 로딩된 sql 제거.
			loadedResouirce = getLoadedResource();
			loadedResouirce.remove(namespace);
			mappedStatements = getMappedStatements();
			//runtime query 로딩시 namespace.statId로 삭제 하면됨.
			mappedStatements.remove(namespace);
			
			log.info("contents:{}",contents);
		
			//재등록.
			byteArrayInputStream = new ByteArrayInputStream(contents.getBytes(charset)); 
			parseQueryXml(byteArrayInputStream,contents,namespace);
			
			mappedStatements = getMappedStatements();
			log.info("mappedStatements:{}",mappedStatements);
			
		}catch(UnsupportedEncodingException ue) {
			throw new RuntimeException(charset+"지원하지 않는다",ue);
		}finally {
			if(byteArrayInputStream!=null) {
				try {
					byteArrayInputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private <T> T getAccessField(Object obj, String fieldName) {
		Field field = null;
		boolean acc = false;
		try {
			field = obj.getClass().getDeclaredField(fieldName);
			acc = field.isAccessible();
			if(!acc) field.setAccessible(true);
			return (T)field.get(obj);
		}catch(Exception e) {
			throw new RuntimeException(field+"필드에 접근 불가능한다.");
		}finally {
			if(field != null) {
				field.setAccessible(acc);
			}
		}
		
	}
	//로드된 리소스를 Configuration 의 loadedResources 필드에 강제 접근해서 가져옴.
	private Set<String> getLoadedResource(){
		String fieldStr = "loadedResources"; //리소드저장필드.
		return (Set<String>)getAccessField(configuration,fieldStr);
	}
	
	private Map<String,MappedStatement> getMappedStatements(){
		String fieldStr = "mappedStatements"; //리소드저장필드.
		return (Map<String,MappedStatement>)getAccessField(configuration,fieldStr);
	}
	
	private Map<String,ResultMap> getResultMaps(){
		String fieldStr = "resultMaps"; //리소드저장필드.
		return (Map<String,ResultMap>)getAccessField(configuration,fieldStr);
	}
	
	
	//동적으로 mapper xml 을 생성한다.
	protected String appendXmlHeadTail(String namespace, String content) {
		StringBuilder mapperXml = new StringBuilder();
		mapperXml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
			.append("\n")
			.append("<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">")
			.append("\n")
			.append("<mapper namespace=\"").append(namespace).append("\">")
			.append("\n")
			.append(content)
			.append("</mapper>");
		return mapperXml.toString();
	}
	
	public abstract void onQueryInitialLoad();
	public abstract void onQueryRefresh(Date serverStartupTime);
	public abstract void onQueryRefreshByTag(List<QueryEntity> reqList);
}
