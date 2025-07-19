package user.dev;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
@MapperScan({"user.dev.mapper","other.dev.mapper"})
public class UserDevApplication extends SpringBootServletInitializer{

	public static void main(String[] args) {
		SpringApplication.run(UserDevApplication.class, args);
	}
	
	@Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(UserDevApplication.class);
    }
    
}
