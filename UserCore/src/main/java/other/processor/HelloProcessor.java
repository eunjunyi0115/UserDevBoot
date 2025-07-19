package other.processor;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject; 

/**  
 * 컴파일러가 Annotation Processor를 인식하려면 다음 경로에 등록 파일이 있어야 합니다:
 * META-INF/services/javax.annotation.processing.Processor
 * 파일에 other.processor.HelloProcessor 등록 하여 컴파일시 processor 처리
 * 또는 java cli는 javac -cp . -processor other.processor.HelloProcessor  -encoding utf-8 other\processor\User.java
 * 
 */
@SupportedAnnotationTypes("other.processor.GenerateHello")
@SupportedSourceVersion(SourceVersion.RELEASE_24) 
public class HelloProcessor extends AbstractProcessor {
    
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    	System.out.println(">>> MyAnnotationProcessor invoked <<<");
        for (Element element : roundEnv.getElementsAnnotatedWith(GenerateHello.class)) {
            try {
                String className = element.getSimpleName().toString();
                System.out.println("className:"+className);
                String greeting = element.getAnnotation(GenerateHello.class).value();
                System.out.println("greeting:"+greeting);
                
                // 새로운 소스 파일 생성
                JavaFileObject file = processingEnv.getFiler().createSourceFile("other.processor.Hello" + className);
                
                try (Writer writer = file.openWriter()) {
                	writer.write("package other.processor;\n");
                    writer.write("public class Hello" + className + " {\n");
                    writer.write("    public String getGreeting() {\n");
                    writer.write("        return \"" + greeting + "\";\n");
                    writer.write("    }\n");
                    writer.write("}\n");
                }  
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }
}