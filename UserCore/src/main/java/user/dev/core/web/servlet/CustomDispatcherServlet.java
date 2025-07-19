package user.dev.core.web.servlet;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomDispatcherServlet extends DispatcherServlet {

	public CustomDispatcherServlet(WebApplicationContext context) {
		super(context);
	} 
	//AnnotationConfigWebApplicationContext
	
    @Override
    protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out.println("[CustomDispatcherServlet] Dispatching: " + request.getRequestURI()+","+request.getContextPath());
        
        super.doDispatch(request, response);
    }

    @Override
    protected void initStrategies(ApplicationContext context) {
        //this.setDetectAllHandlerMappings(true); //모든 핸들러매핑사용
        //this.setDetectAllHandlerAdapters(true); //모든 핸들러아답타사용.
       // this.setContextConfigLocation("classpath:/CustomDispatcherServlet-servlet.xml");
//        HandlerMapping requestMappingHandlerMapping = context.getBean(RequestMappingHandlerMapping.class); //디폴트 핸들러(컨트롤) 매핑
//        HandlerAdapter requestMappingHandlerAdapter = context.getBean(RequestMappingHandlerAdapter.class); //디폴트 핸들러(컨트롤) 매핑
//        this.setHandlerMappings(List.of(requestMappingHandlerMapping));
 //       this.setHandlerAdapters(List.of(requestMappingHandlerAdapter));
        super.initStrategies(context);
    }
    
    @Override
    protected HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
        HandlerExecutionChain handler = super.getHandler(request);
        if (handler != null) {
        	//log.debug("Handler found for request: {}", handler.getHandler().getClass().getName());
        } else {
        	//log.warn("No handler found for request: {}", request.getRequestURI());
        }
        return handler;
    }
    
    @Override
    protected void render(ModelAndView mv, HttpServletRequest request, HttpServletResponse response) throws Exception {
//        //log.debug("Rendering view: {}", mv != null ? mv.getViewName() : "null");
        super.render(mv, request, response);
    }
    
}