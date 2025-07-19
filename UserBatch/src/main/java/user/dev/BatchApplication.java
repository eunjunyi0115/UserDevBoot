package user.dev;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableBatchProcessing 
public class BatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }
    
    @Bean
    public CommandLineRunner run(JobLauncher jobLauncher,
    		@Qualifier("partitioningJob")Job simpleJob) {
    		//@Qualifier("readCustomerJob")Job simpleJob) {
    		//@Qualifier("complexFlowJob")Job simpleJob) {
    		//@Qualifier("csvProcessingJob")Job simpleJob) {
    		//@Qualifier("simpleJob")Job simpleJob) {
        return args -> {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis()) // 중복 실행 방지용 파라미터
                    .toJobParameters();

            jobLauncher.run(simpleJob, jobParameters);
        };
    }
}