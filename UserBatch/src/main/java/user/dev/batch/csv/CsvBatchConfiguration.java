package user.dev.batch.csv;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CsvBatchConfiguration {
    
	//스프링 JPA 를 사용하는 경우는 JpaTransactionManager 가 transactionManager 로 사용하게 된다. 
	//DataSourceAutoConfigure 에서 배치인경우는 jpatransactionManager 이 기본이 되도록 설정했음.
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    
    
//    @Bean
//    public PlatformTransactionManager jpaTransactionManager(EntityManagerFactory entityManagerFactory) {
//        return new JpaTransactionManager(entityManagerFactory);
//    }
    
    @Bean
    public Job csvProcessingJob() {
    	
        return new JobBuilder("csvProcessingJob", jobRepository)
                .start(csvProcessingStep())
                .build();
    }
    
    @Bean
    public Step csvProcessingStep() {
        return new StepBuilder("csvProcessingStep", jobRepository)
                .<Customers, Customers>chunk(1, transactionManager)
                .reader(csvItemReader())
                .processor(csvItemProcessor())
                .writer(csvItemWriter())
                .build();
    }
    @Bean
    @StepScope
    public FlatFileItemReader<Customers> csvItemReader() {
        return new FlatFileItemReaderBuilder<Customers>()
                .name("csvItemReader")
                .resource(new ClassPathResource("customers.csv"))
                .delimited()
                .names("firstName", "lastName", "email", "age")
                .linesToSkip(1) // 헤더 스킵
                .targetType(Customers.class)
                .build();
    }
    
    @Bean
    public ItemProcessor<Customers, Customers> csvItemProcessor() {
        return customer -> {
        	log.info("custom,er===>{}",customer);
            // 비즈니스 로직 예시: 이메일 소문자 변환
            customer.setEmail(customer.getEmail().toLowerCase());
            // 나이 검증
            if (customer.getAge() < 0 || customer.getAge() > 150) {
                return null; // null 반환 시 해당 아이템은 writer로 전달되지 않음
            }
            return customer;
        };
    }
    
    @Bean
    public JpaItemWriter<Customers> csvItemWriter() {
        return new JpaItemWriterBuilder<Customers>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}
