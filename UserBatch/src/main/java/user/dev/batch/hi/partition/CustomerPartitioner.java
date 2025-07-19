package user.dev.batch.hi.partition;

import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import lombok.extern.slf4j.Slf4j;
import user.dev.batch.hi.chunk.CustomerRepository;

@Slf4j
public class CustomerPartitioner implements Partitioner {
	
	private final CustomerRepository customerRepository;
	CustomerPartitioner(CustomerRepository customerRepository){
		this.customerRepository = customerRepository;
	}
	
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> partitions = new HashMap<>();
        // 실제로는 데이터베이스에서 min, max ID를 조회해야 함
        long minId = 1;
        long maxId = customerRepository.count();
        log.info("partition maxlen:{}",maxId);
        log.info("gridSize:{}",gridSize);
        
        long range = (maxId - minId + 1) / gridSize;
        
        for (int i = 0; i < gridSize; i++) {
            ExecutionContext context = new ExecutionContext();
            long start = minId + (i * range);
            long end = (i == gridSize - 1) ? maxId : start + range - 1;
            
            context.putLong("minId", start);
            context.putLong("maxId", end);
            log.info("파티션 :{} =>{}", "partition" + i,context);
            partitions.put("partition" + i, context);
        }
        
        return partitions;
    }
}
