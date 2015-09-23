package me.config;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * 配置文件
 *
 * Created by lirui on 2015/09/22.
 */
public interface IConfig {
	/**
	 * 配置文件名
	 *
	 * @return 配置文件名
	 */
	String getName();

	int getInt(String key);

	/**
	 * 获取key对应的int数值
	 *
	 * @param key        查找的key
	 * @param defaultVal 找不到返回的默认值
	 * @return 找到返回对应的int数值否则返回默认值
	 */
	int getInt(String key, int defaultVal);

	long getLong(String key);

	/**
	 * 获取key对应的long数值
	 *
	 * @param key        查找的key
	 * @param defaultVal 找不到返回的默认值
	 * @return 找到返回对应的long数值否则返回默认值
	 */
	long getLong(String key, long defaultVal);

	boolean getBool(String key);

	/**
	 * 获取key对应的boolean值
	 *
	 * @param key        查找的key
	 * @param defaultVal 找不到返回的默认值
	 * @return 找到返回对应的boolean值否则返回默认值
	 */
	boolean getBool(String key, boolean defaultVal);

	double getDouble(String key);

	/**
	 * 获取key对应的double数值
	 *
	 * @param key        查找的key
	 * @param defaultVal 找不到返回的默认值
	 * @return 找到返回对应的Double值否则返回默认值
	 */
	double getDouble(String key, double defaultVal);

	/**
	 * 获取key对应的String值
	 *
	 * @param key        查找的key
	 * @return 找到返回对应的String值
	 */
	String get(String key);

	/**
	 * 获取key对应的String内容，如果找不到返回提供的默认值
	 *
	 * @param key 查找的key
	 * @param defaultVal 提供的默认值
	 * @return 找到返回对应的配置，否则返回默认值
	 */
	String get(String key, String defaultVal);

	/**
	 * 配置中是否有对应的key
	 *
	 * @param key 查找的key
	 * @return 有的话返回true
	 */
	boolean has(String key);

	Map<String, String> getAll();

	/**
	 * 获取配置的字节流信息
	 *
	 * @return 字节数组
	 */
	byte[] getContent();

	/**
	 * 把配置文件二进制内容用UTF8编码进行解码，并返回对应的字符串
	 *
	 * @return 字符串
	 */
	String getString();

	/**
	 * 把配置文件二进制内容用指定的编码进行解码，并返回对应的字符串
	 *
	 * @param charset 指定编码
	 * @return 字符串
	 */
	String getString(Charset charset);

	/**
	 * 获取UTF8编码的文件行，默认会去掉 '#' 和 '/' 开头的注释行，并且把内容做trim
	 *
	 * @return 文件行
	 */
	List<String> getLines();

	/**
	 * 获取指定编码的文件行，默认会去掉 '#' 和 '/' 开头的注释行，并且把内容做trim
	 *
	 * @param charset 指定编码
	 * @return 文件行
	 */
	List<String> getLines(Charset charset);

	/**
	 * 获取指定编码的文件行，根据是否去掉注释行，并且把内容做trim
	 *
	 * @param charset 指定编码
	 * @param removeComment 是否去掉 '#' 和 '/' 开头的注释行
	 * @return 文件行
	 */
	List<String> getLines(Charset charset, boolean removeComment);

	/**
	 * 注册更新回调方法，并且会马上调用1次回调函数，避免外层还需要手动调用1次
	 *
	 * @param listener 更新回调方法
	 */
	void addListener(IConfigChangeListener listener);

	/**
	 * 注册更新回调方法
	 *
	 * @param listener 更新回调方法
	 * @param loadAfterRegister 注册后立即调用回调函数
	 */
	void addListener(IConfigChangeListener listener, boolean loadAfterRegister);

	/**
	 * 去掉listener
	 *
	 * @param listener 更新回调函数
	 */
	public void removeListener(IConfigChangeListener listener);
}
