package user.dev.core.datasource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;


public class InterceptedDataSourceTransactionManager extends CompositeDataSourceTransactionManager {

	@Autowired
	TransactionHelper transactionHelper;
	
	@Override
	public DataSourceTransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException{
		return super.getTransaction(transactionHelper.getTransactionDefinition(definition));
	}
	
	@Override
	public void commit(TransactionStatus status) throws TransactionException{
		transactionHelper.beforeCommit(status);
		super.commit(status);
		transactionHelper.afterCommit(status);
	}
	
	@Override
	public void rollback(TransactionStatus status) throws TransactionException{
		transactionHelper.beforeRollback(status);
		if(!status.isCompleted()) {
			super.rollback(status);
		}
		transactionHelper.afterRollback(status);
	}
}
