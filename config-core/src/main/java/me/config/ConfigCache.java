package me.config;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 根据缓存获取内容
 * Created by lirui on 15/9/23.
 */
public class ConfigCache implements IConfigCache {
    protected Charset UTF8 = Charset.forName("UTF-8");
    protected Charset GBK = Charset.forName("GBK");
    private boolean parsed;
    private byte[] content;
    private Map<String, String> items = Collections.emptyMap();

    public int getInt(String key) {
        return getInt(key, 0);
    }

    public int getInt(String key, int defaultVal) {
        String val = get(key);
        if (!Strings.isNullOrEmpty(val)) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultVal;
    }

    public long getLong(String key) {
        return getLong(key, 0);
    }

    public long getLong(String key, long defaultVal) {
        String val = get(key);
        if (!Strings.isNullOrEmpty(val)) {
            try {
                return Long.parseLong(val);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultVal;
    }

    public boolean getBool(String key) {
        return getBool(key, false);
    }

    public boolean getBool(String key, boolean defaultVal) {
        String val = get(key);
        if (!Strings.isNullOrEmpty(val)) {
            try {
                return Boolean.parseBoolean(val);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultVal;
    }

    public double getDouble(String key) {
        return getDouble(key, 0);
    }

    public double getDouble(String key, double defaultVal) {
        String val = get(key);
        if (!Strings.isNullOrEmpty(val)) {
            try {
                return Double.parseDouble(val);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultVal;
    }

    /**
     * 只有真正按照kv格式查找的时候，才进行解析对应kv内容。避免解析非KV格式的配置
     *
     * @param key 查找的key
     * @return
     */
    public String get(String key) {
        if (!parsed) {
            synchronized (this) {
                if (!parsed) {
                    Map<String, String> m = Maps.newHashMap();
                    for (String i : getLines()) {
                        int pos = i.indexOf('=');
                        if (pos != -1 && (pos + 1) < i.length()) {
                            m.put(i.substring(0, pos).trim(), i.substring(pos + 1).trim());
                        }
                    }
                    items.clear();
                    items = m;
                    parsed = true;
                }
            }
        }
        return items.get(key);
    }

    public String get(String key, String defaultVal) {
        String val = get(key);
        return val == null ? defaultVal : val;
    }

    public boolean has(String key) {
        return items.containsKey(key);
    }

    public Map<String, String> getAll() {
        if (!parsed) {
            get("_"); // 触发加载配置
        }
        return Collections.unmodifiableMap(items);
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public String getString() {
        return new String(content, UTF8);
    }

    public String getGbkString() {
        return new String(content, GBK);
    }

    public String getString(Charset charset) {
        return new String(content, charset);
    }

    public List<String> getLines() {
        return getLines(UTF8, true);
    }

    public List<String> getLines(Charset charset) {
        return getLines(charset, true);
    }

    public List<String> getLines(Charset charset, boolean removeComment) {
        List<String> raw = Splitter.on('\n').trimResults().omitEmptyStrings().splitToList(getString(charset));
        if (!removeComment) return raw;

        List<String> clean = Lists.newArrayList();
        for (String i : raw) {
            if (i.charAt(0) == '#' || i.startsWith("//")) continue;
            clean.add(i);
        }
        return clean;
    }
}
