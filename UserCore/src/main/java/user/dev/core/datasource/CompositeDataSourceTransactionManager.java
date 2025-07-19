package user.dev.core.datasource;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.HeuristicCompletionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import user.dev.core.datasource.context.RollbackContext;

@Slf4j
@NoArgsConstructor
public class CompositeDataSourceTransactionManager implements PlatformTransactionManager, InitializingBean{

	private List<PlatformTransactionManager> transactionManagers = new ArrayList<PlatformTransactionManager>();
	
	private List<DataSource> datasources;	
	
	private SyncronizationManager syncronizationManager;
	
	public CompositeDataSourceTransactionManager(List<DataSource> dataSources) {
		this(new SyncronizationManager(), dataSources);
	}
	
	public CompositeDataSourceTransactionManager(SyncronizationManager syncronizationManager, List<DataSource> dataSources) {
		this.syncronizationManager = syncronizationManager;
		this.datasources = datasources;
		afterPropertiesSet();
	}
	
	public void setDataSources(List<DataSource> datasources) {
		this.datasources = datasources;
	}

	@Override
	public void afterPropertiesSet() {
		if(syncronizationManager==null) syncronizationManager = new SyncronizationManager();
		if(datasources==null) {
			throw new IllegalArgumentException("datasource required");
		}
		
		for(DataSource datasource: datasources) {
			DataSourceTransactionManager manager = new DataSourceTransactionManager();
			manager.setDataSource(datasource);
			//트랜잭션에 참여한 (Participated) 내부 트랜잭션에서 예외가 발생 했을떄, 전체(Global) 트랜잭션을 롤백할지 여부를 지정하는 설정
			//기본값: true(전체트랜잭션롤백), false(전체틀내잭션 계속 진행가능)
			manager.setGlobalRollbackOnParticipationFailure(false);
			transactionManagers.add(manager);
		}
	}

	@Override
	public DataSourceTransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
		DataSourceTransactionStatus mts = null;
		try { 
			//메인트랜잭션
			mts = new DataSourceTransactionStatus(transactionManagers.get(0));
			if(!syncronizationManager.isSynchronizationActive()) {
				syncronizationManager.initSynchronization();
				mts.setNewSyncronization();
			}
			for(PlatformTransactionManager manager: transactionManagers) {
				mts.registerTransactionManager(definition, manager);
			}
		}catch(TransactionException te) {
			mts.flush();
			throw te;
		}
		return mts;
	}

	@Override
	public void commit(TransactionStatus status) throws TransactionException {
		DataSourceTransactionStatus mts = (DataSourceTransactionStatus)status;
		if(!mts.isRollbackOnly()) {
			if(RollbackContext.isTransactionRollbackMarked()) {
				mts.setRollbackOnly();
			}
		}
		boolean commit = true;
		Exception commitException = null;
		PlatformTransactionManager commitExceptionTransactionManager = null;
		
		List<PlatformTransactionManager> reverseManagers = transactionManagers.reversed();
		for(PlatformTransactionManager manager: reverseManagers) {
			if(commit) {
				try {
					mts.commit(manager);
				}catch(Exception ex) {
					commit = false;
					commitException = ex;
					commitExceptionTransactionManager = manager;
				}
			}else {
				try {
					//커밋실패 후는 모두 롤백
					mts.rollback(manager);
				}catch(Exception ex) {
					log.error("커밋중 오류 롤백["+manager+"]", ex);
				}
			}
		}
		
		if(mts.isNewSyncronization()) {
			syncronizationManager.clearSynchronization();
		}
		
		if(commitException!=null) {
			PlatformTransactionManager lastTransactionManager = transactionManagers.get(transactionManagers.size()-1);
			boolean firstTransactionManagerFailed = commitExceptionTransactionManager  == lastTransactionManager;
			int transactionState = firstTransactionManagerFailed? HeuristicCompletionException.STATE_ROLLED_BACK:HeuristicCompletionException.STATE_MIXED;
			throw new HeuristicCompletionException(transactionState, commitException);
		}
	}

	@Override
	public void rollback(TransactionStatus status) throws TransactionException {
		DataSourceTransactionStatus mts = (DataSourceTransactionStatus)status;
		Exception rollbackException = null;
		PlatformTransactionManager rollbackExceptionTransactionManager = null;
		
		List<PlatformTransactionManager> reverseManagers = transactionManagers.reversed();
		for(PlatformTransactionManager manager: reverseManagers) {
			try {
				mts.rollback(manager);
			}catch(Exception ex) {
				if(rollbackException ==null) {
					rollbackException = ex;
					rollbackExceptionTransactionManager = manager;
				}else {
					log.error("롤백 익셉션 ("+manager+")", ex);
				}
			}
		}
		
		if(mts.isNewSyncronization()) {
			syncronizationManager.clearSynchronization();
		}
		if(rollbackException!=null) {
			throw new UnexpectedRollbackException("롤백중 오류 발생했다("+rollbackExceptionTransactionManager+")", rollbackException);
		}
		
	}
	
}
