package user.dev.core.web.servlet;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class UserSyncServlet extends HttpServlet {

	static final Logger log = LoggerFactory.getLogger(UserSyncServlet.class);
	
	
	 protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		 doService(req,resp);
	 }
	 
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doService(req,resp);
	}
	
	protected void doService(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		byte[] readByte = req.getInputStream().readAllBytes();
		log.info("인입메시지:"+new String(readByte));
//		HandlerExecutionChain mappedHandler = getHandler(req);
//		if (!mappedHandler.applyPreHandle(req, res)) {
//	        return;
//	    }
		
		Map<String,String[]> tmpMap = req.getParameterMap();
		String msg = tmpMap.keySet().stream().reduce("",(k1,k2)->{
			return (k1.length()>0?k1+" | ":k1) +"["+k2+"]"+ Arrays.asList(tmpMap.get(k2)).stream().collect(Collectors.joining(","));
		});
		
		log.info("응다메시지:"+msg);
		
		resp.setContentType("text/html");
		resp.setCharacterEncoding("utf-8");
		resp.getWriter().write("동기 처리 완료 = "+ msg);
	}
}
