package com.ultimatesoftware.dataplatform.vaultjca;

import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.ultimatesoftware.dataplatform.vaultjca.services.SimpleVaultClient;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;


import java.util.Map;

public class VaultConnectionTest {
    @Test
    @Ignore
    public void TestConnectionAndSerializationWorks() throws VaultException {
        VaultConfig conf = new VaultConfig()
                .address("XXXX")
                .token("XXXX")
                .build();

        SimpleVaultClient vault = new SimpleVaultClient(conf, 2);
        Map<String, String> res = vault.read("kv/kafka/kafka-test-1/k8s/__internal/admin/secrets");
        assertThat(res.get("user"), is("admin"));
    }

}
