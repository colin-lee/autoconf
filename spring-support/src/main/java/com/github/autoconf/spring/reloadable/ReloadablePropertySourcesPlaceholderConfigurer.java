package com.github.autoconf.spring.reloadable;

import com.github.autoconf.ConfigFactory;
import com.github.autoconf.api.*;
import com.github.autoconf.impl.LocalConfig;
import com.github.autoconf.spring.properties.bean.DynamicProperty;
import com.github.autoconf.spring.properties.bean.PropertyModifiedEvent;
import com.github.autoconf.spring.properties.event.PropertyChangedEventNotifier;
import com.github.autoconf.watcher.FileUpdateWatcher;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionVisitor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.*;
import org.springframework.core.io.Resource;
import org.springframework.util.StringValueResolver;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/**
 * Specialisation of {@link PropertySourcesPlaceholderConfigurer} that can react to changes in the resources specified. The watching process does not start by
 * default, initiation is triggered by calling <code>ReadablePropertySourcesPlaceholderConfigurer.startWatching()</code>
 *
 * @author James Morgan
 */
public class ReloadablePropertySourcesPlaceholderConfigurer extends PropertySourcesPlaceholderConfigurer {
  private static final Logger LOG =
    LoggerFactory.getLogger(ReloadablePropertySourcesPlaceholderConfigurer.class);
  private PropertyChangedEventNotifier eventNotifier;
  private Multimap<String, DynamicProperty> placeHolders = HashMultimap.create();
  private PropertySourcesPropertyResolver propertyResolver;
  private IConfigFactory configFactory;
  /**
   * 在cms系统中配置的名称，可以是逗号分隔的多个名字
   */
  private String configName;

  public ReloadablePropertySourcesPlaceholderConfigurer() {
  }

  void setEventNotifier(PropertyChangedEventNotifier eventNotifier) {
    this.eventNotifier = eventNotifier;
  }

  public void setConfigFactory(IConfigFactory configFactory) {
    this.configFactory = configFactory;
  }

  public void setConfigName(String configName) {
    this.configName = configName;
  }

  @Override
  protected void loadProperties(final Properties props) throws IOException {
    super.loadProperties(props);
    if (!Strings.isNullOrEmpty(configName)) {
      if (configFactory == null) {
        configFactory = ConfigFactory.getInstance();
      }
      IChangeableConfig config = configFactory.getConfig(configName, new IChangeListener() {
        @Override
        public void changed(IConfig config) {
          reloadCmsConfig(config);
        }
      }, false);
      props.putAll(config.getAll());
    }
  }

  private void reloadCmsConfig(IConfig config) {
    final Properties p = new Properties();
    p.putAll(config.getAll());
    updateProperty(p);
  }

  private void updateProperty(Properties reloadedProperties) {
    PropertySources appliedSources = getAppliedPropertySources();
    MutablePropertySources oldSources = new MutablePropertySources();
    PropertiesPropertySource localPropertySource = null;
    for (PropertySource i : appliedSources) {
      if (i.getName().equals(LOCAL_PROPERTIES_PROPERTY_SOURCE_NAME)) {
        localPropertySource = (PropertiesPropertySource) i;
        Properties old = new Properties();
        old.putAll(localPropertySource.getSource());
        oldSources.addLast(new PropertiesPropertySource(LOCAL_PROPERTIES_PROPERTY_SOURCE_NAME, old));
        for (String name : reloadedProperties.stringPropertyNames()) {
          localPropertySource.getSource().put(name, reloadedProperties.get(name));
        }
      } else {
        oldSources.addLast(i);
      }
    }

    if (localPropertySource != null) {
      PropertySourcesPropertyResolver oldResolver = buildPropertyResolver(oldSources);
      PropertySourcesPropertyResolver newResolver = buildPropertyResolver(appliedSources);

      for (final String property : localPropertySource.getPropertyNames()) {
        final String oldValue = oldResolver.getProperty(property);
        final String newValue = newResolver.getProperty(property);

        boolean propertyExistsAndNotNull =
          localPropertySource.containsProperty(property) && null != newValue;
        boolean propertyChange = null == oldValue || !oldValue.equals(newValue);
        if (propertyExistsAndNotNull && propertyChange) {
          // Post change event to notify any potential listeners
          this.eventNotifier.post(new PropertyModifiedEvent(property, oldValue, newValue));
        }
      }
    }
  }

  public void startWatching() {
    if (null == this.eventNotifier) {
      throw new BeanInitializationException("Event bus not setup, you should not be calling this method...!");
    }
    Resource[] resources = (Resource[]) getFieldValue("locations");
    if (resources == null) {
      return;
    }
    try {
      // Here we actually create and set a FileWatcher to monitor the given locations
      for (Resource i : resources) {
        final LocalConfig c = new LocalConfig(i.getFilename(), i.getFile().toPath());
        FileUpdateWatcher.getInstance().watch(c.getPath(), new IFileListener() {
          @Override
          public void changed(Path path, byte[] content) {
            LOG.info("{} changed", path);
            c.copyOf(content);
            reloadCmsConfig(c);
          }
        });
      }
    } catch (Exception e) {
      LOG.error("Unable to start properties file watcher", e);
    }
  }

  private Object getFieldValue(String name) {
    Class<?> clz = super.getClass();
    while (clz != Object.class) {
      try {
        Field f = clz.getDeclaredField(name);
        if (!f.isAccessible()) {
          f.setAccessible(true);
        }
        return f.get(this);
      } catch (NoSuchFieldException ignored) {
        clz = clz.getSuperclass();
      } catch (IllegalAccessException e) {
        LOG.error("cannot getFieldValue('{}')", name, e);
      }
    }
    return null;
  }

  public PropertySourcesPropertyResolver getPropertyResolver() {
    if (propertyResolver == null) {
      propertyResolver = buildPropertyResolver(getAppliedPropertySources());
    }
    return propertyResolver;
  }

  public Object resolvePlaceHolders(final String text) {
    return getPropertyResolver().resolvePlaceholders(text);
  }

  public Object resolveProperty(final Object property) {
    return getPropertyResolver().getProperty(property.toString());
  }

  private PropertySourcesPropertyResolver buildPropertyResolver(PropertySources sources) {
    PropertySourcesPropertyResolver propertyResolver = new PropertySourcesPropertyResolver(sources);
    propertyResolver.setPlaceholderPrefix(this.placeholderPrefix);
    propertyResolver.setPlaceholderSuffix(this.placeholderSuffix);
    propertyResolver.setValueSeparator(this.valueSeparator);
    return propertyResolver;
  }

  protected void doProcessProperties(ConfigurableListableBeanFactory beanFactoryToProcess, StringValueResolver valueResolver) {
    BeanDefinitionVisitor visitor = new BeanDefinitionVisitor(valueResolver);
    String[] beanNames = beanFactoryToProcess.getBeanDefinitionNames();
    String selfName = (String) getFieldValue("beanName");
    BeanFactory selfFactory = (BeanFactory) getFieldValue("beanFactory");
    for (String name : beanNames) {
      // Check that we're not parsing our own bean definition,
      // to avoid failing on unresolvable placeholders in properties file locations.
      if (!(name.equals(selfName) && beanFactoryToProcess.equals(selfFactory))) {
        BeanDefinition bd = beanFactoryToProcess.getBeanDefinition(name);
        try {
          findDynamicProperties(name, bd);
          visitor.visitBeanDefinition(bd);
        } catch (Exception ex) {
          throw new BeanDefinitionStoreException(bd.getResourceDescription(), name, ex.getMessage(), ex);
        }
      }
    }

    // New in Spring 2.5: resolve placeholders in alias target names and aliases as well.
    beanFactoryToProcess.resolveAliases(valueResolver);

    // New in Spring 3.0: resolve placeholders in embedded values such as annotation attributes.
    beanFactoryToProcess.addEmbeddedValueResolver(valueResolver);
  }

  private void findDynamicProperties(String beanName, BeanDefinition bd) {
    MutablePropertyValues pvs = bd.getPropertyValues();
    PropertyValue[] pvArray = pvs.getPropertyValues();
    for (PropertyValue pv : pvArray) {
      Object value = pv.getValue();
      String rawValue = null;
      if (value instanceof TypedStringValue) {
        rawValue = ((TypedStringValue) value).getValue();
      } else if (value instanceof String) {
        rawValue = (String) value;
      }
      if (rawValue != null) {
        List<String> holders = findHolders(rawValue);
        if (holders != null && holders.size() > 0) {
          DynamicProperty p = new DynamicProperty(beanName, pv.getName(), rawValue);
          p.setPlaceholders(holders);
          placeHolders.put(beanName, p);
        }
      }
    }
  }

  private List<String> findHolders(String rawValue) {
    if (Strings.isNullOrEmpty(rawValue)) {
      return null;
    }
    int start = rawValue.indexOf(this.placeholderPrefix);
    if (start == -1) {
      return null;
    }
    List<String> holders = Lists.newArrayList();
    while (start != -1) {
      start += this.placeholderPrefix.length();
      int stop = rawValue.indexOf(this.placeholderSuffix, start);
      if (stop == -1)
        break;
      holders.add(rawValue.substring(start, stop));
      start = rawValue.indexOf(this.placeholderPrefix, stop + this.placeholderSuffix.length());
    }
    return holders;
  }

  public Multimap<String, DynamicProperty> getPlaceHolders() {
    return placeHolders;
  }
}
