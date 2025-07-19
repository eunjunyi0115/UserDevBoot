package user.dev.core.datasource.context;

import lombok.extern.slf4j.Slf4j;

/**
 * 롤백을 위한 RollbackParameter 객체를 관리 하는 역활을 수행하는 클래스
 * set메소드에 의해 트랜잭션을 Rollabck처리하며 이유(reason)과 호출한 클래스(caller) 를 전달 받는다.
 * setTransactionRollbnack 메소드에 의해 RollbackParameter 객체가 생성되고 객체 생성 유무로 rollback 마킹 여부를 반환한다.
 */
@Slf4j
public class RollbackContext {
	private static ThreadLocal<RollbackParameter> transactionRollbackContext = new ThreadLocal<RollbackParameter>();
	
	public static void clear() {
		if(transactionRollbackContext.get()!=null) {
			transactionRollbackContext.set(null);
		}
		transactionRollbackContext.remove();
	}
	
	public static void setTransactionRollback(String reason, Object caller) {
		RollbackParameter rollbackParameter = new RollbackParameter();
		rollbackParameter.setReason(reason);
		rollbackParameter.setCaller(caller);
		transactionRollbackContext.set(rollbackParameter);
	}
	
	public static boolean isTransactionRollbackMarked() {
		RollbackParameter rollbackParameter = transactionRollbackContext.get();
		if(rollbackParameter!=null) {
			log.debug("ROLLBACL MARKED: Reason["+rollbackParameter.getReason()+"]: Caller Class:["+rollbackParameter.getCaller()+"]");
			return true;
		}
		return false;
	}
}
