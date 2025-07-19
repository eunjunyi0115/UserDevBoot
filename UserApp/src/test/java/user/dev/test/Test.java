package user.dev.test;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.RangeSet;
import com.google.common.collect.Table;
import com.google.common.collect.TreeRangeMap;
import com.google.common.collect.TreeRangeSet;

public class Test {
	public static void main(String[] args) {
		
//		Map<String,List<UserTest>> tmpMap = new HashMap<String,List<UserTest>>();
//		
//		List<UserTest> tmpList = new ArrayList<UserTest>();
//		for(int idx=0;idx<10;idx++) {
//			tmpList.add(new UserTest(Long.parseLong(idx+""),"name_"+idx,"emal_"+idx));
//		}
//		tmpMap.put("DATA", tmpList);
//		
//		ObjectMapper objectMapper = new ObjectMapper();
//		String jsonStr = "";
//		try {
//			jsonStr = objectMapper.writeValueAsString(tmpMap);
//		} catch (JsonProcessingException e) {
//			e.printStackTrace();
//		}
//		System.out.println("jsonStr:"+jsonStr);
//		
//		
//		Map<String,List<UserTest>> decodeMap =  getData(jsonStr, tmpMap.getClass()); //내부 타입이 소거 됨.(허허)
//		System.out.println("decodeMap:"+jsonStr);
//		
//		TypeReference typer = new TypeReference<HashMap<String,List<UserTest>>>(){};
//		decodeMap =  getData(jsonStr, typer);
//		System.out.println("decodeMap2:"+jsonStr);
		
		ImmutableList<String> list1 = ImmutableList.of("a", "b", "c");
		ImmutableList<String> list2 = ImmutableList.<String>builder()
		    .add("x")
		    .add("y", "z")
		    .build();

		// 기존 컬렉션으로부터 생성
		List<String> mutableList = Arrays.asList("1", "2", "3");
		ImmutableList<String> list3 = ImmutableList.copyOf(mutableList);

		System.out.println(list1); // [a, b, c]
		System.out.println(list2); // [a, b, c]
		System.out.println(list3); // [a, b, c]
		
		Table<String, String, Integer> table = HashBasedTable.create();
		table.put("Seoul", "January", -5);
		table.put("Seoul", "July", 28);
		table.put("Busan", "January", 3);
		table.put("Busan", "July", 30);

		System.out.println(table.get("Seoul", "January")); // -5
		System.out.println(table.row("Seoul")); // {January=-5, July=28}
		System.out.println(table.column("January")); // {Seoul=-5, Busan=3}
		
		
		// RangeSet
		RangeSet<Integer> rangeSet = TreeRangeSet.create();
		rangeSet.add(Range.closed(1, 10));
		rangeSet.add(Range.closedOpen(12, 15));

		System.out.println(rangeSet); // true
		
		System.out.println(rangeSet.contains(5)); // true
		System.out.println(rangeSet.contains(15)); // false
		
		
		for(int i=1;i<16;i++) {
			System.out.println(String.format("%d 는 %s 이다",i,rangeSet.contains(i))); // true
		}

		// RangeMap
		RangeMap<Integer, String> rangeMap = TreeRangeMap.create();
		rangeMap.put(Range.closed(1, 30), "low");
		rangeMap.put(Range.closed(31, 80), "medium");
		rangeMap.put(Range.closed(81, 100), "high");

		System.out.println(rangeMap.get(25)); // "low"
		System.out.println(rangeMap.get(50)); // "medium"
	}
	
	
	public static <T> T getData(String jsonStr, Class tmpClass) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			
			JavaType jjavaType = objectMapper.getTypeFactory().constructType(tmpClass);
			System.out.println("jjavaType:"+jjavaType);
			T decodeMap = (T)objectMapper.readValue(jsonStr, jjavaType);
			System.out.println(decodeMap.getClass().getGenericSuperclass());
			
			//System.out.println("decodeMap:"+jsonStr);
			return decodeMap;
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static <T> T getData(String jsonStr, TypeReference tmpClass) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			JavaType jjavaType = objectMapper.getTypeFactory().constructType(tmpClass);
			System.out.println("jjavaType:"+jjavaType);
			
			T decodeMap = (T)objectMapper.readValue(jsonStr, tmpClass);
			System.out.println(decodeMap.getClass().getGenericSuperclass());
			//System.out.println("decodeMap:"+jsonStr);
			return decodeMap;
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}
}

class TypeUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 단순 클래스 타입을 JavaType으로 변환
     */
    public static JavaType toJavaType(Class<?> clazz) {
        return objectMapper.getTypeFactory().constructType(clazz);
    }

    /**
     * 제네릭을 포함한 Type (예: List<User>.class 등)을 JavaType으로 변환
     */
    public static JavaType toJavaType(Type type) {
        return objectMapper.getTypeFactory().constructType(type);
    }

    /**
     * JavaType으로부터 TypeReference와 유사한 JSON 역직렬화
     */
    public static <T> T readValue(String json, JavaType javaType) throws Exception {
        return objectMapper.readValue(json, javaType);
    }
}
