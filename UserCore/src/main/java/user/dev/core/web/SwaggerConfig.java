package user.dev.core.web;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {
	@Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                		.version("2.0.0") // ✔ 실제 API 버전
                        .title("내 API 문서")
                        .description("스프링부트 API 문서입니다")
                        .version("v1.0.0"))
                .externalDocs(new ExternalDocumentation()
                        .description("GitHub")
                        .url("https://github.com/myproject"));
    }
	
	//GroupedOpenApi를 사용해 여러 그룹 API 분리 가능
	@Bean
    public GroupedOpenApi customGroup() {
        return GroupedOpenApi.builder()
            .group("user-group")
            .packagesToScan("user.dev.*")
            .pathsToMatch("/api/dev/**")
            .build();
    }
	
	@Bean
    public GroupedOpenApi otherGroup() {
        return GroupedOpenApi.builder()
            .group("other-group")
            .packagesToScan("other.*")
            .pathsToMatch("/api/other/**")
            .build();
    }
}
