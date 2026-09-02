package com.smartpm.common.config;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;

/**
 * WebClient 配置 — IP 直连绕过 DNS 污染。
 *
 * 网络 DNS 将 api.deepseek.com 解析到虚假 IP (198.18.0.x)，
 * 故在 application.yml 中改用已知真实 IP。TLS 验证全部绕过
 * （证书记录的是域名，不匹配 IP）。
 */
@Configuration
public class AIWebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() throws SSLException {
        var sslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        HttpClient httpClient = HttpClient.create()
                .secure(spec -> spec.sslContext(sslContext));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
