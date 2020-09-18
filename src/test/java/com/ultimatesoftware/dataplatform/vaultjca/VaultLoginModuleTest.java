package com.ultimatesoftware.dataplatform.vaultjca;

import com.ultimatesoftware.dataplatform.vaultjca.services.VaultService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatchers;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static com.ultimatesoftware.dataplatform.vaultjca.VaultAuthenticationLoginCallbackHandler.*;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class VaultLoginModuleTest {

  protected static final String VAULT_KAFKA_ADMIN_PATH = "secrets/kafka/admin";
  protected static final String VAULT_KAFKA_TEMPLATED_ADMIN_PATH = "kafka/" + USERNAME_TEMPLATE_FRAGMENT + "/secret";
  protected static final String VAULT_KAFKA_USERS_PATH = "secrets/kafka/users";
  protected static final String VAULT_KAFKA_TEMPLATED_USERS_PATH = "kafka/" + USERNAME_TEMPLATE_FRAGMENT + "/secret";
  private static final String ADMIN = "admin";
  private static final String ADMINPWD = "adminpwd";
  private VaultService vaultService = mock(VaultService.class);
  private final VaultLoginModule vaultLoginModule = new VaultLoginModule(vaultService);
  private Subject subject;
  private CallbackHandler callbackHandler = mock(CallbackHandler.class);
  private Map<String, String> options;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void init() {
    subject = new Subject(false, new HashSet<>(), new HashSet<>(), new HashSet<>());
    options = new HashMap<>();
    options.put(VaultLoginModule.ADMIN_PATH, VAULT_KAFKA_ADMIN_PATH);
    options.put(VaultAuthenticationLoginCallbackHandler.USERS_PATH, VAULT_KAFKA_USERS_PATH);
  }

  @Test
  public void shouldInitializeLoginModuleForAdmin() {
    trainMockForSuccess();

    vaultLoginModule.initialize(subject, callbackHandler, Collections.EMPTY_MAP, options);

    assertThat(subject.getPublicCredentials(), contains(ADMIN));
    assertThat(subject.getPrivateCredentials(), contains(ADMINPWD));
  }

  @Test
  public void shouldThrowExceptionWhenCredentialsNotPresentInVault() {
    when(vaultService.getSecret(ArgumentMatchers.eq(VAULT_KAFKA_ADMIN_PATH))).thenReturn(new HashMap<>());
    thrown.expect(RuntimeException.class);
    thrown.expectMessage(containsString("Secret not found for path " + VAULT_KAFKA_ADMIN_PATH));
    vaultLoginModule.initialize(subject, callbackHandler, Collections.EMPTY_MAP, options);
  }

  @Test
  public void shouldInitializeLoginModuleForClient() {
    options = new HashMap<>();
    String alice = "alice";
    String alicepwd = "alicepwd";
    options.put(USER_MAP_ENTRY_KEY, alice);
    options.put(PASSWORD_MAP_ENTRY_KEY, alicepwd);

    vaultLoginModule.initialize(subject, callbackHandler, Collections.EMPTY_MAP, options);
    assertThat(subject.getPublicCredentials(), contains(alice));
    assertThat(subject.getPrivateCredentials(), contains(alicepwd));
  }

  @Test
  public void shouldThrownExceptionOnInvalidJaasEntry() {
    thrown.expect(RuntimeException.class);
    thrown.expectMessage(containsString("Not a valid jaas file"));
    vaultLoginModule.initialize(subject, callbackHandler, Collections.EMPTY_MAP, Collections.EMPTY_MAP);

  }

  private void trainMockForSuccess() {
    Map<String, String> adminCredentials = new HashMap<>();
    adminCredentials.put(USER_MAP_ENTRY_KEY, ADMIN);
    adminCredentials.put(PASSWORD_MAP_ENTRY_KEY, ADMINPWD);
    when(vaultService.getSecret(ArgumentMatchers.eq(VAULT_KAFKA_ADMIN_PATH))).thenReturn(adminCredentials);
  }

}