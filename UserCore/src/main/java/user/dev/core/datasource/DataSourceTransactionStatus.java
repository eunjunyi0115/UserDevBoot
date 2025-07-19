package user.dev.core.datasource;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.Assert;

/**
 * 원본 MultiTransactionStatus 이거 확인해 볼 필요 있음.
 */
public class DataSourceTransactionStatus implements TransactionStatus {

	//첫번쨰 트랜잭션 매니저
	private final PlatformTransactionManager mainTransactionManager;
	//전체 트랜잭션 과 상태 저장
	private final Map<PlatformTransactionManager, TransactionStatus> transactionStatus = new ConcurrentHashMap<PlatformTransactionManager, TransactionStatus>();
	//동기화변수
	private boolean newSyncronization;
	
	public DataSourceTransactionStatus(PlatformTransactionManager mainTransactionManager) {
		this.mainTransactionManager = mainTransactionManager;
	}
	
	public TransactionStatus getMainTransactionStatus() {
		return transactionStatus.get(mainTransactionManager);
	}
	
	public Map<PlatformTransactionManager, TransactionStatus> getTransactionStatus(){
		return transactionStatus;
	}
	public TransactionStatus getTransactionStatus(PlatformTransactionManager transactionManager){
		return transactionStatus.get(transactionManager);
	}
	
	public void setNewSyncronization() { this.newSyncronization = true; }
	public boolean isNewSyncronization() { return newSyncronization;}

	//트랜젝션 매니저 추가
	/**
	 * 동일한 TransactionDefinition(조건) 으로 두 트랜잭션을 획득한다.
	 * @param definition
	 * @param transactionManager
	 */
	public void registerTransactionManager(TransactionDefinition definition, PlatformTransactionManager transactionManager) {
		transactionStatus.put(transactionManager, transactionManager.getTransaction(definition));
	}
	
	public void commit(PlatformTransactionManager transactionManager) {
		transactionManager.commit(getTransactionStatus(transactionManager));
	}
	
	public void rollback(PlatformTransactionManager transactionManager) {
		transactionManager.rollback(getTransactionStatus(transactionManager));
	}
	
	/**
	 * getMainTransactionStatus() 의 상태를 확인하는 이유는 두 트랜잭션이 모두 동일한 상태로 관리 되기 때문이다.
	 */
	public boolean isRollbackOnly() {
		return getMainTransactionStatus().isRollbackOnly();
	}
	
	public boolean isCompleted() {
		return getMainTransactionStatus().isCompleted();
	}
	
	//TRUE: 새롭게 시작한 트랜잭션의 경우.
	//FALSE: 기존 트랜잭션에 참여 하고 있는 경우
	public boolean isNewTransaction() {
		return getMainTransactionStatus().isNewTransaction();
	}
	
	public boolean hasSavepoint() {
		return getMainTransactionStatus().hasSavepoint();
	}
	
	public void setRollbackOnly() {
		for(TransactionStatus status: transactionStatus.values()) {
			status.setRollbackOnly();
		}
	}
	
	@Override
	public Object createSavepoint() throws TransactionException {
		SavePoints points = new SavePoints();
		for(TransactionStatus status: transactionStatus.values()) {
			points.save(status);
		}
		return points;
	}

	@Override
	public void rollbackToSavepoint(Object savepoint) throws TransactionException {
		SavePoints points = (SavePoints)savepoint;
		points.rollback();
	}

	@Override
	public void releaseSavepoint(Object savepoint) throws TransactionException {
		SavePoints points = (SavePoints)savepoint;
		points.release();
	}
	
	private static class SavePoints{
		private final Map<TransactionStatus,Object> savePoints = new HashMap<TransactionStatus,Object>();
		
		private void addSavePoint(TransactionStatus status,Object savepoint) {
			Assert.notNull(savepoint, "필수 입력 항목이 누락되었다");
			savePoints.put(status, savepoint);
		}
		
		private void save(TransactionStatus status) {
			Object savepoint = status.createSavepoint();
			addSavePoint(status, savepoint);
		}
		
		public void rollback() {
			for(TransactionStatus status:savePoints.keySet()) {
				status.rollbackToSavepoint(savepointFor(status));
			}
		}
		
		public Object savepointFor(TransactionStatus status) {
			return savePoints.get(status);
		}
		
		public void release() {
			for(TransactionStatus status:savePoints.keySet()) {
				status.releaseSavepoint(savepointFor(status));
			}
		}
	
	}

}
