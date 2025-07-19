package user.dev.core.datasource.mybatis;

import org.apache.ibatis.session.RowBounds;

//마이바티스 RowBounds 확장해서 사용
public class LimitCountRowBounds extends RowBounds{

	private final boolean dataCut;
	
	public LimitCountRowBounds(int limitCount, boolean dataCut) {
		super(0,limitCount);
		if(limitCount<1) {
			throw new RuntimeException("limitCount must be greather than 0");
		}
		this.dataCut = dataCut;
	}
	
	public boolean isDataCut() {
		return dataCut;
	}
}
