//package other.jdk;
//
//import java.lang.foreign.Arena;
//import java.lang.foreign.GroupLayout;
//import java.lang.foreign.MemoryLayout;
//import java.lang.foreign.MemorySegment;
//import java.lang.foreign.SequenceLayout;
//import java.lang.foreign.ValueLayout;
//import java.lang.invoke.VarHandle;
//
//public class MemorySegmentForStructLayoutExample {
//	public static void main(String[] args) {
//        // C 구조체 정의:
//        // struct Person {
//        //     int id;
//        //     double salary;
//        //     char name[32];
//        // }
//        
//        // 1. 구조체 레이아웃 정의
//        SequenceLayout nameLayout = MemoryLayout.sequenceLayout(32, ValueLayout.JAVA_BYTE);
//        
//        GroupLayout personLayout = MemoryLayout.structLayout(
//            ValueLayout.JAVA_INT.withName("id"),
//            MemoryLayout.paddingLayout(4),
//            ValueLayout.JAVA_DOUBLE.withName("salary"),
//            nameLayout.withName("name")
//        );
//        
//        // 레이아웃 정보 출력
//        System.out.println("구조체 크기: " + personLayout.byteSize() + " 바이트");
//        System.out.println("구조체 정렬: " + personLayout.byteAlignment() + " 바이트");
//        
//        // 2. 필드에 접근하기 위한 VarHandle 생성
//        VarHandle idHandle = personLayout.varHandle(
//            MemoryLayout.PathElement.groupElement("id")
//        );
//        
//        VarHandle salaryHandle = personLayout.varHandle(
//            MemoryLayout.PathElement.groupElement("salary")
//        );
//        
//        // 3. 메모리 할당 및 필드 설정
//        try (Arena arena = Arena.ofConfined()) {
//            MemorySegment person = arena.allocate(personLayout);
//            
//            // 필드 값 설정
//            idHandle.set(person, 42);
//            salaryHandle.set(person, 75000.50);
//            
//            // name 필드에 문자열 쓰기
//            byte[] nameBytes = "John Smith".getBytes();
//            MemorySegment nameSegment = person.asSlice(
//                personLayout.byteOffset(MemoryLayout.PathElement.groupElement("name")),
//                nameLayout.byteSize()
//            );
//            
//            // 문자열을 name 필드에 복사 (최대 32바이트)
//            //nameSegment.copyFrom(MemorySegment.ofArray(nameBytes), 0, 0, Math.min(nameBytes.length, 32));
//            nameSegment.copyFrom(MemorySegment.ofArray(nameBytes));
//            
//            // 값 읽기
//            int id = (int) idHandle.get(person);
//            double salary = (double) salaryHandle.get(person);
//            
//            // name 필드 읽기 (널 종료 문자열로 가정)
//            byte[] nameResult = new byte[32];
//            for (int i = 0; i < 32; i++) {
//                byte b = nameSegment.get(ValueLayout.JAVA_BYTE, i);
//                if (b == 0) break; // 널 종료자 발견
//                nameResult[i] = b;
//            }
//            String name = new String(nameResult).trim();
//            
//            System.out.println("ID: " + id);
//            System.out.println("Salary: " + salary);
//            System.out.println("Name: " + name);
//            
//            // 4. 구조체 배열 만들기
//            int personCount = 3;
//            MemorySegment persons = arena.allocate(personLayout, personCount);
//            
//            // 첫 번째 구조체에 접근
//            MemorySegment firstPerson = persons.asSlice(0, personLayout.byteSize());
//            idHandle.set(firstPerson, 1001);
//            
//            // 두 번째 구조체에 접근
//            MemorySegment secondPerson = persons.asSlice(personLayout.byteSize(), personLayout.byteSize());
//            idHandle.set(secondPerson, 1002);
//            
//            // 첫 번째와 두 번째 구조체의 ID 확인
//            System.out.println("첫 번째 사람 ID: " + idHandle.get(firstPerson));
//            System.out.println("두 번째 사람 ID: " + idHandle.get(secondPerson));
//        }
//    }
//}
