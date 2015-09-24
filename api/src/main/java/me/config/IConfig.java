package me.config;

/**
 * 配置文件
 *
 * Created by lirui on 2015/09/22.
 */
public interface IConfig extends IConfigCache, IChangeable {
	/**
	 * 配置文件名
	 *
	 * @return 配置文件名
	 */
	String getName();
}
