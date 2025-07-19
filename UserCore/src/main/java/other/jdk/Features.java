//package other.jdk;
// 
//import java.awt.Point;
//import java.io.ByteArrayOutputStream;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.io.Writer;
//import java.lang.annotation.ElementType;
//import java.lang.annotation.Retention;
//import java.lang.annotation.RetentionPolicy;
//import java.lang.annotation.Target;
//import java.lang.foreign.Arena;
//import java.lang.foreign.FunctionDescriptor;
//import java.lang.foreign.Linker;
//import java.lang.foreign.MemorySegment;
//import java.lang.foreign.SymbolLookup;
//import java.lang.foreign.ValueLayout;
//import java.lang.invoke.MethodHandle;
//import java.net.InetSocketAddress;
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.nio.charset.Charset;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Path;
//import java.time.Duration;
//import java.time.Instant;
//import java.util.ArrayDeque;
//import java.util.ArrayList;
//import java.util.Base64;
//import java.util.Deque;
//import java.util.List;
//import java.util.Set;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.Executors;
//import java.util.concurrent.Flow;
//import java.util.concurrent.StructuredTaskScope;
//import java.util.concurrent.StructuredTaskScope.Subtask;
//import java.util.concurrent.SubmissionPublisher;
//import java.util.concurrent.TimeUnit;
//import java.util.stream.Collectors;
//import java.util.stream.IntStream;
//import java.util.stream.Stream;
//
//import javax.annotation.processing.AbstractProcessor;
//import javax.annotation.processing.RoundEnvironment;
//import javax.annotation.processing.SupportedAnnotationTypes;
//import javax.annotation.processing.SupportedSourceVersion;
//import javax.lang.model.SourceVersion;
//import javax.lang.model.element.Element;
//import javax.lang.model.element.TypeElement;
//import javax.tools.JavaCompiler;
//import javax.tools.JavaFileObject;
//import javax.tools.ToolProvider;
//
//import com.sun.net.httpserver.SimpleFileServer;
//
//public class Features {
//
//	//쓰레드로컬은 가상스레드에서 사용불가하다고 봐야함 대용으로 
//	private static final ScopedValue<String> USER = ScopedValue.newInstance();
//	private static final InheritableThreadLocal<String> ITLOCAL = new InheritableThreadLocal();
//			
//	public static void main(String[] args) throws Throwable {
//		
//		//########################################################
//		//0.가상 스레드 (표준화) Java 21
//		//########################################################
//		Thread vThread = Thread.startVirtualThread(() -> {
//		    System.out.println("Running in virtual thread");
//		});
//		// 가상 스레드 ExecutorService 생성
//		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
//		    IntStream.range(0, 10_000).forEach(i -> {
//		        executor.submit(() -> {
//		            Thread.sleep(Duration.ofMillis(100));
//		            return i;
//		        });
//		    });
//		}
//		//########################################################
//		//1. Java 21 구조적 병렬 프로그래밍 (표준화) StructuredTaskScope  
//		//“함께 시작된 작업은 함께 끝나야 한다”는 원칙을 따르는 동시성 제어 모델
//		//########################################################
//		try (var scope = new StructuredTaskScope.ShutdownOnSuccess<String>()) { //하나라도 성공시반환
//			long stat = System.currentTimeMillis();
//		    scope.fork(() -> {
//		        Thread.sleep(100);
//		        return "first";
//		    });
//		    scope.fork(() -> {
//		        Thread.sleep(500);
//		        return "second";
//		    });
//		    scope.join(); // 하나라도 성공하면 바로 반환
//		    System.out.println("Result["+(System.currentTimeMillis()-stat)+"]: " + scope.result());
//		}
//		
//		try (var scope = new StructuredTaskScope.ShutdownOnFailure()) { //모두처리되어야 종료
//			long stat = System.currentTimeMillis();
//			Subtask<String> fast = scope.fork(() -> "Fast");
//		    Subtask<String> slow = scope.fork(() -> {
//		        Thread.sleep(1000);
//		        return "Slow";
//		    });
//		    scope.join();
//		    scope.throwIfFailed();
//
//		    System.out.println("Result["+(System.currentTimeMillis()-stat)+"]: " + fast.get()+","+slow.get());
//		}
//		
//		//가상 쓰레드의 ScopedValue 자동 복제는 StructuredTaskScope를 써야만 가능하다.
//		StructuredTaskScopeTest();
//		//그외는 전달 객체를 글로벌 하게 가지고 있다가 실행 시점에 ScopedValue where로 전달해야함.
//				
//		//########################################################
//		//2. Java 21 패턴 매칭 Switch (표준화)
//		//########################################################
//		Object target = "대상";
//		String result = switch (target) {
//		    case Integer i when i > 0 -> "Positive integer: " + i;
//		    case Integer i -> "Non-positive integer: " + i;
//		    case String s -> "String of length " + s.length();
//		    case Point p -> "Point at (" + p.getX() + ", " + p.getY() + ")";
//		    case null -> "Null object";
//		    default -> "Other type: " + target.getClass().getName();
//		};
//		System.out.println("result:"+result);
//		
//		//########################################################
//		//3. Java 21 레코드 패턴 (표준화)
//		//########################################################
//		record Point(int x, int y) {}
//		record Rectangle(Point topLeft, Point bottomRight) {}
//		// 레코드 패턴 사용 내부 값까지 변수화
//		Object obj = new Rectangle(new Point(3,4), new Point(5,6));
//	    if (obj instanceof Rectangle(Point(int x1, int y1), Point(int x2, int y2))) {
//	        int width = x2 - x1;
//	        int height = y2 - y1;
//	        System.out.println("Area: " + (width * height));
//	    }
//	    
//	    //########################################################
//	    //4.Java 21 자바 시퀀셜 컬렉션 (Sequenced Collections)
//	    //########################################################
//	    List<String> list = new ArrayList<>(List.of("A", "B", "C"));
//	    String first = list.getFirst();  // "A"
//	    String last = list.getLast();    // "C"
//	    list.addFirst("Z");              // ["Z", "A", "B", "C"]
//	    list.addLast("D");               // ["Z", "A", "B", "C", "D"]
//	    list.reversed();                 // ["D", "C", "B", "A", "Z"]
//	    System.out.println("list: " + list);
//	    
//	    //########################################################
//	    //6. Java 21 외부 메모리 접근 API (표준화)
//	    //########################################################
//	    try (Arena arena = Arena.ofConfined()) {
//	    	MemorySegment src = arena.allocateFrom(ValueLayout.JAVA_INT, 1, 2, 3, 4, 5);
//	        MemorySegment segment = arena.allocate(100);
//	        MemorySegment.copy(src,0,segment,0,src.byteSize());
//	        int index = 0;
//	        int value = segment.get(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT.byteSize()* index);
//	        System.out.println(src.byteSize()+"-"+value);  // 1
//	        
//	        MemorySegment src1 = arena.allocateFrom("방갑다",Charset.defaultCharset());
//	        MemorySegment.copy(src1,0,segment,0,src1.byteSize());
//	        MemorySegment ss = segment.asSlice(0,src1.byteSize());
//	        System.out.println( new String(ss.toArray(ValueLayout.JAVA_BYTE)));
//	        
//	    } 
//	    
//	    //########################################################
//	    //7. Java 18 Simple Web Server 
//	    //########################################################
//	    var server = SimpleFileServer.createFileServer(
//	    		new InetSocketAddress(8080), 
//	    		Path.of("C:\\project\\workspace\\myMultiProject\\module-a\\src\\main\\web"),
//	    		SimpleFileServer.OutputLevel.VERBOSE);
//	    server.createContext("/tst");
//	    //server.start();
//	    
//	    //########################################################
//	    //8. HttpClient API 새로운 HttpClient API가 표준화되었습니다.
//	    //########################################################
//	    // HTTP 클라이언트 생성
//        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2) // HTTP/2 사용
//            .connectTimeout(Duration.ofSeconds(10)).build();
//	    HttpRequest request = HttpRequest.newBuilder()
//	            .uri(URI.create("https://www.google.com")).GET().build();
//	    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//	    System.out.println(response.body());
//	    // 비동기 요청 (콜백 사용)
//	    CompletableFuture<HttpResponse<String>> futureResponse = client.sendAsync(
//	    		request, HttpResponse.BodyHandlers.ofString());
//	    futureResponse.thenAccept(res -> {
//            System.out.println("상태 코드: " + res.statusCode());
//        });  
//	    futureResponse.get(); //비동기 요청 완료 대기.
//	    // HTTP POST 요청 생성
//        String postData = "{\"title\":\"foo\",\"body\":\"bar\",\"userId\":1}";
//        HttpRequest postRequest = HttpRequest.newBuilder().uri(URI.create("https://jsonplaceholder.typicode.com/posts"))
//            .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(postData)).build();
//        // POST 요청 실행
//        HttpResponse<String> postResponse = client.send(postRequest, HttpResponse.BodyHandlers.ofString());
//        System.out.println("응답 본문: " + postResponse.body());
//	    //########################################################
//	    //9. JSHELL 테스트시 좋음
//	    //########################################################
//	    
//	    //########################################################
//	    //10.JDK 23 Linker
//	    //Linker는 자바에서 native 코드(C/C++)의 함수 포인터를 자바 코드로 바인딩하기 위한 API입니다. 
//	    //즉, C 함수의 주소를 자바 함수처럼 호출할 수 있도록 도와주는 역할을 합니다.
//	    //########################################################
//	    LinkerTest();
//	    
//	    //########################################################
//	    //  11. Stream API 개선 - takeWhile, dropWhile Java 1.9
//	    //########################################################
//	    List<Integer> nums = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
//        // 5보다 작은 원소들만 취함
//        List<Integer> lessThan5 = nums.stream()
//                                    .takeWhile(n -> n < 5)
//                                    .collect(Collectors.toList());
//        System.out.println("5보다 작은 숫자: " + lessThan5);
//        // 5보다 작은 원소들을 건너뛰고 나머지 취함
//        List<Integer> greaterThanEqual5 = nums.stream()
//                                           .dropWhile(n -> n < 5)
//                                           .collect(Collectors.toList());
//        System.out.println("5 이상인 숫자: " + greaterThanEqual5);
//        
//        //########################################################
//	    //  12. String indent, lines, strip, transform Java 12
//	    //########################################################
//        String multilineString = "Java\n11\nFeatures";
//        System.out.println("라인별 출력:");
//        multilineString.lines().forEach(System.out::println);
//        
//        // strip() 메서드 - 앞뒤 공백 제거 (유니코드 인식)
//        String paddedString = "  Java 11  ";
//        System.out.println("원본: '" + paddedString + "'");
//        System.out.println("strip 적용: '" + paddedString.strip() + "'");
//        System.out.println("stripLeading 적용: '" + paddedString.stripLeading() + "'");
//        System.out.println("stripTrailing 적용: '" + paddedString.stripTrailing() + "'");
//        String emptyString = "";
//        System.out.println("빈 문자열은 blank인가? " + emptyString.isBlank());
//        
//        String text = "Java 12\nNew Features";
//        String indentedText = text.indent(4);  // 각 줄에 4칸 들여쓰기 추가
//        String original = "hello java 12";
//        String transformed = original.transform(s -> s.toUpperCase());
//        
//        //########################################################
//	    //  13. Teeing Collector
//	    //########################################################
//        double mean = Stream.of(1, 2, 3, 4, 5)
//                .collect(Collectors.teeing(
//                    Collectors.summingDouble(i -> i),
//                    Collectors.counting(),
//                    (sum, count) -> sum / count));
// 
//        System.out.println("평균: " + mean);
//        
//        //########################################################
//        //	14. Java Compiler API  JDK 1.6
//        //########################################################
//        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
//        // 컴파일할 파일
//        File sourceFile = new File("HelloWorld.java");
//        // 컴파일 실행
//        int result1 = compiler.run(null, null, null, sourceFile.getPath());
//        if (result1 == 0) { System.out.println("컴파일 성공!");
//        } else {  System.out.println("컴파일 실패!"); }
//        
//        //########################################################
//        //	15. 향상된 Collection Framework  JDK 1.6
//        //########################################################        
//        Deque<String> deque = new ArrayDeque<>();
//        deque.addFirst("첫 번째");
//        deque.offerFirst("새로운 첫 번째");
//        deque.addLast("마지막");
//        deque.offerLast("새로운 마지막");
//        System.out.println("첫 번째 요소: " + deque.peekFirst());
//        System.out.println("마지막 요소 제거: " + deque.pollLast());
//        
//        //########################################################
//        //	16. BASE64 JDK 1.8
//        //######################################################## 
//        String originalText = "자바 8의 Base64 인코딩 예제";
//        String encodedText = Base64.getEncoder().encodeToString(
//            originalText.getBytes(StandardCharsets.UTF_8));
//        System.out.println("인코딩된 텍스트: " + encodedText);
//        // 디코딩
//        byte[] decodedBytes = Base64.getDecoder().decode(encodedText);
//        String decodedText = new String(decodedBytes, StandardCharsets.UTF_8);
//        System.out.println("디코딩된 텍스트: " + decodedText);
//        
//        //########################################################
//        //	17.  프로세스 API 개선 JDK 1.9
//        //######################################################## 
//        ProcessApiExample();
//        
//        //########################################################
//        //	18.  InputStream의 새로운 메서드 JDK 1.9
//        //######################################################## 
//        // 파일에 텍스트 쓰기
//        try (FileWriter writer = new FileWriter("test.txt")) {writer.write(text);}
//        // Java 9 - readAllBytes()로 모든 내용 읽기
//        try (FileInputStream fis = new FileInputStream("test.txt")) {
//            byte[] data = fis.readAllBytes();
//            System.out.println("readAllBytes(): " + new String(data));
//        }
//        // Java 9 - readNBytes()로 특정 바이트 수만큼 읽기
//        try (FileInputStream fis = new FileInputStream("test.txt")) {
//            byte[] buffer = new byte[10];
//            int bytesRead = fis.readNBytes(buffer, 0, 10);
//        }
//        // Java 9 - transferTo()로 다른 OutputStream으로 내용 전송
//        try (FileInputStream fis = new FileInputStream("test.txt");
//             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
//            fis.transferTo(baos);
//        }
//        
//        //########################################################
//        //	19.  IReactive Streams API JDK 1.9
//        //######################################################## 
//        ReactiveStreamsExample();
//        
//        //########################################################
//        //	20.  애플리케이션 클래스-데이터 공유 (Application Class-Data Sharing) JDK 10
//        //######################################################## 
//        //# 클래스 데이터 아카이브 생성
//        //$ java -Xshare:dump -XX:+UseAppCDS -XX:SharedArchiveFile=app.jsa -XX:SharedClassListFile=classlist.txt
//        //# 생성된 아카이브 사용
//        //$ java -Xshare:on -XX:+UseAppCDS -XX:SharedArchiveFile=app.jsa MyApplication
//        
//        //힙 할당 개선 (Heap Allocation on Alternative Memory Devices)
//        // # 특정 메모리 디바이스에 힙 할당 (예: NV-DIMM)
//        // $ java -XX:AllocateHeapAt=/path/to/memory-device MyApplication
//        
//        //########################################################
//        //	21.  자바 애노테이션 프로세서 JDK 6
//        //########################################################         
//        //GenerateHello와 GenerateHelloProcessor 생성
//        //서비스 등록
//        //META-INF/services/javax.annotation.processing.Processor
//        //내용 com.example.GenerateHelloProcessor 추가
//        //컴파일후에 아래 사용가능.
//        //HelloGenerated.sayHello();
//        
//        //컴파일
//        //javac -cp . -processor com.example.GenerateHelloProcessor com/test/TestClass.java
//        //그레이들
//        //dependencies {
//        //    annotationProcessor project(":your-annotation-processor-module")
//        //}
//	}
//	
//	//	21.  자바 애노테이션 프로세서 JDK 6
//	@Target(ElementType.TYPE)
//	@Retention(RetentionPolicy.SOURCE)
//	public @interface GenerateHello {}
//	
//	@SupportedAnnotationTypes("com.example.GenerateHello")
//	@SupportedSourceVersion(SourceVersion.RELEASE_17)
//	public class GenerateHelloProcessor extends AbstractProcessor {
//		@Override
//	    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
//	        for (Element elem : roundEnv.getElementsAnnotatedWith(GenerateHello.class)) {
//	            try {
//	                JavaFileObject file = processingEnv.getFiler()
//	                    .createSourceFile("com.generated.HelloGenerated");
//
//	                try (Writer writer = file.openWriter()) {
//	                    writer.write("package com.generated;\n");
//	                    writer.write("public class HelloGenerated {\n");
//	                    writer.write("    public static void sayHello() {\n");
//	                    writer.write("        System.out.println(\"Hello from generated class!\");\n");
//	                    writer.write("    }\n");
//	                    writer.write("}\n");
//	                }
//	            } catch (IOException e) {
//	                e.printStackTrace();
//	            }
//	        }
//	        return true;
//	    }
//	}
//	
//	//사용샘플
//	@GenerateHello
//	public class TestClass {}
//	//########################################################         
//	
//	public static void ProcessApiExample() throws IOException {
//        // 현재 프로세스 정보 가져오기
//        ProcessHandle current = ProcessHandle.current();
//        System.out.println("현재 PID: " + current.pid());
//        System.out.println("부모 PID: " +  current.parent().map(ProcessHandle::pid).orElse(-1L));
//        
//        // 프로세스 정보
//        ProcessHandle.Info info = current.info();
//        System.out.println("명령어: " + info.command().orElse("N/A"));
//        System.out.println("시작 시간: " + 
//            info.startInstant().orElse(Instant.MIN));
//        System.out.println("CPU 시간: " + 
//            info.totalCpuDuration().orElse(Duration.ZERO));
//        
//        // 모든 프로세스 열거
//        System.out.println("\n실행 중인 프로세스:");
//        ProcessHandle.allProcesses()
//            .limit(5) // 첫 5개만 출력
//            .forEach(ph -> System.out.println("PID: " + ph.pid() + 
//                " 명령어: " + ph.info().command().orElse("N/A")));
//        
//        // 새 프로세스 시작
//        ProcessBuilder pb = new ProcessBuilder("notepad.exe");
//        Process process = pb.start();
//        
//        // 프로세스 핸들 가져오기
//        ProcessHandle processHandle = process.toHandle();
//        System.out.println("\n시작된 프로세스 PID: " + processHandle.pid());
//        
//        // 프로세스 종료 시 콜백
//        CompletableFuture<ProcessHandle> onExit = processHandle.onExit();
//        onExit.thenAccept(ph -> 
//            System.out.println("프로세스 " + ph.pid() + " 종료됨"));
//        
//        // 5초 후 프로세스 종료
//        new Thread(() -> {
//            try {
//                Thread.sleep(5000);
//                if (processHandle.isAlive()) {
//                    System.out.println("5초 후 프로세스 종료");
//                    processHandle.destroy();
//                }
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }).start();
//    }
//	
//	// ScopedValue 정의
//	public static void StructuredTaskScopeTest() throws InterruptedException {
//		 ScopedValue.where(USER, "Alice").run(() -> {
//			 //이렇게 하면 복제 안됨.
////			 Thread sdt= Thread.ofVirtual().start(()->{System.out.println("Child thread sees: " + USER.get());});
////			 try {
////				sdt.join();
////			} catch (InterruptedException e) {
////				e.printStackTrace();
////			}
//			for(int i=0;i<100;i++) {
//            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
//                scope.fork(() -> {
//                    // ScopedValue는 여기에서 전파되어 사용 가능
//                    System.out.println("Child thread sees: " + USER.get()); // "Alice"
//                    
//                    try (var scope1 = new StructuredTaskScope.ShutdownOnFailure()) {
//                    	scope1.fork(() -> {
//                            // ScopedValue는 여기에서 전파되어 사용 가능
//                            System.out.println("Grand Child thread sees: " + USER.get()); // "Alice"
//                            return null;
//                    	});
//                    	scope1.join();
//                    	scope1.throwIfFailed();
//                    }
//                    return null;
//                });
//                scope.join();
//                scope.throwIfFailed();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//			}
//        });
//    }
//	
//	
//	public static void LinkerTest() throws Throwable {
//        Linker linker = Linker.nativeLinker();
//        SymbolLookup lookup = SymbolLookup.libraryLookup("C:\\project\\MyProject/MyNative.dll", Arena.ofAuto());
//        
//        // native 함수 시그니처: const char* concatStringAndNumber(const char*, int)
//        MethodHandle concatFunc = linker.downcallHandle(
//            lookup.find("concatStringAndNumber").orElseThrow(),
//            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
//        );
//
//        try (Arena arena = Arena.ofConfined()) {
//            MemorySegment text = arena.allocateFrom("INDATA-",Charset.defaultCharset());
//            int number = 42;
//            MemorySegment resultAddr = (MemorySegment) concatFunc.invoke(text, number);
//            System.out.println("resultAddr:"+ resultAddr);
//        }
//    }
//	
//	public static void ReactiveStreamsExample () throws InterruptedException {
//        // Publisher 생성
//        try (SubmissionPublisher<Integer> publisher = new SubmissionPublisher<>()) {
//            // Subscriber 등록
//            publisher.subscribe(new Flow.Subscriber<>() {
//                private Flow.Subscription subscription;
//                @Override
//                public void onSubscribe(Flow.Subscription subscription) {
//                    this.subscription = subscription;
//                    System.out.println("구독 시작");
//                    subscription.request(1); // 1개 요청
//                }
//                @Override
//                public void onNext(Integer item) {
//                    System.out.println("수신: " + item);
//                    try {
//                        // 처리를 위한 시간 지연 시뮬레이션
//                        TimeUnit.MILLISECONDS.sleep(100);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    subscription.request(1); // 다음 항목 요청
//                }
//                @Override
//                public void onError(Throwable throwable) {
//                    System.err.println("오류 발생: " + throwable.getMessage());
//                    throwable.printStackTrace();
//                }
//                @Override
//                public void onComplete() {
//                    System.out.println("스트림 완료");
//                }
//            });
//            
//            // Processor(중간 처리기) 예제
//            // 입력을 제곱하는 프로세서
//            SubmissionPublisher<Integer> processor = new SubmissionPublisher<>();
//            processor.subscribe(new Flow.Subscriber<>() {
//                private Flow.Subscription subscription;
//                @Override
//                public void onSubscribe(Flow.Subscription subscription) {
//                    this.subscription = subscription;
//                    System.out.println("프로세서 구독 시작");
//                    subscription.request(1);
//                }
//                @Override
//                public void onNext(Integer item) {
//                    System.out.println("프로세서 수신: " + item + ", 제곱: " + (item * item));
//                    subscription.request(1);
//                }
//                @Override
//                public void onError(Throwable throwable) {
//                    System.err.println("프로세서 오류: " + throwable.getMessage());
//                }
//                @Override
//                public void onComplete() {
//                    System.out.println("프로세서 완료");
//                }
//            });
//            
//            // 데이터 발행
//            System.out.println("데이터 발행 시작");
//            for (int i = 1; i <= 5; i++) {
//                publisher.submit(i);
//                processor.submit(i * 10);
//                System.out.println("발행: " + i);
//            }
//            // 발행 완료를 위한 대기
//            TimeUnit.SECONDS.sleep(2);
//        }
//        System.out.println("발행자 종료");
//    }
//}
