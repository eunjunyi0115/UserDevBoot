package other;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.RangeSet;
import com.google.common.collect.Table;
import com.google.common.collect.TreeRangeMap;
import com.google.common.collect.TreeRangeSet;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.google.common.math.DoubleMath;
import com.google.common.math.IntMath;
import com.google.common.math.LongMath;
import com.google.common.net.InetAddresses;
import com.google.common.net.InternetDomainName;
import com.google.common.primitives.Booleans;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.RateLimiter;

import lombok.Data;

public class Guava {

	public static void main(String[] args) throws Throwable{
		////////////////////////////////////////////////////////////
		/////////////1. 컬렉션 (Collections)  /////////////////////
		///////////////////////////////////////////////////////////
		//####### ImmutableList - 불변 리스트
			// 생성 방법들
		ImmutableList<String> list1 = ImmutableList.of("a", "b", "c");
		ImmutableList<String> list2 = ImmutableList.<String>builder()
		    .add("x")
		    .add("y", "z")
		    .build();

		// 기존 컬렉션으로부터 생성
		List<String> mutableList = Arrays.asList("1", "2", "3");
		ImmutableList<String> list3 = ImmutableList.copyOf(mutableList);
		System.out.println(list1); // [a, b, c]

		//####### ImmutableSet - 불변 집합
		ImmutableSet<String> set = ImmutableSet.of("red", "green", "blue");
		ImmutableSet<String> set2 = ImmutableSet.<String>builder()
		    .add("apple")
		    .add("banana")
		    .add("cherry")
		    .build();
		System.out.println(set.contains("red")); // true

		//####### ImmutableMap - 불변 맵
		ImmutableMap<String, Integer> map = ImmutableMap.of(
		    "apple", 1,
		    "banana", 2,
		    "cherry", 3
		);

		ImmutableMap<String, String> map2 = ImmutableMap.<String, String>builder()
		    .put("KR", "Korea")
		    .put("US", "United States")
		    .put("JP", "Japan")
		    .build();
		System.out.println(map.get("apple")); // 1

		//####### Multiset - 중복을 허용하는 집합
		Multiset<String> multiset = HashMultiset.create();
		multiset.add("apple");
		multiset.add("apple");
		multiset.add("banana");
		System.out.println(multiset.count("apple")); // 2
		System.out.println(multiset.count("banana")); // 1
		System.out.println(multiset.size()); // 3
		
		//####### Multimap - 하나의 키에 여러 값을 저장
		Multimap<String, String> multimap = ArrayListMultimap.create();
		multimap.put("fruit", "apple");
		multimap.put("fruit", "banana");
		multimap.put("vegetable", "carrot");
		System.out.println(multimap.get("fruit")); // [apple, banana]
		System.out.println(multimap.size()); // 3
		
		//####### BiMap - 양방향 맵
		BiMap<String, Integer> biMap = HashBiMap.create();
		biMap.put("apple", 1);
		biMap.put("banana", 2);
		System.out.println(biMap.get("apple")); // 1
		System.out.println(biMap.inverse().get(1)); // "apple"
		
		//####### Table - 2차원 맵
		Table<String, String, Integer> table = HashBasedTable.create();
		table.put("Seoul", "January", -5);
		table.put("Seoul", "July", 28);
		table.put("Busan", "January", 3);
		table.put("Busan", "July", 30);
		System.out.println(table.get("Seoul", "January")); // -5
		System.out.println(table.row("Seoul")); // {January=-5, July=28}
		System.out.println(table.column("January")); // {Seoul=-5, Busan=3}
		
		//####### RangeSet과 RangeMap
		RangeSet<Integer> rangeSet = TreeRangeSet.create();
		rangeSet.add(Range.closed(1, 10));
		rangeSet.add(Range.closedOpen(11, 15));
		System.out.println(rangeSet.contains(5)); // true
		System.out.println(rangeSet.contains(15)); // false

		//####### RangeMap
		RangeMap<Integer, String> rangeMap = TreeRangeMap.create();
		rangeMap.put(Range.closed(1, 30), "low");
		rangeMap.put(Range.closed(31, 80), "medium");
		rangeMap.put(Range.closed(81, 100), "high");
		System.out.println(rangeMap.get(25)); // "low"
		System.out.println(rangeMap.get(50)); // "medium"

		////////////////////////////////////////////////////////////
		/////////////2. 캐시 (Cache)  /////////////////////
		///////////////////////////////////////////////////////////
		//#######  기본 캐시 사용법
		Cache<String, String> cache = CacheBuilder.newBuilder()
			    .maximumSize(1000)
			    .expireAfterWrite(10, TimeUnit.MINUTES)
			    .build();
		// 값 저장
		cache.put("key1", "value1");
		// 값 조회
		String value = cache.getIfPresent("key1");
		System.out.println(value); // "value1"
		// 값이 없을 경우 기본값 반환
		String value2 = cache.get("key2", () -> "default value");
		System.out.println(value2); // "default value"

		//#######  LoadingCache - 자동 로딩 캐시
		LoadingCache<String, String> loadingCache = CacheBuilder.newBuilder()
		    .maximumSize(1000)
		    .expireAfterAccess(30, TimeUnit.MINUTES)
		    .build(new CacheLoader<String, String>() {
		        @Override
		        public String load(String key) {
		            return "Loaded value for: " + key;
		        }
		    });
		// 값이 없으면 자동으로 load() 메서드 호출
		value = loadingCache.get("someKey");
		System.out.println(value); // "Loaded value for: someKey"
		
		////////////////////////////////////////////////////////////
		/////////////3. 문자열 처리 (Strings)  /////////////////////
		///////////////////////////////////////////////////////////
		//#######  Strings 유틸리티
		// null이나 빈 문자열 체크
		System.out.println(Strings.isNullOrEmpty("")); // true
		System.out.println(Strings.isNullOrEmpty(null)); // true
		System.out.println(Strings.isNullOrEmpty("hello")); // false
		// null을 빈 문자열로 변환
		System.out.println(Strings.nullToEmpty(null)); // ""
		System.out.println(Strings.nullToEmpty("hello")); // "hello"
		// 빈 문자열을 null로 변환
		System.out.println(Strings.emptyToNull("")); // null
		System.out.println(Strings.emptyToNull("hello")); // "hello"
		// 문자열 패딩
		System.out.println(Strings.padStart("7", 3, '0')); // "007"
		System.out.println(Strings.padEnd("hello", 10, '!')); // "hello!!!!!"
		// 문자열 반복
		System.out.println(Strings.repeat("ha", 3)); // "hahaha"
		
		//####### Joiner - 문자열 결합
		List<String> list = Arrays.asList("red", "green", "blue");

		// 기본 조인
		String result1 = Joiner.on(", ").join(list);
		System.out.println(result1); // "red, green, blue"
		// null 값 건너뛰기
		List<String> listWithNull = Arrays.asList("red", null, "blue");
		String result2 = Joiner.on(", ").skipNulls().join(listWithNull);
		System.out.println(result2); // "red, blue"
		// null을 특정 문자열로 대체
		String result3 = Joiner.on(", ").useForNull("N/A").join(listWithNull);
		System.out.println(result3); // "red, N/A, blue"
		// Map 조인
		Map<String, String> map1 = ImmutableMap.of("name", "John", "age", "30");
		String result4 = Joiner.on(", ").withKeyValueSeparator("=").join(map1);
		System.out.println(result4); // "name=John, age=30"
		
		//####### Splitter - 문자열 분할
		String text = "red,green,,blue,";
		// 기본 분할
		Iterable<String> result5 = Splitter.on(',').split(text);
		System.out.println(result5); // [red, green, , blue, ]
		// 빈 문자열 제거
		Iterable<String> result6 = Splitter.on(',').omitEmptyStrings().split(text);
		System.out.println(result6); // [red, green, blue]
		// 공백 제거
		String text2 = "red, green , blue ";
		Iterable<String> result7 = Splitter.on(',')
		    .trimResults()
		    .omitEmptyStrings()
		    .split(text2);
		System.out.println(result7); // [red, green, blue]
		// 정규식으로 분할
		Iterable<String> result8 = Splitter.onPattern("\\s+").split("hello world java");
		System.out.println(result8); // [hello, world, java]
		// Map으로 분할
		String keyValue = "name=John,age=30,city=Seoul";
		Map<String, String> map4 = Splitter.on(',')
		    .withKeyValueSeparator('=')
		    .split(keyValue);
		System.out.println(map4); // {name=John, age=30, city=Seoul}
		
		//####### CharMatcher - 문자 매칭
		String text1 = "Hello123World456";

		// 숫자 제거
		String result10 = CharMatcher.inRange('0', '9').removeFrom(text1);
		System.out.println(result10); // "HelloWorld"

		// 숫자만 유지
		String result11 = CharMatcher.inRange('0', '9').retainFrom(text);
		System.out.println(result11); // "123456"

		// 공백 문자 정리
		String text3 = "  hello   world  ";
		String result12 = CharMatcher.whitespace().trimAndCollapseFrom(text3, ' ');
		System.out.println(result12); // "hello world"

		// 특정 문자 카운트
		int count = CharMatcher.is('l').countIn("hello");
		System.out.println(count); // 2
		
		////////////////////////////////////////////////////////////
		/////////////4. 원시 타입 유틸리티 (Primitives) /////////////////////
		///////////////////////////////////////////////////////////
		//####### Ints 유틸리티
		// 배열을 리스트로 변환
		int[] array = {1, 2, 3, 4, 5};
		List<Integer> list4 = Ints.asList(array);
		System.out.println(list4); // [1, 2, 3, 4, 5]

		// 리스트를 배열로 변환
		int[] backToArray = Ints.toArray(list4);

		// 최대값, 최소값
		System.out.println(Ints.max(1, 3, 2, 5)); // 5
		System.out.println(Ints.min(1, 3, 2, 5)); // 1

		// 문자열을 int로 변환 (안전하게)
		Integer result = Ints.tryParse("123");
		System.out.println(result); // 123

		Integer invalid = Ints.tryParse("abc");
		System.out.println(invalid); // null

		// 배열 연결
		int[] array1 = {1, 2};
		int[] array2 = {3, 4};
		int[] combined = Ints.concat(array1, array2);
		System.out.println(Arrays.toString(combined)); // [1, 2, 3, 4]
		
		//####### 다른 원시 타입들
		// Longs
		List<Long> longList = Longs.asList(1L, 2L, 3L);
		System.out.println(Longs.max(1L, 5L, 3L)); // 5

		// Doubles
		List<Double> doubleList = Doubles.asList(1.1, 2.2, 3.3);
		System.out.println(Doubles.isFinite(Double.POSITIVE_INFINITY)); // false

		// Booleans
		boolean[] boolArray = {true, false, true};
		List<Boolean> boolList = Booleans.asList(boolArray);
		System.out.println(Booleans.countTrue(true, false, true, false)); // 2

		////////////////////////////////////////////////////////////
		/////////////5. 수학 유틸리티 (Math) /////////////////////
		///////////////////////////////////////////////////////////
		// 거듭제곱
		System.out.println(IntMath.pow(2, 10)); // 1024
		// 팩토리얼
		System.out.println(IntMath.factorial(5)); // 120
		// 최대공약수
		System.out.println(IntMath.gcd(12, 18)); // 6
		// 로그
		System.out.println(IntMath.log2(8, RoundingMode.UNNECESSARY)); // 3
		// 제곱근
		System.out.println(IntMath.sqrt(16, RoundingMode.UNNECESSARY)); // 4
		// 나눗셈 (반올림 모드 지정)
		System.out.println(IntMath.divide(7, 3, RoundingMode.CEILING)); // 3
		
		///######### LongMath와 DoubleMath
		// LongMath
		System.out.println(LongMath.factorial(10)); // 3628800
		System.out.println(LongMath.binomial(10, 3)); // 120
		// DoubleMath
		System.out.println(DoubleMath.isMathematicalInteger(3.0)); // true
		System.out.println(DoubleMath.isMathematicalInteger(3.14)); // false
		System.out.println(DoubleMath.roundToInt(3.7, RoundingMode.HALF_UP)); // 4
		
		////////////////////////////////////////////////////////////
		/////////////6. I/O 유틸리티 /////////////////////
		///////////////////////////////////////////////////////////		
		//////######### Files 유틸리티
		
		File file = new File("C:\\Users\\eunju\\Downloads\\main.xhtml");
		// 파일 읽기
		try {
		    // 전체 내용을 문자열로 읽기
		    String content = Files.asCharSource(file, Charsets.UTF_8).read();
		    
		    // 줄별로 읽기
		    List<String> lines = Files.asCharSource(file, Charsets.UTF_8).readLines();
		    
		    // 바이트로 읽기
		    byte[] bytes = Files.asByteSource(file).read();
		    
		} catch (IOException e) {
		    e.printStackTrace();
		}

		// 파일 쓰기
		try {
		    Files.asCharSink(file, Charsets.UTF_8).write("Hello World");
		    
		    // 줄 단위로 쓰기
		    List<String> lines = Arrays.asList("Line 1", "Line 2", "Line 3");
		    Files.asCharSink(file, Charsets.UTF_8).writeLines(lines);
		    
		} catch (IOException e) {
		    e.printStackTrace();
		}

		// 파일 복사
		File destination = new File("copy.txt");
		try {
		    Files.copy(file, destination);
		} catch (IOException e) {
		    e.printStackTrace();
		}
		
		//########## Resources 유틸리티
		try {
		    // 클래스패스에서 리소스 읽기
		    URL resource = Resources.getResource("config.properties");
		    String content = Resources.toString(resource, Charsets.UTF_8);
		    
		    byte[] bytes = Resources.toByteArray(resource);
		    
		} catch (Throwable e) {
		    e.printStackTrace();
		}
		
		////////////////////////////////////////////////////////////
		/////////////7. 해싱 (Hashing) /////////////////////
		///////////////////////////////////////////////////////////
		//########## Hashing 유틸리티
		// MD5 해싱
		HashFunction md5 = Hashing.md5();
		HashCode hash1 = md5.hashString("hello world", Charsets.UTF_8);
		System.out.println(hash1.toString()); // "5d41402abc4b2a76b9719d911017c592"

		// SHA-256 해싱
		HashFunction sha256 = Hashing.sha256();
		HashCode hash2 = sha256.hashString("hello world", Charsets.UTF_8);
		System.out.println(hash2.toString());

		// 정수 해싱
		HashCode hash3 = Hashing.goodFastHash(32).hashInt(12345);
		System.out.println(hash3.asInt());

		// CRC32 해싱
		HashFunction crc32 = Hashing.crc32();
		HashCode hash4 = crc32.hashBytes("hello".getBytes());
		System.out.println(hash4.toString());
		
		//########## BloomFilter
		// 문자열용 BloomFilter 생성 (예상 요소 수: 500, 오탐률: 0.01)
		BloomFilter<CharSequence> bloomFilter = BloomFilter.create(
		    Funnels.stringFunnel(Charsets.UTF_8), 500, 0.01);

		bloomFilter.put("apple");
		bloomFilter.put("banana");
		bloomFilter.put("cherry");

		System.out.println(bloomFilter.mightContain("apple")); // true
		System.out.println(bloomFilter.mightContain("grape")); // false (확률적)
		
		////////////////////////////////////////////////////////////
		/////////////8. 이벤트 버스 (EventBus) /////////////////////
		///////////////////////////////////////////////////////////
		//################# 기본 EventBus 사용
		// 이벤트 클래스\
		@Data
		class OrderEvent {
		    private final String orderId;
		    private final double amount;
		    
		    public OrderEvent(String orderId, double amount) {
		        this.orderId = orderId;
		        this.amount = amount;
		    }
		}
		// 이벤트 리스너
		class OrderEventListener {
		    @Subscribe
		    public void handleOrderEvent(OrderEvent event) {
		        System.out.println("Order processed: " + event.getOrderId() + 
		                          ", Amount: " + event.getAmount());
		    }
		}
		// 사용 예제
		EventBus eventBus = new EventBus();
		OrderEventListener listener = new OrderEventListener();
		// 리스너 등록
		eventBus.register(listener);
		// 이벤트 발행
		eventBus.post(new OrderEvent("ORDER-001", 100.50));
		
		//####### AsyncEventBus
		AsyncEventBus asyncEventBus = new AsyncEventBus(Executors.newCachedThreadPool());
		asyncEventBus.register(new OrderEventListener());
		asyncEventBus.post(new OrderEvent("ORDER-002", 200.75));
		
		////////////////////////////////////////////////////////////
		/////////////9. 리플렉션 (Reflection) /////////////////////
		///////////////////////////////////////////////////////////	
		//######### 	TypeToken - 제네릭 타입 처리
		// 제네릭 타입 토큰 생성
		TypeToken<List<String>> listType = new TypeToken<List<String>>() {};
		Type type = listType.getType();
		System.out.println(type); // java.util.List<java.lang.String>

		// Map의 제네릭 타입
		TypeToken<Map<String, Integer>> mapType = new TypeToken<Map<String, Integer>>() {};
		System.out.println(mapType.getType());
		
		//######### 	ClassPath - 클래스패스 스캔
		try {
		    ClassPath classpath = ClassPath.from(Thread.currentThread().getContextClassLoader());
		    // 특정 패키지의 모든 클래스 찾기
		    for (ClassPath.ClassInfo classInfo : classpath.getTopLevelClasses("com.example")) {
		        System.out.println("Found class: " + classInfo.getName());
		    }
		    // 특정 패키지의 모든 리소스 찾기
		    for (ClassPath.ResourceInfo resourceInfo : classpath.getResources()) {
		        if (resourceInfo.getResourceName().endsWith(".properties")) {
		            System.out.println("Found resource: " + resourceInfo.getResourceName());
		        }
		    }
		} catch (IOException e) {
		    e.printStackTrace();
		}
		
		////////////////////////////////////////////////////////////
		/////////////10. 네트워킹 /////////////////////
		///////////////////////////////////////////////////////////
		//############# InternetDomainName
		InternetDomainName domain = InternetDomainName.from("www.google.com");

		System.out.println(domain.topPrivateDomain()); // google.com
		System.out.println(domain.isTopPrivateDomain()); // false
		System.out.println(domain.hasPublicSuffix()); // true

		InternetDomainName publicSuffix = domain.publicSuffix();
		System.out.println(publicSuffix); // com
		
		//############# InetAddresses
		// IP 주소 파�ing
		InetAddress addr = InetAddresses.forString("192.168.1.1");
		System.out.println(addr.getHostAddress()); // 192.168.1.1

		// IP 주소 유효성 검사
		System.out.println(InetAddresses.isInetAddress("192.168.1.1")); // true
		System.out.println(InetAddresses.isInetAddress("invalid")); // false

		// IPv4/IPv6 구분
		System.out.println(InetAddresses.isInetAddress("::1")); // true (IPv6)
		
		////////////////////////////////////////////////////////////
		/////////////11. 동시성 (Concurrency) /////////////////////
		///////////////////////////////////////////////////////////		
		//############# ListenableFuture
		ListeningExecutorService executor = MoreExecutors.listeningDecorator(
			    Executors.newFixedThreadPool(10));

		// 비동기 작업 실행
		ListenableFuture<String> future = executor.submit(() -> {
		    Thread.sleep(1000);
		    return "Hello from background thread";
		});

		// 콜백 추가
		Futures.addCallback(future, new FutureCallback<String>() {
		    @Override
		    public void onSuccess(String result) {
		        System.out.println("Success: " + result);
		    }
		    
		    @Override
		    public void onFailure(Throwable t) {
		        System.out.println("Failed: " + t.getMessage());
		    }
		}, MoreExecutors.directExecutor());

		// 여러 Future 결합
		ListenableFuture<String> future1 = executor.submit(() -> "Hello");
		ListenableFuture<String> future2 = executor.submit(() -> "World");
		ListenableFuture<List<String>> combined1 = Futures.allAsList(future1, future2);
		
		//############# RateLimiter - 속도 제한
		// 초당 2개의 요청만 허용
		RateLimiter rateLimiter = RateLimiter.create(2.0);

		for (int i = 0; i < 10; i++) {
		    rateLimiter.acquire(); // 필요하면 대기
		    System.out.println("Request " + i + " at " + System.currentTimeMillis());
		}

		// 타임아웃과 함께 사용
		boolean acquired = rateLimiter.tryAcquire(1, TimeUnit.SECONDS);
		if (acquired) {
		    System.out.println("Request allowed");
		} else {
		    System.out.println("Request denied - rate limit exceeded");
		}
		
		//######## Monitor - 동기화
		
	}
	
}
