package com.county.coldchain.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * 领域枚举统一以 {"code":"A","label":"A级"} 输出，便于前端直接展示中文；
 * 入参兼容 code 字符串（如 "SORT"）或该对象。枚举需含 public label 字段。
 */
@Configuration
public class JacksonEnumModule {

    @Bean
    public Hibernate6Module hibernate6Module() {
        Hibernate6Module module = new Hibernate6Module();
        module.enable(Hibernate6Module.Feature.FORCE_LAZY_LOADING);
        module.disable(Hibernate6Module.Feature.USE_TRANSIENT_ANNOTATION);
        return module;
    }

    @Bean
    public com.fasterxml.jackson.databind.Module labelEnumModule() {
        SimpleModule module = new SimpleModule("labelEnum");
        @SuppressWarnings({"rawtypes", "unchecked"})
        JsonSerializer<Enum> ser = new EnumLabelSerializer();
        module.addSerializer(Enum.class, ser);
        module.setDeserializerModifier(new com.fasterxml.jackson.databind.deser.BeanDeserializerModifier() {
            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public JsonDeserializer<?> modifyEnumDeserializer(DeserializationConfig config, JavaType type,
                                                              BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
                if (type.getRawClass() == Enum.class || !type.isEnumType()) {
                    return deserializer;
                }
                return new CodeEnumDeserializer((Class<? extends Enum>) type.getRawClass());
            }
        });
        return module;
    }

    private static String labelOf(Enum<?> e) {
        try {
            Field f = e.getClass().getField("label");
            return String.valueOf(f.get(e));
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            return e.name();
        }
    }

    @SuppressWarnings("rawtypes")
    static class EnumLabelSerializer extends JsonSerializer<Enum> {
        @Override
        public void serialize(Enum value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("code", value.name());
            gen.writeStringField("label", labelOf(value));
            gen.writeEndObject();
        }
    }

    @SuppressWarnings("rawtypes")
    static class CodeEnumDeserializer extends JsonDeserializer<Enum> {
        private final Class<? extends Enum> enumType;

        CodeEnumDeserializer(Class<? extends Enum> enumType) {
            this.enumType = enumType;
        }

        @Override
        public Enum deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.readValueAsTree();
            String code = node.isTextual() ? node.asText()
                    : node.has("code") ? node.get("code").asText() : node.asText();
            if (enumType == null) {
                return null;
            }
            @SuppressWarnings("unchecked")
            Enum<?> v = Enum.valueOf(enumType, code);
            return v;
        }
    }
}
