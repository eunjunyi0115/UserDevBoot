package user.dev.batch.hi.schedule;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class BatchSchedulingConfiguration {
    
    private final JobLauncher jobLauncher;
    private final Job csvProcessingJob;
    
    @Scheduled(cron = "0 0 2 * * ?") // 매일 새벽 2시
    public void runDailyBatch() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .addString("type", "daily")
                    .toJobParameters();
            
            jobLauncher.run(csvProcessingJob, jobParameters);
        } catch (Exception e) {
            System.err.println("배치 실행 중 오류 발생: " + e.getMessage());
        }
    }
    
    @Scheduled(fixedDelay = 300000) // 5분마다
    public void runPeriodicBatch() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .addString("type", "periodic")
                    .toJobParameters();
            
            jobLauncher.run(csvProcessingJob, jobParameters);
        } catch (Exception e) {
            System.err.println("주기적 배치 실행 중 오류 발생: " + e.getMessage());
        }
    }
}