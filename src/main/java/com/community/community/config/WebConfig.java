package com.community.community.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.lang.reflect.Type;
import java.util.List;

@Component
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // Multipart에서 JSON 파트가 올 때 Content-Type이 없어도 처리 가능하게 설정
        converters.add(new AbstractJackson2HttpMessageConverter(new ObjectMapper()) {
            @Override
            public boolean canRead(Class<?> clazz, MediaType mediaType) {
                return super.canRead(clazz, mediaType) ||
                        (mediaType != null && mediaType.includes(MediaType.APPLICATION_OCTET_STREAM));
            }

            @Override
            public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
                return super.canRead(type, contextClass, mediaType) ||
                        (mediaType != null && mediaType.includes(MediaType.APPLICATION_OCTET_STREAM));
            }
        });
    }
}
