package other.jdk;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class VThreadTest {

	
	static class UserData{
		private String name;
		private String addr;
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getAddr() {
			return addr;
		}
		public void setAddr(String addr) {
			this.addr = addr;
		}
		
		public String toString() {
			return "name:"+name+", addr:"+addr;
		}
	}
	
	private static ScopedValue<UserData> SUSER_CONTEXT = ScopedValue.newInstance();
	private static InheritableThreadLocal<UserData> INUSER_CONTEXT = new InheritableThreadLocal<UserData>();
	private static ThreadLocal<UserData> TUSER_CONTEXT = new ThreadLocal<>();
	
	public static void memorySegmentFile() throws IOException {
        Path file = Path.of("test.txt");
        try (FileChannel channel = FileChannel.open(file,
                 StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {

        	 // 파일 크기 확보
        	byte[] msg = "Hello, MemorySegment!".getBytes();
            long size = msg.length;
            channel.truncate(size);
            
            // 파일을 메모리 매핑
            MappedByteBuffer segment = channel.map(FileChannel.MapMode.READ_WRITE, 0, size);

            // 데이터 쓰기
           // ByteBuffer buffer = segment.
            segment.put(msg);

            System.out.println("쓰기 완료");
        }
    }
	
	
	private static final int NAME_SIZE = 32;
	private static final StructLayout PERSON_LAYOUT = MemoryLayout.structLayout(
        MemoryLayout.sequenceLayout(NAME_SIZE, ValueLayout.JAVA_BYTE).withName("name"), // 고정 길이 문자열
        ValueLayout.JAVA_INT.withName("age"),
        MemoryLayout.paddingLayout(4),        // ✅ 패딩 추가해서 offset 40으로 맞춤
        ValueLayout.JAVA_DOUBLE.withName("height")
    );
	
	 // 문자열 입력: UTF-8 → 바이트 배열 → 메모리에 복사
    private static void setName(MemorySegment struct, String name) {
        byte[] bytes = name.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > NAME_SIZE) throw new IllegalArgumentException("이름이 너무 깁니다.");

        MemorySegment nameSegment = struct.asSlice(PERSON_LAYOUT.byteOffset(PathElement.groupElement("name")), NAME_SIZE);
        nameSegment.fill((byte) 0); // 초기화 (널 패딩)
        nameSegment.asByteBuffer().put(bytes); // 값 복사
    }

    // 문자열 읽기: 메모리에서 바이트 → 문자열
    private static String getName(MemorySegment struct) {
        MemorySegment nameSegment = struct.asSlice(PERSON_LAYOUT.byteOffset(PathElement.groupElement("name")), NAME_SIZE);
        ByteBuffer buffer = nameSegment.asByteBuffer();
        byte[] bytes = new byte[NAME_SIZE];
        buffer.get(bytes);

        // 0바이트(널) 전까지 읽기
        int len = 0;
        while (len < bytes.length && bytes[len] != 0) len++;

        return new String(bytes, 0, len, StandardCharsets.UTF_8);
    }
    
	public static void main(String[] args) throws Exception{
		
		memorySegmentFile();
		
		
		
	 try (Arena arena = Arena.ofConfined()) {
            // 구조체 메모리 할당
            MemorySegment person = arena.allocate(PERSON_LAYOUT);
            // VarHandles 생성
            //VarHandle nameHandle = PERSON_LAYOUT.varHandle(PathElement.groupElement("name"));
            VarHandle ageHandle = PERSON_LAYOUT.varHandle(PathElement.groupElement("age"));
            VarHandle heightHandle = PERSON_LAYOUT.varHandle(PathElement.groupElement("height"));
            // 값 설정
            setName(person, "신민");
            ageHandle.set(person, 0L, 30);
            heightHandle.set(person, 0L, 178.5 );
            // 값 읽기
            String name = getName(person);
            int age = (int) ageHandle.get(person, 0L);
            double height = (double) heightHandle.get(person, 0L);

            System.out.printf("Person - Name: %s, Age: %d, Height: %.2f%n", name, age, height);
       }
		
		
		try (Arena arena = Arena.ofConfined()) {
            // 4개의 int 값을 저장할 수 있는 메모리 블록 확보
            MemorySegment segment = arena.allocate(4 * Integer.BYTES);
            // int형 데이터를 접근하기 위한 VarHandle 생성
            VarHandle intHandle = ValueLayout.JAVA_INT.varHandle();
            // 메모리에 값 쓰기
            for (int i = 0; i < 4; i++) {
                intHandle.set(segment, (long)i * Integer.BYTES, i * 10);
            }
            // 메모리에서 값 읽기
            for (int i = 0; i < 4; i++) {
                int value = (int) intHandle.get(segment, (long)i * Integer.BYTES);
                System.out.println("Value at index " + i + ": " + value);
            }
        }
		
		
		
//		var data = new UserData();
//		data.setAddr("주소");
//		data.setName("이름");
//		
//		ScopedValue.where(SUSER_CONTEXT, data).run(()->{
//			
//			INUSER_CONTEXT.set(data);
//			TUSER_CONTEXT.set(data);
//			System.out.println("시작 INUSER_CONTEXT:"+INUSER_CONTEXT.get());
//			System.out.println("시작 TUSER_CONTEXT:"+TUSER_CONTEXT.get());
//			System.out.println("시작 SUSER_CONTEXT:"+SUSER_CONTEXT.get());
//
//			try (ExecutorService executorService = Executors.newSingleThreadExecutor()){
//				
//					executorService.execute(()->{
//						ScopedValue.where(SUSER_CONTEXT, data).run(()->{
//							//SUSER_CONTEXT.where(SUSER_CONTEXT,data1).run(()->{
//							System.out.println("CHILD 시작 INUSER_CONTEXT:"+INUSER_CONTEXT.get());
//							System.out.println("CHILD 시작 TUSER_CONTEXT:"+TUSER_CONTEXT.get());
//							System.out.println("CHILD 시작 SUSER_CONTEXT:"+SUSER_CONTEXT.get());
//							
//							if( SUSER_CONTEXT.isBound() ) {
//								UserData uData = SUSER_CONTEXT.get();
//								uData.setAddr("scpValueNAME");
//							}
//							UserData ituData =  INUSER_CONTEXT.get();
//							if(ituData!=null) {
//								ituData.setAddr("상속주소");
//							}
//							
//							UserData tuData =  TUSER_CONTEXT.get();
//							if(tuData!=null) {
//								tuData.setAddr("그냥 변경 주소");
//							}
//							
//							System.out.println("CHILD 종료 INUSER_CONTEXT:"+INUSER_CONTEXT.get());
//							System.out.println("CHILD 종료 TUSER_CONTEXT:"+TUSER_CONTEXT.get());
//							System.out.println("CHILD 종료 SUSER_CONTEXT:"+SUSER_CONTEXT.get());
//						});
//					
//					});
//				//});
//			};
//			
//			System.out.println("종료 INUSER_CONTEXT:"+INUSER_CONTEXT.get());
//			System.out.println("종료 TUSER_CONTEXT:"+TUSER_CONTEXT.get());
//			System.out.println("종료 SUSER_CONTEXT:"+SUSER_CONTEXT.get());
//				
//		});
//		
//		
//		
//		
//		 // 기존 방식: 고정 쓰레드 풀 사용
//        try (ExecutorService executorService = Executors.newFixedThreadPool(100)) {
//            HttpClient traditionalClient = HttpClient.newBuilder()
//                    .executor(executorService)
//                    .connectTimeout(Duration.ofSeconds(10))
//                    .build();
//            
//            System.out.println("일반 쓰레드 풀로 1000개 요청 보내기 시작");
//            long traditionalStart = System.currentTimeMillis();
//            performRequests(traditionalClient, 1000);
//            long traditionalEnd = System.currentTimeMillis();
//            System.out.println("일반 쓰레드 풀 실행 시간: " + (traditionalEnd - traditionalStart) + "ms");
//        }
//        
//        // 버츄얼 쓰레드 방식
//        try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
//            HttpClient virtualClient = HttpClient.newBuilder()
//                    .executor(virtualExecutor)
//                    .connectTimeout(Duration.ofSeconds(10))
//                    .build();
//            
//            System.out.println("버츄얼 쓰레드로 1000개 요청 보내기 시작");
//            long virtualStart = System.currentTimeMillis();
//            performRequests(virtualClient, 1000);
//            long virtualEnd = System.currentTimeMillis();
//            System.out.println("버츄얼 쓰레드 실행 시간: " + (virtualEnd - virtualStart) + "ms");
//        }
    }
    
    private static void performRequests(HttpClient client, int count) {
        List<CompletableFuture<String>> futures = IntStream.range(0, count)
                .mapToObj(i -> {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("https://httpbin.org/delay/1")) // 1초 지연되는 테스트 API
                            .GET()
                            .build();
                    
                    return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                            .thenApply(response -> {
                                if (i % 100 == 0) {
                                    System.out.println("요청 " + i + " 완료, 상태 코드: " + response.statusCode());
                                }
                                return response.body();
                            });
                })
                .collect(Collectors.toList());
        
        // 모든 요청 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
//		Thread vr = Thread.startVirtualThread(()->{System.out.println("안녕이쉐야");});
//		vr.run();
//		
//		ExecutorService  poll = Executors.newVirtualThreadPerTaskExecutor();
//		poll.execute(()->{
//			// ScopedValue 정의
//			System.out.println("김저ㅣ저ㅣ");
//		
//			// 값 설정 및 해당 스코프 내에서 작업 실행
//			ScopedValue
//			.where(USER_CONTEXT, "방갑다")
//			.where(USER_CONTEXT1, "방가방가")
//		    .run(() -> {
//		    	generateTest();
//		    	two11();
//		    });
//		});
//		
//		poll.shutdown();
//		try {
//			Thread.currentThread().join();
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	
	
}
