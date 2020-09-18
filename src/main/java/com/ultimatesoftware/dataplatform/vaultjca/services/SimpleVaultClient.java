package com.ultimatesoftware.dataplatform.vaultjca.services;

import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class SimpleVaultClient {

    String token;
    String vaultAddr;
    Map<String,String> headers;
    Integer version;

    ObjectMapper mapper;

    private static final Logger log = LoggerFactory.getLogger(SimpleVaultClient.class);

    private static final Map<String, String> EMPTY_MAP = Collections.unmodifiableMap(new HashMap<>());

    public SimpleVaultClient(VaultConfig conf, int version){
        token = conf.getToken();
        this.version = version;
        vaultAddr = conf.getAddress();
        if (conf.getAddress() == null){
            String addr = System.getenv("VAULT_ADDR");
            if (addr == null) {
                throw new RuntimeException("No Vault address specified and VAULT_ADDR env var is empty.");
            }
            vaultAddr = addr;
        }
        headers = new HashMap<>();
        headers.put("X-Vault-Request", "true");

        if (token != null){
            headers.put("X-Vault-Token", token);
        }
        mapper = new ObjectMapper();
    }

    public Map<String, String> extractMap(InputStream payload){
        // Only supports Read Secret (https://www.vaultproject.io/api/secret/kv/kv-v2.html)
        try {
            Map<String, Object> jsonObject = mapper.readValue(payload, new TypeReference<Map<String,Object>>(){});
            Map<String, Object> data = (Map<String, Object>)(jsonObject.get("data")); //V2
            Map<String, String> result = (Map<String, String>)(data.get("data"));

            return result;

        } catch (Exception ignored) {
            return EMPTY_MAP;
        }
    }

    public Map<String, String> read(String path) throws VaultException {
        String adjustedPath = path.replaceAll("^kv/", "kv/data/");

        try {
            URL url = new URL(vaultAddr + "/v1/" + adjustedPath);
            HttpURLConnection cn = (HttpURLConnection)url.openConnection();
            for(Map.Entry<String,String> kv:headers.entrySet()){
                cn.setRequestProperty(kv.getKey(), kv.getValue());
            }
            InputStream resp = cn.getInputStream();
            int status = cn.getResponseCode();
            if (status == 200) {
                return extractMap(resp);
            }
            if (status == 404){
                log.info("Path {} not found", path);
                return EMPTY_MAP;
            }
        } catch (IOException e) {
            throw new VaultException(e);
        }
        return EMPTY_MAP;
    }

    public void write(String path, Map<String, String> unmodifiableMap) {
        throw new UnsupportedOperationException("Write not supported by this implementation.");
    }
}
