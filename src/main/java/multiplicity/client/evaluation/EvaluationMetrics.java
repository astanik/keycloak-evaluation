package multiplicity.client.evaluation;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

/**
 * 
 * @author Alexander Stanik [a.stanik@avm.de]
 * 
 */
public class EvaluationMetrics {

    private static EvaluationMetrics instance;

    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluationMetrics.class);

    private static final MetricRegistry metrics = new MetricRegistry();

    private final Counter scheduledExecutions;
    
    private final Timer clientRegistration;


    private EvaluationMetrics() {
        super();
        scheduledExecutions = metrics.counter("scheduled-executions");
        clientRegistration = metrics.timer("client-registration");
    }

    public static EvaluationMetrics getInstance() {
        if (instance == null) {
            synchronized (EvaluationMetrics.class) {
                if (instance == null) {
                    instance = new EvaluationMetrics();
                }
            }
        }
        return instance;
    }

    public void startReport() {
        ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build();
        reporter.start(1, TimeUnit.MINUTES);

        final Slf4jReporter slfReporter = Slf4jReporter.forRegistry(metrics)
            .outputTo(LOGGER)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build();
        slfReporter.start(1, TimeUnit.MINUTES);
        
        CsvReporter csvReporter = CsvReporter.forRegistry(metrics)
            .formatFor(Locale.US)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build(new File(System.getProperty("jboss.server.log.dir")));
        csvReporter.start(1, TimeUnit.SECONDS);
    }

    public void incrementScheduledExecutions() {
        this.scheduledExecutions.inc();
    }

    public Context registerClient() {
        return this.clientRegistration.time();
    }
}
