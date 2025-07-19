package other.jdk;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.Charset;

public class MemorySegmentTest {
	public static void main(String[] args) {
        // try-with-resources를 사용하여 Arena의 자동 닫힘 보장
        try (Arena arena = Arena.ofConfined()) {
            // 1. 네이티브 메모리 할당 (100 바이트)
            MemorySegment segment = arena.allocate(100);
            System.out.println("할당된 메모리 크기: " + segment.byteSize() + " 바이트");
            
            int offset = 0;
            // 2. 메모리에 값 쓰기
            System.out.println("offset:"+offset);
            segment.set(ValueLayout.JAVA_INT, offset, 42);        // 첫 위치에 정수 42 저장
            offset += Integer.BYTES;
            
            System.out.println("offset:"+offset);
            segment.set(ValueLayout.JAVA_INT, offset, 99);        // 4바이트 위치에 정수 99 저장
            offset += Integer.BYTES;
            
            System.out.println("offset:"+offset);
            segment.set(ValueLayout.JAVA_DOUBLE, offset, 3.14);   // 8바이트 위치에 double 값 저장
            offset += Double.BYTES;
            
            System.out.println("offset:"+offset);
            segment.setString(offset, "Hello, MemorySegment!    ",Charset.forName("utf-8"));
            offset += "Hello, MemorySegment!   ".getBytes().length;
            
            System.out.println("offset:"+offset);
            segment.setString(offset, "GOOD!",Charset.forName("utf-8"));
            offset += "GOOD!".getBytes().length;
            
            
            System.out.println("offset:"+offset);
            byte[] setByte = segment.toArray(ValueLayout.JAVA_BYTE);
            System.out.println(new String(setByte));
            
//            byte[] tmpByte = new byte[] {1,2,3,4,5,6,7,8};
//            MemorySegment stringSegment = arena.allocate(tmpByte.length);  //native 세그먼트만 사용가능.
//            stringSegment.copyFrom(MemorySegment.ofArray(tmpByte)); 
//            
//            System.out.println("ADDRESS:"+ ValueLayout.ADDRESS.byteSize());
//            segment.set(ValueLayout.ADDRESS, offset , stringSegment);
//            System.out.println("stringSegment:"+stringSegment);
            
            setByte = segment.toArray(ValueLayout.JAVA_BYTE);
            System.out.println("전체:"+new String(setByte));
            
            // 3. 메모리에서 값 읽기
            int value1 = segment.get(ValueLayout.JAVA_INT,  Integer.BYTES * 0);
            System.out.println("읽은 값 1: " + value1);
            int value2 = segment.get(ValueLayout.JAVA_INT, Integer.BYTES * 1);//4);
            System.out.println("읽은 값 2: " + value2);
            double value3 = segment.get(ValueLayout.JAVA_DOUBLE, Integer.BYTES * 2); //8);
            System.out.println("읽은 값 3: " + value3);
//            MemorySegment addSeg = segment.get(ValueLayout.ADDRESS, Integer.BYTES * 2 + Double.BYTES);
//            System.out.println("addSeg:"+addSeg.address()+"  "+ addSeg);
//            String value4 = segment.getString(Integer.BYTES * 2 + Double.BYTES);
//            System.out.println("읽은 값 4: " + value4);
//            
//            String value5 = segment.getString(40);
//            System.out.println("읽은 값 5: " + value5);
            
            
            int strOffset = Integer.BYTES * 2 + Double.BYTES;
            byte[] bytes = new byte[24];
            MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, strOffset, bytes, 0, bytes.length);
            System.out.println("읽은 값 4: " + new String(bytes));
            
            strOffset+=bytes.length;
            bytes = new byte[5];
            MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, strOffset, bytes, 0, bytes.length);
            System.out.println("읽은 값 5: " + new String(bytes));
            
//            MemorySegment addSeg = segment.get(ValueLayout.ADDRESS, 40);
//            System.out.println("tmpByte:"+addSeg.address()+"  "+ addSeg);
//            
//            addSeg = segment.get(ValueLayout.ADDRESS,48);
//            System.out.println("tmpByte:"+addSeg.address()+"  "+ addSeg);
           
//            MemorySegment strMessage = segment.get(ValueLayout.ADDRESS, Integer.BYTES + Double.BYTES); //8);
//            byte[] result = stringSegment.toArray(ValueLayout.JAVA_BYTE);
            
          
            
            // 4. 바이트 배열 복사
//            byte[] bytes = "Hello, MemorySegment!".getBytes();
//            MemorySegment stringSegment = arena.allocate(bytes.length);
//            stringSegment.copyFrom(MemorySegment.ofArray(bytes)); // 배열에서 세그먼트로 복사
//            
//            // 5. 바이트 배열로 다시 변환
//            byte[] result = stringSegment.toArray(ValueLayout.JAVA_BYTE);
//            System.out.println("문자열 결과: " + new String(result));
        }
        // Arena가 자동으로 닫히고 할당된 모든 메모리가 해제됩니다.
    }
}
