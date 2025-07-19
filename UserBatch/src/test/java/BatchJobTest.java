import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.batch.job.enabled=false"
})
class BatchJobTest {
    
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;
    
    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;
    
    @Autowired
    private Job simpleJob;
    
    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(simpleJob);
        jobRepositoryTestUtils.removeJobExecutions();
    }
    
    @Test
    void testSimpleJob() throws Exception {
        // Given
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        
        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        
        // Then
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
    }
    
    @Test
    void testSpecificStep() throws Exception {
        // Given & When
        JobExecution stepExecution = jobLauncherTestUtils.launchStep("simpleStep");
        
        // Then
        assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
    }
}