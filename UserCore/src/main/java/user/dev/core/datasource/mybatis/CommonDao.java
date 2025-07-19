package user.dev.core.datasource.mybatis;

import java.util.List;

import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

public interface CommonDao {

	public <T> T select(String queryId);
	
	public <T> T select(String queryId,Object parameter);
	
	public <T> T select(String queryId,Object parameter, String spec);
	
	public <E> List<E> selectList(String queryId);
	
	public <E> List<E> selectList(String queryId,Object parameter);
	
	public <E> List<E> selectList(String queryId,Object parameter, String spec);
	
	public <E> List<E> selectList(String queryId,Object parameter, RowBounds rowBounds);
	
	public <E> List<E> selectList(String queryId,Object parameter, String spec, RowBounds rowBounds);
	
	public <T> void selectList(String queryId, ResultHandler<T> handler);
	
	public <T> void selectList(String queryId, ResultHandler<T> handler,String spec);
	
	public <T> void selectList(String queryId, Object parameter, ResultHandler<T> handler);
	
	public <T> void selectList(String queryId, Object parameter, ResultHandler<T> handler, String spec);
	
	public int insert(String queryId);
	
	public int insert(String queryId, Object parameter);
	
	public int insert(String queryId, Object parameter,String spec);
	
	public int update(String queryId);
	
	public int update(String queryId, Object parameter);
	
	public int update(String queryId, Object parameter,String spec);
	
	public int delete(String queryId);
	
	public int delete(String queryId, Object parameter);
	
	public int delete(String queryId, Object parameter,String spec);
	
	public int batchInsert(String queryId, List<?> parameter);
	
	public int batchInsert(String queryId, List<?> parameterList , boolean useAddBatch);
	
	public int batchInsert(String queryId, List<?> parameter,String spec);
	
	public int batchInsert(String queryId, List<?> parameter,String spec, boolean useAddBatch);

	public int batchUpdate(String queryId, List<?> parameter);
	
	public int batchUpdate(String queryId, List<?> parameterList , boolean useAddBatch);
	
	public int batchUpdate(String queryId, List<?> parameter,String spec);
	
	public int batchUpdate(String queryId, List<?> parameter,String spec, boolean useAddBatch);
	
	public int batchDelete(String queryId, List<?> parameter);
	
	public int batchDelete(String queryId, List<?> parameterList , boolean useAddBatch);
	
	public int batchDelete(String queryId, List<?> parameter,String spec);
	
	public int batchDelete(String queryId, List<?> parameter,String spec, boolean useAddBatch);
}
