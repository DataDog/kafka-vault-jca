package com.ultimatesoftware.dataplatform.vaultjca.services;

import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

public class DefaultVaultService implements VaultService {
  private static final Logger log = LoggerFactory.getLogger(DefaultVaultService.class);
  private static Integer KV_VERSION = 2;

  private final SimpleVaultClient vault;

  public DefaultVaultService() {
    try {
      this.vault = new SimpleVaultClient(new VaultConfig().build(), KV_VERSION);
    } catch (VaultException e) {
      log.error("Error creating Vault service", e);
      throw new RuntimeException(e);
    }
  }

  protected DefaultVaultService(String vaultAddr, String token) {
    try {
      this.vault = new SimpleVaultClient(new VaultConfig().address(vaultAddr).token(token).build(), KV_VERSION);
    } catch (VaultException e) {
      log.error("Error creating Vault service", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public Map<String, String> getSecret(String path) {
    try {
      Map<String, String> res = vault.read(path);
      return res;

    } catch (VaultException e) {
      if (e.getHttpStatusCode() == 404) {
        return Collections.EMPTY_MAP;
      }
      throw new RuntimeException(e);
    }
  }

  @Override
  public void writeSecret(String path, Map<String, String> value) {
      vault.write(path, Collections.unmodifiableMap(value));
  }
}
