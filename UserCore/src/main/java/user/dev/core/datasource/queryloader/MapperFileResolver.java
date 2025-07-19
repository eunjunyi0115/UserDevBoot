package user.dev.core.datasource.queryloader;

import org.springframework.core.io.Resource;

public interface MapperFileResolver {

	public Resource resolveMapperFile(Resource[] mappers, QueryEntity sqlInfo);
}
