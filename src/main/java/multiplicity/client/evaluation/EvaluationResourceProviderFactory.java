package multiplicity.client.evaluation;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

/**
 * 
 * @author Alexander Stanik [a.stanik@avm.de]
 * 
 */
public class EvaluationResourceProviderFactory implements RealmResourceProviderFactory {

    private static final Logger LOGGER = Logger.getLogger(EvaluationResourceProviderFactory.class);

    private static final String ID = "evaluation";
    
    private static final AtomicInteger EXECUTIONS_PER_MINUTE = new AtomicInteger();
    
    private ScheduledFuture<?> loadProducerFuture;

    public String getId() {
        return ID;
    }

    public RealmResourceProvider create(KeycloakSession session) {
        return new EvaluationResourceProvider(session);
    }

    public void init(Scope config) {
    }

    public void postInit(KeycloakSessionFactory sessionfactory) {
        final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        final ScheduledLoadProducer producer = new ScheduledLoadProducer();
        loadProducerFuture = service.scheduleAtFixedRate(producer, 0, 1, TimeUnit.MINUTES);
        LOGGER.info("Created ScheduledLoadProducer");
        EvaluationMetrics.getInstance().startReport();
    }

    public void close() {
        loadProducerFuture.cancel(true);
    }

    protected static class ScheduledLoadProducer implements Runnable {
        
        EvaluationMetrics metrics = EvaluationMetrics.getInstance();

        @Override
        public void run() {
            LOGGER.info("Starting " + EXECUTIONS_PER_MINUTE.toString() + " new threads");
            metrics.incrementScheduledExecutions();
        }
        
    }
}
