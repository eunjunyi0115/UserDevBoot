package user.dev.core.datasource;

import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TransactionHelper {

	public TransactionDefinition getTransactionDefinition(TransactionDefinition definition) {
		
		//타임아웃 값을 세팅 하도록 체트 시간은 elapise 계산후 세팅.
		DefaultTransactionDefinition def = new DefaultTransactionDefinition(definition);
		def.setTimeout(5000);
		return def;
	}
	
	
	public void beforeCommit(TransactionStatus status) {
		if(status.isNewTransaction()) {
			log.info("beforeCommit("+status+")");
		}
	}
	
	public void afterCommit(TransactionStatus status) {
		if(status.isNewTransaction()) {
			log.info("afterCommit("+status+")");
		}
	}
	
	public void afterRollback(TransactionStatus status) {
		if(status.isNewTransaction()) {
			log.info("afterRollback("+status+")");
		}
	}
	
	public void beforeRollback(TransactionStatus status) {
		if(status.isNewTransaction()) {
			log.info("beforeRollback("+status+")");
		}
	}
}
