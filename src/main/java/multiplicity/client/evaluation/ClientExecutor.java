package multiplicity.client.evaluation;

import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.scheduled.ScheduledTaskRunner;

/**
 * 
 * @author Alexander Stanik [a.stanik@avm.de]
 * 
 */
public class ClientExecutor extends ScheduledTaskRunner {

    public ClientExecutor(KeycloakSessionFactory sessionFactory, String realm, int clientId) {
        super(sessionFactory, new ClientProducer(realm, clientId));
    }

}
