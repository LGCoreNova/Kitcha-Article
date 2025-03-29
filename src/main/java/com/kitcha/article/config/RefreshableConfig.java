package com.kitcha.article.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Component
@RefreshScope //cloud config server에서 설정값을 변경할 때, 해당 bean을 refresh하여 새로운 설정값을 주입받도록 함
public class RefreshableConfig {
    
    @Value("${some.property:defaultValue}")
    private String someProperty;
    
    // getters and setters
    public String getSomeProperty() {
        return someProperty;
    }
}
