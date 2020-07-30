package com.ultimatesoftware.dataplatform.vaultjca.services;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decorator Cache implementation based on Google's Guava Cache that delegates call to any
 * other {@link VaultService} implementation
 *
 * <p>Keeps an in memory a maximum of 100 entries that expire after 2 Min by default or
 * the user can set its own value using an environment variable VAULT_CACHE_TTL_MIN.</p>
 *
 * <p>Also implements a poor-man circuit breaker on a path by path basis so as not to flood Vault
 * with queries if a non-existent user is queried over and over again.</p>
 */
public class CacheDecoratorVaultService implements VaultService {

  private static final Logger log = LoggerFactory.getLogger(CacheDecoratorVaultService.class);
  private static final String VAULT_CACHE_TTL_MIN = "VAULT_CACHE_TTL_MIN";
  private static final String VAULT_ERROR_CACHE_TTL_MIN = "VAULT_ERROR_CACHE_TTL_MIN";
  protected final Cache<String, Map<String, String>> cache;
  protected final Cache<String, Boolean> errorCache;
  private final VaultService vaultService;

  private final Map<String,String> EMPTY_MAP = new HashMap<>();

  public CacheDecoratorVaultService(VaultService vaultService) {
    Preconditions.checkArgument(!(vaultService instanceof CacheDecoratorVaultService), "Use any other implementation of VaultService as a delegator");
    long cacheTtl = (System.getenv(VAULT_CACHE_TTL_MIN) != null) ? Long.parseLong(System.getenv(VAULT_CACHE_TTL_MIN)) : 2;
    long errorCacheTtl = (System.getenv(VAULT_ERROR_CACHE_TTL_MIN) != null) ? Long.parseLong(System.getenv(VAULT_ERROR_CACHE_TTL_MIN)) : 5;
    this.vaultService = vaultService;
    this.cache = CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(cacheTtl, TimeUnit.MINUTES) // We want to check the actual pwd in Vault from time to time.
        .build();
    this.errorCache = CacheBuilder.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(errorCacheTtl, TimeUnit.MINUTES)
            .build();
    log.info("Cache initialized with TTL {}", cacheTtl);
    log.info("Error cache initialized with TTL {}", errorCacheTtl);
  }

  @Override
  public Map<String, String> getSecret(String path) {
    try {
      log.debug("Hit count {}, miss count {}, size {}", cache.stats().hitCount(), cache.stats().missCount(), cache.size());
      if(errorCache.getIfPresent(path) != null){
        log.info("Path {} had error-ed less than {} minutes ago. Will retry then.");
        return EMPTY_MAP;
      }
      return cache.get(path, () -> vaultService.getSecret(path));
    } catch (ExecutionException e) {
      //TODO: Add a global circuit breaker for Vault if the error is not tied to a specific path
      errorCache.put(path, true); // Won't retry the same user path for a while
      throw new RuntimeException(e);
    }
  }

  @Override
  public void writeSecret(String path, Map<String, String> value) {
    vaultService.writeSecret(path, value);
    cache.put(path, value);
    log.debug("Hit count {}, miss count {}, size {}", cache.stats().hitCount(), cache.stats().missCount(), cache.size());
  }
}
