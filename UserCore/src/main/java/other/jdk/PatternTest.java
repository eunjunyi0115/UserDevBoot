package other.jdk;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternTest {
	public static void main(String[] args) {
//		1. (?idmsuxU-idmsuxU) : 패턴 플래그를 "범위 내"에서 설정/해제합니다.
//			idmsuxU는 옵션 플래그를 나타냅니다.
//			i: 대소문자 무시 (case-insensitive)
//			d: UNIX lines (줄 처리)
//			m: multiline (^, $가 라인 시작/끝에 매칭)
//			s: dotall (dot이 개행 포함)
//			u: Unicode case
//			x: comments & 공백 무시
//			U: Unicode-aware case folding
//			(idmsuxU-idmsuxU)는 "이 그룹에서만" 플래그를 켜거나 끕니다
		String str = "Abc\nDef";
        // 기본적으로 대소문자 구분
        Pattern p1 = Pattern.compile("abc");
        System.out.println("1. (?idmsuxU-idmsuxU) 1: "+p1.matcher(str).find()); // false
        // 전체에서 대소문자 무시
        Pattern p2 = Pattern.compile("(?i)abc");
        System.out.println("1. (?idmsuxU-idmsuxU) 2: "+p2.matcher(str).find()); // true
        // 그룹 내에서만 대소문자 무시
        Pattern p3 = Pattern.compile("(?i)A(?-i)bc");  //-i 는 그룹영역만 대소문자구분이 된다.
        System.out.println("1. (?idmsuxU-idmsuxU) 3: "+p3.matcher("Abc").matches()); // true
        System.out.println("1. (?idmsuxU-idmsuxU) 4: "+p3.matcher("AbC").matches()); // false
        System.out.println("1. (?idmsuxU-idmsuxU) 5: "+p3.matcher("abc").matches()); // true
        
//		2.(?idmsux-idmsux:X) 특정 그룹 X 내부에서만 플래그를 변경합니다.
        str = "FOObar";
        // 전체 패턴은 기본(대소문자 구분)
        Pattern p = Pattern.compile("foo(?i:bar)");
        System.out.println("2. (?idmsux-idmsux:X) 1 :"+ p.matcher(str).matches()); // false
        p2 = Pattern.compile("(?i)foo(?-i:bar)");
        System.out.println("2. (?idmsux-idmsux:X) 2 :"+ p2.matcher("FOObar").matches()); // true (bar는 대소문자 구분)

//		3. (?=X) ( X가 있는 조건의 앞만 추출 (소비 X))  매칭 앞에 값 획득
        str = "iam a apple pie";
        // 'apple' 뒤에 ' pie'가 있어야 함
        p = Pattern.compile("apple(?= pie)");
        Matcher m = p.matcher(str);
        if (m.find()) {
            System.out.println("3. (?=X) Matched 1: " + m.group()); // apple
        }
        str = "abc_def ghi_def xyz_def";
        // '_def' 앞에 있는 단어만 찾기
        p = Pattern.compile("\\w+(?=_def)");
        m = p.matcher(str);
        while (m.find()) {
            System.out.println("3. (?=X) Matched 2: " +m.group());
        }
        
//		4.(?!X) (앞에 X가 없으면 매칭) 매칭 앞에 값 획득
        str = "foo bar";
        // 'foo' 뒤에 'baz'가 없으면 매칭
        p = Pattern.compile("foo(?!baz)");
        m = p.matcher(str);
        if (m.find()) {
            System.out.println("4. (?!X) Matched 1: " + m.group()); // foo
        }
        p = Pattern.compile("foo(?! bar)");
        m = p.matcher(str);
        if (m.find()) {
            System.out.println("4. (?!X) Matched 2: " + m.group()); // foo
        }
        
//      5. (?<=X) (긍정형 뒤탐색) (X는 소비 X) 매칭 뒤에 값 획득
        str = "2023년-신규  입니다";
        // '년-' 뒤에 '신규'가 있으면 '신규' 매칭
        p = Pattern.compile("(?<=년-).+(?=\\s+)");
        m = p.matcher(str);
        if (m.find()) {
            System.out.println("5. (?<=X) Matched 1: " + m.group()); // 신규
        }
        str = "Price: $123";
        p = Pattern.compile("(?<=\\$)\\d+");
        m = p.matcher(str);
        if (m.find()) {
            System.out.println("5. (?<=X) Matched 2: " + m.group()); // 출력: 123
        }
        str = "ID:1234, ID:5678, ID:9012";
        // 'ID:' 뒤의 숫자만 추출
        p = Pattern.compile("(?<=ID:)\\d+");
        m = p.matcher(str);
        while (m.find()) {
            System.out.println("5. (?<=X) Matched 3: " +m.group());
        }
        
//      6. (?<!X) 앞에 X가 없으면 매칭. 매칭 뒤에 값 획득
        str = "abc123";
        // 'abc'가 없으면 숫자 매칭
        p = Pattern.compile("(?<!abc)123");
        m = p.matcher(str);
        if (m.find()) {
            System.out.println("6. (?<!X) Matched 1: " + m.group()); // X (매칭 안 됨)
        }
        String str2 = "def123";
        m = p.matcher(str2);
        if (m.find()) {
            System.out.println("6. (?<!X) Matched 2: " + m.group()); // 123
        }
        
//      7. (?>X) 룹 내 X를 "원자적"으로 처리, backtracking(되돌아가기) 금지
        str = "aaaaaab";
        // 일반 그룹 (backtracking 허용)
        p1 = Pattern.compile("a+ab");
        System.out.println("7. (?>X) 1: " +p1.matcher(str).matches()); // true
        // Atomic 그룹 (backtracking 금지)
        p2 = Pattern.compile("(?>a+)ab");
        System.out.println("7. (?>X) 2: " +p2.matcher(str).matches()); // false
        
//        패턴	        의미	                        특징
//        (?idmsuxU)	전체/그룹 플래그 켜기	        옵션 설정
//        (?idmsux:X)	그룹 X 내부에서만 플래그 변경	그룹 국한 옵션
//        (?=X)	        Positive Lookahead	            조건만 검사
//        (?!X)			Negative Lookahead				조건만 검사
//        (?<=X)		Positive Lookbehind				뒤 조건 검사
//        (?<!X)		Negative Lookbehind				뒤 조건 검사
//        (?>X)			Atomic Group					backtracking 차단
	}
}
