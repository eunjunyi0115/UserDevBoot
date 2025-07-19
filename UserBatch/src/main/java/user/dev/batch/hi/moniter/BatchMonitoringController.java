package user.dev.batch.hi.moniter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BatchMonitoringController {
    
    private final JobExplorer jobExplorer;
    private final JobOperator jobOperator;
    
    @GetMapping("/batch/jobs")
    public List<String> getJobNames() {
        return jobExplorer.getJobNames();
    }
    
    @GetMapping("/batch/jobs/{jobName}/executions")
    public List<JobExecution> getJobExecutions(@PathVariable String jobName) {
        return jobExplorer.getJobInstances(jobName, 0, 10)
                .stream()
                .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream())
                .collect(Collectors.toList());
    }
    
    @GetMapping("/batch/jobs/{jobName}/status")
    public Map<String, Object> getJobStatus(@PathVariable String jobName) {
        List<JobInstance> instances = jobExplorer.getJobInstances(jobName, 0, 1);
        if (instances.isEmpty()) {
            return Map.of("status", "No executions found");
        }
        
        JobInstance latestInstance = instances.get(0);
        List<JobExecution> executions = jobExplorer.getJobExecutions(latestInstance);
        if (executions.isEmpty()) {
            return Map.of("status", "No executions found");
        }
        
        JobExecution latestExecution = executions.get(0);
        return Map.of(
            "status", latestExecution.getStatus(),
            "startTime", latestExecution.getStartTime(),
            "endTime", latestExecution.getEndTime(),
            "exitStatus", latestExecution.getExitStatus()
        );
    }
    
    @PostMapping("/batch/jobs/{jobName}/stop")
    public String stopJob(@PathVariable String jobName) {
        try {
            Set<Long> runningExecutions = jobOperator.getRunningExecutions(jobName);
            for (Long executionId : runningExecutions) {
                jobOperator.stop(executionId);
            }
            return "Job stopped successfully";
        } catch (Exception e) {
            return "Error stopping job: " + e.getMessage();
        }
    }
}
