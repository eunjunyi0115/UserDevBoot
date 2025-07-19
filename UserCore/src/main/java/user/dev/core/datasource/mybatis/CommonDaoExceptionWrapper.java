package user.dev.core.datasource.mybatis;

import org.apache.ibatis.exceptions.TooManyResultsException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.TransactionTimedOutException;

import jakarta.persistence.QueryTimeoutException;

public class CommonDaoExceptionWrapper implements ExceptionWrapper{

	public static final String TOO_MANY_RESULT_ERROR_CODE = "DSOWRP001";
	public static final String DUPLICATE_KEY_ERROR_CODE = "DSOWRP002";
	public static final String TRANSACTION_TIMED_OUT_ERROR_CODE = "DSOWRP003";
	
	/**
	 * 익셉션 커스터 마이징
	 */
	public RuntimeException execute(RuntimeException e) {
		Exception ext = e;
		while(ext != null) {
			if(ext instanceof TooManyResultsException) {
				return new RuntimeException("다건이 리턴되었다",e);
			}
			if(ext instanceof DuplicateKeyException) {
				return new RuntimeException("중복 오류이다",e);
			}
			if(ext instanceof TransactionTimedOutException || ext instanceof QueryTimeoutException) {
				return new RuntimeException("타임아웃 오류이다",e);
			}
			
			ext = (Exception)ext.getCause();
		}
		return null;
	}

}
