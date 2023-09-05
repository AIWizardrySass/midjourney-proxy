package com.github.novicezk.midjourney.loadbalancer.rule;

import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;

import java.util.List;

public interface IRule {

	/**
	 * 根据tag选择服务器
	 * @param instances 服务实例
	 * @param tag tag服务
	 * @return 返回选中的服务器
	 */
	default DiscordInstance choose(List<DiscordInstance> instances, String tag) {
		return choose(instances);
	}

	DiscordInstance choose(List<DiscordInstance> instances);
}
