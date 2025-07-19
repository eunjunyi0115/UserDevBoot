//package other.jdk;
//
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.time.Duration;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//public class VirtualThreadTest {
//
//    public static void main(String[] args) {
//    	
//    	System.out.println( "availableProcessors:"+Runtime.getRuntime().availableProcessors() );
//        // 테스트할 URL 목록
//        List<String> urls = List.of(
//            "https://www.google.com",
//            "https://www.github.com",
//            "https://www.stackoverflow.com",
//            "https://www.oracle.com"
//        //   , "https://www.openjdk.org"
//        );
//        
//        // 각 URL에 100번씩 요청
//        int requestsPerUrl = 700;
//        List<String> allUrls = new ArrayList<>();
//        for (int i = 0; i < requestsPerUrl; i++) {
//            allUrls.addAll(urls);
//        }
//        System.out.println("총 요청 수: " + allUrls.size());
//        
//        // 전통적인 스레드 풀을 사용한 HTTP 요청
//        long platformThreadStart = System.currentTimeMillis();
//        executeWithPlatformThreads(allUrls);
//        long platformThreadTime = System.currentTimeMillis() - platformThreadStart;
//        System.out.println("플랫폼 스레드 실행 시간: " + platformThreadTime + "ms");
//        
//        // 가상 스레드를 사용한 HTTP 요청
//        long virtualThreadStart = System.currentTimeMillis();
//        executeWithVirtualThreads(allUrls);
//        long virtualThreadTime = System.currentTimeMillis() - virtualThreadStart;
//        System.out.println("가상 스레드 실행 시간: " + virtualThreadTime + "ms");
//        
//        System.out.println("성능 향상: " + (float)platformThreadTime / virtualThreadTime + "x");
//        
//        
//    }
//    
//    private static void executeWithVirtualThreads(List<String> urls) {
//        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
//            HttpClient client = HttpClient.newBuilder()
//                .executor(executor)
//                .connectTimeout(Duration.ofSeconds(1000))
//                .build();
//                
//            List<CompletableFuture<HttpResponse<Void>>> futures = new ArrayList<>();
//            
//            for (String url : urls) {
//            	
//                HttpRequest request = HttpRequest.newBuilder()
//                    .uri(URI.create(url))
//                    .timeout(Duration.ofSeconds(10))
//                    .GET()
//                    .build();
//                    
//                CompletableFuture<HttpResponse<Void>> future = client.sendAsync(
//                    request, HttpResponse.BodyHandlers.discarding());
//                    
//                futures.add(future);
//            }
//            
//            // 모든 요청 완료 대기
//            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//        }
//    }
//    
//    private static void executeWithPlatformThreads(List<String> urls) {
//    	
//        try (ExecutorService executor = Executors.newFixedThreadPool(20)) { // 일반적인 서버 스레드 풀 크기
//            HttpClient client = HttpClient.newBuilder()
//                .executor(executor)
//                .connectTimeout(Duration.ofSeconds(1000))
//                .build();
//                
//            List<CompletableFuture<HttpResponse<Void>>> futures = new ArrayList<>();
//            
//            for (String url : urls) {
//                HttpRequest request = HttpRequest.newBuilder()
//                    .uri(URI.create(url))
//                    .timeout(Duration.ofSeconds(10))
//                    .GET()
//                    .build();
//                    
//                CompletableFuture<HttpResponse<Void>> future = client.sendAsync(
//                    request, HttpResponse.BodyHandlers.discarding());
//                    
//                futures.add(future);
//            }
//            
//            // 모든 요청 완료 대기
//            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//        }
//    }
//}