package user.dev.core.datasource.queryloader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.io.Resource;

/**
 * 기본 Mapper File Resolver, 이 파일은 mapperNamespace명과 xml 파일명이 같은 file을 하나만 찿는다.
 * 따라서 이 MapperFileResolver 를 사용하려면 디렉토리 구분에 관계없이 sql xml 파일명이 유니크 해야한다. 
 * 또한 xml 파일명과 mybatis 의 mapper namespace 명이 동일 해야 한다.
 * 긍뢰는 별도로 구현해야 한다.
 */
public class DefaultMapperFileResolver implements MapperFileResolver {

	protected Map<String,Resource> resourceMap = new ConcurrentHashMap<String,Resource>();
	
	@Override
	public Resource resolveMapperFile(Resource[] mappers, QueryEntity sqlInfo) {
		Resource result = resourceMap.get(sqlInfo.getNamespace());
		if(result == null) {
			String fileName = findMapperFileName(sqlInfo.getNamespace());
			for(Resource mapper: mappers) {
				if(mapper.getFilename().equals(fileName)) {
					result = mapper;
					resourceMap.put(sqlInfo.getNamespace(), mapper);
					break;
				}
			}
		}
		return result;
	}
	
	public String findMapperFileName(String mapperNamespace) {
		return mapperNamespace+".xml";
	}

}
