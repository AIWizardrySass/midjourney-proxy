package com.github.novicezk.midjourney.config;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ReflectUtil;
import com.github.novicezk.midjourney.ProxyProperties;
import com.github.novicezk.midjourney.domain.DiscordAccount;
import com.github.novicezk.midjourney.loadbalancer.rule.IRule;
import com.github.novicezk.midjourney.service.DiscordAccountStoreService;
import com.github.novicezk.midjourney.service.NotifyService;
import com.github.novicezk.midjourney.service.TaskStoreService;
import com.github.novicezk.midjourney.service.TranslateService;
import com.github.novicezk.midjourney.service.store.InMemoryTaskStoreServiceImpl;
import com.github.novicezk.midjourney.service.store.RedisAccountStoreService;
import com.github.novicezk.midjourney.service.store.RedisTaskStoreServiceImpl;
import com.github.novicezk.midjourney.service.translate.BaiduTranslateServiceImpl;
import com.github.novicezk.midjourney.service.translate.GPTTranslateServiceImpl;
import com.github.novicezk.midjourney.service.translate.NoTranslateServiceImpl;
import com.github.novicezk.midjourney.support.DiscordAccountHelper;
import com.github.novicezk.midjourney.support.DiscordHelper;
import com.github.novicezk.midjourney.support.Task;
import com.github.novicezk.midjourney.wss.handle.MessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
public class BeanConfig {
	@Autowired
	private ApplicationContext applicationContext;
	@Autowired
	private ProxyProperties properties;

	@Bean
	TranslateService translateService() {
		return switch (this.properties.getTranslateWay()) {
			case BAIDU -> new BaiduTranslateServiceImpl(this.properties.getBaiduTranslate());
			case GPT -> new GPTTranslateServiceImpl(this.properties);
			default -> new NoTranslateServiceImpl();
		};
	}

	@Bean
	TaskStoreService taskStoreService(RedisConnectionFactory redisConnectionFactory) {
		ProxyProperties.TaskStore.Type type = this.properties.getTaskStore().getType();
		Duration timeout = this.properties.getTaskStore().getTimeout();
		return switch (type) {
			case IN_MEMORY -> new InMemoryTaskStoreServiceImpl(timeout);
			case REDIS -> new RedisTaskStoreServiceImpl(timeout, taskRedisTemplate(redisConnectionFactory));
		};
	}

	@Bean
	DiscordAccountStoreService discordAccountStoreService(RedisConnectionFactory redisConnectionFactory) {
		return new RedisAccountStoreService(accountRedisTemplate(redisConnectionFactory));
	}

	@Bean
	RedisTemplate<String, Task> taskRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
		RedisTemplate<String, Task> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(redisConnectionFactory);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setHashKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(Task.class));
		return redisTemplate;
	}


	@Bean
	RedisTemplate<String, DiscordAccount> accountRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
		RedisTemplate<String, DiscordAccount> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(redisConnectionFactory);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setHashKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(DiscordAccount.class));
		return redisTemplate;
	}

	@Bean
	public RestTemplate restTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		if (CharSequenceUtil.isNotBlank(properties.getProxy().getHost())) {
			Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(properties.getProxy().getHost(), properties.getProxy().getPort()));
			factory.setProxy(proxy);
		}
		restTemplate.setRequestFactory(factory);
		return restTemplate;
	}

	@Bean
	public IRule loadBalancerRule() {
		String ruleClassName = IRule.class.getPackageName() + "." + this.properties.getAccountChooseRule();
		return ReflectUtil.newInstance(ruleClassName);
	}

	@Bean
	List<MessageHandler> messageHandlers() {
		return this.applicationContext.getBeansOfType(MessageHandler.class).values().stream().toList();
	}

	@Bean
	DiscordAccountHelper discordAccountHelper(DiscordHelper discordHelper, TaskStoreService taskStoreService, NotifyService notifyService) throws IOException {
		var resources = this.applicationContext.getResources("classpath:api-params/*.json");
		Map<String, String> paramsMap = new HashMap<>();
		for (var resource : resources) {
			String filename = resource.getFilename();
			String params = IoUtil.readUtf8(resource.getInputStream());
			paramsMap.put(filename.substring(0, filename.length() - 5), params);
		}
		List<MessageHandler> messageHandlers = messageHandlers();
		log.info("messageHandlers size:{}", messageHandlers.size());
		return new DiscordAccountHelper(discordHelper, this.properties, restTemplate(), taskStoreService, notifyService, messageHandlers, paramsMap);
	}


}
