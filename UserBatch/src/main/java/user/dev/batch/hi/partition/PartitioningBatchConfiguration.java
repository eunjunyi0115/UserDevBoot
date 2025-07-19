package user.dev.batch.hi.partition;

import java.util.ArrayList;
import java.util.Map;

import org.h2.util.MathUtils;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import user.dev.batch.hi.chunk.Customer;
import user.dev.batch.hi.chunk.CustomerRepository;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class PartitioningBatchConfiguration {	
    
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final CustomerRepository customerRepository;
    
    @Bean
    public Job partitioningJob() {
        return new JobBuilder("partitioningJob", jobRepository)
        		.start(insertStep())
        		.next(managerStep())
               // .start(managerStep())
                .build();
    }
    
    @Bean
    public Tasklet insertDatasklet() {
        return (contribution, chunkContext) -> {
        	log.info("insertDataasklet 실행!");
        	var CustomerList = new ArrayList<Customer>();
        	for(int i=0;i<80;i++) {
        		CustomerList.add(Customer.builder().firstName("이"+i).lastName("은준"+i).email("eunjunyi_"+i+"@aaa.co"+i).age((i+1)*MathUtils.randomInt(10)).build());
        	}
        	customerRepository.saveAll(CustomerList);
        	
        	log.info("insertDataasklet 완료료료룡!");
            return RepeatStatus.FINISHED;
        };
    }
    
    
    @Bean
    public Step insertStep() {
    	return new StepBuilder("managerStep", jobRepository)
        		.tasklet(insertDatasklet(),transactionManager).build();
    }
    
    @Bean
    public Step managerStep() {
        return new StepBuilder("managerStep", jobRepository)
                .partitioner("workerStep", partitioner())
                .step(workerStep())
                .gridSize(4) // 파티션 개수
                .taskExecutor(taskExecutor())
                .build();
    }
    
    @Bean
    public Step workerStep() {
        return new StepBuilder("workerStep", jobRepository)
                .<Customer, Customer>chunk(5, transactionManager)
                .reader(partitionItemReader(null, null))
                .processor(partitionItemProcessor())
                .writer(partitionItemWriter())
                .build();
    }
    
    @Bean
    public Partitioner partitioner() {
        return new CustomerPartitioner(customerRepository);
    }
    
    @Bean
    @StepScope
    public JpaPagingItemReader<Customer> partitionItemReader(
            @Value("#{stepExecutionContext[minId]}") Long minId,
            @Value("#{stepExecutionContext[maxId]}") Long maxId) {
        log.info("stepExecutionContext = minId:{} maxId:{}" , minId, maxId);
        return new JpaPagingItemReaderBuilder<Customer>()
                .name("partitionItemReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT c FROM Customer c WHERE c.id >= :minId AND c.id <= :maxId")
                .parameterValues(Map.of("minId", minId, "maxId", maxId))
                .pageSize(5)
                .build();
    }
    
    @Bean
    public ItemProcessor<Customer, Customer> partitionItemProcessor() {
        return customer -> {
            // 처리 로직
            customer.setEmail(customer.getEmail().toUpperCase());
            System.out.println("Processing customer: " + customer.getId() + 
                " in thread: " + Thread.currentThread().getName());
            return customer;
        };
    }
    
    @Bean
    public JpaItemWriter<Customer> partitionItemWriter() {
        return new JpaItemWriterBuilder<Customer>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
    
    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("partition-");
        executor.initialize();
        return executor;
    }
}
