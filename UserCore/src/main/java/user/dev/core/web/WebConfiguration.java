package user.dev.core.web;

import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.extern.slf4j.Slf4j;
import user.dev.core.web.filter.UserLogFilter;
import user.dev.core.web.servlet.CustomDispatcherServlet;
import user.dev.core.web.servlet.UserAsyncServlet;
import user.dev.core.web.servlet.UserSyncServlet;

@Configuration
public class WebConfiguration {
	
	//필터 적용
	@Bean
	public FilterRegistrationBean<UserLogFilter> getUserLogFilter(){
		FilterRegistrationBean<UserLogFilter>  filter = new FilterRegistrationBean<UserLogFilter> ();
		filter.setFilter(new UserLogFilter());
		filter.addUrlPatterns("/*");
		filter.setOrder(1);
		filter.setName("UserLogFilter");
		return filter;
	}
	
	//Customer Dispatch 서블릿 등록
	@Bean
	public ServletRegistrationBean<CustomDispatcherServlet> getCustomDispatcherServlet(ApplicationContext context){
		AnnotationConfigWebApplicationContext servletAppContext = new AnnotationConfigWebApplicationContext();
        //servletAppContext.setParent(context); //컨텍스트공유
        servletAppContext.scan("other.dev"); //스켄
        //servletAppContext.refresh();  
        servletAppContext.register(CustomWebConfig.class); // Java 기반 설정 등록
        
		var syncServletBean = 
				new ServletRegistrationBean<CustomDispatcherServlet>(new CustomDispatcherServlet(servletAppContext),"/telegram/*");
		//syncServletBean.setName("CustomDispatcherServlet");
		//syncServletBean.setLoadOnStartup(1);
		//syncServletBean.addInitParameter("initParam", "initData");
		return syncServletBean;
	}
	
	
	//Custom 싱크 서플릿.
	@Bean
	public ServletRegistrationBean<UserSyncServlet> getUserSyncServlet(){
		var syncServletBean = 
				new ServletRegistrationBean<UserSyncServlet>(new UserSyncServlet(),"/servlet/sync/*");
		syncServletBean.setName("UserSyncServlet");
		syncServletBean.setLoadOnStartup(2);
		syncServletBean.addInitParameter("initParam", "initData");
		return syncServletBean;
	}
	
	//Customer 어싱크 서플릿
	@Bean
	public ServletRegistrationBean<UserAsyncServlet> getUserAsyncServlet(){
		var syncServletBean = 
				new ServletRegistrationBean<UserAsyncServlet>(new UserAsyncServlet(),"/servlet/async/*");
		syncServletBean.setName("UserAsyncServlet");
		syncServletBean.setLoadOnStartup(3);
		syncServletBean.addInitParameter("initParam", "initData");
		syncServletBean.setAsyncSupported(true); //비동기 지원 명시
		return syncServletBean;
	}
	
	@Configuration
    @EnableWebMvc
    @ConditionalOnWebApplication
    public static class CustomWebConfig implements WebMvcConfigurer {
        @Override
        public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
            converters.add(new MappingJackson2HttpMessageConverter()); // JSON 처리 지원
        }
        
//        @Bean
//        public SomeCustomController someCustomController() {
//            return new SomeCustomController();
//        }
    }
	
}
