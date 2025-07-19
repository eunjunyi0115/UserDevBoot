package user.dev.batch.hi.chunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.h2.util.MathUtils;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.utility.RandomString;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
@Slf4j
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final CustomDatabaseItemReader reader;
    private final CustomerRepository customerRepository;

    @Bean
    public Job readCustomerJob() {
        return new JobBuilder("readCustomerJob", jobRepository)
                .start(insertData())
                .next(readCustomerStep())
                .build();
    }
    
    @Bean
    public Tasklet insertDataasklet() {
        return (contribution, chunkContext) -> {
        	log.info("insertDataasklet 실행!");
        	var CustomerList = new ArrayList<Customer>();
        	for(int i=0;i<20;i++) {
        		CustomerList.add(Customer.builder().firstName("이"+i).lastName("은준"+i).email("eunjunyi_"+i+"@aaa.co"+i).age((i+1)*MathUtils.randomInt(10)).build());
        	}
        	customerRepository.saveAll(CustomerList);
        	
        	log.info("insertDataasklet 완료료료룡!");
            return RepeatStatus.FINISHED;
        };
    }
    
    @Bean
    public Step insertData() {
    	return new StepBuilder("insertData",jobRepository )
    			.tasklet(insertDataasklet(), transactionManager)
				.build();
    }
    
    @Bean
    public Step readCustomerStep() {
        return new StepBuilder("readCustomerStep", jobRepository)
                .<Customer, Customer>chunk(5, transactionManager)
                .reader(reader)
                .processor(processor())
                .writer(logWriter())
                .build();
    }

    @Bean
    public ItemProcessor<Customer, Customer> processor(){
    	return item->{
    		log.info("processor item:{}",item);
    		return item;
    	};
    }
    
    @Bean
    public ItemWriter<Customer> logWriter() {
        return items -> {
            for (Customer customer : items) {
                System.out.println("Read: " + customer.getFirstName() + " " + customer.getLastName());
            }
        };
    }
}