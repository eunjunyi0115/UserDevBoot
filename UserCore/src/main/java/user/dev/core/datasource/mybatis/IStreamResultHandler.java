package user.dev.core.datasource.mybatis;

import org.apache.ibatis.session.ResultHandler;

public interface IStreamResultHandler<T> extends ResultHandler<T> {

	default public void preHandle() {}
	default public void postHandle() {}
}
