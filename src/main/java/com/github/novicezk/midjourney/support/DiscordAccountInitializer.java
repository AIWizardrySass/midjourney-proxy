package com.github.novicezk.midjourney.support;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.text.CharSequenceUtil;
import com.github.novicezk.midjourney.ProxyProperties;
import com.github.novicezk.midjourney.ReturnCode;
import com.github.novicezk.midjourney.domain.DiscordAccount;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import com.github.novicezk.midjourney.loadbalancer.DiscordLoadBalancer;
import com.github.novicezk.midjourney.service.DiscordAccountStoreService;
import com.github.novicezk.midjourney.util.AsyncLockUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscordAccountInitializer implements ApplicationRunner {
    private final DiscordLoadBalancer discordLoadBalancer;
    private final ProxyProperties properties;
    private final DiscordAccountStoreService discordAccountStoreService;

    private final DiscordAccountHelper discordAccountHelper;

    /**
     * 连接初始化，如果redis中存在账号，则从redis中获取，否则从配置文件中获取
     *
     * @param args
     */
    @Override
    public void run(ApplicationArguments args) {
        List<DiscordAccount> allAccount = discordAccountStoreService.getAllAccount();

        if (!CollectionUtils.isEmpty(allAccount)) {
            allAccount.stream().map(discordAccountHelper::createDiscordInstance).forEach(this.discordLoadBalancer::addAccount);
        } else {
            List<ProxyProperties.DiscordAccountConfig> configAccounts = this.properties.getAccounts();

            for (ProxyProperties.DiscordAccountConfig configAccount : configAccounts) {
                DiscordAccount account = new DiscordAccount();
                BeanUtil.copyProperties(configAccount, account);
                account.setId(configAccount.getChannelId());

                boolean res = this.discordLoadBalancer.addAccount(discordAccountHelper.createDiscordInstance(account));
                if (res) {
                    discordAccountStoreService.saveAccount(account);
                }
            }
        }

        Set<String> enableInstanceIds = this.discordLoadBalancer.getAliveInstances().stream()
                .filter(DiscordInstance::isAlive)
                .map(DiscordInstance::getInstanceId)
                .collect(Collectors.toSet());
        log.info("当前可用账号数 [{}] - {}", enableInstanceIds.size(), String.join(", ", enableInstanceIds));
    }

}
