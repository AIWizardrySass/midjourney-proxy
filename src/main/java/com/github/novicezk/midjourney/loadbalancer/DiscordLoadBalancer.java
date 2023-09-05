package com.github.novicezk.midjourney.loadbalancer;


import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.text.CharSequenceUtil;
import com.github.novicezk.midjourney.ReturnCode;
import com.github.novicezk.midjourney.domain.DiscordAccount;
import com.github.novicezk.midjourney.loadbalancer.rule.IRule;
import com.github.novicezk.midjourney.service.DiscordAccountStoreService;
import com.github.novicezk.midjourney.support.DiscordAccountHelper;
import com.github.novicezk.midjourney.support.Task;
import com.github.novicezk.midjourney.support.TaskCondition;
import com.github.novicezk.midjourney.util.AsyncLockUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscordLoadBalancer {
	private final IRule rule;

	private final List<DiscordInstance> instances = Collections.synchronizedList(new ArrayList<>());

	private final DiscordAccountHelper discordAccountHelper;

	private final DiscordAccountStoreService discordAccountStoreService;

	/**
	 * 增加账号
	 * @param account
	 */
	public boolean addAccount(DiscordAccount account) {
		try {
			DiscordInstance instance = this.discordAccountHelper.createDiscordInstance(account);
			if (!account.isEnable()) {
				return false;
			}
			instance.startWss();
			AsyncLockUtils.LockObject lock = AsyncLockUtils.waitForLock("wss:" + account.getChannelId(), Duration.ofSeconds(10));
			if (ReturnCode.SUCCESS != lock.getProperty("code", Integer.class, 0)) {
				throw new ValidateException(lock.getProperty("description", String.class));
			}
			instances.add(instance);
			return true;
		} catch (Exception e) {
			log.error("Account({}) init fail, disabled: {}", account.getDisplay(), e.getMessage());
			account.setEnable(false);
			return false;
		}
	}

	public List<DiscordInstance> getAllInstances() {
		return this.instances;
	}

	public List<DiscordInstance> getAliveInstances() {
		return this.instances.stream().filter(DiscordInstance::isAlive).toList();
	}

	public DiscordInstance chooseInstance(String tag) {
		return this.rule.choose(getAliveInstances(), tag);
	}

	public DiscordInstance getDiscordInstance(String instanceId) {
		if (CharSequenceUtil.isBlank(instanceId)) {
			return null;
		}
		return this.instances.stream()
				.filter(instance -> CharSequenceUtil.equals(instanceId, instance.getInstanceId()))
				.findFirst().orElse(null);
	}

	public void removeAccount(String id) {
		DiscordInstance instance = getDiscordInstance(id);
		if (instance == null || instance.isAlive()) {
			return;
		}
		instances.remove(instance);
	}

	public boolean retryConnect(String id) {
		DiscordInstance instance = getDiscordInstance(id);
		if (instance == null || instance.isAlive()) {
			return true;
		}

		try {
			instance.account().setEnable(true);
			instance.startWss();
			AsyncLockUtils.LockObject lock = AsyncLockUtils.waitForLock("wss:" + instance.account().getChannelId(), Duration.ofSeconds(10));
			if (ReturnCode.SUCCESS != lock.getProperty("code", Integer.class, 0)) {
				throw new ValidateException(lock.getProperty("description", String.class));
			}
			updateAccount(instance.account());
			return true;
		} catch (Exception e) {
			log.error("Account({}) init fail, disabled: {}", instance.account().getDisplay(), e.getMessage());
			instance.account().setEnable(false);
			return false;
		}
	}
	public Set<String> getQueueTaskIds() {
		Set<String> taskIds = Collections.synchronizedSet(new HashSet<>());
		for (DiscordInstance instance : getAliveInstances()) {
			taskIds.addAll(instance.getRunningFutures().keySet());
		}
		return taskIds;
	}

	public Stream<Task> findRunningTask(TaskCondition condition) {
		return getAliveInstances().stream().flatMap(instance -> instance.getRunningTasks().stream().filter(condition));
	}

	public Task getRunningTask(String id) {
		for (DiscordInstance instance : getAliveInstances()) {
			Optional<Task> optional = instance.getRunningTasks().stream().filter(t -> id.equals(t.getId())).findFirst();
			if (optional.isPresent()) {
				return optional.get();
			}
		}
		return null;
	}

	public Task getRunningTaskByNonce(String nonce) {
		if (CharSequenceUtil.isBlank(nonce)) {
			return null;
		}
		TaskCondition condition = new TaskCondition().setNonce(nonce);
		for (DiscordInstance instance : getAliveInstances()) {
			Optional<Task> optional = instance.getRunningTasks().stream().filter(condition).findFirst();
			if (optional.isPresent()) {
				return optional.get();
			}
		}
		return null;
	}

	private void updateAccount(DiscordAccount account) {
		this.discordAccountStoreService.saveAccount(account);
	}

}
