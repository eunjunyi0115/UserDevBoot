package user.dev.batch.hi.tasklet;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ComplexFlowConfiguration {
    
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    
    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);    // 기본 스레드 수
        executor.setMaxPoolSize(8);     // 최대 스레드 수
        executor.setQueueCapacity(10);  // 작업 대기 큐
        executor.setThreadNamePrefix("batch-thread-");
        executor.initialize();
        return executor;
    }
    
    @Bean
    public Job complexFlowJob() {
        Flow mainFlow = new FlowBuilder<SimpleFlow>("mainFlow")
                .start(validationStep())
                .on("FAILED").to(errorHandlingStep()) //on은 바로 위에 결과에만 대응함.
                .from(validationStep()).on("COMPLETED").to(parallelFlow())  //from 으로 명시적으로 validationStep이 성공인경우만 실행.
                .end();
        
        return new JobBuilder("complexFlowJob", jobRepository)
                .start(mainFlow)
                .end()
                .build();
    }
    
    @Bean
    public Flow parallelFlow() {
        Flow flow1 = new FlowBuilder<SimpleFlow>("flow1")
                .start(dataProcessingStep1())
                .build();
        
        Flow flow2 = new FlowBuilder<SimpleFlow>("flow2")
                .start(dataProcessingStep2())
                .build();
        
        return new FlowBuilder<SimpleFlow>("parallelFlow")
                //.split(new SimpleAsyncTaskExecutor())  //SimpleAsyncTaskExecutor 은 쓰레드를 계속 생성함.
                .split(taskExecutor()) //쓰레드풀 사용
                .add(flow1, flow2) // 두 Flow를 병렬 실행
                .build();
    }
    
    @Bean
    public Step validationStep() {
        return new StepBuilder("validationStep", jobRepository)
                .tasklet(validationTasklet(), transactionManager)
                .build();
    }
    
    @Bean
    public Step errorHandlingStep() {
        return new StepBuilder("errorHandlingStep", jobRepository)
                .tasklet(errorHandlingTasklet(), transactionManager)
                .build();
    }
    
    @Bean
    public Step dataProcessingStep1() {
        return new StepBuilder("dataProcessingStep1", jobRepository)
                .tasklet(dataProcessingTasklet1(), transactionManager)
                .build();
    }
    
    @Bean
    public Step dataProcessingStep2() {
        return new StepBuilder("dataProcessingStep2", jobRepository)
                .tasklet(dataProcessingTasklet2(), transactionManager)
                .build();
    }
    
    @Bean
    public Tasklet validationTasklet() {
        return (contribution, chunkContext) -> {
            // 데이터 유효성 검사 로직
        	log.info("validationTasklet start");
            boolean isValid = performDataValidation();
            log.info("validationTasklet performDataValidation {}",isValid);
            if (!isValid) {
                contribution.setExitStatus(ExitStatus.FAILED);
            }
            return RepeatStatus.FINISHED;
        };
    }
    
    @Bean
    public Tasklet errorHandlingTasklet() {
        return (contribution, chunkContext) -> {
            System.out.println("에러 처리 중...");
            // 에러 로깅, 알림 발송 등
            return RepeatStatus.FINISHED;
        };
    }
    
    @Bean
    public Tasklet dataProcessingTasklet1() {
        return (contribution, chunkContext) -> {
            System.out.println("데이터 처리 1 실행 중... 스레드: " + Thread.currentThread().getName());
            Thread.sleep(2000); // 작업 시뮬레이션
            return RepeatStatus.FINISHED;
        };
    }
    
    @Bean
    public Tasklet dataProcessingTasklet2() {
        return (contribution, chunkContext) -> {
            System.out.println("데이터 처리 2 실행 중... 스레드: " + 
                Thread.currentThread().getName());
            Thread.sleep(3000); // 작업 시뮬레이션
            return RepeatStatus.FINISHED;
        };
    }
    
    private boolean performDataValidation() {
        // 실제 검증 로직
        return Math.random() > 0.1; // 90% 확률로 성공
    }
}
