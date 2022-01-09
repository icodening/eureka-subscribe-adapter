package cn.icodening.eureka.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.discovery.converters.wrappers.DecoderWrapper;
import com.netflix.discovery.converters.wrappers.EncoderWrapper;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Eureka编解码器
 *
 * @author icodening
 * @date 2022.01.09
 */
public class EurekaCodec implements EncoderWrapper,
        DecoderWrapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public <T> String encode(T object) throws IOException {
        return objectMapper.writeValueAsString(object);
    }

    @Override
    public <T> void encode(T object, OutputStream outputStream) throws IOException {
        objectMapper.writeValue(outputStream, object);
    }

    @Override
    public String codecName() {
        return "subscribe-client-codec";
    }

    @Override
    public boolean support(MediaType mediaType) {
        return MediaType.APPLICATION_JSON_TYPE.equals(mediaType);
    }

    @Override
    public <T> T decode(String textValue, Class<T> type) throws IOException {
        return objectMapper.readValue(textValue, type);
    }

    @Override
    public <T> T decode(InputStream inputStream, Class<T> type) throws IOException {
        return objectMapper.readValue(inputStream, type);
    }
}
