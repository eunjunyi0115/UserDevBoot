//package other.jdk;
//
//import java.time.Duration;
//import java.util.concurrent.Executors;
//
//public class MainTest { 
//	public static void main(String[] args) {
//		Runnable task = () -> {
//		    try {
//		        Thread.sleep(Duration.ofSeconds(1));
//		        System.out.println("Task completed!");
//		    } catch (InterruptedException e) {
//		        throw new RuntimeException(e);
//		    }
//		};
//
//		// 가상 스레드 생성 및 실행
//		Thread.startVirtualThread(task);
//
//		// 혹은 ExecutorService 사용
//		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
//		    for (int i = 0; i < 10_000; i++) {
//		        executor.submit(task);
//		    }
//		}
//	}
//}