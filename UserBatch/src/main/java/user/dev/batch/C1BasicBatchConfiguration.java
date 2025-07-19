package user.dev.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
/**
 * @EnableBatchProcessing : Spring Batch를 사용하기 위해 필요한 기본 설정을 자동으로 구성
 * JobBuilderFactory	Job을 생성할 수 있는 팩토리
 * StepBuilderFactory	Step을 생성할 수 있는 팩토리
 * JobRepository	배치 Job/Step의 메타데이터를 DB에 저장하고 관리
 * JobLauncher	Job 실행기
 * JobRegistry	Job 이름으로 Job 객체를 관리
 * PlatformTransactionManager	트랜잭션 관리
 * JobExplorer	실행된 Job/Step 정보 조회 도구
 */
@EnableBatchProcessing
@RequiredArgsConstructor
public class C1BasicBatchConfiguration {
	
	private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    
    @Bean
    public Job simpleJob() {
        return new JobBuilder("simpleJob", jobRepository)
                .start(simpleStep())
                .build();
    }
    
    @Bean
    public Step simpleStep() {
        return new StepBuilder("simpleStep", jobRepository)
                .tasklet(simpleTasklet(), transactionManager)
                .build();
    }
    
    @Bean
    public Tasklet simpleTasklet() {
        return (contribution, chunkContext) -> {
        	log.info("Simple Batch Job 실행!");
            System.out.println("Simple Batch Job 실행!");
            return RepeatStatus.FINISHED;
        };
    }
}
