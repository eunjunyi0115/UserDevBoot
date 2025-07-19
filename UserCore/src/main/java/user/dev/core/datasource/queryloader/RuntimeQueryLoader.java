package user.dev.core.datasource.queryloader;

import java.util.List;

/**
 * 서버기동시 쿼리를 로딩하지 않고 Runtime에 필요한 시점에 쿼리를 로딩하기 위한 CustoimerQueryLoader의 구현해야할 인터페이스
 */
public interface RuntimeQueryLoader {

	/**
	 * 런타임에 특정 쿼리들을 로딩한다. 
	 * @param queryInfos 런타임에 로딩할 쿼리 정보를 담고 있는 리스트
	 * @param reload Mybatis 가 이미 ㅎ ㅐ당 쿼리를 로딩 했더라도 다시 로딩할지 여부
	 * @throws Exception
	 */
	public void loadQueryAtRuntime(List<QueryEntity> queryInfos, boolean reload) throws Exception;
	
	/**
	 * 특정쿼리가 reload가 필요한지 여부 정보
	 * @param sqlId
	 * @return 해당쿼리가 reload가 필요한지 여부.
	 */
	public boolean needToReload(String sqlId);
	
}
