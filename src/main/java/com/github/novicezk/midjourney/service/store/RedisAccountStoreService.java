package com.github.novicezk.midjourney.service.store;

import com.github.novicezk.midjourney.domain.DiscordAccount;
import com.github.novicezk.midjourney.service.DiscordAccountStoreService;
import org.springframework.data.redis.core.*;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用于存储discord账号信息
 *
 * @author yubo.ge
 * @date 2023/9/6 00:56
 **/
public class RedisAccountStoreService implements DiscordAccountStoreService {

    private static final String KEY_PREFIX = "mj-discord-account-store::";

    private final RedisTemplate<String, DiscordAccount> redisTemplate;

    public RedisAccountStoreService(RedisTemplate<String, DiscordAccount> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void saveAccount(DiscordAccount accountConfig) {
        this.redisTemplate.opsForValue().set(getRedisKey(accountConfig.getChannelId()), accountConfig);
    }

    @Override
    public void removeAccount(String accountId) {
        this.redisTemplate.delete(getRedisKey(accountId));
    }

    @Override
    public List<DiscordAccount> getAllAccount() {
        /**
         * 查询出所有的节点信息，由于这个数据不大，因此可以使用key扫描
         */
        Set<String> keys = this.redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(KEY_PREFIX + "*").count(1000).build());
            return cursor.stream().map(String::new).collect(Collectors.toSet());
        });

        if (CollectionUtils.isEmpty(keys)) {
            return Collections.emptyList();
        }
        ValueOperations<String, DiscordAccount> operations = this.redisTemplate.opsForValue();
        return keys.stream().map(operations::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private String getRedisKey(String id) {
        return KEY_PREFIX + id;
    }
}
