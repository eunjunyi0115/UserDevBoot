package user.dev.controller;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import user.dev.core.datasource.config.DataSourceAutoconfigure;
import user.dev.core.datasource.mybatis.CommonDao;
import user.dev.core.datasource.mybatis.CommonDaoMultiSqlSessionFactory;
import user.dev.core.datasource.mybatis.CommonDaoSqlSessionFactoryBean;
import user.dev.dto.model.UserTest;

@Slf4j
@RestController
@Tag(name = "사용자 관리", description = "사용자 CRUD API")
public class UserController {
	
	@Autowired
	private CommonDao commDao;
	
	@Autowired
	private CommonDaoMultiSqlSessionFactory factory;
	
	@RequestMapping("/telegram/call")
	@Operation(summary = "사용자 목록 조회", description = "모든 사용자 정보를 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
	public ResponseEntity<List<UserTest>> calltelegram(@RequestParam Map inMap) {
		log.info(""+inMap);
		
		List<UserTest> result = commDao.selectList("UserTest.findAll");
		//List<UserTest> result = userTestMapper.findAll();
		HttpHeaders headers = new HttpHeaders();
	    headers.add("X-Custom-Header", "MyHeaderValue");
	    return new ResponseEntity<>(result, headers, HttpStatus.OK);
	}
	
	@RequestMapping( "/sync/call")
	@Transactional
	@Operation(summary = "사용자 call Msg", description = "call Msg를 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
	public ResponseEntity<List<UserTest>> callMsg(@RequestParam Map inMap) {
		log.info(""+inMap);
		//TransactionSynchronizationManager.bindResource(inMap, inMap);
		log.info("getTransactionName:"+TransactionSynchronizationManager.getCurrentTransactionName());
		log.info("getResourceMap:"+TransactionSynchronizationManager.getResourceMap());
		log.info("getSynchronizations:"+TransactionSynchronizationManager.getSynchronizations());
		//TransactionSynchronizationManager.getResource(inMap)
		log.info("isActualTransactionActive:"+TransactionSynchronizationManager.isActualTransactionActive());
		log.info("isCurrentTransactionReadOnly:"+TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		log.info("isSynchronizationActive:"+TransactionSynchronizationManager.isSynchronizationActive());
		
		List<UserTest> result =  null;
		Map<String,CommonDaoSqlSessionFactoryBean> map1 = factory.getCommonDaoSqlSessionFactoryBeanMap();
		log.info("CommonDaoMultiSqlSessionFactory: {}",map1);
		
		try {
			log.info("작업전:{}",	map1.get(DataSourceAutoconfigure.FIRST_DATASOURCE_NAME).getObject());
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			result = commDao.selectList("UserTest.findAll");
		}catch(Exception e) {
			e.printStackTrace();
		}
		try {
			log.info("작업후:{}",	map1.get(DataSourceAutoconfigure.FIRST_DATASOURCE_NAME).getObject());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		HttpHeaders headers = new HttpHeaders();
	    headers.add("X-Custom-Header", "MyHeaderValue");
	    headers.setContentType(MediaType.APPLICATION_JSON);

	    return new ResponseEntity<>(result, headers, HttpStatus.OK); 
	}
	
	@RequestMapping("/async/call")
	//@ResponseBody
	@Operation(summary = "사용자 callableMsg", description = "callableMsg를 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
	public Callable<Map> callableMsg(@RequestParam Map inMap) {
		log.info("인입파라미터"+inMap);
		return ()->{
			log.info("Callable 시작");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			log.info("Callable 종료");
			inMap.put("key","value");
			return inMap;
		};
	}
	
	@RequestMapping("/async/deferred")
	@Operation(summary = "사용자 deferredMsg", description = "deferredMsg를 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
	public DeferredResult<Map<String,String>> deferredMsg(@RequestParam Map inMap) {
		log.info("인입파라미터"+inMap);
		var output = new DeferredResult<Map<String,String>>();
		 
		new Thread(() -> {
            try {
                Thread.sleep(2000);
                output.setResult(inMap);
            } catch (InterruptedException e) {
                output.setErrorResult("에러 발생");
            }
        }).start();
		log.info("deferred 종료");
		return output;
	}
}
