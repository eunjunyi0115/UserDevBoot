package user.dev.core.server;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 내장 웹서버의 동작을 커스터마이징 할수 있는 인터페이스.
 * 주로 포트, 컨텍스트 경로, ssl설정, 커넥션 제한 등을 변경 할수 있다.
 * 프로그램 적으로 동적변경을 할떄 사용하면 좋을듯
 * 
 * 여기서 포트를 정하면 포트가 추가적으로 생성된다.
 */
@Component
@Slf4j
public class CustomTomcatServletWebServerFactory implements WebServerFactoryCustomizer<TomcatServletWebServerFactory>{

	final static int port = 8090;
	
	@Override
	public void customize(TomcatServletWebServerFactory factory) {
		Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
		connector.setPort(port);
		factory.addAdditionalTomcatConnectors(connector);
		
		//factory.setContextPath("/custom"); //컨텍스트를 바꿈. 모든 포트에 적용됨.
		
		factory.addConnectorCustomizers(ctr->{
			ctr.setProperty("maxThreads", "200");
			ctr.setProperty("acceptCount", "100");
		});
		
		//log.debug("서버 설정 커스터마이징 처리 완료");
		
	}

}
