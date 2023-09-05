package com.github.novicezk.midjourney.loadbalancer.rule;

import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import org.apache.logging.log4j.util.Strings;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 根据标签匹配到对应的服务器,
 * 如果存在一个标签多个服务器，则将查询出来的多个服务器使用最少等待空闲规则.
 * 如果一个服务也不存在，则全部服务器使用最少等待空闲.
 *
 * @author yubo.ge
 * @date 2023/9/5 19:18
 **/
public class TagMatchRule extends BestWaitIdleRule {

    @Override
    public DiscordInstance choose(List<DiscordInstance> instances, String tag) {
        if (Strings.isEmpty(tag)) {
            return super.choose(instances);
        }

        List<DiscordInstance> insts = instances.stream().filter(DiscordInstance::isAlive)
                .filter(instance -> tag.equalsIgnoreCase(instance.account().getTag()))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(insts)) {
            return super.choose(instances);
        }
        return super.choose(insts);
    }
}
