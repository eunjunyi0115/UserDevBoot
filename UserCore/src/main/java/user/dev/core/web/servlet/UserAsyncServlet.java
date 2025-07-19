package user.dev.core.web.servlet;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class UserAsyncServlet extends HttpServlet {

	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		//비동기 컨텍스트
		AsyncContext asyncContext = req.startAsync();
		asyncContext.start(()->{
			try {
				
				Map<String,String[]> tmpMap = req.getParameterMap();
				String msg = tmpMap.keySet().stream().reduce("",(k1,k2)->{
					return (k1.length()>0?k1+" | ":k1) +"["+k2+"]"+ Arrays.asList(tmpMap.get(k2)).stream().collect(Collectors.joining(","));
				});
				
				Thread.sleep(5000);
				HttpServletResponse response = (HttpServletResponse)asyncContext.getResponse();
				response.setContentType("text/plain");
				response.setCharacterEncoding("utf-8");
				response.getWriter().write("비동기 처리 완료 = "+ msg);
			}catch(Throwable e) {
				e.printStackTrace();
			}finally {
				asyncContext.complete(); //반드시 호출.
				System.out.println("ASYNC 종료");
			}
		});
		System.out.println("UserAsyncServlet doPost 종료");
    }
	
}
