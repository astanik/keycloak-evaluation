package multiplicity.client.evaluation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.timer.ScheduledTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer.Context;

/**
 * 
 * @author Alexander Stanik [a.stanik@avm.de]
 * 
 */
public class ClientProducer implements ScheduledTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientProducer.class);

    private static final EvaluationMetrics metrics = EvaluationMetrics.getInstance();

    private final String clientPrefix = "testClient-";
    
    private final int clientId;
    
    private final String realmName;

    public ClientProducer(String realm, int clientId) {
        super();
        this.clientId = clientId;
        this.realmName = realm;
    }

    @Override
    public void run(KeycloakSession session) {
            try (Context timer = metrics.registerClient()) {
                RealmModel realmModel = session.realms().getRealmByName(this.realmName);
                if (realmModel == null) {
                    return;
                }
                String clientId = clientPrefix + this.clientId;
                // ClientID != PRIMARY key (ID)
                ClientModel client = realmModel.getClientByClientId(clientId);
                if (client == null) {
                    // create client
                    client = realmModel.addClient(clientId);
                    client.setClientId(clientId);
                    LOGGER.info("new Client created: " + clientId);
                }
                // configure client
                client.setEnabled(true);
                client.setPublicClient(false);
                client.setDirectAccessGrantsEnabled(true);
                client.setServiceAccountsEnabled(true);
                client.setBaseUrl("auth/realms/master/evaluation/" + clientId);
                // set redirect URLs
                Set<String> redirectUrls = new HashSet<String>();
                redirectUrls.add("https://example.org/*");
                redirectUrls.add("http://localhost/*");
                redirectUrls.add("http://localhost:8080/*");
                client.setRedirectUris(redirectUrls);
                // set credentials
                client.setClientAuthenticatorType(KeycloakModelUtils.getDefaultClientAuthenticatorType());
                client.setSecret("secret");
                // set realm roles, user role
                addRealmRoles(client);
            }
    }

    private void addRealmRoles(ClientModel client) {
        ProtocolMapperModel userRole = new ProtocolMapperModel();
        userRole.setName("user-role");
        userRole.setProtocol("openid-connect");
        userRole.setProtocolMapper("oidc-hardcoded-role-mapper");
        Map<String, String> config = new HashMap<String, String>();
        config.put("access.token.claim", "");
        config.put("claim.name", "");
        config.put("id.token.claim", "");
        config.put("jsonType.label", "");
        config.put("multivalued", "");
        config.put("role", "user");
        config.put("userinfo.token.claim", "");
        config.put("usermodel.realmRoleMapping.rolePrefix", "");
        userRole.setConfig(config);
        if (client.getProtocolMapperByName("openid-connect", "user-role") == null)
            client.addProtocolMapper(userRole);
    }

}
