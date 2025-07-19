package user.dev.core.datasource;

import org.springframework.transaction.support.TransactionSynchronizationManager;

public class SyncronizationManager {
	
	//현재 쓰레드에서 트랜젝션 싱크로나이제이션 활성화.
	public void initSynchronization(){
		TransactionSynchronizationManager.initSynchronization();
	}
	
	//현재 쓰레드에서 트랜젝션 싱크로나이제이션 활성화 여부
	public boolean isSynchronizationActive(){
		return TransactionSynchronizationManager.isSynchronizationActive();
	}
	
	public void clearSynchronization() {
		TransactionSynchronizationManager.clear();
	}
	
}
