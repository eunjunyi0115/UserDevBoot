package user.dev.core.datasource.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import user.dev.core.datasource.DataSourceProperties;
import user.dev.core.datasource.queryloader.DefaultMapperFileResolver;
import user.dev.core.datasource.queryloader.XmlRuntimeQueryLoader;

//@Configuration
public class XmlRuntimeQueryLoaderAutoConfiguration {
	@Autowired
	DataSourceProperties dataSourceProperties;
	
	//XML 프로퍼티 사용해야함.
	@Bean("xmlRuntimeQueryLoader")
	public XmlRuntimeQueryLoader xmlRuntimeQueryLoader() {
		XmlRuntimeQueryLoader loader = new XmlRuntimeQueryLoader();
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		try {
			String mapperLocation = dataSourceProperties.getDatasourceList().get(DataSourceAutoconfigure.FIRST_DATASOURCE_NAME).mybatis().mapperLocations();
			Resource[] resource = resolver.getResources(mapperLocation);
			loader.setMapperLocations(resource);
		}catch(IOException e) {
			throw new RuntimeException("쿼리로더 생성중 오류 발생했다.");
		}
		loader.setAutoReload(true);
		loader.setMapperFileResolver(new DefaultMapperFileResolver());
		
		return loader;
	}
}
