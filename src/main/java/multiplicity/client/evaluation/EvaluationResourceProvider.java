package multiplicity.client.evaluation;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

/**
 * 
 * @author Alexander Stanik [a.stanik@avm.de]
 * 
 */
public class EvaluationResourceProvider implements RealmResourceProvider {

    private final KeycloakSession session;

    public EvaluationResourceProvider(KeycloakSession session) {
        super();
        this.session = session;
    }

    public Object getResource() {
        return new EvaluationResource(this.session, this.session.getContext());
    }

    public void close() {
    }

}
