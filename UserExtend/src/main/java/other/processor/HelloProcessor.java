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

@SupportedAnnotationTypes("GenerateHello")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class HelloProcessor extends AbstractProcessor {
    
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(GenerateHello.class)) {
            try {
                String className = element.getSimpleName().toString();
                System.out.println("className:"+className);
                String greeting = element.getAnnotation(GenerateHello.class).value();
                System.out.println("greeting:"+greeting);
                
                // 새로운 소스 파일 생성
                JavaFileObject file = processingEnv.getFiler().createSourceFile("Hello" + className);
                
                try (Writer writer = file.openWriter()) {
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