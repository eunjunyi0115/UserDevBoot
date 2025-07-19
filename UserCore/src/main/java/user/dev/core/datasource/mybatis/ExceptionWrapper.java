package user.dev.core.datasource.mybatis;

/**
 *  CommonDao 컴포넌트 내부에서 발생하는 Exception을 CommonDaoException 으로  wrapping 하는 인터페이스
 */
public interface ExceptionWrapper {

	public RuntimeException execute(RuntimeException e);
}
