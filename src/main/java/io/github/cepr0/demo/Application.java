package io.github.cepr0.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Map;

@Slf4j
@RestController
@ControllerAdvice
@SpringBootApplication
public class Application implements WebMvcConfigurer, ResponseBodyAdvice<Object> {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@GetMapping("/hello")
	public ResponseEntity<?> hello() {
		return ResponseEntity.ok(Map.of("message", "hello"));
	}

	@EventListener
	public void onReady(final ApplicationReadyEvent e) {
		Map result = restTemplate().getForObject("http://localhost:8080/hello", Map.class);
		if (result != null) {
			log.info("[i] Request result: '{}'", result.get("message"));
		}
	}

	@Bean
	public RestTemplate restTemplate() {

		ClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
		RestTemplate restTemplate = new RestTemplate(factory);

		var interceptors = restTemplate.getInterceptors();
		if (CollectionUtils.isEmpty(interceptors)) interceptors = new ArrayList<>();

		interceptors.add(new OutgoingInterceptor());
		restTemplate.setInterceptors(interceptors);
		return restTemplate;
	}

	@Override
	public void addInterceptors(final InterceptorRegistry registry) {
		registry.addInterceptor(new IncomingInterceptor());
	}

	@Override
	public boolean supports(final MethodParameter returnType, final Class<? extends HttpMessageConverter<?>> converterType) {
		return true;
	}

	@Override
	public Object beforeBodyWrite(final Object body, final MethodParameter returnType, final MediaType selectedContentType, final Class<? extends HttpMessageConverter<?>> selectedConverterType, final ServerHttpRequest request, final ServerHttpResponse response) {
		log.info("[i] ResponseBodyAdvice: response body {}", body);
		return body;
	}

	class OutgoingInterceptor implements ClientHttpRequestInterceptor {
		@Override
		public ClientHttpResponse intercept(final HttpRequest request, final byte[] bytes, final ClientHttpRequestExecution execution) throws IOException {
			log.info("[i] Outgoing interceptor: requested URL is '{}'", request.getURI());
			ClientHttpResponse response = execution.execute(request, bytes);
			String body = StreamUtils.copyToString(response.getBody(), Charset.defaultCharset());
			log.info("[i] Outgoing interceptor: response body is '{}'", body);
			return response;
		}
	}

	class IncomingInterceptor implements HandlerInterceptor {
		@Override
		public void postHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler, final ModelAndView mw) throws Exception {
			log.info("[i] Incoming interceptor: requested URL is '{}'", request.getRequestURL().toString());
		}
	}
}