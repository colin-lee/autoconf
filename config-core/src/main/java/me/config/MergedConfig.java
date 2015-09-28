package me.config;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 合并多个配置文件的内容为一个，同名配置，排在前面的优先。<br/>
 * 1. 仅支持kv类型的合并 <br/>
 * 2. 仅支持UTF8编码 <br/>
 * Created by lirui on 15/9/24.
 */
public class MergedConfig extends BaseConfig {
	private final String name;
	private final List<IChangeableConfig> configs;
	public MergedConfig(List<IChangeableConfig> configs) {
		super();
		this.name = Joiner.on(',').join(Collections2.transform(configs, new Function<IChangeableConfig, String>() {
			@Override
			public String apply(IChangeableConfig input) {
				return input.getName();
			}
		}));

		// 注册单个配置文件的更新回调功能
		IChangeListener listener = new IChangeListener() {
			@Override
			public void dataChanged(IConfig config) {
				merge();
			}
		};
		for(IChangeableConfig c: configs) {
			c.addListener(listener, false);
		}

		// 同名配置，排在前面的优先
		this.configs = Lists.newArrayList(configs);
		Collections.reverse(this.configs);

		// 首次merge配置
		merge();
	}

	private void merge() {
		Map<String, String> m = Maps.newHashMap();
		for(IChangeableConfig c: this.configs) {
			m.putAll(c.getAll());
		}
		StringBuilder sbd = new StringBuilder();
		for(Map.Entry<String, String> i: m.entrySet()) {
			sbd.append(i.getKey()).append('=').append(i.getValue()).append('\n');
		}
		setContent(sbd.toString().getBytes(UTF8));
	}

	public String getName() {
		return name;
	}
}
