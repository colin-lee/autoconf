package me.config;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * 根据缓存获取内容，支持纯文本或者KV格式的解析。并且使用懒加载模式，只在需要的时候做解析。
 * Created by lirui on 2015/9/23.
 */
public class Config extends MapSource implements IConfig {
    protected Charset UTF8 = Charset.forName("UTF-8");
    protected Charset GBK = Charset.forName("GBK");
	private boolean parsed = false;
    private byte[] content;

    public synchronized byte[] getContent() {
		if (content == null) {
			Map<String, String> m = getAll();
			if (m.size() == 0) {
				content = new byte[0];
			} else {
				StringBuilder sbd = new StringBuilder();
				for (Map.Entry<String, String> i: m.entrySet()) {
					sbd.append(i.getKey()).append('=').append(i.getValue()).append('\n');
				}
				content = sbd.toString().getBytes(UTF8);
			}
		}
        return content;
    }

    public synchronized void setContent(byte[] content) {
        this.content = content;
    }

	public synchronized void setSource(Map<String, String> m) {
		copyOf(m);
		this.content = null;
	}

	@Override
	public String get(String key) {
		if (! parsed) {
			synchronized (this) {
				if (! parsed) {
					Map<String, String> m = Maps.newHashMap();
					for (String i : getLines(UTF8, true)) {
						int pos = i.indexOf('=');
						if (pos != -1 && (pos + 1) < i.length()) {
							String k = i.substring(0, pos).trim();
							String v = i.substring(pos + 1).trim();
							m.put(k, v);
						}
					}
					copyOf(m);
					parsed = true;
				}
			}
		}
		return super.get(key);
	}

	@Override
	public Map<String, String> getAll() {
		if (! parsed) {
			get("_"); // 触发加载配置
		}
		return super.getAll();
	}

	public String getString() {
        return new String(getContent(), UTF8);
    }

    public String getGbkString() {
        return new String(getContent(), GBK);
    }

    public String getString(Charset charset) {
        return new String(getContent(), charset);
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
