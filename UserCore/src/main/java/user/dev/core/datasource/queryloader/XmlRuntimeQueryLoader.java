package user.dev.core.datasource.queryloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.springframework.core.io.Resource;

import lombok.extern.slf4j.Slf4j;

/**
 *  mybatis 쿼리를 Runtime 에 필요한 시점에 file(mybatis mapper xml)로 부터 로딩하는 클래스.
 *  mybatis 는 최초 서버 기동시 모든 쿼리를 validation 하고 로딩 하는데, 이 클래스는
 *  서비기동시에는 아무 작업을 하지 ㅇ낳고 runtime 에 필요한 시점에 쿼리를 로딩한다.
 *  이때, mybatis mapper lxml 파일에서 원하는 쿼리만 골라서 새로운 mapper 파일을 생성해서 그 부분만 로딩하게 되는데 이를 위해서 약간의 제약이 필요한다.
 *  1. 파일명은 mapper의 namespace 와 이름이 같아야 한다.
 *  2. mapper xml 파일 내에 partial쿼리나 parameterMap 등을 따로 정의 해서 사용할수 없다.
 *  3. resultMap은 사용할수 있으나, xml 파일에서 sql문들 보다 위쪾에 위치 해야 한다.
 */
@Slf4j
public class XmlRuntimeQueryLoader extends AbstractCustomQueryLoader implements RuntimeQueryLoader {

	private Map<String, Boolean> reloadInfo = new ConcurrentHashMap<String, Boolean>();
	private Resource[] mapperLocations;
	private MapperFileResolver mapperFileResolver;
	private XMLInputFactory factory = XMLInputFactory.newInstance();
	private boolean autoReload;
	private Map<String,Long> resourceRefreshMap = new ConcurrentHashMap<String, Long>();
	private static final String SQL_FRAGMENT_REF = "include refid";
	
	//자바 버전에 따른 property값 확인 필요
	private static final String REPORT_CDATA = "http://java.sun.com/xml/stream/properties/report-cdata-event";
	
	//mybatis 의 쿼리 파일들 Resource[]
	public void setMapperLocations(Resource[] mapperLocations) {
		this.mapperLocations = mapperLocations;
	}
	
	public void setMapperFileResolver(MapperFileResolver mapperFileResolver) {
		this.mapperFileResolver = mapperFileResolver;
	}
	
	public void setAutoReload(boolean autoReload) {
		this.autoReload = autoReload;
	}
	
	@Override
	public void loadQueryAtRuntime(List<QueryEntity> queryInfos, boolean reload) throws Exception {
		String namespace = "";
		Resource mapperFile = null;
		for(QueryEntity sqlInfo: queryInfos) {
			namespace = sqlInfo.getNamespace();
			mapperFile = getMapperFile(sqlInfo);
			
			if(mapperFile == null) {
				throw new RuntimeException("mapper.xml 존재 하지 않는다. namespace:"+namespace);
			}
			//RELOAD를 체크 할수 있도록 XML 파일의 최종 변경시간을 기억해 둔다.
			if(autoReload) {
				//mapperFile.getFiel().lastModified() 하면 안됨. jar 안에 있으면 오류남.
				Long lastModified = mapperFile.lastModified();
				Long beforeLastModified = resourceRefreshMap.put(sqlInfo.getQueryId(), lastModified);
				if(beforeLastModified == null || lastModified > beforeLastModified) {
					reload = true;
				}
			}
			MyBatisQueryLoadingInfo loadingInfo = getQueryFromMapperXml(mapperFile, sqlInfo);
			
			//파일일 경우 외부 <sql>에 대한 참조가 가능하도록 한다.
			if(loadingInfo.getXmlQuery().contains(SQL_FRAGMENT_REF)) {
				loadSqlElement(loadingInfo.getXmlQuery(), namespace, sqlInfo.getStmtId());
			}
			//실제로 로딩하기 전에 한번더 해당 쿼리가 있는지 확인 또는 reload일 경우 로딩
			synchronized (this) {
				if(reload || needToReload(sqlInfo.getQueryId())) {
					super.parseQueryXmlWithMyBatisQueryLoadingInfo(loadingInfo, "utf-8", true);
					reloadInfo.put(sqlInfo.getQueryId(), false); //리로딩후 false로 변경. ex)Employee.selectEmp
					reloadInfo.put(sqlInfo.getStmtId(), false);  //리로딩후 false로 변경. ex)selectEmp
				}else if(!getConfiguration().hasStatement(sqlInfo.getQueryId())) {
					super.parseQueryXmlWithMyBatisQueryLoadingInfo(loadingInfo, "utf-8", false);
					reloadInfo.put(sqlInfo.getQueryId(), false); //리로딩후 false로 변경. ex)Employee.selectEmp
					reloadInfo.put(sqlInfo.getStmtId(), false);  //리로딩후 false로 변경. ex)selectEmp
				}
			}
		}
	}
	
	/**
	 * 쿼리들이 모여 있는 mapper파일들 중에서 관련 있는 mapper을 찿는다.
	 * 쿼리 id 와 mapper 파일과의 관계를 엮어 주는 mapperFileResolver가 세팅되어 있다면, 
	 * 이를 사용해서 찿고, mapperFileResolver가  null 이면 전체 xml 파일을 뒤지면서 해당 쿼리가 있는 파일을 찿는다.
	 * @param sqlInfo 해당 쿼리에 대한 queryId, namespace.statementid 형식의 문자열
	 * @return 해당쿼리의 namespace 와 관련 있는 mapper Resource 객체
	 */
	private Resource getMapperFile(QueryEntity sqlInfo) {
		if(mapperFileResolver != null) {
			return mapperFileResolver.resolveMapperFile(mapperLocations, sqlInfo);
		}else {
			return resolveMapperFile(sqlInfo);
		}
	}
	
	/**
	 * Mapper xml파일들을 loop를 돌면서 namespace 값이 일치하는 mapper 을 찿아서 리턴한다.
	 * 찿지 못하면 null 을 리턴한다.
	 * @param namespace 찿고자 하는 namespace명
	 * @return mapper xml 의 namespace 가 일치하는 Mapper Resource 객체
	 */
	private Resource resolveMapperFile(QueryEntity sqlInfo) {
		boolean whileBreak = false;
		boolean found = false;
		Resource result = null;
		XMLStreamReader xmlStreamReader = null;
		InputStream resourceInputStream = null;
		for(Resource mapperFile: mapperLocations) {
			whileBreak = false;
			try {
				resourceInputStream = mapperFile.getInputStream();
				xmlStreamReader = factory.createXMLStreamReader(resourceInputStream);
				int event = 0;
				String tag = "";
				while(xmlStreamReader.hasNext()) {
					try {
						event = xmlStreamReader.next();
					}catch(Exception ex) {
						if(event == XMLStreamConstants.DTD && ex.getCause() instanceof UnknownHostException) {
							//Open source Stax구현체인 WoodStox를 사용해서 xml을 parsing할 경우 xml 파일에 dtd가 기술되어 있으면 실제 해당 url로 접속해서 dtd를
							//만약 서버가 internet이 안되거나 host명을 인식할수 없는 site이면 이로인해서 오류가 발생하는 데 이를 ignore하도록 한다.
						}else {
							throw ex;
						}
					}
					switch(event) {
						case XMLStreamConstants.START_ELEMENT:
							tag = xmlStreamReader.getLocalName();
							if(tag.equals("mapper")) { //<mapper tag 찿아
								if(xmlStreamReader.getAttributeCount() == 0) {
									throw new RuntimeException("Mapper file namespace attribute must be exist in the mapper tag.");
								}
								if(sqlInfo.getNamespace().equals(xmlStreamReader.getAttributeValue(0))) {
									found = true; 
									result = mapperFile;
								}
								whileBreak = true;
							}
						default: break;
					}
					if(whileBreak) break; //while문 빠져나감.
				}
			}catch(Throwable te) {
				throw new RuntimeException("Exception occurred while XMLStreamReader proicessing in "+mapperFile.getFilename(), te);
			}finally {
				if(xmlStreamReader != null) {
					try {
						xmlStreamReader.close();
					} catch (XMLStreamException e) {
						e.printStackTrace();
					}
				}
				if(resourceInputStream != null) {
					try {
						resourceInputStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			if(found) break;
		}
		return result;
	}
	
	private MyBatisQueryLoadingInfo getQueryFromMapperXml(Resource mapperFile, QueryEntity sqlInfo) {
        String stmtCnts = null;
        StringBuilder queryBuilder = new StringBuilder();
        StringBuilder resultMapBuilder = null;
        Map<String, StringBuilder> resultMap = null;
        Map<String, String> attributeMap = null;
        XMLStreamReader xmlStreamReader = null;
        InputStream resourceInputStream = null;
        try {
            resourceInputStream = mapperFile.getInputStream();
            
            //CDATA를 ignore하는 현상으로 인해 property에 아래코드추가
            factory.setProperty(XMLInputFactory.IS_COALESCING, false);
			factory.setProperty(REPORT_CDATA, true);
			
            xmlStreamReader = factory.createXMLStreamReader(resourceInputStream);
            int event = 0;
            int beforeEvent = 0;
            boolean sqlFound = false;
            boolean resultMapFound = false;
            boolean end = false;
            String queryStartTag = "";
            String resultMapStartTag = "";
            String tag = "";
            while(xmlStreamReader.hasNext()){
                try {
                    event = xmlStreamReader.next();
                    if(sqlFound){
                        checkCdataTag(queryBuilder, beforeEvent, event);
                    } else if(resultMapFound) {
                        checkCdataTag(resultMapBuilder, beforeEvent, event);
                    }
                } catch(Exception ex) {
                    if(event == XMLStreamConstants.DTD && ex.getCause() instanceof UnknownHostException) {
                        // Open source StAX 구현체인 Woodstox를 사용해서 xml을 parsing할 경우 xml 파일에 dtd가 기술되어 있으면 실제 해당 url로 접속해서 dtd를 가져온다.
                        // 만약 서버가 internet이 안되거나 host명을 인식할 수 없는 site이면 이로인해서 오류가 발생하는데 이를 ignore하도록 한다.
                    } else {
                        throw ex;
                    }
                }
                switch(event) {
                    case XMLStreamConstants.START_ELEMENT:
                        tag = xmlStreamReader.getLocalName();
                        if("mapper".equals(tag)){ // <mapper tag 찾아서...
                            if(xmlStreamReader.getAttributeCount() == 0){
                                throw new RuntimeException( "Mapper file namespace attribute must be exist in the mapper tag. Check mapper file[" + mapperFile.getFilename() + "].");
                            }
                            if(!"namespace".equals(xmlStreamReader.getAttributeLocalName(0)) || !sqlInfo.getNamespace().equals(xmlStreamReader.getAttributeValue(0))){
                                throw new RuntimeException("Mapper file namespace is not matched. expect[namespace=" + sqlInfo.getNamespace() + "], but["
                                        + xmlStreamReader.getAttributeLocalName(0) + "=" + xmlStreamReader.getAttributeValue(0) + "] is setted in " + mapperFile.getFilename() + "'s first attribute.");
                            }
                        } else { // <mapper tag 내의 <insert>, <delete>, <update>, <select> tag 면
                            if(sqlFound){ // 찾았는데 다른 tag 시작이면, ex) dynamic query, <if test="num != null">
                                makeStartTag(queryBuilder, xmlStreamReader, tag);
                            } else if(isXmlTagSqlType(tag) && "id".equals(xmlStreamReader.getAttributeLocalName(0)) && sqlInfo.getStmtId().equals(xmlStreamReader.getAttributeValue(0))){ // 원하는 statement id 면
                                attributeMap = makeSqlStartTag(queryBuilder, xmlStreamReader, tag);
                                queryStartTag = tag;
                                sqlFound = true;
                            } else if("resultMap".equals(tag) && "id".equals(xmlStreamReader.getAttributeLocalName(0))){
                                // <mapper tag 내의 <resultMap> tag 면
                                resultMapBuilder = new StringBuilder();
                                if(resultMap == null) {
                                    resultMap = new ConcurrentHashMap<String, StringBuilder>();
                                }
                                resultMap.put(xmlStreamReader.getAttributeValue(0), resultMapBuilder);
                                resultMapBuilder.append("\t");
                                makeStartTagWithLineFeed(resultMapBuilder, xmlStreamReader, tag);
                                resultMapStartTag = tag;
                                resultMapFound = true;
                            } else if(resultMapFound) {
                                resultMapBuilder.append("\t\t");
                                makeStartTag(resultMapBuilder, xmlStreamReader, tag);
                            }
                        }
                        break;
                    // mybatis xml 파일에 CDATA를 사용한 경우 parsing하지 못하는 현상이 있어서 CDATA 처리를 추가함. 20160113, 아산병원, jmmoon
                    case XMLStreamConstants.CDATA:
                        if(sqlFound){
                            makeQueryText(queryBuilder, xmlStreamReader);
                        } else if(resultMapFound) {
                            makeQueryText(resultMapBuilder, xmlStreamReader);
                        }
                        break;

                    case XMLStreamConstants.CHARACTERS:
                        if(sqlFound){
                            makeQueryText(queryBuilder, xmlStreamReader);
                        } else if(resultMapFound) {
                            makeQueryText(resultMapBuilder, xmlStreamReader);
                        }
                        break;

                    case XMLStreamConstants.END_ELEMENT:
                        tag = xmlStreamReader.getLocalName();
                        if(sqlFound){
                            end = makeEndTag(queryBuilder, queryStartTag, tag);
                        } else if(resultMapFound) {
                            if(resultMapStartTag.equalsIgnoreCase(tag)){
                                resultMapBuilder.append("\t");
                            }
                            boolean resultMapEnd = makeEndTagWithLineFeed(resultMapBuilder, resultMapStartTag, tag);
                            if(resultMapEnd) {
                                resultMapFound = false;
                            }
                        }
                        break;

                    default: break;
                }
                beforeEvent = event;
                if(sqlFound && end){
                    break; // while문을 빠져나간다.
                }
            }
            if(!sqlFound){ // while문을 다 돌아도 못 찾으면
                throw new RuntimeException("Can't find " + sqlInfo.getStmtId() + " in the Mapper file " + mapperFile.getFilename());
            }
        } catch (RuntimeException de) {
            throw de;
        } catch (Throwable e) {
            throw new RuntimeException("Fail to get Xml file reader from mapper file[" + mapperFile.getFilename() +"]" , e);
        } finally {
            if(xmlStreamReader != null) {
                try {
                    xmlStreamReader.close();
                } catch (Exception ignore) {}
            }
            if(resourceInputStream != null) {
                try {
                    resourceInputStream.close();
                } catch (Exception ignore) {}
            }
        }
        MyBatisQueryLoadingInfo loadInfo = new MyBatisQueryLoadingInfo(sqlInfo.getNamespace(), sqlInfo.getStmtId());
        if(resultMap != null && attributeMap.containsKey("resultMap")) {
            loadInfo.setResultMapId(attributeMap.get("resultMap"));
            stmtCnts = resultMap.get(attributeMap.get("resultMap")).toString() + "\n" + queryBuilder.toString();
        } else {
            stmtCnts = queryBuilder.toString();
        }
        stmtCnts = appendXmlHeadTail(sqlInfo.getNamespace(), stmtCnts + "\n");
        loadInfo.setXmlQuery(stmtCnts);
        return loadInfo;
    }
	
	/**
	 * XMLStreamReader.getText() 호출이 해당 event 내의 전체 text를 가져오는 것이 아니다.
	 * 예로 CDATA부가 길면 XMLStreamReader.getText()를 호출해도 CDATA의 일부 TEXT만 가져오고 다시 LOOP돌떄 여전히 EVENT가 CDATA가 된다.
	 * 이러한 특성 떄문에 아래와 같이 before, currentEvent 를 이용해서 체크해서 CRATA Tag시작부와 종료부를 처리한다
	 * @param stBuilder 쿼리를 조립할 BUILDER
	 * @param beforeEvent XMLStreamContants의 이전 event
	 * @param currentEvent XMLStreamContants의 현재 event
	 */
	private void checkCdataTag(StringBuilder stBuilder, int beforeEvent, int currentEvent) {
		if(currentEvent == XMLStreamConstants.CDATA) {
			if(beforeEvent != currentEvent) {
				stBuilder.append("<![CDATA[");
			}
		}else if(beforeEvent == XMLStreamConstants.CDATA) {
			if(beforeEvent != currentEvent) {
				stBuilder.append("]]>");
			}
		}
	}
	
	private boolean makeEndTag(StringBuilder stBuilder, String queryStartTag, String tag) {
		stBuilder.append("</").append(tag).append(">");
		if(queryStartTag.equalsIgnoreCase(tag)) return true;
		return false;
	}
	
	private boolean makeEndTagWithLineFeed(StringBuilder stBuilder, String queryStartTag, String tag) {
		stBuilder.append("</").append(tag).append(">\n");
		if(queryStartTag.equalsIgnoreCase(tag)) return true;
		return false;
	}
	
	//mybatis xml 파일에 CDATA를 사용한 경우 PARSING하지 못하는 현상이 있어서 cdata처리를 추가함
	private void makeQueryText(StringBuilder stBuilder, XMLStreamReader xmlStreamReader) {
		stBuilder.append(xmlStreamReader.getText());
	}

	 /**
     * 시작 tag가 <select, <update, <delete, <insert인 경우의 startTag를 조립하는 메소드
     * 조립하고 tag내의 attribute 정보를 map으로 리턴한다.
     *
     * @param stBuiler 문자열 조립을 위한 StringBuilder 객체
     * @param xmlStreamReader, xml file을 읽는 reader 객체
     * @param tag, xml 시작 tag 명
     */
    private Map<String, String> makeSqlStartTag(StringBuilder stBuiler, XMLStreamReader xmlStreamReader, String tag) {
        Map<String, String> attributeMap = new HashMap<String, String>();
        String attribute = null;
        String value = null;
        stBuiler.append("\t").append("<").append(tag).append(" ");
        for(int i=0; i<xmlStreamReader.getAttributeCount(); i++){
            attribute = xmlStreamReader.getAttributeLocalName(i);
            value = xmlStreamReader.getAttributeValue(i);
            attributeMap.put(attribute, value);
            stBuiler.append(attribute)
                    .append("=\"").append(value)
                    .append("\"");
            if(i != xmlStreamReader.getAttributeCount()-1){ // 마지막은 빈칸추가하지 않는다.
                stBuiler.append(" ");
            }
        }
        stBuiler.append(">");
        return attributeMap;
    }

    /**
     * 시작 tag가 <select, <update, <delete, <insert 가 아닌 경우의 startTag를 조립하는 메소드
     * @param stBuiler 문자열 조립을 위한 StringBuilder 객체
     * @param xmlStreamReader, xml file을 읽는 reader 객체
     * @param tag, xml 시작 tag 명
     */
    private void makeStartTag(StringBuilder stBuiler, XMLStreamReader xmlStreamReader, String tag) {
        stBuiler.append("<").append(tag).append(" ");
        for(int i=0; i<xmlStreamReader.getAttributeCount(); i++){
            stBuiler.append(xmlStreamReader.getAttributeLocalName(i))
                    .append("=\"").append(xmlStreamReader.getAttributeValue(i))
                    .append("\"");
            if(i != xmlStreamReader.getAttributeCount()-1){ // 마지막은 빈칸추가하지 않는다.
                stBuiler.append(" ");
            }
        }
        stBuiler.append(">");
    }

    private void makeStartTagWithLineFeed(StringBuilder stBuiler, XMLStreamReader xmlStreamReader, String tag) {
        stBuiler.append("<").append(tag).append(" ");
        for(int i=0; i<xmlStreamReader.getAttributeCount(); i++){
            stBuiler.append(xmlStreamReader.getAttributeLocalName(i))
                    .append("=\"").append(xmlStreamReader.getAttributeValue(i))
                    .append("\"");
            if(i != xmlStreamReader.getAttributeCount()-1){ // 마지막은 빈칸추가하지 않는다.
                stBuiler.append(" ");
            }
        }
        stBuiler.append(">\n");
    }

    private boolean isXmlTagSqlType(String logicalName){
        // TODO : 대소문자 구분 없이 가능하게??
        return logicalName.equals("select") || logicalName.equals("update") 
        		|| logicalName.equals("insert") || logicalName.equals("delete")
        		|| logicalName.equals("sql");
    }

    /**
     * runtime 쿼리 로딩의 경우 서버 시작시간 이후의 쿼리를 refresh하라는 요청을 받으면
     * 현재까지 로딩된 리스트 모두를 reload가 필요하다고 정보만 갱신한다.
     * 추후 실제 요청시에 이곳의 정보가 reload가 필요하다고 되어있으면, 다시 로딩을 한다.
     */
    /* (non-Javadoc)
     * @see devonframe.dataaccess.mybatis.DbQueryLoader#onQueryRefresh(java.util.Date)
     */
    @Override
    public void onQueryRefresh(Date serverStartupTime) {
        for(String key : reloadInfo.keySet()){
            reloadInfo.put(key, true);
        }
    }

    /**
     * runtime 쿼리 로딩의 경우 특정 SqlList를 받아서 refresh를 하라는 요청을 받으면
     * 현재까지 로딩된 리스트에서 해당 sqlid가 reload가 필요하다고 정보만 갱신한다.
     * 추후 실제 요청시에 이곳의 정보가 reload가 필요하다고 되어있으면, 다시 로딩을 한다.
     */
    /* (non-Javadoc)
     * @see devonframe.dataaccess.mybatis.DbQueryLoader#onQueryRefreshByTag(java.util.List)
     */
    @Override
    public void onQueryRefreshByTag(List<QueryEntity> reqList) {
        for(QueryEntity queryVo : reqList) {
            if(reloadInfo.containsKey(queryVo.getQueryId())){
                reloadInfo.put(queryVo.getQueryId(), true);
                reloadInfo.put(queryVo.getStmtId(), true);
            }
        }
    }

    @Override
    public boolean needToReload(String sqlId) {
        int dotIndex = sqlId.lastIndexOf(".");
        if(autoReload && dotIndex > 0){
            String namespace = sqlId.substring(0, sqlId.lastIndexOf("."));
            QueryEntity sqlInfo = new QueryEntity();
            sqlInfo.setNamespace(namespace);
            sqlInfo.setStmtId(sqlId.substring(sqlId.lastIndexOf(".") + 1));
            Resource resource = getMapperFile(sqlInfo);
            long beforeLastModified = resourceRefreshMap.get(sqlId);
            // 예전에 sql문 로딩 시의 file 변경일시와 비교해서 파일이 더 최신이면 reload 대상으로 간주한다.
            try {
                if(resource.lastModified() > beforeLastModified){
                    return true;
                }
            } catch (IOException ignore) {}
        }
        return reloadInfo.containsKey(sqlId) && reloadInfo.get(sqlId);
    }
    
    /**
	 * Sql element를 sql fragment에 저장
	 * @param stmtCnts sql문
	 * @param namespace namespace id
	 * @param stmtId sql아이디
	 */
	private void loadSqlElement(String stmtCnts, String namespace, String stmtId) {
		//include refid로 다른 sql문을 참조하는지 확인
		Pattern includePattern = Pattern.compile("(include refid)[=\"'\\w\\s]+.[\\w\\s]+");
		Matcher matcher = includePattern.matcher(stmtCnts);
		
		try{
			while(matcher.find()) {
				//include refid를 저장
				String[] includes = matcher.group().trim().split("[\"'.]");
				//sqlFragment에 없을 경우 namespace를 제외하고 id만 저장(쿼리조회용)
				String sqlNamespace = includes[1];
				String sqlStmtId = includes[2];
				
				QueryEntity queryEntity = new QueryEntity();
				queryEntity.setNamespace(sqlNamespace);
				queryEntity.setStmtId(sqlStmtId);
				
				List<QueryEntity> queryInfos = new ArrayList<QueryEntity>(1);
				queryInfos.add(queryEntity);
				
				loadQueryAtRuntime(queryInfos, false);
			}
		}catch(Exception ex) {
			throw new RuntimeException("Fail to get sql element refered in "+namespace+"."+stmtId, ex);
		}
	}
	

	@Override
	public void onQueryInitialLoad() {
		log.info("Runtime 에 쿼리를 로딩하므로 초기로딩 시 아무작업도 하지 않는다.");
	}

}
