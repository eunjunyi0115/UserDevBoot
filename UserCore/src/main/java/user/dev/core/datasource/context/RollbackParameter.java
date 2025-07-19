package user.dev.core.datasource.context;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class RollbackParameter {
	private String reason;
	private Object caller;
}
