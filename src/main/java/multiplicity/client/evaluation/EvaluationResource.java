package multiplicity.client.evaluation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.RealmManager;

import com.codahale.metrics.Timer.Context;

/**
 * 
 * @author Alexander Stanik [a.stanik@avm.de]
 * 
 */
public class EvaluationResource {

    private static final Logger LOGGER = Logger.getLogger(EvaluationResource.class);

    private final KeycloakSession session;

    private final KeycloakContext context;
    
    private static final ExecutorService executor = Executors.newFixedThreadPool(30);

    public EvaluationResource(KeycloakSession session, KeycloakContext context) {
        this.session = session;
        this.context = context;
    }

    public String buildPage(String content) {
        final String html = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "   <body>\n" +
            content +
            "   </body>\n" +
            "</html>\n";
        return html;
    }

    @Path("/")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String get() {
        String url = this.context.getUri().getAbsolutePath().toString();
        if (!url.endsWith("/"))
            url = url + "/";
        LOGGER.info("get " + url);
        List<ClientModel> clients = this.session.clientStorageManager().getClients(this.context.getRealm());
        return this.buildPage("Hallo Welt! (Clients = " + clients.size() + ")"
            + "<p>" + url + "createSetup?realm=testRealm&clients=10000</p>");
    }

    @Path("/createSetup")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response createSetup(@QueryParam("realm") String realm, @QueryParam("clients") int clients) {
        if (realm == null || realm.isEmpty())
            return Response.status(Status.BAD_REQUEST).build();
        if (clients <= 0)
            return Response.status(Status.BAD_REQUEST).build();

        RealmModel realmModel = this.session.realms().getRealmByName(realm);
        if (realmModel == null) {
            // create realm, use RealmManager
            RealmManager manager = new RealmManager(session);
            manager.setContextPath(context.getContextPath());
            realmModel = manager.createRealm(realm, realm);
            realmModel.setDisplayName(realm);
            realmModel.setDisplayNameHtml(realm);
            realmModel.setEnabled(true);
            realmModel.setResetPasswordAllowed(true);
            realmModel.addRequiredCredential(CredentialRepresentation.PASSWORD);
            realmModel.setSsoSessionIdleTimeout(1800);
            realmModel.setAccessTokenLifespan(60);
            realmModel.setAccessTokenLifespanForImplicitFlow(Constants.DEFAULT_ACCESS_TOKEN_LIFESPAN_FOR_IMPLICIT_FLOW_TIMEOUT);
            realmModel.setSsoSessionMaxLifespan(36000);
            realmModel.setOfflineSessionIdleTimeout(Constants.DEFAULT_OFFLINE_SESSION_IDLE_TIMEOUT);
            // KEYCLOAK-7688 Offline Session Max for Offline Token
            realmModel.setOfflineSessionMaxLifespanEnabled(false);
            realmModel.setOfflineSessionMaxLifespan(Constants.DEFAULT_OFFLINE_SESSION_MAX_LIFESPAN);
            realmModel.setAccessCodeLifespan(60);
            realmModel.setAccessCodeLifespanUserAction(300);
            realmModel.setAccessCodeLifespanLogin(1800);
            realmModel.setSslRequired(SslRequired.EXTERNAL);
            realmModel.setRegistrationAllowed(false);
            realmModel.setRegistrationEmailAsUsername(false);
            LOGGER.info("new Realm created: " + realmModel.getName());
        } else {
            LOGGER.info("Realm already exists: " + realmModel.getName());
        }
        createClients(this.session, realmModel, clients);
        return Response.ok().build();
    }

    private void createClients(KeycloakSession session, RealmModel realmModel, int clients) {
        for (int i = 0; i < clients; i++) {
            executor.execute(new ClientExecutor(session.getKeycloakSessionFactory(), realmModel.getName(), i));
        }
    }

}
