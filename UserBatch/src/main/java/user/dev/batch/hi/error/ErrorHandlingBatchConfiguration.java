package user.dev.batch.hi.error;

import java.net.ConnectException;
import java.util.Arrays;
import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.transaction.PlatformTransactionManager;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ErrorHandlingBatchConfiguration {
    
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    
    @Bean
    public Job errorHandlingJob() {
        return new JobBuilder("errorHandlingJob", jobRepository)
                .start(resilientStep())
                .build();
    }
    
    @Bean
    public Step resilientStep() {
        return new StepBuilder("resilientStep", jobRepository)
                .<String, String>chunk(3, transactionManager)
                .reader(faultyItemReader())
                .processor(faultyItemProcessor())
                .writer(resilientItemWriter())
                .faultTolerant()
                .skipLimit(5) // 최대 5개까지 스킵 허용
                .skip(RuntimeException.class)
                .retryLimit(3) // 최대 3번 재시도
                .retry(ConnectException.class)
                .listener(customSkipListener())
                .listener(customRetryListener())
                .build();
    }
    
    @Bean
    @StepScope
    public ItemReader<String> faultyItemReader() {
        List<String> items = Arrays.asList(
            "item1", "item2", "error_item", "item4", "item5", 
            "retry_item", "item7", "item8", "skip_item", "item10"
        );
        return new ListItemReader<>(items);
    }
    
    @Bean
    public ItemProcessor<String, String> faultyItemProcessor() {
        return item -> {
            if ("error_item".equals(item)) {
                throw new RuntimeException("처리 중 오류 발생: " + item);
            }
            if ("retry_item".equals(item)) {
                // 재시도 시뮬레이션
                if (Math.random() < 0.7) {
                    throw new ConnectException("연결 오류: " + item);
                }
            }
            if ("skip_item".equals(item)) {
                throw new RuntimeException("스킵할 아이템: " + item);
            }
            return item.toUpperCase();
        };
    }
    
    @Bean
    public ItemWriter<String> resilientItemWriter() {
        return items -> {
            for (String item : items) {
                System.out.println("Writing: " + item);
            }
        };
    }
    
    @Bean
    public SkipListener<String, String> customSkipListener() {
        return new SkipListener<String, String>() {
            @Override
            public void onSkipInRead(Throwable t) {
                System.out.println("Skip in read: " + t.getMessage());
            }
            
            @Override
            public void onSkipInProcess(String item, Throwable t) {
                System.out.println("Skip in process: " + item + ", error: " + t.getMessage());
            }
            
            @Override
            public void onSkipInWrite(String item, Throwable t) {
                System.out.println("Skip in write: " + item + ", error: " + t.getMessage());
            }
        };
    }
    
    @Bean
    public RetryListener customRetryListener() {
        return new RetryListener() {
            @Override
            public <T, E extends Throwable> void onError(
                    RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                System.out.println("Retry attempt: " + context.getRetryCount() + 
                    ", error: " + throwable.getMessage());
            }
        };
    }
}
