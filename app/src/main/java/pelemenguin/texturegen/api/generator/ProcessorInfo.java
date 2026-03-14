package pelemenguin.texturegen.api.generator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

@Retention(RetentionPolicy.RUNTIME)
public @interface ProcessorInfo {
    String value();
    Class<? extends JsonDeserializer<? extends Processor>> deserializer() default NoSerializer.class;
    Class<? extends JsonSerializer<? extends Processor>> serializer() default NoSerializer.class;

    public static abstract class NoSerializer implements JsonDeserializer<Processor>, JsonSerializer<Processor> {}
}
