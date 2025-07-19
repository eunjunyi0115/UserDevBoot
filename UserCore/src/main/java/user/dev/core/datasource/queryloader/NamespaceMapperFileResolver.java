package user.dev.core.datasource.queryloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.springframework.core.io.Resource;

/**
 * Namespace 기반의 mapper file resolver 이 파일은 mapperNamespace 명과 xml 파일명이 같은 file을 찿는다.
 * file을 찿아서 파일내에 statement id 가 존재 하는지 확인해서 있으면 해당 file을 선택한다.
 * 없을 경우 동일 이름의 다른 xml 파일을 찿아서 다시 확인한다.
 * 모든 파일을 찿아도 없을 경우 null을 리턴한다.
 * 따라서 이 MapperFileResolver 을 사용하면 디렉토리가 다르다면 file명이 동일 할수 있다.
 * 단, file명 = namespace 명 이어야 하고, namespace.statementid는 전체에서 유니크 해야한다.
 */
public class NamespaceMapperFileResolver implements MapperFileResolver{

	protected Map<String,Resource> resourceMap = new ConcurrentHashMap<String, Resource>();
	protected XMLInputFactory factory = XMLInputFactory.newInstance();
	
	@Override
	public Resource resolveMapperFile(Resource[] mappers, QueryEntity sqlInfo) {
		Resource result = resourceMap.get(sqlInfo.getQueryId());
		if(result == null) {
			String fileName = findMapperFileName(sqlInfo.getNamespace());
			for(Resource mapper: mappers) {
				if(mapper.getFilename().equals(fileName) && containsStatement(mapper, sqlInfo)) {
					result = mapper;
					resourceMap.put(sqlInfo.getQueryId(), mapper);
					break;
				}
			}
		}
		return null;
	}

	private boolean containsStatement(Resource mapperFile, QueryEntity sqlInfo) {
		XMLStreamReader xmlStreamReader = null;
		InputStream resourceInputStream = null;
		try {
			resourceInputStream = mapperFile.getInputStream();
			xmlStreamReader = factory.createXMLStreamReader(resourceInputStream);
			int event = 0;
			boolean sqlFound = false;
			String tag = "";
			while(xmlStreamReader.hasNext()) {
				try {
					event = xmlStreamReader.next();
				}catch(Exception ex) {
					if(event == XMLStreamConstants.DTD && ex.getCause() instanceof UnknownHostException) {
					}else {
						throw ex;
					}
				}
				switch(event) {
					case XMLStreamConstants.START_ELEMENT:
						tag = xmlStreamReader.getLocalName();
						if("mapper".equals(tag)) {
							if(xmlStreamReader.getAttributeCount()==0) {
								throw new RuntimeException("Mapper file namespace attribute must be exist inthe mapper tag. checkMapper FILE("+mapperFile.getFilename()+")");
							}
							if(!"namespace".equals(xmlStreamReader.getAttributeName(0)) || 
							   !sqlInfo.getNamespace().equals(xmlStreamReader.getAttributeValue(0))) {
								throw new RuntimeException("Mapper file namespace is not matched. expect[namespace="+sqlInfo.getNamespace()+"], but ["
										+ xmlStreamReader.getAttributeName(0)+"="+ xmlStreamReader.getAttributeValue(0)+" is setted in "+mapperFile.getFilename()+"]");
							}
						}else { //mapper내의 <insert>, <delete>, <update>, <select>  tag 면
							if(isXmlTagSqlType(tag) && "id".equals(xmlStreamReader.getAttributeLocalName(0)) 
									&& sqlInfo.getStmtId().equals(xmlStreamReader.getAttributeValue(0))) {
								sqlFound = true;
							}
						}
						break;
						
					default: break;
				}
				if(sqlFound)  break;
			}
			return sqlFound;
		}catch(RuntimeException re) {
			throw re;
		}catch(Exception e) {
			throw new RuntimeException("Fail to get Xml file reader from mapper file["+mapperFile.getFilename()+"]", e);
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
	}
	
	private boolean isXmlTagSqlType(String logicalName) {
		return logicalName.equals("select") || logicalName.equals("update") || 
				logicalName.equals("insert") || logicalName.equals("delete");
	}
	
	public String findMapperFileName(String mapperNamespace) {
		return mapperNamespace+".xml";
	}
}
