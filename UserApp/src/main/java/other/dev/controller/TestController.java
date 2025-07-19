package other.dev.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import other.dev.dto.model.OtherTest;
import other.dev.mapper.OtherTestMapper;
import user.dev.dto.model.UserTest;

@Slf4j
@RestController
public class TestController {

	@Autowired
	private OtherTestMapper otherTestMapper;
	
	@RequestMapping("/call")
	public ResponseEntity<List<OtherTest>> calltelegram(@RequestParam Map inMap) {
		log.info("시작시작시작"+inMap);
		List<OtherTest> result = otherTestMapper.findAll();
		HttpHeaders headers = new HttpHeaders();
	    headers.add("X-Custom-Header", "MyHeaderValue");
	    return new ResponseEntity<>(result, headers, HttpStatus.OK);
	}
}
