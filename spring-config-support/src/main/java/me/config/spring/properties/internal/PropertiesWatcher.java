package me.config.spring.properties.internal;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class PropertiesWatcher implements Runnable {
  final ExecutorService service;
  private final Resource[] locations;
  private final EventPublisher eventPublisher;
  private Logger log = LoggerFactory.getLogger(PropertiesWatcher.class);
  private WatchService watchService;

  public PropertiesWatcher(final Resource[] locations, final EventPublisher eventPublisher) throws IOException {
    this.locations = locations;
    this.eventPublisher = eventPublisher;
    this.watchService = FileSystems.getDefault().newWatchService();
    this.service = Executors.newCachedThreadPool();
  }

  @Override
  public void run() {
    final Multimap<Path, Resource> pathsAndResources = findAvailableResourcePaths();
    for (final Path pathToWatch : pathsAndResources.keySet()) {
      final Collection<Resource> availableResources = pathsAndResources.get(pathToWatch);
      log.info("Starting ResourceWatcher on file {}", availableResources);
      this.service.submit(new ResourceWatcher(pathToWatch, availableResources));
    }
  }

  public void stop() {
    try {
      log.info("Closing File Watching Service");
      this.watchService.close();

      log.info("Shutting down Thread Service");
      this.service.shutdownNow();
    } catch (final IOException e) {
      log.error("Unable to stop file watcher", e);
    }
  }

  private Multimap<Path, Resource> findAvailableResourcePaths() {
    final Multimap<Path, Resource> map = HashMultimap.create();
    for (final Resource resource : this.locations) {
      map.put(getResourceParentPath(resource), resource);
    }
    return map;
  }

  private Path getResourceParentPath(final Resource resource) {
    try {
      return Paths.get(resource.getFile().getParentFile().toURI());
    } catch (final IOException e) {
      log.error("Unable to get resource path", e);
    }
    return null;
  }

  private void publishResourceChangedEvent(final Resource resource) {
    this.eventPublisher.onResourceChanged(resource);
  }

  private WatchService getWatchService() {
    return this.watchService;
  }

  public interface EventPublisher {
    void onResourceChanged(Resource resource);
  }


  private class ResourceWatcher implements Runnable {

    private final Path path;
    private final Collection<Resource> resources;

    public ResourceWatcher(final Path path, final Collection<Resource> resources) {
      this.path = path;
      this.resources = resources;
    }

    @Override
    public void run() {
      try {
        log.info("START watching for modification events for path {}", this.path);
        while (!Thread.currentThread().isInterrupted()) {
          final WatchKey pathBeingWatched = this.path.register(getWatchService(), ENTRY_MODIFY);

          WatchKey watchKey = null;
          try {
            watchKey = getWatchService().take();
          } catch (final ClosedWatchServiceException e) {
            log.info("END watching for path {}", this.path);
            Thread.currentThread().interrupt();
          } catch (InterruptedException e) {
            log.info("END watching for path {}", this.path);
            Thread.currentThread().interrupt();
          }

          if (watchKey != null) {
            for (final WatchEvent<?> event : pathBeingWatched.pollEvents()) {
              log.info("File modification Event Triggered");
              final Path target = path(event.context());
              if (isValidTargetFile(target)) {
                final Path watchedPath = path(watchKey.watchable());
                final Kind<?> eventKind = event.kind();

                logNewEvent(watchedPath, eventKind, target);
                publishResourceChangedEvent(getResource(target));
              }
            }
            if (!watchKey.reset()) {
              log.info("END watching for path {}", this.path);
              Thread.currentThread().interrupt();
              return;
            }
          }
        }
      } catch (final Exception e) {
        log.error("Exception thrown when watching resources, path {}\nException:", this.path.toString(), e.getMessage());
        stop();
      }
    }

    private void logNewEvent(final Path watchedPath, final Kind<?> eventKind, final Path target) {
      log.info("Watched Resource changed, modified file [{}]", target.getFileName().toString());
      log.info("  Event Kind [{}]", eventKind);
      log.info("      Target [{}]", target);
      log.info("Watched Path [{}]", watchedPath);
    }

    private Path path(final Object object) {
      return (Path) object;
    }

    private boolean isValidTargetFile(final Path target) {
      for (final Resource resource : this.resources) {
        if (pathMatchesResource(target, resource)) {
          return true;
        }
      }
      return false;
    }

    public Resource getResource(final Path target) {
      for (final Resource resource : this.resources) {
        if (pathMatchesResource(target, resource)) {
          return resource;
        }
      }
      return null;
    }

    private boolean pathMatchesResource(final Path target, final Resource resource) {
      return target.getFileName().toString().equals(resource.getFilename());
    }
  }

}
