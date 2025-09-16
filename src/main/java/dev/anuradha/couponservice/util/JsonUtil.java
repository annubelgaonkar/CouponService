package dev.anuradha.couponservice.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

public class JsonUtil {
    public static final ObjectMapper MAPPER = new ObjectMapper();
}
