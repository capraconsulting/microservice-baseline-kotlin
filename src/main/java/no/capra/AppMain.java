package no.capra;

import no.capra.config.BasicAuthSecurityHandler;
import no.capra.config.JerseyConfig;
import no.capra.config.JsonJettyErrorHandler;
import no.capra.health.HealthEndpoint;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppMain {
    private static final Logger log = LoggerFactory.getLogger(AppMain.class);

    static final String CONTEXT_PATH = System.getProperty("server.context.path", "/");
    private final Integer port;
    private Server server;

    AppMain(Integer port) {
        this.port = port;
    }

    public static void main(String[] args) throws Exception {
        Integer port = Integer.parseInt(System.getProperty("server.port", "8080"));
        new AppMain(port).start();
        log.info("Server stopped");
    }

    void start() throws InterruptedException {
        log.debug("Starting server at port {}", port);
        server = new Server(port);
        server.setHandler(getServletContextHandler());
        server.setStopAtShutdown(true);

        try {
            server.start();
        } catch (Exception e) {
            log.error("Error during Jetty startup. Exiting", e);
        }
        String healthEndpoint = "http://localhost:" + getPort() + HealthEndpoint.HEALTH_PATH;
        log.info("Server started at port {}. Health endpoint at: {}", port, healthEndpoint);
        server.join();
    }

    private ServletContextHandler getServletContextHandler() {
        ServletContextHandler contextHandler = new ServletContextHandler();
        contextHandler.setContextPath(CONTEXT_PATH);

        // Basic Authentication
        SecurityHandler basicAuthSecurityHandler = BasicAuthSecurityHandler
                .getBasicAuthSecurityHandler("username", "password", "realm");
        contextHandler.setSecurityHandler(basicAuthSecurityHandler);

        // Add Jersey servlet to the Jetty context
        ServletHolder jerseyServlet = new ServletHolder(new ServletContainer(new JerseyConfig()));
        contextHandler.addServlet(jerseyServlet, "/*");

        // Error responses as application/json with proper charset
        contextHandler.setErrorHandler(new JsonJettyErrorHandler());

        return contextHandler;
    }

    //used by TestServer
    void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            log.warn("Error while stopping Jetty server", e);
        }
    }

    Integer getPort() {
        return port;
    }

    boolean isStarted() {
        return server != null && server.isStarted();
    }


}
