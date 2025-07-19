//package devonenterprise.poc;
//
//import java.time.Duration;
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//import java.util.stream.Collectors;
//
//import org.springframework.beans.factory.annotation.Value;
//
//import devon.core.collection.LData;
//import devon.core.collection.LMultiData;
//import devon.core.config.ConfigurationUtil;
//import devon.core.exception.DevonException;
//import devon.core.exception.LException;
//import devon.core.log.LLog;
//import devon.core.util.DateUtil;
//import devonenterprise.backoffice.common.BackOfficeConstants;
//import devonenterprise.backoffice.common.BackOfficeDao;
//import devonenterprise.backoffice.common.BackOfficeUtil;
//import devonenterprise.backoffice.daemon.constants.DaemonConstants;
//import devonenterprise.backoffice.daemon.context.DaemonContext;
//import devonenterprise.backoffice.daemon.dao.DaemonDao;
//import devonenterprise.backoffice.daemon.job.AbstractUserDaemonJob;
//import devonenterprise.backoffice.daemon.util.DaemonUtil;
//import devonenterprise.core.constants.JOBNODE;
//import devonenterprise.core.constants.TableInfo;
//import devonenterprise.core.constants.TableInfo.DAEMONMAIN;
//import devonenterprise.core.constants.TableInfo.DAEMONSVC;
//import devonframework.business.transaction.nested.jdbc.LNestedJDBCTransactionManager;
//import devonenterprise.backoffice.common.DeferredThreadPool;
//
//public class HealthCheckDeferredJob extends AbstractUserDeferredJob{
//
//	//이전 서비스 정보 저장 객체.
//	Map<String, LData> prevCheckDfrdList = new HashMap<String, LData>();
//	
//	//DLTI10001 최종 처리 완료 시간을 저장.
//	Map<String, LocalDateTime> timeCheckMap = new HashMap<String, LocalDateTime>();
//	
//	//디퍼드 정지 호출시 처리 안되면 3회 재처리
//	private final static int retryIndex = 3;
//	
//	//백업 디퍼드 중지 인식후 시작하기 위한 간격 TERM SEC(초)
//	private int restartTermSec = 60;
//	
//	//메인도 죽고, 만약 백업도 죽었을 경우,
//	//백업을 바로 재기동 시도 할지 여부 restartTermSec 만큼 기다리고 기동할지 여부
//	private boolean isBackRestartNow = false; //true:즉시기동. false: 기다린후 기동.
//	
//	private static final String RESTART_WAIT_SEC ="/devon/....";
//	private static final String BACKUP_TERM_YN ="/devon/....";
//	
//	private boolean isFirst = true;
//	
//	private final static String TARGET_DEFERRED_ID = "DLTI10001"; 
//	private final static String BACKUP_POSTFIX = "_2"; 
//	private final static String BACKUP_DEFERRED_ID = TARGET_DEFERRED_ID+BACKUP_POSTFIX; 
//	private final static String BACKUP_USER_ID = "helthcheckJob";
//	
//	private void printMsg(String msg) {
//		if(LLog.err.isEnabled()) LLog.err.print(msg);
//	}
//	
//	/*
//	 * DB2 로우락 처리가 안된다고 함
//	 * 헬쓰체크는 DLTI10001_2(백업 데몬) 이 존재 하는 인스턴스에서만 기동되어야 한다.
//	 * DLTI10001(원데몬)이 죽은경우 DLTI10001_2를 기동
//	 *  - DLTI10001 이 죽었다고 판단되는 경우 restartTerm(15)분 만큼 정상인거 처럼 판단하도록 하고,
//	 *  - 15분이 지나면, DLTI10001, DLTI10001_2 를 강제 종료한후.
//	 *  - DLTI10001_2를 기동한다.
//	 *  - 기동요청 후 timeCheckMap(최종완료시간) 을 현재 시간으로 업데이트 한다.
//	 */
//	@Override
//	public void execute(DeferredContext ctx) throws LException {
//		
//		//모든 디퍼드 메인 정보를 획득
//		LMultiData allDeferred = DeferredDao.retrieveDeferredMainInfoList(new LData());
//		Map<String, LData> currentCheckDfrdList = new HashMap<String, LData>();
//		
//		//모든 디퍼드 메인 정보중 TARGET_DEFERRED_ID(DLTI10001) 로 시작되는 객체만 필터링 한다.
//		List<LData> hckDeferredList = allDeferred.stream().map(m->(LData)m)
//				.filter(t->t.getString(DEFERREDMAIN.Deferred_ID).startsWith(TARGET_DEFERRED_ID)).collect(Collectors.toList());
//		
//		if(hckDeferredList.size()<2) {
//			//2개보다 적으면 처리 하지 않음.
//			printMsg("HELTH CHECK NEED IS "+BACKUP_DEFERRED_ID);
//			return;
//		}
//		
//		//필더된 디퍼드 정보의 현재 실행중인 서비스 정보를 조회 하여 세팅 한다.
//		for(LData tgtDf : hckDeferredList) {
//			String dfrdId = tgtDf.getString(DEFERREDMAIN.Deferred_ID);
//			LData svcLData = DeferredDao.retrieveDeferredSvcMgmt(dfrdId);
//			svcLData.setNullToInitialize(true);
//			tgtDf.put(DeferredSVC.EXEINST_NM, svcLData.getString(DEFERREDSVC.EXEINST_NM));
//			tgtDf.put(DEFERREDSVC.EXESVR_NM, svcLData.getString(DEFERREDSVC.EXESVR_NM));
//			tgtDf.put(DEFERREDSVC.CHK_DT, svcLData.getString(DEFERREDSVC.CHK_DT));
//			tgtDf.put(DEFERREDSVC.STAT_TY, svcLData.getString(DEFERREDSVC.STAT_TY));
//			
//			//현재 상태를 currentCheckDfrdList 에 보관하고, 실행 종료시 prevCheckDfrdList 에 적재한다.
//			currentCheckDfrdList.put(dfrdId, tgtDf);
//		}
//		printMsg("TARGET DATA"+hckDeferredList);
//		
//		boolean isStart = false;
//		String runningDefId = ""; //구동중인 디퍼드 아이디
//		
//		//@@@@@@@@@@@@@@@@@@ 박재웅 책임님 추가 #################
//		String addStateCode = ""; //상태저장.
//		//@@@@@@@@@@@@@@@@@@ 박재웅 책임님 추가 종료 #################
//		
//		for(LData tgtDf: hckDeferredList) {
//			String dfrdId = tgtDf.getString(DEFERREDMAIN.Deferred_ID);
//			
//			//현재 정지 여부를 확인.
//			String runStateCode = isStopCheck(tgtDf);
//			
//			//@@@@@@@@@@@@@@@@@@@@@ 박재웅 책임님 추가 #################
//			addStateCode = addStateCode + runStateCode; //상태추가  확인.
//			//@@@@@@@@@@@@@@@@@@@@@ 박재웅 책임님 추가 종료 #################
//			
//			printMsg("BACKUP_POSTFIX_runStateCode: ["+dfrdId+"]"+ runStateCode);
//			if( dfrdId.endsWith(BACKUP_POSTFIX)) {
//				//백업데몬(DLTI10001_2)은 상태가 N(정상) 이면 기동중으로 판단한다.
//				if("N".equals(runStateCode)) {
//					//둘중에 하나라도 기동중인 시간을 최종시간으로 한다.
//					//메인이 죽은 상태에서 백업도 만약 죽으면 바로 기동할것인가?
//					//아니면 대기시간만큼 대기후 백업을 기동할것인가?
//					if(!isBackRestartNow) {
//						timeCheckMap.put(TARGET_DEFERRED_ID, LocalDateTime.now());
//					}
//					isStart = true; //기동중
//					//break;
//				}
//			}else {
//				//메인데몬(DLTI10001)은 시간 체크한다 15분 정도 안돌아도 정상으로 간주 한다.
//				if("N".equals(runStateCode)) { //정상이면
//					//최종 정상 처리 시간 업데이트하고 정상으로 판단.
//					timeCheckMap.put(dfrdId, LocalDateTime.now());
//					printMsg(tgtDf.getString(DEFERREDSVC.EXEINST_NM) + " RUNNING.");
//					isStart = true; //기동중
//					//break;
//				}else {
//					//비정상시 현재시간과 최종 정상 처리 시간을 비교한다.
//					LocalDateTime curTime = LocalDateTime.now();
//					//timeCheckMap 에 DLTI10001의 최종 정상 처리시간이 없는 경우 현재 시간으로 세팅한다.
//					timeCheckMap.putIfAbsent(dfrdId, curTime);
//					
//					LocalDateTime nochangeStrtLoopTime = timeCheckMap.get(dfrdId);
//					//restartTerm(15)분이 이전은 정상기동인거처럼 리턴.
//					long checkSec = restartTermSec;
//					printMsg("PRE TIME:"+ nochangeStrtLoopTime+" CURRENT TIME:" +curTime);
//					if(Duration.between(nochangeStrtLoopTime, curTime).getSeconds() <= checkSec ) {
//						isStart = true; //기동중
//						//break;
//					}
//				}
//			}
//		}
//		
//		//@@@@@@@@@@@@@@@@@@ 박재웅 책임님 추가 모두 살아 있는경우 2번 죽이는 로직 추가.#################
//		printMsg("addStateCode:"+ addStateCode); 
//		//둘다 기동이면 addStateCode NN 이되므로, 백업 디퍼드를 강제 종료해서 한개만 기동 되도록 한다.
//		if(addStateCode.matches("N{2,}")) { 
//			printMsg("ALL SECCESS => BACK UP DEFERRED STOP ["+addStateCode+"]");
//			
//			//강제 종료 요청
//			Thread svc = DeferredThreadPool.getInstance().get(DeferredConstants.BACKUP_DEFERRED_ID);
//			if (svc != null && svc.isAlive()) {
//				LData chgStateLData = new LData();
//				chgStateLData.setString(DEFERREDSVC.Deferred_ID, deferredId);
//				//강제종료요청으로 상태 업데이트.
//				chgStateLData.setString(DEFERREDSVC.STAT_TY, Value.PROC_STAT_FORCED_END_REQUEST);
//				chgStateLData.setString(TableInfo.COMMON.REG_USER_ID, strtUsrId);
//				printMsg("TARGET_DEFERRED_ID stop change:"+chgStateLData);
//				changeSvcState(chgStateLData);
//				
//				DeferredUtil.stopDeferredWaitUntilDie(svc);
//			}
//		}		
//		//@@@@@@@@@@@@@@@@@@ 박재웅 책임님 추가 모두 살아 있는경우 2번 죽이는 로직 추가. 종료#################
//		
//		printMsg("isStart:"+isStart);
//		
//		//DLTI10001 와 DLTI10001_2 모두 실패 상태인 경우.
//		if(!isStart) {
//			//모두 종료후 기동 시킨다.
//			String currentDefId = "";
//			
//			for(LData tgtDf : hckDeferredList) {
//				//노드정보조회
//				LData param = new LData();
//				String deferredId = tgtDf.getString(DEFERREDMAIN.Deferred_ID);
//				param.setString(JOBNODE.NODE_NM, tgtDf.getString(DEFERREDSVC.EXESVR_NM));
//				param.setString(JOBNODE.INST_NM, tgtDf.getString(DEFERREDSVC.EXEINST_NM));
//				param.setString(JOBNODE.ENVR_TY, ConfigurationUtil.getEnvType());
//				param.setString(JOBNODE.TYPE_CD, BackOfficeConstants.JOBNODE.CODE.TYPE_CD.DeferredJOB.getValue());
//				LData nodeInfo = BackOfficeDao.retrieveJobNode(param);
//				
//				//백업 데몬(DLTI10001_2)를 시작하기전에 2중 기동을 방지 하기 위해 2개의 Deferred를 모두 종료 요청을 한다.
//				printMsg("CHECK TARGET ALL STOP START:"+isStart);
//				String result = sendDfrdStopMessage(deferredId, nodeInfo);
//				printMsg("CHECK TARGET ALL STOP stop:"+isStart);
//				
//				//TARGET_DEFERRED_ID(DLTI10001)로 종료 요청을 하면 강제종료 상태가 되므로, 정상종료 상태로 변경한다.
//				if(deferredId.equals(TARGET_DEFERRED_ID)) {
//					String callBeforeStatTyp = tgtDf.getString(DEFERREDSVC.STAT_TY);
//					//이전 강제 종료 상태가 아닌 경우 정상종료로 상태 변경.
//					if(! callBeforeStatTyp.equals(DeferredConstants.Value.PROC_STAT_FORCED_END)) {
//						LData chgStateLData = new LData();
//						chgStateLData.setString(DEFERREDSVC.Deferred_ID, deferredId);
//						chgStateLData.setString(DEFERREDSVC.STAT_TY, Value.PROC_STAT_NORM_END);
//						printMsg("TARGET_DEFERRED_ID state change:"+chgStateLData);
//						changeSvcState(chgStateLData);
//					}
//				}
//			}
//			
//			
//			try {
//				Thread.sleep(1000); //1초 정도 텀을 두고 기동한다.
//			}catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//			
//			printMsg(BACKUP_DEFERRED_ID + " RUN REQUEST");
//			
//			//DLTI10001_2 데몬정보를 필터링 한다.
//			Optional<LData> backupDeferredOtp = hckDeferredList.stream()
//					.filter(ls->ls.getString(DEFERREDMAIN.Deferred_ID).equals(BACKUP_DEFERRED_ID)).findFirst();
//			
//			//DLTI10001_2 서비스 상태가 강제 종료 상태이면 정상종료로 변경 하고 스타트 시킨다.
//			if(backupDeferredOtp.isPresent()) {
//				LData backupDefLData = backupDeferredOtp.get();
//				String stattyCd = backupDefLData.getString(DEFERREDSVC.STAT_TY);
//				if(stattyCd.equals(DeferredConstants.Value.PROC_STAT_FORCED_END)) {
//					
//					printMsg(BACKUP_DEFERRED_ID + " STATE ID PROC_STAT_FORCED_END CHANGE");
//					LData param = new LData();
//					param.setString(DEFERREDSVC.Deferred_ID, BACKUP_DEFERRED_ID);
//					param.setString(DEFERREDSVC.STAT_TY, Value.PROC_STAT_NORM_END);
//					changeSvcState(param);
//					printMsg(BACKUP_DEFERRED_ID +" STATE ID PROC_STAT_FORCED_END CHANGE END");
//				}
//			}
//			
//			//DLTI10001_2(백업 데몬)(을 기동 요청 한다.
//			DeferredUtil.startDeferred(BACKUP_DEFERRED_ID, BACKUP_USER_ID);
//			printMsg(BACKUP_DEFERRED_ID +" RUN REQUEST END");
//			
//			//최종 처리 수행시간을 삭제 한다.
//			timeCheckMap.remove(TARGET_DEFERRED_ID);
//			printMsg(TARGET_DEFERRED_ID+" SUCCESS REMOVE CURRENT TIME MAP: "+timeCheckMap);
//		}
//		
//		prevCheckDfrdList = currentCheckDfrdList; //이전정보 변경.
//	}
//	
//	/**
//	 * 상태가 정상이더라도 체크 타임이 변하지 않으면 멈춘것으로 판단한다.
//	 * false 가 되는 조건을 확인해야 한다.
//	 */
//	public String isStopCheck(LData deferred) throws DevonException {
//		String dfrdId = deferred.getString(DEFERREDMAIN.Deferred_ID);
//		String chkDt = deferred.getString(DEFERREDSVC.CHK_DT);
//		long poolTime = deferred.getLong(DEFERREDMAIN.POOLTIME_NO);
//		long milliSecondBetween = DateUtil.getMilliSecondBetween(chkDt, DateUtil.getCurrentMileSecond());
//		
//		//처리상태
//		String statTy = deferred.getString(DEFERREDSVC.STAT_TY);
//		if(!statTy.equals(Value.PROC_STAT_WORKING)) { //실행중 아니면
//			return "S"; //STOP 상태
//		}
//		if(milliSecondBetween > poolTime) {
//			if(prevCheckDfrdList.containsKey(dfrdId)) {
//				LData preDeferredSvc = prevCheckDfrdList.get(dfrdId);
//				String preChkDt = preDeferredSvc.getString(DEFERREDSVC.CHK_DT);
//				
//				//이전 체크DT 와 같으면 재기동
//				if(chkDt.equals(preChkDt)) {
//					return "T"; //체크타임미변경
//				}
//			}
//		}
//		return "N"; //정상
//	}
//	
//	/**
//	 * dfrdId로 중지 요청을 보낸다.
//	 * 정상처리가 안되는 경우는 3회 retry 호출 한다.
//	 */
//	private String sendDfrdStopMessage(String dfrdId, LData tempNodeInfo) {
//		String retStr = "2"; //어벤드
//		if(!"Y".equals(tempNodeInfo.getString(JOBNODE.USE_YN))) {
//			return "";
//		}
//		
//		Map<String, Object> jmsConnInfo = new HashMap<String, Object>();
//		Map<String, Object> input = new HashMap<String, Object>();
//		jmsConnInfo.put(BackOfficeConstants.INITIAL_CONTEXT_FACTORY, 
//				tempNodeInfo.getString(JOBNODE.INITIAL_CONTEXT_FACTORY));
//		jmsConnInfo.put(BackOfficeConstants.JMS_URL, tempNodeInfo.getString(JOBNODE.JNDIURL_NM));
//		jmsConnInfo.put(BackOfficeConstants.JMS_FACTORY_NM, tempNodeInfo.getString(JOBNODE.FACTORY_NM));
//		jmsConnInfo.put(BackOfficeConstants.JMS_DESTINATION_NM, tempNodeInfo.getString(JOBNODE.QUEUE_NM));
//		
//		input.put(DeferredConstants.JMS_REQ_TYPE, DeferredConstants.Value.Deferred_STOP_REQUEST);
//		input.put(TableInfo.DEFERREDMAIN.Deferred_ID, dfrdId);
//		input.put(TableInfo.COMMON.MOD_USER_ID, BACKUP_USER_ID);
//		input.put(TableInfo.JOBNODE.INST_NM, tempNodeInfo.getString(JOBNODE.INST_NM));
//		input.put(TableInfo.JOBNODE.ENVR_TY, tempNodeInfo.getString(JOBNODE.ENVR_TY));
//		
//		printMsg("sendDfrdStopMessage STOP:"+ input);
//		
//		for(int idx=0; retStr.equals("2") && idx<  retryIndex ;idx++ ) {
//			try {
//				retStr = (String)BackOfficeUtil.sendJmsMessageSync(jmsConnInfo, input);
//				printMsg("sendDfrdStopMessage STOP END:["+idx+"]"+ retStr);
//			}catch(Throwable e) {
//				printMsg("sendDfrdStopMessage = ["+dfrdId+"] FAIL ["+idx+"]:"+ e.toString());
//				retStr = "2";
//			}
//		}
//		return retStr;
//	}
//	
//	/**
//	 * 서비스의 상태를 변경 한다. 09(강제종료), 10(정상종료)
//	 * 정상종료인 경우만 인스턴스 기동시 Deferred가 실행 된다.
//	 */
//	private void changeSvcState(LData param) {
//		LNestedJDBCTransactionManager tm = new LNestedJDBCTransactionManager();
//		try {
//			tm.nestedBegin();
//			DeferredDao.updateDeferredSvcStat(param);
//			tm.nestedCommit();
//		}catch(Throwable e) {
//			e.printStackTrace();
//			try {
//				tm.nestedRollback();
//			}catch(Throwable t) {
//				t.printStackTrace();
//			}
//		}finally {
//			try {
//				tm.nestedRelease();
//			}catch(Throwable t) {
//				t.printStackTrace();
//			}
//		}
//	}
//
//	@Override
//	public void finalize(DeferredContext context) throws LException {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	public void stopDeferrd(DeferredContext context) throws LException {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	protected void beforeExecute(DeferredContext ctx) throws LException {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	protected void afterExecute(DeferredContext ctx) throws LException {
//		// TODO Auto-generated method stub
//		
//	}
//}